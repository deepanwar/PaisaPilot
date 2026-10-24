package com.deepak.paisapilot.domain.group

import com.deepak.paisapilot.domain.money.Paise

/** Recorded transfer: [fromMemberId] paid [toMemberId]. Reduces the payer's debt. */
data class Settlement(
    val id: String,
    val groupId: String,
    val fromMemberId: String,
    val toMemberId: String,
    val amount: Paise,
    val occurredEpochDay: Long,
    val note: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
