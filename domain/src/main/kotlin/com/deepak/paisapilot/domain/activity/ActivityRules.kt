package com.deepak.paisapilot.domain.activity

import com.deepak.paisapilot.domain.personal.Category

/** Date desc, then createdAt desc, then id desc. Same order the database query uses. */
val ActivityStableOrder: Comparator<ActivityItem> =
    compareByDescending<ActivityItem> { it.date }
        .thenByDescending { it.createdAt }
        .thenByDescending { it.id }

/**
 * Applies [filter] and returns the matches in stable order. Search is trimmed and case-insensitive
 * over title, note and the category label ([labelOf] because labels live in app resources).
 */
fun applyActivityFilter(
    items: List<ActivityItem>,
    filter: ActivityFilter,
    labelOf: (Category) -> String,
): List<ActivityItem> {
    val query = filter.query.trim()
    return items
        .filter { matches(it, filter, query, labelOf) }
        .sortedWith(ActivityStableOrder)
}

private fun matches(
    item: ActivityItem,
    filter: ActivityFilter,
    query: String,
    labelOf: (Category) -> String,
): Boolean {
    val month = filter.month
    if (month != null && (item.date.year != month.year || item.date.month != month.month)) {
        return false
    }
    return when (item) {
        is ActivityItem.Personal -> {
            val expense = item.expense
            if (filter.category != null && expense.category != filter.category) return false
            query.isEmpty() ||
                expense.title.orEmpty().contains(query, ignoreCase = true) ||
                expense.note.orEmpty().contains(query, ignoreCase = true) ||
                labelOf(expense.category).contains(query, ignoreCase = true)
        }
    }
}
