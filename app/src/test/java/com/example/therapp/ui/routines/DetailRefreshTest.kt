package com.example.therapp.ui.routines

import com.example.therapp.common.api.ErrorMessages
import com.example.therapp.ui.presenter.routines.detail.BACKGROUND_FAILURES_BEFORE_NOTICE
import com.example.therapp.ui.presenter.routines.detail.SerialRefresh
import com.example.therapp.ui.presenter.routines.detail.showRefreshFailure
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Deslizar y el sondeo de 10 s no se pisan; un fallo pasajero automático no se muestra. */
class DetailRefreshTest {

    @Test
    fun pollSkipsWhileTheUserRefreshIsRunning() = runBlocking {
        val serial = SerialRefresh()
        val release = CompletableDeferred<Unit>()
        val calls = mutableListOf<String>()

        val swipe = async { serial.run { calls += "deslizar"; release.await() } }
        yield()
        val poll = serial.runIfIdle { calls += "sondeo" }

        assertNull(poll)
        release.complete(Unit)
        swipe.await()
        assertEquals(listOf("deslizar"), calls)
    }

    @Test
    fun userRefreshWaitsForThePollAndThenAsksAgain() = runBlocking {
        val serial = SerialRefresh()
        val release = CompletableDeferred<Unit>()
        val events = mutableListOf<String>()

        val poll = async {
            serial.runIfIdle {
                events += "sondeo empieza"
                release.await()
                events += "sondeo termina"
            }
        }
        yield()
        val swipe = async { serial.run { events += "deslizar" } }
        yield()
        assertEquals(listOf("sondeo empieza"), events)

        release.complete(Unit)
        poll.await()
        swipe.await()
        // La respuesta del deslizar llega después: es la que queda en pantalla.
        assertEquals(listOf("sondeo empieza", "sondeo termina", "deslizar"), events)
    }

    @Test
    fun userRefreshAlwaysShowsItsFailure() {
        assertTrue(showRefreshFailure(502, background = false, consecutiveFailures = 1))
        assertTrue(showRefreshFailure(ErrorMessages.CODE_NO_CONNECTION, background = false, consecutiveFailures = 1))
    }

    @Test
    fun transientBackgroundFailureWaitsUntilItRepeats() {
        for (failures in 1 until BACKGROUND_FAILURES_BEFORE_NOTICE) {
            assertFalse(showRefreshFailure(502, background = true, consecutiveFailures = failures))
            assertFalse(showRefreshFailure(ErrorMessages.CODE_TIMEOUT, background = true, consecutiveFailures = failures))
        }
        assertTrue(showRefreshFailure(502, background = true, consecutiveFailures = BACKGROUND_FAILURES_BEFORE_NOTICE))
    }

    @Test
    fun permanentBackgroundFailureShowsAtOnce() {
        assertTrue(showRefreshFailure(404, background = true, consecutiveFailures = 1))
    }
}
