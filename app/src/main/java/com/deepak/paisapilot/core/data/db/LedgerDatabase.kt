package com.deepak.paisapilot.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.deepak.paisapilot.core.data.db.dao.GroupDao
import com.deepak.paisapilot.core.data.db.dao.LedgerSchemaDao
import com.deepak.paisapilot.core.data.db.dao.PersonalExpenseDao
import com.deepak.paisapilot.core.data.db.dao.SettlementDao
import com.deepak.paisapilot.core.data.db.dao.SharedExpenseDao
import com.deepak.paisapilot.core.data.db.entity.ExpenseGroupEntity
import com.deepak.paisapilot.core.data.db.entity.ExpenseShareEntity
import com.deepak.paisapilot.core.data.db.entity.MemberEntity
import com.deepak.paisapilot.core.data.db.entity.PersonalExpenseEntity
import com.deepak.paisapilot.core.data.db.entity.SettlementEntity
import com.deepak.paisapilot.core.data.db.entity.SharedExpenseEntity

const val LEDGER_DATABASE_NAME = "paisapilot.db"

@Database(
    entities = [
        PersonalExpenseEntity::class,
        ExpenseGroupEntity::class,
        MemberEntity::class,
        SharedExpenseEntity::class,
        ExpenseShareEntity::class,
        SettlementEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class LedgerDatabase : RoomDatabase() {
    abstract fun schemaDao(): LedgerSchemaDao

    abstract fun personalExpenseDao(): PersonalExpenseDao

    abstract fun groupDao(): GroupDao

    abstract fun sharedExpenseDao(): SharedExpenseDao

    abstract fun settlementDao(): SettlementDao
}
