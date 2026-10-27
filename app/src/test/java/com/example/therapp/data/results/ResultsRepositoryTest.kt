package com.example.therapp.data.results

import com.example.therapp.common.api.ApiResponse
import com.example.therapp.common.api.ErrorMessages
import com.example.therapp.data.exercise.remote.SendApi
import com.example.therapp.data.results.remote.ResultsApi
import com.example.therapp.domain.model.ResultStatus
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/** El backend se simula con MockWebServer: nunca se llama al real. */
class ResultsRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: ResultsRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        repository = ResultsRepository(retrofit.create(ResultsApi::class.java), retrofit.create(SendApi::class.java))
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun json(code: Int, body: String) = MockResponse().setResponseCode(code).setBody(body)

    // Forma de ExerciseResultSerializer: ejercicio anidado, estado con su texto
    // y una entrada por articulación con su serie (que la app no lee).
    private val results = """
        [
          {"id": 4, "exercise": {"id": 11, "name": "Flexión de rodilla", "difficulties": []},
           "training_id": 5, "status": "Error al procesar video", "video": "https://x/a.mp4",
           "points": [], "start_time": null, "concept": null, "score": null},
          {"id": 6, "exercise": {"id": 11, "name": "Flexión de rodilla"}, "training_id": 5,
           "status": "Video procesado",
           "points": [{"point_tracked": {"skeleton_point": {"id": 25, "codename": "LEFT_KNEE",
                         "verbose": "Rodilla izquierda", "left_point": 23, "right_point": 27},
                       "skeleton_point_id": 25, "exercise_id": 11, "max_angle": 130.0, "min_angle": 20.0,
                       "left_point_id": 23, "right_point_id": 27},
                       "max_angle": 118.5, "min_angle": 34.2,
                       "angle_series": [{"t_ms": 0.0, "angle": 34.2}, {"t_ms": 33.3, "angle": null}],
                       "error": null}]},
          {"id": 7, "exercise": {"id": 12}, "training_id": 5, "status": "Video en procesamiento", "points": null},
          {"id": 8, "exercise": null, "status": "Video procesado"}
        ]
    """.trimIndent()

    @Test
    fun outcomesWithTheBackendShape() = runBlocking {
        server.enqueue(json(200, results))

        val outcomes = (repository.getOutcomes(5) as ApiResponse.Success).data

        assertEquals("/exercise_results?training_id=5", server.takeRequest().path)
        // El resultado sin ejercicio (borrado en el panel) se omite.
        assertEquals(listOf(4, 6, 7), outcomes.map { it.resultId })
        assertEquals(ResultStatus.ERROR, outcomes[0].status)
        val processed = outcomes[1]
        assertEquals(ResultStatus.PROCESSED, processed.status)
        assertEquals(11, processed.exerciseId)
        val joint = processed.joints.single()
        assertEquals(25, joint.skeletonPointId)
        assertEquals("Rodilla izquierda", joint.name)
        assertEquals(34.2, joint.minAngle!!, 0.0)
        assertEquals(118.5, joint.maxAngle!!, 0.0)
        assertEquals(ResultStatus.PROCESSING, outcomes[2].status)
        assertTrue(outcomes[2].joints.isEmpty())
    }

    @Test
    fun anotherPatientsSessionIsNotFound() = runBlocking {
        server.enqueue(json(404, "{}"))
        val failure = repository.getOutcomes(99) as ApiResponse.Failure
        assertEquals(404, failure.code)
        assertEquals(ResultsRepository.NOT_FOUND, failure.errorMessage)
    }

    @Test
    fun findsTheSessionOfAStartedRoutine() = runBlocking {
        server.enqueue(json(201, """{"id": 5}"""))
        assertEquals(5, (repository.findTraining(3) as ApiResponse.Success).data)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/training", request.path)
        assertEquals("""{"schedule_training_id":3}""", request.body.readUtf8())
    }

    @Test
    fun endTraining() = runBlocking {
        server.enqueue(json(200, "{}"))
        assertTrue(repository.endTraining(5) is ApiResponse.Success)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/training/end?training_id=5", request.path)
    }

    @Test
    fun endTrainingRejected() = runBlocking {
        server.enqueue(json(400, "{}"))
        val failure = repository.endTraining(5) as ApiResponse.Failure
        assertEquals(400, failure.code)
        assertEquals(ErrorMessages.UNEXPECTED, failure.errorMessage)
    }

    @Test
    fun renderGatewayErrorWhileTheBackendRestarts() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(502).setBody("<html><body>Bad Gateway</body></html>"))
        val failure = repository.getOutcomes(5) as ApiResponse.Failure
        assertEquals(502, failure.code)
        assertEquals(ErrorMessages.SERVER_UNAVAILABLE, failure.errorMessage)
    }

    @Test
    fun withoutNetwork() = runBlocking {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        val failure = repository.getOutcomes(5) as ApiResponse.Failure
        assertEquals(ErrorMessages.NO_CONNECTION, failure.errorMessage)
    }
}
