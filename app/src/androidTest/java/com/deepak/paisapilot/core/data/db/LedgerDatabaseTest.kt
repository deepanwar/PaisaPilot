package com.deepak.paisapilot.core.data.db

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.deepak.paisapilot.core.data.db.dao.LedgerSchemaDao
import com.deepak.paisapilot.core.data.db.entity.ExpenseGroupEntity
import com.deepak.paisapilot.core.data.db.entity.ExpenseShareEntity
import com.deepak.paisapilot.core.data.db.entity.MemberEntity
import com.deepak.paisapilot.core.data.db.entity.PersonalExpenseEntity
import com.deepak.paisapilot.core.data.db.entity.SharedExpenseEntity
import com.deepak.paisapilot.domain.money.MoneyLimits
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LedgerDatabaseTest {
    private lateinit var db: LedgerDatabase
    private lateinit var dao: LedgerSchemaDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LedgerDatabase::class.java,
        ).build()
        dao = db.schemaDao()
    }

    @After
    fun tearDown() = db.close()

    private fun group(id: String) = ExpenseGroupEntity(id, "Trip $id", 1L, null)

    private fun member(id: String, groupId: String, name: String) =
        MemberEntity(id, groupId, name, name.trim().lowercase(), false, true)

    private fun expense(id: String, groupId: String, payer: String) =
        SharedExpenseEntity(id, groupId, 1_000L, null, "food", 20_000L, payer, null, 1L, 1L)

    private suspend fun seedSharedExpense() {
        dao.insert(group("g1"))
        dao.insert(member("m1", "g1", "Alex"))
        dao.insert(expense("e1", "g1", "m1"))
        dao.insert(ExpenseShareEntity("e1", "m1", 1_000L))
    }

    @Test
    fun schemaCreates() = runBlocking {
        val tables = mutableSetOf<String>()
        db.openHelper.writableDatabase
            .query("SELECT name FROM sqlite_master WHERE type='table'").use {
                while (it.moveToNext()) tables += it.getString(0)
            }
        assertEquals(
            setOf(
                "personal_expense", "expense_group", "member",
                "shared_expense", "expense_share", "settlement",
            ),
            tables.filter { !it.startsWith("android_") && !it.startsWith("room_") && !it.startsWith("sqlite_") }.toSet(),
        )
    }

    @Test
    fun foreignKeysAreEnabled() {
        db.openHelper.writableDatabase.query("PRAGMA foreign_keys").use {
            it.moveToFirst()
            assertEquals(1, it.getInt(0))
        }
    }

    @Test
    fun shareForMissingExpenseFails() = runBlocking {
        dao.insert(group("g1"))
        dao.insert(member("m1", "g1", "Alex"))
        assertThrows(SQLiteConstraintException::class.java) {
            runBlocking { dao.insert(ExpenseShareEntity("missing", "m1", 1L)) }
        }
        Unit
    }

    @Test
    fun deletingReferencedMemberIsRestricted() = runBlocking {
        seedSharedExpense()
        assertThrows(SQLiteConstraintException::class.java) {
            runBlocking { dao.deleteMember("m1") }
        }
        Unit
    }

    @Test
    fun deletingSharedExpenseCascadesToShares() = runBlocking {
        seedSharedExpense()
        dao.deleteSharedExpense("e1")
        assertEquals(0, dao.shareCount())
    }

    @Test
    fun memberNameUniqueIgnoringCaseWithinGroupOnly() = runBlocking {
        dao.insert(group("g1"))
        dao.insert(group("g2"))
        dao.insert(member("m1", "g1", "Alex"))
        assertThrows(SQLiteConstraintException::class.java) {
            runBlocking { dao.insert(member("m2", "g1", "alex")) }
        }
        dao.insert(member("m3", "g2", "alex"))
    }

    @Test
    fun personalExpenseRoundTrip() = runBlocking {
        val max = PersonalExpenseEntity(
            "p1", MoneyLimits.MAX_ENTRY_PAISE, null, "food", 20_000L, null, 5L, 6L,
        )
        dao.insert(max)
        assertEquals(max, dao.personalExpense("p1"))
        assertNull(dao.personalExpense("p1")!!.title)
        assertNotNull(dao.personalExpense("p1"))
    }
}
