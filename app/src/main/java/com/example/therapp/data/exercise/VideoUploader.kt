package com.example.therapp.data.exercise

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.BufferedSink
import okio.buffer
import okio.source
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Qualifier
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Video que se va a subir: tamaño, tipo y cómo leerlo. */
interface VideoSource {
    val sizeBytes: Long
    val contentType: String

    /** Solo para la extensión que pide el backend; no lleva datos del paciente. */
    val fileName: String
    fun open(): InputStream
}

/** Cliente HTTP de la subida a Supabase (ver ApiModule). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class UploadClient

/**
 * PUT del video a la URL firmada de Supabase, con progreso.
 *
 * Va con un cliente propio, sin el interceptor de la app ni el log: el token
 * de la plataforma no debe llegar a Supabase, y la URL firmada es en sí una
 * credencial que no puede quedar en el log. Como en el panel, el PUT lleva
 * solo el Content-Type que validó el backend.
 */
class VideoUploader @Inject constructor(
    @UploadClient private val client: OkHttpClient
) {
    /** Devuelve el código HTTP; lanza IOException si falla la red. */
    suspend fun put(url: String, video: VideoSource, onProgress: (Float) -> Unit): Int {
        val request = Request.Builder()
            .url(url)
            .put(ProgressRequestBody(video, onProgress))
            .build()
        val call = client.newCall(request)
        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use { continuation.resume(it.code) }
                }
            })
        }
    }
}

private class ProgressRequestBody(
    private val video: VideoSource,
    private val onProgress: (Float) -> Unit
) : RequestBody() {
    override fun contentType(): MediaType = video.contentType.toMediaType()

    override fun contentLength(): Long = video.sizeBytes

    override fun writeTo(sink: BufferedSink) {
        video.open().source().buffer().use { source ->
            var sent = 0L
            while (true) {
                val read = source.read(sink.buffer, CHUNK_BYTES)
                if (read == -1L) break
                sink.emit()
                sent += read
                onProgress((sent.toFloat() / video.sizeBytes).coerceAtMost(1f))
            }
        }
    }

    companion object {
        private const val CHUNK_BYTES = 64L * 1024
    }
}
