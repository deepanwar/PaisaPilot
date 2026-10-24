package com.deepak.paisapilot.core.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.deepak.paisapilot.core.data.IdGenerator
import com.deepak.paisapilot.core.data.repo.GroupActionResult
import com.deepak.paisapilot.core.data.repo.GroupRepository
import com.deepak.paisapilot.core.data.repo.RoomGroupRepository
import com.deepak.paisapilot.core.data.repo.RoomPersonalExpenseRepository
import com.deepak.paisapilot.core.data.repo.RoomSharedExpenseRepository
import com.deepak.paisapilot.core.data.repo.SharedExpenseRepository
import com.deepak.paisapilot.core.data.repo.SharedWriteResult
import com.deepak.paisapilot.domain.group.Share
import com.deepak.paisapilot.domain.group.SharedExpense
import com.deepak.paisapilot.domain.group.SharedValidation
import com.deepak.paisapilot.domain.group.computeNets
import com.deepak.paisapilot.domain.money.Paise
import com.deepak.paisapilot.domain.personal.Category
import com.deepak.paisapilot.domain.personal.PersonalExpense
import com.deepak.paisapilot.domain.time.monthContaining
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SharedExpenseRepositoryTest {
    private lateinit var db: LedgerDatabase
    private lateinit var groups: GroupRepository
    private lateinit var repo: SharedExpenseRepository
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
        repo = RoomSharedExpenseRepository(db.sharedExpenseDao(), db.groupDao(), transactions, clock)
        runBlocking { groups.createGroup("g1", "Flat", "Me", listOf("Alex")) }
    }

    @After
    fun tearDown() = db.close()

    private fun expense(
        id: String = "e1",
        groupId: String = "g1",
        amount: Long = 20_000,
        payer: String = me,
        shares: List<Share> = listOf(Share(me, Paise(10_000)), Share(alex, Paise(10_000))),
        date: LocalDate = LocalDate.of(2026, 10, 24),
    ) = SharedExpense(
        id = id,
        groupId = groupId,
        amount = Paise(amount),
        title = "Dinner",
        category = Category.Food,
        date = date,
        payerMemberId = payer,
        note = null,
        shares = shares,
        createdAt = 0,
        updatedAt = 0,
    )

    private fun shareRows() = db.openHelper.readableDatabase
        .query("SELECT COUNT(*) FROM expense_share").use { it.moveToFirst(); it.getInt(0) }

    // AC-SHR-01
    @Test
    fun createPersistsParentAndSharesWithNets() = runBlocking {
        assertEquals(SharedWriteResult.Saved, repo.create(expense()))
        val stored = repo.observe("e1").first()!!
        assertEquals(listOf(10_000L, 10_000L), stored.shares.map { it.paise.value })
        assertEquals(1_000L, stored.createdAt)
        val nets = computeNets(listOf(stored))
        assertEquals(10_000L, nets[me])
        assertEquals(-10_000L, nets[alex])
    }

    // AC-SHR-03
    @Test
    fun payerNotBeneficiaryHasNoSelfShare() = runBlocking {
        val e = expense(amount = 1_000, shares = listOf(Share(alex, Paise(1_000))))
        assertEquals(SharedWriteResult.Saved, repo.create(e))
        assertEquals(listOf(alex), repo.observe("e1").first()!!.shares.map { it.memberId })
    }

    // AC-SHR-04
    @Test
    fun mismatchedSharesAreRejectedAndNothingStored() = runBlocking {
        val e = expense(amount = 10_000, shares = listOf(Share(me, Paise(6_000)), Share(alex, Paise(3_000))))
        assertEquals(
            SharedWriteResult.Invalid(SharedValidation.SharesMismatch(1_000)),
            repo.create(e),
        )
        assertNull(repo.observe("e1").first())
        assertEquals(0, shareRows())
    }

    // AC-SHR-09
    @Test
    fun zeroPaiseRowsAreKept() = runBlocking {
        val e = expense(
            amount = 1,
            shares = listOf(Share(me, Paise(1)), Share(alex, Paise(0))),
        )
        assertEquals(SharedWriteResult.Saved, repo.create(e))
        assertEquals(2, shareRows())
    }

    @Test
    fun duplicateSaveKeepsFirstValues() = runBlocking {
        assertEquals(SharedWriteResult.Saved, repo.create(expense()))
        now.set(2_000)
        val again = expense(
            amount = 40_000,
            shares = listOf(Share(me, Paise(20_000)), Share(alex, Paise(20_000))),
        )
        assertEquals(SharedWriteResult.AlreadySaved, repo.create(again))
        assertEquals(20_000L, repo.observe("e1").first()!!.amount.value)
        assertEquals(2, shareRows())
    }

    @Test
    fun foreignPayerOrBeneficiaryIsRejected() = runBlocking {
        groups.createGroup("g2", "Trip", "Me", listOf("Zed")) // m3 = Me, m4 = Zed
        assertEquals(SharedWriteResult.MemberInvalid, repo.create(expense(payer = "m4")))
        val foreignShare = expense(shares = listOf(Share(me, Paise(10_000)), Share("m4", Paise(10_000))))
        assertEquals(SharedWriteResult.MemberInvalid, repo.create(foreignShare))
        assertEquals(SharedWriteResult.MemberInvalid, repo.create(expense(payer = "nope")))
        assertEquals(0, shareRows())
    }

    @Test
    fun inactivePayerOrNewInactiveBeneficiaryIsRejected() = runBlocking {
        assertEquals(GroupActionResult.Done, groups.setMemberActive(alex, false))
        assertEquals(SharedWriteResult.MemberInvalid, repo.create(expense(payer = alex)))
        assertEquals(SharedWriteResult.MemberInvalid, repo.create(expense()))
    }

    @Test
    fun archivedGroupIsReadOnly() = runBlocking {
        groups.archive("g1")
        assertEquals(SharedWriteResult.GroupArchived, repo.create(expense()))
    }

    @Test
    fun missingGroupIsNotFound() = runBlocking {
        assertEquals(SharedWriteResult.NotFound, repo.create(expense(groupId = "nope")))
    }

    // AC-SHR-07
    @Test
    fun editReplacesSharesKeepingIdGroupAndCreatedAt() = runBlocking {
        repo.create(expense())
        now.set(3_000)
        val edited = expense(
            amount = 30_000,
            payer = alex,
            shares = listOf(Share(me, Paise(5_000)), Share(alex, Paise(25_000))),
        )
        assertEquals(SharedWriteResult.Saved, repo.update(edited))
        val stored = repo.observe("e1").first()!!
        assertEquals("g1", stored.groupId)
        assertEquals(alex, stored.payerMemberId)
        assertEquals(listOf(5_000L, 25_000L), stored.shares.map { it.paise.value })
        assertEquals(1_000L, stored.createdAt)
        assertEquals(3_000L, stored.updatedAt)
        assertEquals(2, shareRows())
        assertEquals(1, repo.observeGroup("g1").first().size)
        val nets = computeNets(listOf(stored))
        assertEquals(-25_000L + 30_000L, nets[alex])
        assertEquals(0L, nets.values.sum())
    }

    @Test
    fun invalidEditLeavesStoredExpenseUntouched() = runBlocking {
        repo.create(expense())
        val bad = expense(amount = 30_000)
        assertTrue(repo.update(bad) is SharedWriteResult.Invalid)
        val stored = repo.observe("e1").first()!!
        assertEquals(20_000L, stored.amount.value)
        assertEquals(2, stored.shares.size)
    }

    @Test
    fun editKeepsInactiveHistoricalMemberButNotNewInactive() = runBlocking {
        repo.create(expense())
        groups.setMemberActive(alex, false)
        val kept = expense(
            amount = 30_000,
            shares = listOf(Share(me, Paise(10_000)), Share(alex, Paise(20_000))),
        )
        assertEquals(SharedWriteResult.Saved, repo.update(kept))
        // A second expense cannot newly include the inactive member.
        repo.create(expense(id = "e2", shares = listOf(Share(me, Paise(20_000)))).copy(amount = Paise(20_000)))
        val addInactive = expense(id = "e2")
        assertEquals(SharedWriteResult.MemberInvalid, repo.update(addInactive))
    }

    @Test
    fun editMissingExpenseOrOnArchivedGroupIsRefused() = runBlocking {
        assertEquals(SharedWriteResult.NotFound, repo.update(expense()))
        repo.create(expense())
        groups.archive("g1")
        assertEquals(SharedWriteResult.GroupArchived, repo.update(expense(amount = 20_000)))
    }

    // AC-SHR-08 (settlement advisory half waits for M4)
    @Test
    fun deleteRemovesExpenseAndShares() = runBlocking {
        repo.create(expense())
        assertEquals(GroupActionResult.Done, repo.delete("e1"))
        assertNull(repo.observe("e1").first())
        assertEquals(0, shareRows())
        assertEquals(GroupActionResult.NotFound, repo.delete("e1"))
    }

    @Test
    fun deleteOnArchivedGroupIsReadOnly() = runBlocking {
        repo.create(expense())
        groups.archive("g1")
        assertEquals(GroupActionResult.ReadOnly, repo.delete("e1"))
        assertNotNull(repo.observe("e1").first())
    }

    @Test
    fun groupListIsNewestFirst() = runBlocking {
        repo.create(expense(id = "old", date = LocalDate.of(2026, 10, 1)))
        repo.create(expense(id = "new", date = LocalDate.of(2026, 10, 20)))
        now.set(2_000)
        repo.create(expense(id = "new2", date = LocalDate.of(2026, 10, 20)))
        assertEquals(listOf("new2", "new", "old"), repo.observeGroup("g1").first().map { it.id })
        assertTrue(repo.observeGroup("g1").first().all { it.shares.size == 2 })
    }

    // AC-SHR-10
    @Test
    fun sharedExpensesNeverAppearInPersonalTotals() = runBlocking {
        val personal = RoomPersonalExpenseRepository(db.personalExpenseDao(), clock)
        personal.create(
            PersonalExpense(
                "p1", Paise(10_000), "Tea", Category.Food, LocalDate.of(2026, 10, 24), null, 0, 0,
            ),
        )
        repo.create(expense())
        val month = monthContaining(LocalDate.of(2026, 10, 24))
        assertEquals(10_000L, personal.observeMonthTotal(month).first().value)
        assertEquals(listOf("p1"), personal.observeMonth(month).first().map { it.id })
        assertEquals(20_000L, groups.observeMonthTotal("g1", month).first().value)
    }
}
