package com.example.therapp.ui.presenter.routines.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.therapp.common.api.ApiResponse
import com.example.therapp.common.api.ErrorMessages
import com.example.therapp.data.results.ResultsRepository
import com.example.therapp.data.routines.RoutineDetailCache
import com.example.therapp.domain.model.ExerciseOutcome
import com.example.therapp.domain.model.ExpectedRange
import com.example.therapp.domain.model.ResultStatus
import com.example.therapp.domain.model.ScheduledTraining
import com.example.therapp.domain.model.latestByExercise
import com.example.therapp.domain.model.scheduledStatusHasSession
import com.example.therapp.domain.repository.RoutinesRepository
import com.example.therapp.ui.navigation.routes.HomeRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Rango esperado de un ejercicio mientras se consulta /scheduled_training/diffs. */
sealed class RangeState {
    object Loading : RangeState()
    data class Loaded(val range: ExpectedRange?) : RangeState()
    data class Failed(val message: String) : RangeState()
}

/** "Terminar rutina": confirmación, envío y su resultado. */
sealed class FinishState {
    object Idle : FinishState()
    object Confirming : FinishState()
    object Ending : FinishState()
    data class Failed(val message: String) : FinishState()
}

@HiltViewModel
class RoutineDetailViewModel @Inject constructor(
    private val repository: RoutinesRepository,
    private val results: ResultsRepository,
    private val cache: RoutineDetailCache,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val route = savedStateHandle.toRoute<HomeRoutes.RoutineDetailScreen>()
    val scheduledTrainingId = route.scheduledTrainingId

    val training: StateFlow<ScheduledTraining?> = repository.getScheduledTrainingById(scheduledTrainingId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    /** Por id de ejercicio. */
    val ranges = mutableStateMapOf<Int, RangeState>()

    /** Último envío de cada ejercicio, por id; sin entrada = pendiente. */
    var outcomes by mutableStateOf<Map<Int, ExerciseOutcome>>(emptyMap())
        private set

    /**
     * Hay un estado guardado o ya respondió el backend. Mientras no, la
     * pantalla no muestra el estado ni "Hacer ejercicio": sin nada a la vista,
     * todos se verían "Pendiente" aunque uno se esté procesando.
     */
    var outcomesKnown by mutableStateOf(false)
        private set

    var isRefreshing by mutableStateOf(false)
        private set

    var refreshError by mutableStateOf<String?>(null)
        private set

    var finishState by mutableStateOf<FinishState>(FinishState.Idle)
        private set

    /** Terminada desde la app aunque la copia local aún diga "Iniciado". */
    var finishedHere by mutableStateOf(false)
        private set

    // Se conoce cuando la rutina ya tiene sesión (al enviar el primer video).
    private var trainingId: Int? = null
    private var refreshJob: Job? = null
    private var pollJob: Job? = null
    private val serial = SerialRefresh()
    private var consecutiveFailures = 0

    /**
     * Primero lo guardado (rangos y último estado), sin pantalla de carga;
     * después, lo del backend en segundo plano. Las consultas del estado
     * esperan a que termine, para que lo guardado no pise lo nuevo.
     */
    private val restore: Job = viewModelScope.launch {
        val cachedRanges = cache.ranges(scheduledTrainingId)
        cachedRanges.forEach { (exerciseId, range) -> ranges.putIfAbsent(exerciseId, RangeState.Loaded(range)) }
        cache.progress(scheduledTrainingId)?.let { saved ->
            trainingId = saved.trainingId
            outcomes = latestByExercise(saved.outcomes)
            outcomesKnown = true
        }
    }

    init {
        viewModelScope.launch {
            restore.join()
            val loaded = training.filterNotNull().first()
            loaded.routine.exercises.forEach { loadRange(it.id, quiet = it.id in cachedRangeIds()) }
        }
    }

    private fun cachedRangeIds() = ranges.filterValues { it is RangeState.Loaded }.keys

    /**
     * [quiet]: ya hay un rango guardado a la vista; no se muestra "Cargando" y,
     * si falla, queda el guardado.
     */
    fun loadRange(exerciseId: Int, quiet: Boolean = false) {
        if (!quiet) ranges[exerciseId] = RangeState.Loading
        viewModelScope.launch {
            when (val result = repository.getExpectedRange(scheduledTrainingId, exerciseId)) {
                is ApiResponse.Success -> {
                    ranges[exerciseId] = RangeState.Loaded(result.data)
                    cache.saveRange(scheduledTrainingId, exerciseId, result.data)
                }
                is ApiResponse.Failure -> if (!quiet) ranges[exerciseId] = RangeState.Failed(result.errorMessage)
                is ApiResponse.Error -> if (!quiet) ranges[exerciseId] = RangeState.Failed(result.errorMessage)
                ApiResponse.Loading -> Unit
            }
        }
    }

    /**
     * Al abrir la pantalla y al volver a ella (por ejemplo, después de enviar
     * un video): estado actualizado en segundo plano y, mientras algo se
     * procesa, consulta cada 10 s. En segundo plano no consulta.
     */
    fun onResume() {
        update(userInitiated = false)
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (true) {
                delay(POLL_MILLIS)
                if (outcomes.values.any { it.status == ResultStatus.PROCESSING }) {
                    loadOutcomes(background = true)
                }
            }
        }
    }

    fun onPause() {
        pollJob?.cancel()
    }

    /** Al deslizar: la rutina (su estado), los rangos que fallaron y los resultados. */
    fun refresh() = update(userInitiated = true)

    /**
     * Solo lo pedido por el usuario muestra el indicador. Si desliza mientras
     * corre la actualización de abrir la pantalla, se suma a esa: el indicador
     * aparece y los fallos se le muestran (isRefreshing).
     */
    private fun update(userInitiated: Boolean) {
        if (userInitiated) isRefreshing = true
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            val routines = repository.refreshScheduledTraining()
            ranges.filterValues { it is RangeState.Failed }.keys.forEach { loadRange(it) }
            loadOutcomes(background = !isRefreshing)
            // Con la copia local se sigue; el aviso dice que puede estar vieja.
            if (routines is ApiResponse.Failure && refreshError == null &&
                showRefreshFailure(routines.code, !isRefreshing, consecutiveFailures)
            ) {
                refreshError = routines.errorMessage
            }
            isRefreshing = false
        }
    }

    /** De a una (SerialRefresh): el sondeo no se cruza con el deslizar. */
    private suspend fun loadOutcomes(background: Boolean) {
        restore.join()
        if (background) serial.runIfIdle { fetchOutcomes(background) } else serial.run { fetchOutcomes(background) }
    }

    private suspend fun fetchOutcomes(background: Boolean) {
        val id = trainingId ?: findTraining(background) ?: return
        when (val result = results.getOutcomes(id)) {
            is ApiResponse.Success -> {
                outcomes = latestByExercise(result.data)
                outcomesKnown = true
                refreshSucceeded()
                cache.saveProgress(scheduledTrainingId, id, outcomes.values)
            }
            is ApiResponse.Failure -> refreshFailed(result.errorMessage, result.code, background)
            is ApiResponse.Error -> refreshFailed(result.errorMessage, ErrorMessages.CODE_UNEXPECTED, background)
            ApiResponse.Loading -> Unit
        }
    }

    private suspend fun findTraining(background: Boolean): Int? {
        val current = repository.getScheduledTrainingById(scheduledTrainingId).first() ?: return null
        // Sin iniciar: todavía no hay sesión ni resultados, y no se crea una solo por mirar.
        if (!scheduledStatusHasSession(current.status)) {
            outcomes = emptyMap()
            outcomesKnown = true
            refreshSucceeded()
            return null
        }
        return when (val result = results.findTraining(scheduledTrainingId)) {
            is ApiResponse.Success -> result.data.also { trainingId = it }
            is ApiResponse.Failure -> null.also { refreshFailed(result.errorMessage, result.code, background) }
            is ApiResponse.Error ->
                null.also { refreshFailed(result.errorMessage, ErrorMessages.CODE_UNEXPECTED, background) }
            ApiResponse.Loading -> null
        }
    }

    private fun refreshSucceeded() {
        consecutiveFailures = 0
        refreshError = null
    }

    // Un fallo pasajero del sondeo no se muestra: queda lo último que se vio.
    private fun refreshFailed(message: String, code: Int, background: Boolean) {
        consecutiveFailures++
        if (showRefreshFailure(code, background, consecutiveFailures)) refreshError = message
    }

    fun askFinish() {
        finishState = FinishState.Confirming
    }

    fun dismissFinish() {
        if (finishState !is FinishState.Ending) finishState = FinishState.Idle
    }

    fun finish() {
        val id = trainingId ?: return
        if (finishState is FinishState.Ending) return
        finishState = FinishState.Ending
        viewModelScope.launch {
            finishState = when (val result = results.endTraining(id)) {
                is ApiResponse.Success -> {
                    finishedHere = true
                    // La lista de rutinas también debe decir "Finalizado".
                    repository.refreshScheduledTraining()
                    FinishState.Idle
                }
                is ApiResponse.Failure -> FinishState.Failed(result.errorMessage)
                is ApiResponse.Error -> FinishState.Failed(result.errorMessage)
                ApiResponse.Loading -> FinishState.Ending
            }
        }
    }

    companion object {
        const val POLL_MILLIS = 10_000L
    }
}
