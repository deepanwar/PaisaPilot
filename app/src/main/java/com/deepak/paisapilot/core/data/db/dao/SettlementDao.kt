package com.deepak.paisapilot.core.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.deepak.paisapilot.core.data.db.entity.SettlementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettlementDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSettlement(entity: SettlementEntity)

    @Update
    suspend fun updateSettlement(entity: SettlementEntity): Int

    @Query("DELETE FROM settlement WHERE id = :id")
    suspend fun deleteSettlement(id: String): Int

    @Query("SELECT * FROM settlement WHERE id = :id")
    suspend fun getSettlement(id: String): SettlementEntity?

    @Query("SELECT * FROM settlement WHERE group_id = :groupId")
    suspend fun getGroupSettlements(groupId: String): List<SettlementEntity>

    @Query("SELECT * FROM settlement WHERE id = :id")
    fun observeSettlement(id: String): Flow<SettlementEntity?>

    @Query(
        "SELECT * FROM settlement WHERE group_id = :groupId " +
            "ORDER BY occurred_epoch_day DESC, created_at DESC, id DESC",
    )
    fun observeGroupSettlements(groupId: String): Flow<List<SettlementEntity>>
}
