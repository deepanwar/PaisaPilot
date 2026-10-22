package com.deepak.paisapilot.domain.activity

import com.deepak.paisapilot.domain.money.Paise
import com.deepak.paisapilot.domain.personal.Category
import com.deepak.paisapilot.domain.personal.PersonalExpense
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityRulesTest {
    private fun item(
        id: String,
        date: LocalDate,
        category: Category = Category.Other,
        title: String? = null,
        note: String? = null,
        created: Long = 1L,
    ) = ActivityItem.Personal(
        PersonalExpense(id, Paise(100), title, category, date, note, created, created),
    )

    private val labels = mapOf(
        Category.Food to "Food",
        Category.Bills to "Bills",
        Category.Other to "Other",
    )
    private val labelOf: (Category) -> String = { labels[it] ?: it.key }

    private val oct1 = LocalDate.of(2026, 10, 1)
    private val oct20 = LocalDate.of(2026, 10, 20)
    private val sep5 = LocalDate.of(2026, 9, 5)

    private fun ids(items: List<ActivityItem>) = items.map { it.id }

    @Test
    fun defaultFilterKeepsEverythingInStableOrder() {
        val items = listOf(item("a", sep5), item("b", oct20), item("c", oct1))
        assertEquals(listOf("b", "c", "a"), ids(applyActivityFilter(items, ActivityFilter(), labelOf)))
    }

    @Test
    fun ordersTiesByCreatedAtThenId() {
        val items = listOf(
            item("a", oct20, created = 1),
            item("b", oct20, created = 2),
            item("c", oct20, created = 2),
        )
        assertEquals(listOf("c", "b", "a"), ids(applyActivityFilter(items, ActivityFilter(), labelOf)))
    }

    @Test
    fun searchIsTrimmedAndCaseInsensitiveOverTitleNoteAndCategoryLabel() {
        val items = listOf(
            item("t", oct20, title = "Morning TEA"),
            item("n", oct20, note = "bought chai tea"),
            item("c", oct20, category = Category.Food),
            item("x", oct20, title = "Taxi"),
        )
        fun q(text: String) = ids(applyActivityFilter(items, ActivityFilter(query = text), labelOf)).toSet()
        assertEquals(setOf("t", "n"), q("  tea  "))
        assertEquals(setOf("c"), q("FOOD"))
        assertEquals(setOf("x"), q("taxi"))
        assertEquals(4, q("   ").size)
        assertTrue(q("impossible-term").isEmpty())
    }

    @Test
    fun monthFilterMatchesYearAndMonth() {
        val items = listOf(
            item("a", sep5),
            item("b", oct20),
            item("c", LocalDate.of(2025, 10, 20)),
        )
        val filter = ActivityFilter(month = LocalDate.of(2026, 10, 1))
        assertEquals(listOf("b"), ids(applyActivityFilter(items, filter, labelOf)))
    }

    @Test
    fun personalFoodInMonthOnlyReturnsMatches() {
        val items = listOf(
            item("a", oct20, Category.Food),
            item("b", oct20, Category.Bills),
            item("c", sep5, Category.Food),
        )
        val filter = ActivityFilter(
            month = LocalDate.of(2026, 10, 1),
            scope = ActivityScope.Personal,
            type = ActivityType.Expenses,
            category = Category.Food,
        )
        assertEquals(listOf("a"), ids(applyActivityFilter(items, filter, labelOf)))
    }

    @Test
    fun clearedResetsEverything() {
        val filter = ActivityFilter(
            query = "x",
            month = oct1,
            scope = ActivityScope.Personal,
            type = ActivityType.Expenses,
            category = Category.Food,
        )
        assertFalse(filter.isDefault)
        assertTrue(filter.cleared().isDefault)
        assertEquals(ActivityFilter(), filter.cleared())
    }
}
