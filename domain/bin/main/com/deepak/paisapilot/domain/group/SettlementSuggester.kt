package com.deepak.paisapilot.domain.group

import com.deepak.paisapilot.domain.common.StableIdOrder
import com.deepak.paisapilot.domain.money.Paise

/** Derived allocation, not a historical obligation. Shown as "Suggested settlements". */
data class SuggestedSettlement(val fromMemberId: String, val toMemberId: String, val amount: Paise)

/**
 * Greedy allocation: repeatedly pair the largest debtor with the largest creditor (ties by stable
 * member ID) and transfer the smaller residual. Deterministic for any input order.
 */
fun suggestSettlements(nets: Map<String, Long>): List<SuggestedSettlement> {
    val order = compareByDescending<Pair<String, Long>> { it.second }.thenBy(StableIdOrder) { it.first }
    val debtors = nets.filterValues { it < 0L }.map { it.key to -it.value }.sortedWith(order).toMutableList()
    val creditors = nets.filterValues { it > 0L }.map { it.key to it.value }.sortedWith(order).toMutableList()
    val result = ArrayList<SuggestedSettlement>()
    while (debtors.isNotEmpty() && creditors.isNotEmpty()) {
        val debtor = debtors.removeAt(0)
        val creditor = creditors.removeAt(0)
        val transfer = minOf(debtor.second, creditor.second)
        result += SuggestedSettlement(debtor.first, creditor.first, Paise(transfer))
        val debtLeft = debtor.second - transfer
        val creditLeft = creditor.second - transfer
        if (debtLeft > 0L) debtors.insertSorted(debtor.first to debtLeft, order)
        if (creditLeft > 0L) creditors.insertSorted(creditor.first to creditLeft, order)
    }
    return result
}

private fun MutableList<Pair<String, Long>>.insertSorted(
    item: Pair<String, Long>,
    order: Comparator<Pair<String, Long>>,
) {
    val index = indexOfFirst { order.compare(item, it) < 0 }
    if (index < 0) add(item) else add(index, item)
}
