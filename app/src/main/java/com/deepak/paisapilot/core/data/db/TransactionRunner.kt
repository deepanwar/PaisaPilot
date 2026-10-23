package com.deepak.paisapilot.core.data.db

import androidx.room.RoomDatabase
import androidx.room.withTransaction

/** Lets repositories run multi-row writes atomically without depending on Room directly. */
interface TransactionRunner {
    suspend fun <T> run(block: suspend () -> T): T
}

class RoomTransactionRunner(private val database: RoomDatabase) : TransactionRunner {
    override suspend fun <T> run(block: suspend () -> T): T = database.withTransaction { block() }
}
