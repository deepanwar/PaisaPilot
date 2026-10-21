package com.deepak.paisapilot.core.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.deepak.paisapilot.core.data.repo.PersonalExpenseRepository
import com.deepak.paisapilot.core.data.repo.RoomPersonalExpenseRepository
import com.deepak.paisapilot.core.data.repo.SaveResult
import com.deepak.paisapilot.core.data.repo.UpdateResult
import com.deepak.paisapilot.domain.money.MoneyLimits
import com.deepak.paisapilot.domain.money.Paise
import com.deepak.paisapilot.domain.personal.Category
import com.deepak.paisapilot.domain.personal.PersonalExpense
import com.deepak.paisapilot.domain.personal.PersonalValidation
import com.deepak.paisapilot.domain.time.monthContaining
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

private class MutableClock(var millis: Long) : Clock() {
    override fun getZone() = ZoneOffset.UTC

    override fun withZone(zone: java.time.ZoneId?): Clock = this

    override fun instant(): Instant = Instant.ofEpochMilli(millis)
}

@RunWith(AndroidJUnit4::class)
class PersonalExpenseRepositoryTest {
    private lateinit var db: LedgerDatabase
    private lateinit var repo: PersonalExpenseRepository
    private val clock = MutableClock(1_000L)
    private val oct = monthContaining(LocalDate.of(2026, 10, 20))

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LedgerDatabase::class.java,
        ).build()
        repo = RoomPersonalExpenseRepository(db.personalExpenseDao(), clock)
    }

    @After
    fun tearDown() = db.close()

    private fun expense(
        id: String = "p1",
        paise: Long = 12_550L,
        date: LocalDate = LocalDate.of(2026, 10, 20),
        title: String? = null,
    ) = PersonalExpense(id, Paise(paise), title, Category.Other, date, null, 0L, 0L)

    @Test
    fun createThenObserveReturnsRecord() = runBlocking {
        assertEquals(SaveResult.Created, repo.create(expense(title = "Tea")))
        val stored = repo.observe("p1").first()!!
        assertEquals(Paise(12_550L), stored.amount)
        assertEquals("Tea", stored.title)
        assertEquals(1_000L, stored.createdAt)
        assertEquals(1_000L, stored.updatedAt)
    }

    @Test
    fun duplicateCreateIsIdempotent() = runBlocking {
        assertEquals(SaveResult.Created, repo.create(expense(paise = 12_550L)))
        clock.millis = 2_000L
        assertEquals(SaveResult.AlreadySaved, repo.create(expense(paise = 99_900L)))
        assertEquals(1, repo.observeMonth(oct).first().size)
        val stored = repo.observe("p1").first()!!
        assertEquals(Paise(12_550L), stored.amount)
        assertEquals(1_000L, stored.createdAt)
    }

    @Test
    fun maxPaiseAndNullTitleRoundTrip() = runBlocking {
        repo.create(expense(paise = MoneyLimits.MAX_ENTRY_PAISE))
        val stored = repo.observe("p1").first()!!
        assertEquals(Paise(MoneyLimits.MAX_ENTRY_PAISE), stored.amount)
        assertNull(stored.title)
        assertNull(stored.note)
        assertEquals(LocalDate.of(2026, 10, 20), stored.date)
        assertEquals(Category.Other, stored.category)
    }

    @Test
    fun updateKeepsIdAndCreatedAtAndMovesTotal() = runBlocking {
        repo.create(expense(paise = 12_550L))
        clock.millis = 5_000L
        val result = repo.update(expense(paise = 22_550L).copy(createdAt = 777L))
        assertEquals(UpdateResult.Updated, result)
        val stored = repo.observe("p1").first()!!
        assertEquals(Paise(22_550L), stored.amount)
        assertEquals(1_000L, stored.createdAt)
        assertEquals(5_000L, stored.updatedAt)
        assertEquals(1, repo.observeMonth(oct).first().size)
        assertEquals(Paise(22_550L), repo.observeMonthTotal(oct).first())
    }

    @Test
    fun updateMissingIdIsNotFoundAndCreatesNothing() = runBlocking {
        assertEquals(UpdateResult.NotFound, repo.update(expense(id = "ghost")))
        assertNull(repo.observe("ghost").first())
    }

    @Test
    fun deleteAndRestore() = runBlocking {
        repo.create(expense(title = "Tea"))
        val snapshot = repo.observe("p1").first()!!
        assertTrue(repo.delete("p1"))
        assertFalse(repo.delete("p1"))
        assertNull(repo.observe("p1").first())
        assertEquals(Paise(0L), repo.observeMonthTotal(oct).first())
        assertEquals(SaveResult.Created, repo.restore(snapshot))
        assertEquals(snapshot, repo.observe("p1").first())
    }

    @Test
    fun monthBoundariesAndOrdering() = runBlocking {
        repo.create(expense(id = "a", paise = 100, date = LocalDate.of(2026, 9, 30)))
        repo.create(expense(id = "b", paise = 200, date = LocalDate.of(2026, 10, 1)))
        repo.create(expense(id = "c", paise = 300, date = LocalDate.of(2026, 10, 31)))
        repo.create(expense(id = "d", paise = 400, date = LocalDate.of(2026, 11, 1)))
        clock.millis = 2_000L
        repo.create(expense(id = "e", paise = 500, date = LocalDate.of(2026, 10, 31)))
        val rows = repo.observeMonth(oct).first()
        assertEquals(listOf("e", "c", "b"), rows.map { it.id })
        assertEquals(Paise(1_000L), repo.observeMonthTotal(oct).first())
    }

    @Test
    fun invalidInputWritesNothing() = runBlocking {
        val over = repo.create(expense(paise = MoneyLimits.MAX_ENTRY_PAISE + 1))
        assertEquals(SaveResult.Invalid(PersonalValidation.AmountOverLimit), over)
        val zero = repo.create(expense(id = "z", paise = 0))
        assertEquals(SaveResult.Invalid(PersonalValidation.AmountNotPositive), zero)
        assertTrue(repo.observeMonth(oct).first().isEmpty())
        repo.create(expense())
        val bad = repo.update(expense(paise = 0))
        assertEquals(UpdateResult.Invalid(PersonalValidation.AmountNotPositive), bad)
        assertEquals(Paise(12_550L), repo.observe("p1").first()!!.amount)
    }
}
