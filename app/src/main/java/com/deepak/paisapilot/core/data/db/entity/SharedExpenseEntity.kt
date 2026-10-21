package com.deepak.paisapilot.core.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "shared_expense",
    foreignKeys = [
        ForeignKey(
            entity = ExpenseGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["group_id"],
        ),
        ForeignKey(
            entity = MemberEntity::class,
            parentColumns = ["id"],
            childColumns = ["payer_member_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index("group_id"),
        Index("payer_member_id"),
        Index("occurred_epoch_day"),
    ],
)
data class SharedExpenseEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "group_id") val groupId: String,
    @ColumnInfo(name = "amount_paise") val amountPaise: Long,
    val title: String?,
    @ColumnInfo(name = "category_key") val categoryKey: String,
    @ColumnInfo(name = "occurred_epoch_day") val occurredEpochDay: Long,
    @ColumnInfo(name = "payer_member_id") val payerMemberId: String,
    val note: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
