package com.example.therapp.data.exercise

import com.example.therapp.data.exercise.remote.ExerciseResultReq
import com.example.therapp.data.exercise.remote.IdRes
import com.example.therapp.data.exercise.remote.SendApi
import com.example.therapp.data.exercise.remote.StartTrainingReq
import com.example.therapp.data.exercise.remote.UploadUrlReq
import com.example.therapp.data.exercise.remote.UploadUrlRes
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayInputStream
import java.net.ConnectException
import java.net.SocketTimeoutException

/**
 * El backend y Supabase se simulan con MockWebServer (el mismo servidor hace
 * de los dos): nunca se llama a los reales.
 */
class ExerciseVideoSenderTest {

    private class BytesVideo(private val bytes: ByteArray, size: Long = bytes.size.toLong()) : VideoSource {
        override val sizeBytes = size
        override val contentType = "video/mp4"
        override val fileName = "video.mp4"
        var opened = 0
        override fun open() = ByteArrayInputStream(bytes).also { opened++ }
    }

    private lateinit var server: MockWebServer
    private val requests = mutableListOf<RecordedRequest>()
    private val bodies = mutableListOf<String>()

    // Respuestas por ruta; cada lista se consume en orden y la última se repite.
    private val replies = mutableMapOf<String, MutableList<MockResponse>>()

    private fun reply(path: String, vararg responses: MockResponse) {
        replies[path] = responses.toMutableList()
    }

    private fun json(code: Int, body: String) = MockResponse().setResponseCode(code).setBody(body)

