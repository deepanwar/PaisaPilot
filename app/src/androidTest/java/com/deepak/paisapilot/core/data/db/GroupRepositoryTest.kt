package com.deepak.paisapilot.core.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.deepak.paisapilot.core.data.IdGenerator
import com.deepak.paisapilot.core.data.db.entity.ExpenseShareEntity
import com.deepak.paisapilot.core.data.db.entity.SharedExpenseEntity
import com.deepak.paisapilot.core.data.repo.AddMemberResult
import com.deepak.paisapilot.core.data.repo.CreateGroupResult
import com.deepak.paisapilot.core.data.repo.GroupActionResult
import com.deepak.paisapilot.core.data.repo.GroupRepository
import com.deepak.paisapilot.core.data.repo.RoomGroupRepository
import com.deepak.paisapilot.domain.group.MemberNameResult
import com.deepak.paisapilot.domain.group.NewGroupResult
import com.deepak.paisapilot.domain.money.Paise
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GroupRepositoryTest {
    private lateinit var db: LedgerDatabase
    private lateinit var repo: GroupRepository
    private val counter = AtomicInteger()
    private val clock = Clock.fixed(Instant.ofEpochMilli(5_000L), ZoneOffset.UTC)

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LedgerDatabase::class.java,
        ).build()
        repo = RoomGroupRepository(
            db.groupDao(),
            RoomTransactionRunner(db),
            clock,
            IdGenerator { "m${counter.incrementAndGet()}" },
        )
    }

    @After
    fun tearDown() = db.close()

    private suspend fun flat(): String {
        assertEquals(CreateGroupResult.Created, repo.createGroup("g1", "Flat", "Me", listOf("Alex")))
        return "g1"
    }

    // AC-GRP-02
    @Test
    fun createPersistsGroupAndTwoMembers() = runBlocking {
        flat()
        assertEquals(1, repo.observeGroups().first().size)
        val members = repo.observeMembers("g1").first()
        assertEquals(listOf("Me", "Alex"), members.map { it.name })
        assertEquals(listOf(true, false), members.map { it.isLocalOwner })
        assertEquals(2, members.map { it.id }.toSet().size)
        assertEquals(2, repo.observeGroups().first().single().memberCount)
    }

    @Test
    fun repeatedCreateIsIdempotent() = runBlocking {
        flat()
        assertEquals(CreateGroupResult.AlreadySaved, repo.createGroup("g1", "Flat", "Me", listOf("Alex")))
        assertEquals(1, repo.observeGroups().first().size)
        assertEquals(2, repo.observeMembers("g1").first().size)
    }

    // AC-GRP-04: invalid input writes nothing
    @Test
    fun invalidCreateWritesNothing() = runBlocking {
        val result = repo.createGroup("g1", "Flat", "Me", emptyList())
        assertEquals(CreateGroupResult.Invalid(NewGroupResult.TooFewMembers), result)
        assertTrue(repo.observeGroups().first().isEmpty())
    }

    // AC-GRP-03 + G6
    @Test
    fun duplicateNameBlockedEvenForInactiveMember() = runBlocking {
        flat()
        val alex = repo.observeMembers("g1").first().first { it.name == "Alex" }
        repo.setMemberActive(alex.id, false)
        assertEquals(
            AddMemberResult.Invalid(MemberNameResult.Duplicate),
            repo.addMember("g1", " ALEX "),
        )
        assertEquals(2, repo.observeMembers("g1").first().size)
    }

    // AC-GRP-05
    @Test
    fun addMemberKeepsExistingMembers() = runBlocking {
        flat()
        assertEquals(AddMemberResult.Added, repo.addMember("g1", "Bea"))
        val members = repo.observeMembers("g1").first()
        assertEquals(3, members.size)
        assertTrue(members.first { it.name == "Bea" }.active)
    }

    @Test
    fun memberLimitIs30() = runBlocking {
        repo.createGroup("g1", "Big", "Me", List(29) { "p$it" })
        assertEquals(
            AddMemberResult.Invalid(MemberNameResult.GroupFull),
            repo.addMember("g1", "extra"),
        )
    }

    // AC-GRP-06
    @Test
    fun deactivatedMemberKeepsRowAndHistory() = runBlocking {
        flat()
        val members = repo.observeMembers("g1").first()
        val owner = members.first { it.isLocalOwner }
        val alex = members.first { !it.isLocalOwner }
        db.schemaDao().insert(
            SharedExpenseEntity("e1", "g1", 1_000L, null, "food", 20_000L, owner.id, null, 1L, 1L),
        )
        db.schemaDao().insert(ExpenseShareEntity("e1", alex.id, 1_000L))
        assertEquals(GroupActionResult.Done, repo.setMemberActive(alex.id, false))
        val after = repo.observeMembers("g1").first().first { it.id == alex.id }
        assertFalse(after.active)
        assertEquals(1, db.schemaDao().shareCount())
    }

    @Test
    fun ownerCannotBeDeactivated() = runBlocking {
        flat()
        val owner = repo.observeMembers("g1").first().first { it.isLocalOwner }
        assertEquals(GroupActionResult.NotAllowed, repo.setMemberActive(owner.id, false))
        assertTrue(repo.observeMembers("g1").first().first { it.isLocalOwner }.active)
    }

    // AC-GRP-07
    @Test
    fun archiveFreezesGroup() = runBlocking {
        flat()
        assertEquals(GroupActionResult.Done, repo.archive("g1"))
        assertEquals(5_000L, repo.observeGroup("g1").first()!!.archivedAt)
        assertEquals(AddMemberResult.GroupArchived, repo.addMember("g1", "Bea"))
        val alex = repo.observeMembers("g1").first().first { !it.isLocalOwner }
        assertEquals(GroupActionResult.ReadOnly, repo.setMemberActive(alex.id, false))
        assertEquals(GroupActionResult.NotAllowed, repo.archive("g1"))
        assertEquals(2, repo.observeMembers("g1").first().size)
    }

    @Test
    fun deleteEmptyGroupRemovesMembersToo() = runBlocking {
        flat()
        assertEquals(GroupActionResult.Done, repo.delete("g1"))
        assertNull(repo.observeGroup("g1").first())
        assertTrue(repo.observeMembers("g1").first().isEmpty())
    }

    @Test
    fun deleteBlockedWhenGroupHasHistory() = runBlocking {
        flat()
        val owner = repo.observeMembers("g1").first().first { it.isLocalOwner }
        db.schemaDao().insert(
            SharedExpenseEntity("e1", "g1", 1_000L, null, "food", 20_000L, owner.id, null, 1L, 1L),
        )
        assertTrue(repo.observeHasHistory("g1").first())
        assertEquals(GroupActionResult.NotAllowed, repo.delete("g1"))
        assertNotNull(repo.observeGroup("g1").first())
        assertEquals(2, repo.observeMembers("g1").first().size)
    }

    @Test
    fun monthTotalSumsOnlyThatGroupAndMonth() = runBlocking {
        flat()
        repo.createGroup("g2", "Trip", "Me", listOf("Zed"))
        val owner1 = repo.observeMembers("g1").first().first { it.isLocalOwner }
        val owner2 = repo.observeMembers("g2").first().first { it.isLocalOwner }
        val oct = monthContaining(LocalDate.of(2026, 10, 20))
        val day = LocalDate.of(2026, 10, 5).toEpochDay()
        val dao = db.schemaDao()
        dao.insert(SharedExpenseEntity("e1", "g1", 10_000L, null, "food", day, owner1.id, null, 1L, 1L))
        dao.insert(SharedExpenseEntity("e2", "g1", 2_500L, null, "food", day, owner1.id, null, 1L, 1L))
        dao.insert(SharedExpenseEntity("e3", "g1", 700L, null, "food", day - 40, owner1.id, null, 1L, 1L))
        dao.insert(SharedExpenseEntity("e4", "g2", 999L, null, "food", day, owner2.id, null, 1L, 1L))
        assertEquals(Paise(12_500L), repo.observeMonthTotal("g1", oct).first())
    }
}
