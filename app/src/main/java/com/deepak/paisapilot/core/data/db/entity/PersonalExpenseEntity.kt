package com.deepak.paisapilot.core.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "personal_expense",
    indices = [Index("occurred_epoch_day")],
)
data class PersonalExpenseEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "amount_paise") val amountPaise: Long,
    val title: String?,
    @ColumnInfo(name = "category_key") val categoryKey: String,
    @ColumnInfo(name = "occurred_epoch_day") val occurredEpochDay: Long,
    val note: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
