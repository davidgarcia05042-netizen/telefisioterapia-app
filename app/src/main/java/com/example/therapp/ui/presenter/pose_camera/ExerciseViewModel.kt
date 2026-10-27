package com.example.therapp.ui.presenter.pose_camera

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.therapp.data.exercise.ExerciseVideoSender
import com.example.therapp.data.exercise.SendJob
import com.example.therapp.data.exercise.SendOutcome
import com.example.therapp.data.exercise.SendStep
import com.example.therapp.data.exercise.VideoSource
import com.example.therapp.ui.navigation.routes.HomeRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import java.io.InputStream
import javax.inject.Inject

sealed class ExercisePhase {
    /** Cámara con la guía en vivo. */
    object Camera : ExercisePhase()

    /** Video grabado o elegido, para revisarlo antes de enviarlo. */
    data class Review(val video: RecordedVideo) : ExercisePhase()

    data class Sending(val video: RecordedVideo, val step: SendStep, val progress: Float) : ExercisePhase()

    data class Failed(val video: RecordedVideo, val message: String, val retryable: Boolean) : ExercisePhase()

    object Sent : ExercisePhase()
}

@HiltViewModel
class ExerciseViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val sender: ExerciseVideoSender
) : ViewModel() {

    val target = savedStateHandle.toRoute<HomeRoutes.ExerciseCamera>()

    var phase by mutableStateOf<ExercisePhase>(ExercisePhase.Camera)
        private set

    // Lo ya logrado del envío en curso (sesión, video subido), para reintentar.
    private var job: SendJob? = null

    init {
        // Grabaciones que quedaron si la app se cerró antes de enviar.
        RecordedVideos.clearStale(context, keep = null)
    }

    fun review(video: RecordedVideo) {
        job = null
        phase = ExercisePhase.Review(video)
    }

    /** "Repetir" o "Volver a grabar": se descarta la grabación y vuelve la cámara. */
    fun retake() {
        currentVideo()?.discard()
        job = null
        phase = ExercisePhase.Camera
    }

    /** "Enviar" y "Reintentar". */
    fun send() {
        val video = currentVideo() ?: return
        if (phase is ExercisePhase.Sending) return
        val current = job ?: SendJob(target.scheduledTrainingId, target.exerciseId, UriVideoSource(context, video))
            .also { job = it }
        phase = ExercisePhase.Sending(video, SendStep.STARTING, 0f)
        viewModelScope.launch {
            var shown = -1
            val outcome = sender.send(current) { step, progress ->
                // Sin redibujar por cada bloque de 64 KB: basta cada 1 %.
                val percent = (progress * 100).toInt()
                if (step != SendStep.UPLOADING || percent != shown) {
                    shown = percent
                    phase = ExercisePhase.Sending(video, step, progress)
                }
            }
            phase = when (outcome) {
                is SendOutcome.Sent -> {
                    // Ya está en Supabase: la copia del teléfono sobra.
                    video.discard()
                    job = null
                    ExercisePhase.Sent
                }
                is SendOutcome.Failed -> ExercisePhase.Failed(video, outcome.message, outcome.retryable)
            }
        }
    }

    private fun currentVideo(): RecordedVideo? = when (val current = phase) {
        is ExercisePhase.Review -> current.video
        is ExercisePhase.Sending -> current.video
        is ExercisePhase.Failed -> current.video
        else -> null
    }

    override fun onCleared() {
        super.onCleared()
        // Al salir sin enviar, la grabación no se queda en el teléfono.
        currentVideo()?.discard()
    }
}

/** El video grabado (archivo privado) o el elegido de la galería (content://). */
private class UriVideoSource(
    private val context: Context,
    private val video: RecordedVideo
) : VideoSource {
    override val sizeBytes = video.sizeBytes
    override val contentType = video.contentType

    // Solo la extensión le sirve al backend.
    override val fileName = "video" + when (video.contentType) {
        "video/quicktime" -> ".mov"
        "video/webm" -> ".webm"
        "video/x-matroska" -> ".mkv"
        "video/x-msvideo" -> ".avi"
        else -> ".mp4"
    }

    override fun open(): InputStream =
        context.contentResolver.openInputStream(video.uri)
            ?: throw java.io.IOException("No se pudo abrir el video.")
}
