package com.deepak.paisapilot.di

import android.content.Context
import androidx.room.Room
import com.deepak.paisapilot.core.data.db.LEDGER_DATABASE_NAME
import com.deepak.paisapilot.core.data.db.LedgerDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun provideLedgerDatabase(@ApplicationContext context: Context): LedgerDatabase =
        Room.databaseBuilder(context, LedgerDatabase::class.java, LEDGER_DATABASE_NAME).build()
}
