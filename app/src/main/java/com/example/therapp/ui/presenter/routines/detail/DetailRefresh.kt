package com.example.therapp.ui.presenter.routines.detail

import com.example.therapp.common.api.ErrorMessages
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Las consultas del estado de la rutina van de a una. Antes, deslizar mientras
 * corría el sondeo de 10 s lanzaba una segunda consulta, y la respuesta más
 * vieja podía llegar después y pisar la nueva.
 */
class SerialRefresh {
    private val mutex = Mutex()

    /** La del usuario (deslizar, volver a la pantalla): espera a la que esté en curso y después consulta. */
    suspend fun <T> run(block: suspend () -> T): T = mutex.withLock { block() }

    /** La del sondeo: si ya hay una en curso, no hace nada (null). */
    suspend fun <T> runIfIdle(block: suspend () -> T): T? {
        if (!mutex.tryLock()) return null
        try {
            return block()
        } finally {
            mutex.unlock()
        }
    }
}

/** Fallos pasajeros seguidos de las consultas automáticas antes de avisar (unos 30 s de sondeo). */
const val BACKGROUND_FAILURES_BEFORE_NOTICE = 3

/**
 * Si un fallo al actualizar se muestra. Lo pidió el usuario (deslizar): sí.
 * Automático: solo si no es pasajero o si ya se repitió; mientras tanto la
 * pantalla sigue con lo último que tenía. En M5, Render respondió 502 durante
 * unos 20 s mientras el ejercicio se procesaba.
 */
fun showRefreshFailure(code: Int, background: Boolean, consecutiveFailures: Int): Boolean =
    !background || !ErrorMessages.isTransient(code) || consecutiveFailures >= BACKGROUND_FAILURES_BEFORE_NOTICE
