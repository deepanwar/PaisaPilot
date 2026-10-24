package com.deepak.paisapilot.domain.group

import com.deepak.paisapilot.domain.money.Paise
import com.deepak.paisapilot.domain.personal.Category
import java.time.LocalDate

internal fun balanceExpense(id: String, amount: Long, payer: String, shares: List<Pair<String, Long>>) =
    SharedExpense(
        id = id,
        groupId = "g1",
        amount = Paise(amount),
        title = null,
        category = Category.Food,
        date = LocalDate.of(2026, 10, 24),
        payerMemberId = payer,
        note = null,
        shares = shares.map { Share(it.first, Paise(it.second)) },
        createdAt = 1L,
        updatedAt = 1L,
    )

internal fun balanceSettlement(id: String, from: String, to: String, amount: Long) =
    Settlement(
        id = id,
        groupId = "g1",
        fromMemberId = from,
        toMemberId = to,
        amount = Paise(amount),
        occurredEpochDay = 1L,
        note = null,
        createdAt = 1L,
        updatedAt = 1L,
    )
