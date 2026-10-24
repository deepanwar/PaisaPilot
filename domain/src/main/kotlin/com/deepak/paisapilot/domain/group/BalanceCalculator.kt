package com.deepak.paisapilot.domain.group

import com.deepak.paisapilot.domain.common.StableIdOrder
import java.util.SortedMap

/**
 * Signed net paise per member from expenses only (E): payer credited the full amount, each share
 * debited. Positive means owed. Sums to zero. Throws [ArithmeticException] on overflow.
 */
fun computeExpenseNets(expenses: List<SharedExpense>): SortedMap<String, Long> {
    val nets = HashMap<String, Long>()
    for (expense in expenses) {
        nets.addChecked(expense.payerMemberId, expense.amount.value)
        for (share in expense.shares) nets.addChecked(share.memberId, -share.paise.value)
    }
    return nets.toSortedMap(StableIdOrder)
}

/**
 * Signed net paise per member including settlements (N). A settlement from A to B of T does
 * `net_A += T`, `net_B -= T`. Sums to zero and does not depend on input order.
 */
fun computeNets(expenses: List<SharedExpense>, settlements: List<Settlement>): SortedMap<String, Long> {
    val nets = HashMap(computeExpenseNets(expenses))
    for (settlement in settlements) {
        nets.addChecked(settlement.fromMemberId, settlement.amount.value)
        nets.addChecked(settlement.toMemberId, -settlement.amount.value)
    }
    return nets.toSortedMap(StableIdOrder)
}

private fun MutableMap<String, Long>.addChecked(memberId: String, delta: Long) {
    merge(memberId, delta, Math::addExact)
}
