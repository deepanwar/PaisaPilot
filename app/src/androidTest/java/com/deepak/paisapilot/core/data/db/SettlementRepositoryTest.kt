package com.deepak.paisapilot.core.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.deepak.paisapilot.core.data.IdGenerator
import com.deepak.paisapilot.core.data.repo.GroupActionResult
import com.deepak.paisapilot.core.data.repo.GroupRepository
import com.deepak.paisapilot.core.data.repo.RoomGroupRepository
import com.deepak.paisapilot.core.data.repo.RoomSettlementRepository
import com.deepak.paisapilot.core.data.repo.RoomSharedExpenseRepository
import com.deepak.paisapilot.core.data.repo.SettlementRepository
import com.deepak.paisapilot.core.data.repo.SettlementWriteResult
import com.deepak.paisapilot.core.data.repo.SharedExpenseRepository
import com.deepak.paisapilot.domain.group.Settlement
import com.deepak.paisapilot.domain.group.SettlementValidation
import com.deepak.paisapilot.domain.group.Share
import com.deepak.paisapilot.domain.group.SharedExpense
import com.deepak.paisapilot.domain.group.computeNets
import com.deepak.paisapilot.domain.money.Paise
import com.deepak.paisapilot.domain.personal.Category
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettlementRepositoryTest {
    private lateinit var db: LedgerDatabase
    private lateinit var groups: GroupRepository
    private lateinit var expenses: SharedExpenseRepository
    private lateinit var repo: SettlementRepository
    private val counter = AtomicInteger()
    private val now = AtomicInteger(1_000)
    private val clock = object : Clock() {
        override fun getZone() = ZoneOffset.UTC
        override fun withZone(zone: java.time.ZoneId?) = this
        override fun instant(): Instant = Instant.ofEpochMilli(now.get().toLong())
    }

    // createGroup makes m1 = Me, m2 = Alex.
    private val me = "m1"
    private val alex = "m2"

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LedgerDatabase::class.java,
        ).build()
        val transactions = RoomTransactionRunner(db)
        groups = RoomGroupRepository(
            db.groupDao(),
            transactions,
            clock,
            IdGenerator { "m${counter.incrementAndGet()}" },
        )
        expenses = RoomSharedExpenseRepository(db.sharedExpenseDao(), db.groupDao(), transactions, clock)
        repo = RoomSettlementRepository(
            db.settlementDao(),
            db.sharedExpenseDao(),
            db.groupDao(),
            transactions,
            clock,
        )
        runBlocking {
            groups.createGroup("g1", "Flat", "Me", listOf("Alex"))
            // Me pays 10_000 for Alex only: Alex owes Me 10_000.
            expenses.create(expense("e1", amount = 10_000))
        }
    }

    @After
    fun tearDown() = db.close()

    private fun expense(id: String, amount: Long) = SharedExpense(
        id = id,
        groupId = "g1",
        amount = Paise(amount),
        title = null,
        category = Category.Food,
        date = LocalDate.of(2026, 10, 24),
        payerMemberId = me,
        note = null,
        shares = listOf(Share(alex, Paise(amount))),
        createdAt = 0,
        updatedAt = 0,
    )

    private fun settlement(id: String = "s1", from: String = alex, to: String = me, amount: Long = 2_000) =
        Settlement(
            id = id,
            groupId = "g1",
            fromMemberId = from,
            toMemberId = to,
            amount = Paise(amount),
            occurredEpochDay = LocalDate.of(2026, 10, 24).toEpochDay(),
            note = null,
            createdAt = 0,
            updatedAt = 0,
        )

    private suspend fun nets() = computeNets(
        expenses.observeGroup("g1").first(),
        repo.observeGroup("g1").first(),
    )

    private fun settlementRows() = db.openHelper.readableDatabase
        .query("SELECT COUNT(*) FROM settlement").use { it.moveToFirst(); it.getInt(0) }

    // AC-BAL-02
    @Test
    fun suggestedSettlementMovesNetsAndLeavesExpenseUnchanged() = runBlocking {
        assertEquals(SettlementWriteResult.Saved, repo.create(settlement(amount = 4_000)))
        val nets = nets()
        assertEquals(6_000L, nets[me])
        assertEquals(-6_000L, nets[alex])
        assertEquals(10_000L, expenses.observeGroup("g1").first().single().amount.value)
    }

    // AC-BAL-04
    @Test
    fun overSuggestionAndWrongDirectionAreRejectedWithoutStoring() = runBlocking {
        assertEquals(
            SettlementWriteResult.Invalid(SettlementValidation.ExceedsSuggestion(10_000)),
            repo.create(settlement(amount = 10_001)),
        )
        assertEquals(
            SettlementWriteResult.Invalid(SettlementValidation.PairNotSuggested),
            repo.create(settlement(id = "s2", from = me, to = alex, amount = 1_000)),
        )
        assertEquals(0, settlementRows())
    }

    @Test
    fun secondSettlementIsCappedByTheFirst() = runBlocking {
        repo.create(settlement(amount = 8_000))
        assertEquals(
            SettlementWriteResult.Invalid(SettlementValidation.ExceedsSuggestion(2_000)),
            repo.create(settlement(id = "s2", amount = 2_001)),
        )
    }

    @Test
    fun repeatedSaveKeepsFirstValues() = runBlocking {
        repo.create(settlement(amount = 2_000))
        assertEquals(SettlementWriteResult.AlreadySaved, repo.create(settlement(amount = 3_000)))
        assertEquals(1, settlementRows())
        assertEquals(2_000L, repo.observe("s1").first()!!.amount.value)
    }

    // AC-BAL-05
    @Test
    fun editKeepsIdChangesNetsByDifferenceAndKeepsCreatedAt() = runBlocking {
        repo.create(settlement(amount = 2_000))
        val before = repo.observe("s1").first()!!
        now.set(2_000)
        assertEquals(SettlementWriteResult.Saved, repo.update(settlement(amount = 2_500)))
        val after = repo.observe("s1").first()!!
        assertEquals(2_500L, after.amount.value)
        assertEquals(before.createdAt, after.createdAt)
        assertEquals(2_000L, after.updatedAt)
        assertEquals(1, settlementRows())
        assertEquals(7_500L, nets()[me])
    }

    @Test
    fun editBeyondTotalDebtIsRejected() = runBlocking {
        repo.create(settlement(amount = 2_000))
        assertEquals(
            SettlementWriteResult.Invalid(SettlementValidation.ExceedsSuggestion(10_000)),
            repo.update(settlement(amount = 10_001)),
        )
        assertEquals(2_000L, repo.observe("s1").first()!!.amount.value)
    }

    @Test
    fun updateOfMissingSettlementIsNotFound() = runBlocking {
        assertEquals(SettlementWriteResult.NotFound, repo.update(settlement(id = "nope")))
    }

    // AC-BAL-06
    @Test
    fun deleteRevertsNetsAndKeepsExpense() = runBlocking {
        repo.create(settlement(amount = 2_000))
        assertEquals(GroupActionResult.Done, repo.delete("s1"))
        assertNull(repo.observe("s1").first())
        assertEquals(10_000L, nets()[me])
        assertEquals(1, expenses.observeGroup("g1").first().size)
        assertEquals(GroupActionResult.NotFound, repo.delete("s1"))
    }

    // AC-BAL-08 / AC-SHR-08: expense edit leaves the settlement; the old amount stays editable downward.
    @Test
    fun expenseEditDoesNotTouchSettlementAndLegacyEditStaysAllowed() = runBlocking {
        repo.create(settlement(amount = 8_000))
        expenses.update(expense("e1", amount = 5_000))
        assertEquals(8_000L, repo.observe("s1").first()!!.amount.value)
        assertEquals(0L, nets().values.sum())
        assertEquals(SettlementWriteResult.Saved, repo.update(settlement(amount = 7_000)))
        assertEquals(
            SettlementWriteResult.Invalid(SettlementValidation.PairNotSuggested),
            repo.create(settlement(id = "s2", amount = 100)),
        )
    }

    @Test
    fun archivedGroupIsReadOnly() = runBlocking {
        repo.create(settlement(amount = 2_000))
        groups.archive("g1")
        assertEquals(SettlementWriteResult.GroupArchived, repo.create(settlement(id = "s2", amount = 100)))
        assertEquals(GroupActionResult.ReadOnly, repo.delete("s1"))
        assertTrue(settlementRows() == 1)
    }
}
