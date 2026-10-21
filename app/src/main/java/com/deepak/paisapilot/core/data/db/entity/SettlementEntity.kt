package com.deepak.paisapilot.core.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "settlement",
    foreignKeys = [
        ForeignKey(
            entity = ExpenseGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["group_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = MemberEntity::class,
            parentColumns = ["id"],
            childColumns = ["from_member_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = MemberEntity::class,
            parentColumns = ["id"],
            childColumns = ["to_member_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index("group_id"),
        Index("from_member_id"),
        Index("to_member_id"),
    ],
)
data class SettlementEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "group_id") val groupId: String,
    @ColumnInfo(name = "from_member_id") val fromMemberId: String,
    @ColumnInfo(name = "to_member_id") val toMemberId: String,
    @ColumnInfo(name = "amount_paise") val amountPaise: Long,
    @ColumnInfo(name = "occurred_epoch_day") val occurredEpochDay: Long,
    val note: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
