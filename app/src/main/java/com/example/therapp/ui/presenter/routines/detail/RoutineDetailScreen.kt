package com.example.therapp.ui.presenter.routines.detail

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.therapp.domain.model.Exercise
import com.example.therapp.domain.model.ExerciseOutcome
import com.example.therapp.domain.model.availableFrom
import com.example.therapp.domain.model.availableFromNotice
import java.time.LocalDate
import com.example.therapp.domain.model.ExpectedRange
import com.example.therapp.domain.model.MeasuredJoint
import com.example.therapp.domain.model.RangeCheck
import com.example.therapp.domain.model.ResultStatus
import com.example.therapp.domain.model.canFinishRoutine
import com.example.therapp.domain.model.checkRange
import com.example.therapp.domain.model.erroredExercisesNotice
import com.example.therapp.domain.model.formatDegrees
import com.example.therapp.domain.model.formatRange
import com.example.therapp.domain.model.rangeFor
import com.example.therapp.domain.model.scheduledStatusIsFinished
import com.example.therapp.ui.components.VideoPlayer

private val Green = Color(0xFF2E7D32)
private val Blue = Color(0xFF1976D2)
private val Amber = Color(0xFFB26A00)

private const val MEASURE_ERROR_MESSAGE =
    "No se pudo medir este video. Grábalo de nuevo con todo el cuerpo a la vista y buena luz."

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineDetailScreen(
    viewModel: RoutineDetailViewModel = hiltViewModel(),
    onDoExercise: (scheduledTrainingId: Int, exercise: Exercise) -> Unit = { _, _ -> }
) {
    val training by viewModel.training.collectAsState()

    // Al abrir y al volver de la cámara: estado al día y consulta cada 10 s
    // mientras algo se procesa; en segundo plano, nada.
    LifecycleResumeEffect(viewModel) {
        viewModel.onResume()
        onPauseOrDispose { viewModel.onPause() }
    }

    val current = training
    if (current == null) {
        Text(text = "Cargando rutina...", modifier = Modifier.padding(16.dp))
        return
    }
    val routine = current.routine
    val exercises = routine.exercises.sortedBy { it.order }
    val outcomes = viewModel.outcomes
    val finished = viewModel.finishedHere || scheduledStatusIsFinished(current.status)
    // Solo hoy o vencidas: en una futura se ve el detalle, sin "Hacer ejercicio".
    val today = LocalDate.now()
    val opensOn = availableFrom(current.startTime, today)
    val notYetNotice = opensOn?.takeIf { !finished }?.let { availableFromNotice(it, today) }

    PullToRefreshBox(
        isRefreshing = viewModel.isRefreshing,
        onRefresh = { viewModel.refresh() },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                RoutineHeader(
                    name = routine.name,
                    description = routine.description,
                    refreshError = viewModel.refreshError.takeIf { !viewModel.isRefreshing }
                )
            }
            if (exercises.isEmpty()) {
                item {
                    Text(
                        text = "Esta rutina no tiene ejercicios.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(exercises, key = { it.id }) { exercise ->
                val outcome = outcomes[exercise.id]
                ExerciseItem(
                    exercise = exercise,
                    range = viewModel.ranges[exercise.id] ?: RangeState.Loading,
                    outcome = outcome,
                    statusKnown = viewModel.outcomesKnown,
                    canRecord = !finished && opensOn == null && viewModel.outcomesKnown && (outcome == null ||
                        outcome.status == ResultStatus.PENDING || outcome.status == ResultStatus.ERROR),
                    notYetNotice = notYetNotice,
                    onRetryRange = { viewModel.loadRange(exercise.id) },
                    onDoExercise = { onDoExercise(viewModel.scheduledTrainingId, exercise) }
                )
            }
            if (exercises.isNotEmpty()) {
                item {
                    FinishBlock(
                        finished = finished,
                        canFinish = canFinishRoutine(exercises.map { it.id }, outcomes),
                        state = viewModel.finishState,
                        onFinish = viewModel::askFinish
                    )
                }
            }
        }
    }

    val finishState = viewModel.finishState
    if (finishState is FinishState.Confirming || finishState is FinishState.Ending) {
        val ending = finishState is FinishState.Ending
        AlertDialog(
            onDismissRequest = viewModel::dismissFinish,
            title = { Text("¿Terminar la rutina?") },
            text = {
                val errored = exercises.filter { outcomes[it.id]?.status == ResultStatus.ERROR }.map { it.name }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    erroredExercisesNotice(errored)?.let { notice ->
                        Text(text = notice, color = MaterialTheme.colorScheme.error)
                    }
                    Text(
                        "Tu terapeuta verá la rutina como finalizada. Después no podrás enviar más videos de esta rutina."
                    )
                }
            },
            confirmButton = {
                TextButton(enabled = !ending, onClick = viewModel::finish) {
                    Text(if (ending) "Terminando..." else "Terminar")
                }
            },
            dismissButton = {
                TextButton(enabled = !ending, onClick = viewModel::dismissFinish) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun RoutineHeader(name: String, description: String, refreshError: String?) {
    Column {
        Text(
            text = name,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        if (description.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (refreshError != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No se pudo actualizar el estado. $refreshError",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Ejercicios",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Un ejercicio: su estado, el rango esperado (la dificultad asignada en esta
 * rutina programada), lo medido si ya se procesó, el video de referencia y
 * "Hacer ejercicio" mientras no haya un envío en curso o procesado.
 */
@Composable
fun ExerciseItem(
    exercise: Exercise,
    range: RangeState,
    outcome: ExerciseOutcome?,
    statusKnown: Boolean,
    canRecord: Boolean,
    notYetNotice: String?,
    onRetryRange: () -> Unit,
    onDoExercise: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${exercise.order}. ${exercise.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                // Solo la primera vez, sin nada guardado, hasta que responde el backend.
                if (statusKnown) {
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusChip(outcome?.status ?: ResultStatus.PENDING)
                }
            }
            if (exercise.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = exercise.description,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            ExpectedRangeBlock(range = range, onRetry = onRetryRange)

            when (outcome?.status) {
                ResultStatus.PROCESSING -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "El servicio está midiendo tu video. Esta pantalla se actualiza sola.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                ResultStatus.PROCESSED -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    MeasuredBlock(joints = outcome.joints, expected = (range as? RangeState.Loaded)?.range,
                        rangeKnown = range is RangeState.Loaded)
                }

                ResultStatus.ERROR -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = MEASURE_ERROR_MESSAGE,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                ResultStatus.PENDING, null -> Unit
            }

            if (!exercise.video.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Video de referencia",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                VideoPlayer(
                    videoUrl = exercise.video,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (canRecord) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onDoExercise
                ) {
                    Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Hacer ejercicio")
                }
            }

            // Rutina futura: el aviso va donde estaría el botón.
            if (notYetNotice != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = notYetNotice,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpectedRangeBlock(range: RangeState, onRetry: () -> Unit) {
    Text(
        text = "Rango esperado",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(4.dp))
    when (range) {
        RangeState.Loading -> Text(
            text = "Cargando...",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        is RangeState.Failed -> Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "No se pudo cargar. ${range.message}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onRetry) { Text("Reintentar") }
        }

        is RangeState.Loaded -> {
            val expected = range.range
            if (expected == null || expected.joints.isEmpty()) {
                Text(
                    text = "Tu terapeuta no asignó un rango para este ejercicio.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                if (expected.difficultyName.isNotBlank()) {
                    Text(
                        text = "Dificultad: ${expected.difficultyName}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                expected.joints.forEach { joint ->
                    Text(
                        text = "${joint.name}: ${formatRange(joint.minAngle, joint.maxAngle)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

/** Mínimo y máximo por articulación, con la marca de dentro/fuera del rango. */
@Composable
private fun MeasuredBlock(joints: List<MeasuredJoint>, expected: ExpectedRange?, rangeKnown: Boolean) {
    Text(
        text = "Tu resultado",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(4.dp))
    if (joints.isEmpty()) {
        Text(
            text = "El servicio no devolvió mediciones.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        joints.forEach { joint ->
            Column {
                Text(
                    text = joint.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Mínimo ${joint.minAngle?.let(::formatDegrees) ?: "sin dato"}" +
                        " · Máximo ${joint.maxAngle?.let(::formatDegrees) ?: "sin dato"}",
                    style = MaterialTheme.typography.bodyMedium
                )
                // Mientras el rango esperado carga (o si falló) no se marca nada.
                if (rangeKnown) {
                    val expectedJoint = expected?.rangeFor(joint)
                    RangeMark(check = checkRange(joint, expectedJoint))
                }
            }
        }
    }
}

@Composable
private fun RangeMark(check: RangeCheck) {
    val (icon, color, text) = when (check) {
        RangeCheck.Reached -> Triple(Icons.Default.CheckCircle, Green, "Alcanzó el rango")
        is RangeCheck.NotReached -> Triple(Icons.Default.Warning, Amber, "No alcanzó el rango")
        RangeCheck.NoRange -> Triple(null, MaterialTheme.colorScheme.onSurfaceVariant, "Sin rango esperado para comparar.")
        RangeCheck.Unmeasured -> Triple(null, MaterialTheme.colorScheme.onSurfaceVariant, "Sin medición.")
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = color,
            fontWeight = if (icon != null) FontWeight.SemiBold else FontWeight.Normal
        )
    }
    // Lo que faltó, primero el mínimo y después el máximo.
    if (check is RangeCheck.NotReached) {
        check.gaps.forEach { gap ->
            Text(
                text = gap,
                style = MaterialTheme.typography.bodySmall,
                color = Amber,
                modifier = Modifier.padding(start = 20.dp)
            )
        }
    }
}

@Composable
private fun FinishBlock(finished: Boolean, canFinish: Boolean, state: FinishState, onFinish: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (finished) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Green, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Rutina finalizada. Tu terapeuta ve tus resultados en el panel.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Green
                )
            }
            return@Column
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = canFinish && state !is FinishState.Ending,
            onClick = onFinish
        ) {
            Text("Terminar rutina")
        }
        if (!canFinish) {
            Text(
                text = "Podrás terminar la rutina cuando todos los ejercicios tengan un resultado.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (state is FinishState.Failed) {
            Text(
                text = "No se pudo terminar la rutina. ${state.message}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun StatusChip(status: ResultStatus) {
    val (icon, color) = when (status) {
        ResultStatus.PENDING -> Icons.Default.Schedule to Color.Gray
        ResultStatus.PROCESSING -> Icons.Default.Sync to Blue
        ResultStatus.PROCESSED -> Icons.Default.CheckCircle to Green
        ResultStatus.ERROR -> Icons.Default.Error to Color(0xFFC62828)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), CircleShape)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = status.label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}
