package com.deepak.paisapilot.core.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "member",
    foreignKeys = [
        ForeignKey(
            entity = ExpenseGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["group_id"],
        ),
    ],
    indices = [Index(value = ["group_id", "normalized_name"], unique = true)],
)
data class MemberEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "group_id") val groupId: String,
    val name: String,
    @ColumnInfo(name = "normalized_name") val normalizedName: String,
    @ColumnInfo(name = "is_local_owner") val isLocalOwner: Boolean,
    val active: Boolean,
)
