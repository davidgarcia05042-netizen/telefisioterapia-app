package com.example.therapp.ui.presenter.routines

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.therapp.common.api.ApiResponse
import com.example.therapp.domain.model.ScheduledTraining
import com.example.therapp.ui.components.RoutineItem
import java.time.LocalDate
import com.example.therapp.domain.model.scheduledDate

/**
 * @author Santiago Varela Daza
 * @email svarela03@uan.edu.co
 * @github https://github.com/sanvarela03
 * @since 9/4/2025
 * @version 1.0
 */

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun RoutinesScreen(
    viewModel: RoutinesViewModel = hiltViewModel(),
    onRoutineClick: (Int) -> Unit
) {
    val state by viewModel.scheduledTraining.collectAsState()
    val isRefreshing = viewModel.isRefreshing
    val refreshError = viewModel.refreshError

    // Variable para controlar el espacio entre los RoutineItems
    val itemSpacing = 1.dp

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refresh() },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Rutinas",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (refreshError != null && !isRefreshing) {
                Text(
                    text = "No se pudieron actualizar las rutinas. $refreshError",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // Primera carga: todavía no hay copia local.
            val firstLoad = isRefreshing &&
                (state as? ApiResponse.Success<List<ScheduledTraining>>)?.data?.isEmpty() == true

            if (firstLoad) {
                Text("Cargando entrenamientos...")
            } else when (state) {
                is ApiResponse.Loading -> {
                    if (!isRefreshing) {
                        Text("Cargando entrenamientos...")
                    }
                }

                is ApiResponse.Success -> {
                    val trainings = (state as ApiResponse.Success<List<ScheduledTraining>>).data

                    val today = LocalDate.now()
                    val tomorrow = today.plusDays(1)

                    // El mismo cálculo del día que el detalle (scheduledDate),
                    // para que "hoy" y "Disponible desde" no se contradigan.
                    val routinesOverdue = trainings.filter { scheduledDate(it.startTime)?.isBefore(today) == true }
                    val routinesToday = trainings.filter { scheduledDate(it.startTime)?.isEqual(today) == true }
                    val routinesTomorrow = trainings.filter { scheduledDate(it.startTime)?.isEqual(tomorrow) == true }
                    val futureRoutines = trainings.filter { scheduledDate(it.startTime)?.isAfter(tomorrow) == true }

                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        // Rutinas para hoy
                        item { RoutineSectionTitle("Rutinas para hoy") }
                        if (routinesToday.isEmpty()) {
                            item { EmptySectionMessage("No tienes rutinas programadas para hoy.") }
                        } else {
                            items(routinesToday) { training ->
                                RoutineItem(
                                    routine = training.routine,
                                    startTime = training.startTime,
                                    onItemClicked = { onRoutineClick(training.id) }
                                )
                                Spacer(modifier = Modifier.height(itemSpacing))
                            }
                        }

                        // Rutinas para mañana
                        item { RoutineSectionTitle("Rutinas para mañana") }
                        if (routinesTomorrow.isEmpty()) {
                            item { EmptySectionMessage("No tienes rutinas programadas para mañana.") }
                        } else {
                            items(routinesTomorrow) { training ->
                                RoutineItem(
                                    routine = training.routine,
                                    startTime = training.startTime,
                                    onItemClicked = { onRoutineClick(training.id) }
                                )
                                Spacer(modifier = Modifier.height(itemSpacing))
                            }
                        }

                        // Rutinas futuras
                        item { RoutineSectionTitle("Rutinas futuras") }
                        if (futureRoutines.isEmpty()) {
                            item { EmptySectionMessage("No hay rutinas futuras programadas.") }
                        } else {
                            items(futureRoutines) { training ->
                                RoutineItem(
                                    routine = training.routine,
                                    startTime = training.startTime,
                                    onItemClicked = { onRoutineClick(training.id) }
                                )
                                Spacer(modifier = Modifier.height(itemSpacing))
                            }
                        }

                        // Rutinas vencidas
                        item { RoutineSectionTitle("Rutinas vencidas") }
                        if (routinesOverdue.isEmpty()) {
                            item { EmptySectionMessage("No tienes rutinas vencidas.") }
                        } else {
                            items(routinesOverdue) { training ->
                                RoutineItem(
                                    routine = training.routine,
                                    startTime = training.startTime,
                                    onItemClicked = { onRoutineClick(training.id) }
                                )
                                Spacer(modifier = Modifier.height(itemSpacing))
                            }
                        }
                        
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }

                is ApiResponse.Error -> {
                    val error = (state as ApiResponse.Error).errorMessage
                    Text("Error: $error")
                }

                is ApiResponse.Failure -> {
                    Text("Error de conexión")
                }
            }
        }
    }
}

@Composable
fun RoutineSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(vertical = 12.dp),
        color = MaterialTheme.colorScheme.secondary
    )
}

@Composable
fun EmptySectionMessage(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        fontStyle = FontStyle.Italic,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp)
    )
}