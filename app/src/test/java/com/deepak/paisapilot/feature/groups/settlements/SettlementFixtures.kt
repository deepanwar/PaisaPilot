package com.deepak.paisapilot.feature.groups.settlements

import com.deepak.paisapilot.domain.group.Group
import com.deepak.paisapilot.domain.group.Member
import com.deepak.paisapilot.domain.group.Settlement
import com.deepak.paisapilot.domain.group.Share
import com.deepak.paisapilot.domain.group.SharedExpense
import com.deepak.paisapilot.domain.money.Paise
import com.deepak.paisapilot.domain.personal.Category
import com.deepak.paisapilot.feature.groups.FakeGroupRepository
import com.deepak.paisapilot.feature.groups.FakeSettlementRepository
import com.deepak.paisapilot.feature.groups.FakeSharedExpenseRepository
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/** Owner "o" paid 10_000 for Alex "a" (6_000) and Sam "s" (4_000): nets o +10_000, a -6_000, s -4_000. */
class SettlementEnv(
    val groups: FakeGroupRepository,
    val expenses: FakeSharedExpenseRepository,
    val settlements: FakeSettlementRepository,
)

fun expense(id: String = "e1", amount: Long = 10_000, a: Long = 6_000, s: Long = 4_000) = SharedExpense(
    id = id,
    groupId = "g",
    amount = Paise(amount),
    title = "Dinner",
    category = Category.Food,
    date = LocalDate.of(2026, 10, 18),
    payerMemberId = "o",
    note = null,
    shares = listOf(Share("a", Paise(a)), Share("s", Paise(s))),
    createdAt = 1L,
    updatedAt = 1L,
)

fun settlement(id: String = "s1", from: String = "a", to: String = "o", amount: Long = 2_000) = Settlement(
    id = id,
    groupId = "g",
    fromMemberId = from,
    toMemberId = to,
    amount = Paise(amount),
    occurredEpochDay = LocalDate.of(2026, 10, 20).toEpochDay(),
    note = null,
    createdAt = 1L,
    updatedAt = 1L,
)

@OptIn(ExperimentalCoroutinesApi::class)
fun settlementTest(
    archived: Boolean = false,
    block: suspend TestScope.(SettlementEnv) -> Unit,
) {
    val dispatcher = StandardTestDispatcher()
    Dispatchers.setMain(dispatcher)
    try {
        runTest(dispatcher) {
            val groups = FakeGroupRepository().apply {
                this.groups.value = listOf(Group("g", "Flat", 0L, if (archived) 1L else null))
                members.value = listOf(
                    Member("o", "g", "stored-name", isLocalOwner = true, active = true),
                    Member("a", "g", "Alex", isLocalOwner = false, active = true),
                    Member("s", "g", "Sam", isLocalOwner = false, active = true),
                )
            }
            val expenses = FakeSharedExpenseRepository().apply { this.expenses.value = listOf(expense()) }
            block(SettlementEnv(groups, expenses, FakeSettlementRepository()))
        }
    } finally {
        Dispatchers.resetMain()
    }
}
