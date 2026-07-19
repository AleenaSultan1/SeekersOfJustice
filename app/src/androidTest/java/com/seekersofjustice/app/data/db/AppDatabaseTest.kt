package com.seekersofjustice.app.data.db

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var caseDao: CaseDao
    private lateinit var memoDao: MemoDao

    private fun sampleCase(fir: String = "123/2026") = CaseEntity(
        firNumber = fir,
        fullName = "Test Person",
        age = 30,
        sex = "FEMALE",
        relation = "WITNESS",
        createdAt = System.currentTimeMillis(),
    )

    private fun sampleMemo(caseId: Long) = MemoEntity(
        caseId = caseId,
        audioPath = "/data/audio/test.wav",
        durationMs = 12_000,
        language = "URDU",
        status = "PENDING",
        createdAt = System.currentTimeMillis(),
    )

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).build()
        caseDao = db.caseDao()
        memoDao = db.memoDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndObserveCase() = runTest {
        val id = caseDao.insert(sampleCase())
        val cases = caseDao.observeCases().first()
        assertEquals(1, cases.size)
        assertEquals(id, cases[0].id)
        assertEquals("123/2026", cases[0].firNumber)
    }

    @Test
    fun duplicateFirNumberAborts() = runTest {
        caseDao.insert(sampleCase())
        try {
            caseDao.insert(sampleCase())
            throw AssertionError("Expected SQLiteConstraintException for duplicate FIR")
        } catch (expected: SQLiteConstraintException) {
            // expected
        }
        assertEquals(1, caseDao.observeCases().first().size)
    }

    @Test
    fun caseWithMemosRelation() = runTest {
        val caseId = caseDao.insert(sampleCase())
        memoDao.insert(sampleMemo(caseId))
        memoDao.insert(sampleMemo(caseId))

        val rel = caseDao.observeCaseWithMemos(caseId).first()
        assertNotNull(rel)
        assertEquals(2, rel!!.memos.size)
        assertTrue(rel.memos.all { it.caseId == caseId })
    }

    @Test
    fun cascadeDeleteRemovesMemos() = runTest {
        val caseId = caseDao.insert(sampleCase())
        val memoId = memoDao.insert(sampleMemo(caseId))

        caseDao.delete(caseId)
        assertEquals(null, memoDao.getMemo(memoId))
    }

    @Test
    fun statusUpdateAndResultRoundTrip() = runTest {
        val caseId = caseDao.insert(sampleCase())
        val memoId = memoDao.insert(sampleMemo(caseId))

        memoDao.updateStatus(memoId, "PROCESSING")
        assertEquals("PROCESSING", memoDao.getMemo(memoId)!!.status)

        val keyPoints = listOf("پہلا نکتہ", "دوسرا نکتہ", "point three")
        memoDao.setResult(
            memoId = memoId,
            transcript = "یہ ایک آزمائشی بیان ہے۔",
            translationEn = "This is a test statement.",
            detectedLanguage = "ur",
            keyPoints = keyPoints,
            status = "DONE",
        )
        val memo = memoDao.observeMemo(memoId).first()!!
        assertEquals("DONE", memo.status)
        assertEquals(keyPoints, memo.keyPoints)
        assertEquals("This is a test statement.", memo.translationEn)
    }

    @Test
    fun resetStuckProcessingAndPendingIds() = runTest {
        val caseId = caseDao.insert(sampleCase())
        val m1 = memoDao.insert(sampleMemo(caseId))
        val m2 = memoDao.insert(sampleMemo(caseId))
        memoDao.updateStatus(m1, "PROCESSING")

        memoDao.resetStuckProcessing()
        val pending = memoDao.pendingMemoIds()
        assertEquals(setOf(m1, m2), pending.toSet())
    }
}
