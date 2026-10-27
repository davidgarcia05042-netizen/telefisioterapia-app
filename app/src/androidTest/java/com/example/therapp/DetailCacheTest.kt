package com.example.therapp

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.therapp.data.local.AppDatabase
import com.example.therapp.data.routines.CachedProgress
import com.example.therapp.data.routines.RoutineDetailCache
import com.example.therapp.data.routines.local.ScheduledTrainingEntity
import com.example.therapp.domain.model.ExerciseOutcome
import com.example.therapp.domain.model.ExpectedRange
import com.example.therapp.domain.model.JointRange
import com.example.therapp.domain.model.MeasuredJoint
import com.example.therapp.domain.model.ResultStatus
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** La copia del detalle de la rutina en Room (base en memoria, en el emulador). */
@RunWith(AndroidJUnit4::class)
class DetailCacheTest {

    private lateinit var db: AppDatabase
    private lateinit var cache: RoutineDetailCache

    private val range = ExpectedRange("Media", listOf(JointRange("Rodilla izquierda", 37.0, 120.0, 25)))
    private val processed = ExerciseOutcome(6, 11, ResultStatus.PROCESSED, listOf(MeasuredJoint(25, "Rodilla izquierda", 34.2, 118.5)))

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        cache = RoutineDetailCache(db.detailCacheDao)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun rangesAndProgressPerScheduledTraining() = runBlocking {
        cache.saveRange(3, 11, range)
        cache.saveRange(3, 12, null)
        cache.saveRange(4, 11, ExpectedRange("Alta", emptyList()))
        cache.saveProgress(3, 5, listOf(processed))

        assertEquals(mapOf(11 to range, 12 to null), cache.ranges(3))
        assertEquals(CachedProgress(5, listOf(processed)), cache.progress(3))
        assertNull(cache.progress(4))
    }

    @Test
    fun theLatestSaveWins() = runBlocking {
        cache.saveProgress(3, 5, listOf(ExerciseOutcome(6, 11, ResultStatus.PROCESSING, emptyList())))
        cache.saveProgress(3, 5, listOf(processed))
        assertEquals(listOf(processed), cache.progress(3)?.outcomes)
    }

    @Test
    fun refreshingTheRoutineListKeepsTheDetailCache() = runBlocking {
        cache.saveRange(3, 11, range)
        cache.saveProgress(3, 5, listOf(processed))

        db.routineDao.updateAllData(emptyList<ScheduledTrainingEntity>(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList())

        assertEquals(mapOf(11 to range), cache.ranges(3))
        assertEquals(5, cache.progress(3)?.trainingId)
    }

    @Test
    fun signingOutClearsIt() = runBlocking {
        cache.saveRange(3, 11, range)
        cache.saveProgress(3, 5, listOf(processed))

        db.detailCacheDao.clearAll()

        assertTrue(cache.ranges(3).isEmpty())
        assertNull(cache.progress(3))
    }
}
