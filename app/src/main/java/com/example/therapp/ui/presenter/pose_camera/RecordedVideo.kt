package com.example.therapp.ui.presenter.pose_camera

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

/**
 * Video listo para revisar y enviar: el que se acaba de grabar (un archivo
 * privado de la app, [file]) o uno elegido de la galería ([file] nulo: no es
 * nuestro y no se borra).
 */
data class RecordedVideo(
    val uri: Uri,
    val file: File?,
    val sizeBytes: Long,
    val contentType: String,
    val durationMillis: Long?,
) {
    /** Borra el archivo si lo grabó la app (al repetir, al salir o tras enviarlo). */
    fun discard() {
        file?.delete()
    }
}

object RecordedVideos {
    /** Igual al tope del servicio, del bucket y del backend (MAX_VIDEO_BYTES). */
    const val MAX_BYTES = 50L * 1024 * 1024
    const val MAX_DURATION_MILLIS = 60_000L

    /**
     * Grabaciones del modo ejercicio: en la caché privada de la app, no en la
     * galería (el video del paciente no queda en el teléfono a la vista de
     * otras apps). Se borran al enviar o al salir.
     */
    fun directory(context: Context): File =
        File(context.cacheDir, "exercise_videos").apply { mkdirs() }

    fun newFile(context: Context): File =
        File(directory(context), "video_${System.currentTimeMillis()}.mp4")

    fun fromFile(context: Context, file: File): RecordedVideo {
        val uri = Uri.fromFile(file)
        return RecordedVideo(uri, file, file.length(), "video/mp4", duration(context, uri))
    }

    /** Video elegido de la galería; null si no se puede leer su tamaño. */
    fun fromContentUri(context: Context, uri: Uri): RecordedVideo? {
        val size = context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0) else null }
            ?: return null
        val type = context.contentResolver.getType(uri) ?: "video/mp4"
        return RecordedVideo(uri, null, size, type, duration(context, uri))
    }

    private fun duration(context: Context, uri: Uri): Long? = try {
        MediaMetadataRetriever().run {
            try {
                setDataSource(context, uri)
                extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            } finally {
                release()
            }
        }
    } catch (e: Exception) {
        null
    }

    /** Grabaciones que quedaron de una sesión anterior (la app se cerró antes de enviar). */
    fun clearStale(context: Context, keep: File?) {
        directory(context).listFiles()?.filter { it != keep }?.forEach { it.delete() }
    }

    /** "8,4 MB" */
    fun formatSize(bytes: Long): String =
        String.format(java.util.Locale("es", "CO"), "%.1f MB", bytes / (1024.0 * 1024.0))

    /** "0:12" */
    fun formatDuration(millis: Long): String {
        val seconds = millis / 1000
        return "%d:%02d".format(seconds / 60, seconds % 60)
    }
}