    @Before
    fun setUp() {
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                requests += request
                bodies += request.body.readUtf8()
                val key = request.path!!.substringBefore("?")
                val queue = replies[key] ?: return MockResponse().setResponseCode(500)
                return if (queue.size > 1) queue.removeAt(0) else queue.first()
            }
        }
        server.start()
        reply("/training", json(201, """{"id": 3}"""))
        reply("/storage/upload_url", json(200, signed()))
        reply("/storage/v1/object/upload/sign/videos/results/a.mp4", json(200, """{"Key": "videos/results/a.mp4"}"""))
        reply("/exercise_results", json(201, """{"id": 9}"""))
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun signed() = """
        {"upload_url": "${server.url("/storage/v1/object/upload/sign/videos/results/a.mp4")}?token=firma",
         "public_url": "https://proyecto.supabase.co/storage/v1/object/public/videos/results/a.mp4",
         "path": "results/a.mp4"}
    """.trimIndent()

    private fun sender(api: SendApi? = null) = ExerciseVideoSender(
        api ?: Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SendApi::class.java),
        VideoUploader(OkHttpClient())
    ).apply { retryDelaysMillis = listOf(0L, 0L) }

    private fun paths() = requests.map { "${it.method} ${it.path!!.substringBefore("?")}" }

    @Test
    fun sendsTheFourStepsInOrder() {
        val video = BytesVideo(ByteArray(300_000) { (it % 251).toByte() })
        val progress = mutableListOf<Float>()
        val outcome = runBlocking {
            sender().send(SendJob(7, 11, video)) { step, value -> if (step == SendStep.UPLOADING) progress += value }
        }

        assertEquals(SendOutcome.Sent(9), outcome)
        assertEquals(
            listOf(
                "POST /training",
                "POST /storage/upload_url",
                "PUT /storage/v1/object/upload/sign/videos/results/a.mp4",
                "POST /exercise_results"
            ),
            paths()
        )
        assertEquals("""{"schedule_training_id":7}""", bodies[0])
        assertEquals("""{"kind":"result","content_type":"video/mp4","filename":"video.mp4"}""", bodies[1])
        val put = requests[2]
        assertEquals("token=firma", put.requestUrl!!.query)
        assertEquals("video/mp4", put.getHeader("Content-Type"))
        assertEquals("300000", put.getHeader("Content-Length"))
        // El token de la plataforma no viaja a Supabase.
        assertNull(put.getHeader("Authorization"))
        assertEquals(
            """{"training_id":3,"exercise_id":11,"video":"https://proyecto.supabase.co/storage/v1/object/public/videos/results/a.mp4"}""",
            bodies[3]
        )
        assertEquals(1f, progress.last(), 0f)
    }

    @Test
    fun aVideoOver50MbIsNotSent() {
        val video = BytesVideo(ByteArray(0), size = 51L * 1024 * 1024)
        val outcome = runBlocking { sender().send(SendJob(7, 11, video)) { _, _ -> } } as SendOutcome.Failed
        assertEquals("El video pesa 51,0 MB y el máximo es 50 MB. Grábalo de nuevo, más corto.", outcome.message)
        assertFalse(outcome.retryable)
        assertEquals(0, server.requestCount)
        assertEquals(0, video.opened)
    }

    @Test
    fun aNetworkCutIsRetriedAutomatically() {
        reply(
            "/training",
            MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START),
            json(201, """{"id": 3}""")
        )
        val outcome = runBlocking { sender().send(SendJob(7, 11, BytesVideo(ByteArray(10)))) { _, _ -> } }
        assertEquals(SendOutcome.Sent(9), outcome)
        assertEquals(2, paths().count { it == "POST /training" })
    }

    @Test
    fun retryResumesFromTheFailedStep() {
        reply(
            "/storage/v1/object/upload/sign/videos/results/a.mp4",
            json(500, "{}"),
            json(200, "{}")
        )
        val job = SendJob(7, 11, BytesVideo(ByteArray(10)))
        val first = runBlocking { sender().send(job) { _, _ -> } } as SendOutcome.Failed
        assertTrue(first.retryable)
        assertEquals("No se pudo subir el video (código 500). Inténtalo de nuevo.", first.message)
        assertEquals(3, job.trainingId)
        assertNull(job.publicUrl)

        requests.clear()
        val second = runBlocking { sender().send(job) { _, _ -> } }
        assertEquals(SendOutcome.Sent(9), second)
        // Sin otra sesión, y con una URL firmada nueva.
        assertEquals(
            listOf(
                "POST /storage/upload_url",
                "PUT /storage/v1/object/upload/sign/videos/results/a.mp4",
                "POST /exercise_results"
            ),
            paths()
        )
    }

    @Test
    fun clearMessagesForEachRejection() {
        fun failWith(path: String, response: MockResponse): SendOutcome.Failed {
            setUp()
            reply(path, response)
            return runBlocking { sender().send(SendJob(7, 11, BytesVideo(ByteArray(10)))) { _, _ -> } }
                as SendOutcome.Failed
        }
        assertEquals(
            "Esta rutina ya no está disponible. Actualiza tus rutinas.",
            failWith("/training", json(404, """{"detail": "No encontrado."}""")).message
        )
        assertEquals(
            "El almacenamiento de videos no está disponible en el servidor. Avísale a tu terapeuta.",
            failWith("/storage/upload_url", json(503, """{"error": "falta SUPABASE_URL"}""")).message
        )
        assertTrue(failWith("/storage/upload_url", json(502, "{}")).retryable)
        assertEquals(
            "El almacenamiento rechazó el video por su tamaño (máximo 50 MB).",
            failWith("/storage/v1/object/upload/sign/videos/results/a.mp4", json(413, "{}")).message
        )
        assertEquals(
            "El servicio de medición no está disponible en el servidor. Avísale a tu terapeuta.",
            failWith("/exercise_results", json(503, "{}")).message
        )
        assertEquals(
            "El ejercicio no pertenece a la rutina de esta sesión.",
            failWith(
                "/exercise_results",
                json(400, """{"errors": [{"exercise_id": "El ejercicio no pertenece a la rutina de esta sesión."}]}""")
            ).message
        )
    }

    /** SendApi real salvo POST /exercise_results, que falla como se le indique. */
    private inner class FlakyRegister(private val failures: MutableList<Exception>) : SendApi {
        private val real = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SendApi::class.java)
        var registerCalls = 0
        override suspend fun startTraining(body: StartTrainingReq) = real.startTraining(body)
        override suspend fun uploadUrl(body: UploadUrlReq): Response<UploadUrlRes> = real.uploadUrl(body)
        override suspend fun createResult(body: ExerciseResultReq): Response<IdRes> {
            registerCalls++
            if (failures.isNotEmpty()) throw failures.removeAt(0)
            return real.createResult(body)
        }
    }

    @Test
    fun theFinalNoticeIsRetriedOnlyIfItNeverLeft() {
        val notSent = FlakyRegister(mutableListOf(ConnectException("sin red")))
        assertEquals(
            SendOutcome.Sent(9),
            runBlocking { sender(notSent).send(SendJob(7, 11, BytesVideo(ByteArray(10)))) { _, _ -> } }
        )
        assertEquals(2, notSent.registerCalls)

        // Se cortó esperando la respuesta: pudo quedar registrado; no se repite solo.
        val maybeSent = FlakyRegister(mutableListOf(SocketTimeoutException("lectura")))
        val outcome = runBlocking { sender(maybeSent).send(SendJob(7, 11, BytesVideo(ByteArray(10)))) { _, _ -> } }
        assertEquals(SendOutcome.Failed(ExerciseVideoSender.NO_NETWORK, retryable = true), outcome)
        assertEquals(1, maybeSent.registerCalls)
    }
}
