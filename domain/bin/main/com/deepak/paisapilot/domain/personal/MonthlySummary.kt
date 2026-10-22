package com.deepak.paisapilot.domain.personal

import com.deepak.paisapilot.domain.money.Paise

data class CategoryTotal(val category: Category, val amount: Paise, val percent: Int)

data class MonthlySummary(
    val total: Paise,
    val count: Int,
    val categories: List<CategoryTotal>,
)

/**
 * Summarizes one month of expenses; the caller supplies only that month's rows.
 * Amounts are authoritative. Percentages are whole numbers rounded half up from integer paise,
 * so they can sum to 99 or 101.
 */
fun summarizeMonth(expenses: List<PersonalExpense>): MonthlySummary {
    var total = 0L
    val byCategory = LinkedHashMap<Category, Long>()
    for (expense in expenses) {
        val paise = expense.amount.value
        total = Math.addExact(total, paise)
        byCategory.merge(expense.category, paise, Math::addExact)
    }
    val rows = byCategory.entries
        .sortedWith(compareByDescending<Map.Entry<Category, Long>> { it.value }.thenBy { it.key.ordinal })
        .map { (category, amount) ->
            CategoryTotal(category, Paise(amount), wholePercent(amount, total))
        }
    return MonthlySummary(Paise(total), expenses.size, rows)
}

private fun wholePercent(part: Long, total: Long): Int {
    if (total <= 0L) return 0
    // (part * 100 / total) rounded half up, as (part * 200 + total) / (2 * total).
    val twice = Math.multiplyExact(part, 200L)
    return ((twice + total) / (2 * total)).toInt()
}
