package com.deepak.paisapilot.core.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.deepak.paisapilot.core.data.db.entity.ExpenseShareEntity
import com.deepak.paisapilot.core.data.db.entity.SharedExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SharedExpenseDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertExpense(entity: SharedExpenseEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertShares(entities: List<ExpenseShareEntity>)

    @Update
    suspend fun updateExpense(entity: SharedExpenseEntity): Int

    @Query("DELETE FROM expense_share WHERE shared_expense_id = :expenseId")
    suspend fun deleteShares(expenseId: String): Int

    @Query("DELETE FROM shared_expense WHERE id = :id")
    suspend fun deleteExpense(id: String): Int

    @Query("SELECT * FROM shared_expense WHERE id = :id")
    suspend fun getExpense(id: String): SharedExpenseEntity?

    @Query("SELECT * FROM shared_expense WHERE group_id = :groupId")
    suspend fun getGroupExpenses(groupId: String): List<SharedExpenseEntity>

    @Query("SELECT * FROM expense_share WHERE shared_expense_id = :expenseId")
    suspend fun getShares(expenseId: String): List<ExpenseShareEntity>

    @Query("SELECT * FROM shared_expense WHERE id = :id")
    fun observeExpense(id: String): Flow<SharedExpenseEntity?>

    @Query("SELECT * FROM expense_share WHERE shared_expense_id = :expenseId")
    fun observeShares(expenseId: String): Flow<List<ExpenseShareEntity>>

    @Query(
        "SELECT * FROM shared_expense WHERE group_id = :groupId " +
            "ORDER BY occurred_epoch_day DESC, created_at DESC, id DESC",
    )
    fun observeGroupExpenses(groupId: String): Flow<List<SharedExpenseEntity>>

    @Query(
        "SELECT s.* FROM expense_share s JOIN shared_expense e ON e.id = s.shared_expense_id " +
            "WHERE e.group_id = :groupId",
    )
    fun observeGroupShares(groupId: String): Flow<List<ExpenseShareEntity>>
}
