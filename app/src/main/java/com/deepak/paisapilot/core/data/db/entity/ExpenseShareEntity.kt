package com.deepak.paisapilot.core.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "expense_share",
    primaryKeys = ["shared_expense_id", "member_id"],
    foreignKeys = [
        ForeignKey(
            entity = SharedExpenseEntity::class,
            parentColumns = ["id"],
            childColumns = ["shared_expense_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = MemberEntity::class,
            parentColumns = ["id"],
            childColumns = ["member_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("member_id")],
)
data class ExpenseShareEntity(
    @ColumnInfo(name = "shared_expense_id") val sharedExpenseId: String,
    @ColumnInfo(name = "member_id") val memberId: String,
    @ColumnInfo(name = "share_paise") val sharePaise: Long,
)
