package com.deepak.paisapilot.core.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.deepak.paisapilot.core.data.db.entity.ExpenseGroupEntity
import com.deepak.paisapilot.core.data.db.entity.ExpenseShareEntity
import com.deepak.paisapilot.core.data.db.entity.MemberEntity
import com.deepak.paisapilot.core.data.db.entity.PersonalExpenseEntity
import com.deepak.paisapilot.core.data.db.entity.SharedExpenseEntity

/** Minimal access for schema tests. Real DAOs arrive with the repositories in M1+. */
@Dao
interface LedgerSchemaDao {
    @Insert
    suspend fun insert(entity: PersonalExpenseEntity)

    @Insert
    suspend fun insert(entity: ExpenseGroupEntity)

    @Insert
    suspend fun insert(entity: MemberEntity)

    @Insert
    suspend fun insert(entity: SharedExpenseEntity)

    @Insert
    suspend fun insert(entity: ExpenseShareEntity)

    @Query("SELECT * FROM personal_expense WHERE id = :id")
    suspend fun personalExpense(id: String): PersonalExpenseEntity?

    @Query("DELETE FROM member WHERE id = :id")
    suspend fun deleteMember(id: String)

    @Query("DELETE FROM shared_expense WHERE id = :id")
    suspend fun deleteSharedExpense(id: String)

    @Query("SELECT COUNT(*) FROM expense_share")
    suspend fun shareCount(): Int
}
