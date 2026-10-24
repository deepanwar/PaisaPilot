package com.deepak.paisapilot.di

import android.content.Context
import androidx.room.Room
import com.deepak.paisapilot.core.data.db.LEDGER_DATABASE_NAME
import com.deepak.paisapilot.core.data.IdGenerator
import com.deepak.paisapilot.core.data.db.LedgerDatabase
import com.deepak.paisapilot.core.data.db.RoomTransactionRunner
import com.deepak.paisapilot.core.data.db.TransactionRunner
import com.deepak.paisapilot.core.data.db.dao.GroupDao
import com.deepak.paisapilot.core.data.db.dao.PersonalExpenseDao
import com.deepak.paisapilot.core.data.db.dao.SettlementDao
import com.deepak.paisapilot.core.data.db.dao.SharedExpenseDao
import com.deepak.paisapilot.core.data.repo.ActivityRepository
import com.deepak.paisapilot.core.data.repo.DefaultActivityRepository
import com.deepak.paisapilot.core.data.repo.GroupRepository
import com.deepak.paisapilot.core.data.repo.RoomGroupRepository
import com.deepak.paisapilot.core.data.repo.PersonalExpenseRepository
import com.deepak.paisapilot.core.data.repo.RoomPersonalExpenseRepository
import com.deepak.paisapilot.core.data.repo.RoomSettlementRepository
import com.deepak.paisapilot.core.data.repo.RoomSharedExpenseRepository
import com.deepak.paisapilot.core.data.repo.SettlementRepository
import com.deepak.paisapilot.core.data.repo.SharedExpenseRepository
import com.deepak.paisapilot.feature.personal.CategoryLabelProvider
import com.deepak.paisapilot.feature.personal.ResourceCategoryLabelProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun provideLedgerDatabase(@ApplicationContext context: Context): LedgerDatabase =
        Room.databaseBuilder(context, LedgerDatabase::class.java, LEDGER_DATABASE_NAME).build()

    @Provides
    fun providePersonalExpenseDao(database: LedgerDatabase): PersonalExpenseDao =
        database.personalExpenseDao()

    @Provides
    fun provideGroupDao(database: LedgerDatabase): GroupDao = database.groupDao()

    @Provides
    fun provideSharedExpenseDao(database: LedgerDatabase): SharedExpenseDao =
        database.sharedExpenseDao()

    @Provides
    fun provideSettlementDao(database: LedgerDatabase): SettlementDao = database.settlementDao()

    @Provides
    @Singleton
    fun provideTransactionRunner(database: LedgerDatabase): TransactionRunner =
        RoomTransactionRunner(database)

    @Provides
    @Singleton
    fun provideGroupRepository(
        dao: GroupDao,
        transactions: TransactionRunner,
        clock: Clock,
        idGenerator: IdGenerator,
    ): GroupRepository = RoomGroupRepository(dao, transactions, clock, idGenerator)

    @Provides
    @Singleton
    fun providePersonalExpenseRepository(
        dao: PersonalExpenseDao,
        clock: Clock,
    ): PersonalExpenseRepository = RoomPersonalExpenseRepository(dao, clock)

    @Provides
    @Singleton
    fun provideSharedExpenseRepository(
        dao: SharedExpenseDao,
        groupDao: GroupDao,
        transactions: TransactionRunner,
        clock: Clock,
    ): SharedExpenseRepository = RoomSharedExpenseRepository(dao, groupDao, transactions, clock)

    @Provides
    @Singleton
    fun provideSettlementRepository(
        dao: SettlementDao,
        expenseDao: SharedExpenseDao,
        groupDao: GroupDao,
        transactions: TransactionRunner,
        clock: Clock,
    ): SettlementRepository = RoomSettlementRepository(dao, expenseDao, groupDao, transactions, clock)

    @Provides
    @Singleton
    fun provideActivityRepository(personal: PersonalExpenseRepository): ActivityRepository =
        DefaultActivityRepository(personal)

    @Provides
    @Singleton
    fun provideCategoryLabels(@ApplicationContext context: Context): CategoryLabelProvider =
        ResourceCategoryLabelProvider(context)
}
