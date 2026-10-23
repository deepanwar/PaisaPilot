package com.deepak.paisapilot.core.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.deepak.paisapilot.core.data.db.entity.ExpenseGroupEntity
import com.deepak.paisapilot.core.data.db.entity.MemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertGroup(entity: ExpenseGroupEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMember(entity: MemberEntity)

    @Query("SELECT * FROM expense_group WHERE id = :id")
    suspend fun getGroup(id: String): ExpenseGroupEntity?

    @Query("SELECT * FROM expense_group WHERE id = :id")
    fun observeGroup(id: String): Flow<ExpenseGroupEntity?>

    @Query("SELECT * FROM expense_group ORDER BY created_at DESC, id DESC")
    fun observeGroups(): Flow<List<ExpenseGroupEntity>>

    @Query("SELECT * FROM member")
    fun observeAllMembers(): Flow<List<MemberEntity>>

    @Query("SELECT * FROM member WHERE group_id = :groupId ORDER BY is_local_owner DESC, normalized_name, id")
    suspend fun getMembers(groupId: String): List<MemberEntity>

    @Query("SELECT * FROM member WHERE group_id = :groupId ORDER BY is_local_owner DESC, normalized_name, id")
    fun observeMembers(groupId: String): Flow<List<MemberEntity>>

    @Query("SELECT * FROM member WHERE id = :id")
    suspend fun getMember(id: String): MemberEntity?

    @Query("UPDATE member SET active = :active WHERE id = :id")
    suspend fun setMemberActive(id: String, active: Boolean): Int

    @Query("UPDATE expense_group SET archived_at = :archivedAt WHERE id = :id")
    suspend fun setArchivedAt(id: String, archivedAt: Long): Int

    @Query(
        "SELECT (SELECT COUNT(*) FROM shared_expense WHERE group_id = :groupId) + " +
            "(SELECT COUNT(*) FROM settlement WHERE group_id = :groupId)",
    )
    suspend fun countHistory(groupId: String): Int

    @Query(
        "SELECT (SELECT COUNT(*) FROM shared_expense WHERE group_id = :groupId) + " +
            "(SELECT COUNT(*) FROM settlement WHERE group_id = :groupId)",
    )
    fun observeHistoryCount(groupId: String): Flow<Int>

    @Query(
        "SELECT COALESCE(SUM(amount_paise), 0) FROM shared_expense " +
            "WHERE group_id = :groupId AND occurred_epoch_day BETWEEN :startDay AND :endDay",
    )
    fun observeMonthTotal(groupId: String, startDay: Long, endDay: Long): Flow<Long>

    @Query("DELETE FROM member WHERE group_id = :groupId")
    suspend fun deleteMembers(groupId: String): Int

    @Query("DELETE FROM expense_group WHERE id = :id")
    suspend fun deleteGroup(id: String): Int
}
