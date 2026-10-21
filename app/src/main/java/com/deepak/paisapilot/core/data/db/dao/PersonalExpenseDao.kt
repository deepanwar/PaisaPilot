package com.deepak.paisapilot.core.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.deepak.paisapilot.core.data.db.entity.PersonalExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonalExpenseDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: PersonalExpenseEntity)

    @Update
    suspend fun update(entity: PersonalExpenseEntity): Int

    @Query("DELETE FROM personal_expense WHERE id = :id")
    suspend fun delete(id: String): Int

    @Query("SELECT * FROM personal_expense WHERE id = :id")
    suspend fun getById(id: String): PersonalExpenseEntity?

    @Query("SELECT * FROM personal_expense WHERE id = :id")
    fun observeById(id: String): Flow<PersonalExpenseEntity?>

    @Query(
        "SELECT * FROM personal_expense " +
            "WHERE occurred_epoch_day BETWEEN :startDay AND :endDay " +
            "ORDER BY occurred_epoch_day DESC, created_at DESC, id DESC",
    )
    fun observeBetween(startDay: Long, endDay: Long): Flow<List<PersonalExpenseEntity>>

    @Query(
        "SELECT COALESCE(SUM(amount_paise), 0) FROM personal_expense " +
            "WHERE occurred_epoch_day BETWEEN :startDay AND :endDay",
    )
    fun observeTotalBetween(startDay: Long, endDay: Long): Flow<Long>
}
