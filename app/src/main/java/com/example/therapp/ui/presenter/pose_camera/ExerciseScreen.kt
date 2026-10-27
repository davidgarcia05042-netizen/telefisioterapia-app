package com.example.therapp.ui.presenter.pose_camera

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.therapp.data.exercise.SendStep
import com.example.therapp.ui.components.VideoPlayer

/**
 * "Hacer ejercicio" desde el detalle de la rutina: la cámara sabe a qué
 * ejercicio pertenece el video (A4 en docs/DECISIONES_TECNICAS.md). Grabar → revisar con
 * "Repetir" o "Enviar".
 */
@Composable
fun ExerciseScreen(
    exerciseName: String,
    navController: NavController,
    viewModel: ExerciseViewModel = hiltViewModel()
) {
    when (val phase = viewModel.phase) {
        ExercisePhase.Camera -> CameraScreen(
            navController = navController,
            exerciseName = exerciseName,
            onVideoReady = viewModel::review
        )

        is ExercisePhase.Review -> {
            BackHandler { viewModel.retake() }
            ReviewPanel(
                exerciseName = exerciseName,
                video = phase.video,
                onRetake = viewModel::retake,
                onSend = { viewModel.send() }
            )
        }

        is ExercisePhase.Sending -> {
            // Salir cancelaría la subida a medias.
            BackHandler { }
            SendingPanel(exerciseName = exerciseName, phase = phase)
        }

        is ExercisePhase.Failed -> {
            BackHandler { viewModel.retake() }
            FailedPanel(
                exerciseName = exerciseName,
                phase = phase,
                onRetry = { viewModel.send() },
                onRetake = viewModel::retake
            )
        }

        ExercisePhase.Sent -> SentPanel(
            exerciseName = exerciseName,
            onBack = { navController.popBackStack() }
        )
    }
}

@Composable
private fun SendingPanel(exerciseName: String, phase: ExercisePhase.Sending) {
    StatusColumn(exerciseName) {
        Text(text = phase.step.label, style = MaterialTheme.typography.titleMedium)
        if (phase.step == SendStep.UPLOADING) {
            LinearProgressIndicator(
                progress = { phase.progress },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "${(phase.progress * 100).toInt()} % de ${RecordedVideos.formatSize(phase.video.sizeBytes)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        Text(
            text = "No cierres esta pantalla hasta que termine.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FailedPanel(
    exerciseName: String,
    phase: ExercisePhase.Failed,
    onRetry: () -> Unit,
    onRetake: () -> Unit
) {
    StatusColumn(exerciseName) {
        Text(
            text = "No se pudo enviar el video",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error
        )
        Text(text = phase.message, style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(modifier = Modifier.weight(1f), onClick = onRetake) {
                Text("Volver a grabar")
            }
            if (phase.retryable) {
                Button(modifier = Modifier.weight(1f), onClick = onRetry) {
                    Text("Reintentar")
                }
            }
        }
    }
}

@Composable
private fun SentPanel(exerciseName: String, onBack: () -> Unit) {
    StatusColumn(exerciseName) {
        Text(
            text = "Video enviado",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF2E7D32)
        )
        Text(
            text = "El servicio lo está midiendo. El resultado aparecerá en tu rutina y tu terapeuta lo verá en el panel.",
            style = MaterialTheme.typography.bodyMedium
        )
        Button(modifier = Modifier.fillMaxWidth(), onClick = onBack) {
            Text("Volver a la rutina")
        }
    }
}

@Composable
private fun StatusColumn(exerciseName: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = exerciseName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        content()
    }
}

@Composable
private fun ReviewPanel(
    exerciseName: String,
    video: RecordedVideo,
    onRetake: () -> Unit,
    onSend: () -> Unit
) {
    val tooBig = video.sizeBytes > RecordedVideos.MAX_BYTES
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = exerciseName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Revisa el video antes de enviarlo: que se vea todo el cuerpo y con buena luz.",
            style = MaterialTheme.typography.bodyMedium
        )
        VideoPlayer(
            videoUrl = video.uri.toString(),
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
        )
        Text(
            text = listOfNotNull(
                video.durationMillis?.let { RecordedVideos.formatDuration(it) },
                RecordedVideos.formatSize(video.sizeBytes)
            ).joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (tooBig) {
            // DM6: el servicio y el bucket aceptan hasta 50 MB; no se sube.
            Text(
                text = "El video pesa ${RecordedVideos.formatSize(video.sizeBytes)} y el máximo es 50 MB. " +
                    "Grábalo de nuevo, más corto.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(modifier = Modifier.weight(1f), onClick = onRetake) {
                Text("Repetir")
            }
            Button(modifier = Modifier.weight(1f), enabled = !tooBig, onClick = onSend) {
                Text("Enviar")
            }
        }
    }
}
