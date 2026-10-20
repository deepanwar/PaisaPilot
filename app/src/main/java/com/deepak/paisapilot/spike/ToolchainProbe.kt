package com.deepak.paisapilot.spike

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

const val TOOLCHAIN_PROBE_DATABASE_NAME = "paisapilot.db"

/**
 * Compile proof for Room KSP. Not schema v1.
 * Delete this database when T-003 adds the ledger tables.
 */
@Entity(tableName = "toolchain_probe")
data class ToolchainProbeEntity(
    @PrimaryKey val id: String,
    val label: String,
)

@Dao
interface ToolchainProbeDao {
    @Query("SELECT COUNT(*) FROM toolchain_probe")
    fun count(): Int
}

@Database(
    entities = [ToolchainProbeEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class ToolchainProbeDatabase : RoomDatabase() {
    abstract fun toolchainProbeDao(): ToolchainProbeDao
}
