package com.deepak.paisapilot.domain.personal

import com.deepak.paisapilot.domain.money.MoneyLimits
import com.deepak.paisapilot.domain.money.Paise
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MonthlySummaryTest {
    private fun e(id: String, paise: Long, category: Category) = PersonalExpense(
        id, Paise(paise), null, category, LocalDate.of(2026, 10, 5), null, 1L, 1L,
    )

    @Test
    fun foodAndBillsMatchAcceptanceFixture() {
        val s = summarizeMonth(listOf(e("a", 10_000, Category.Food), e("b", 5_000, Category.Bills)))
        assertEquals(Paise(15_000), s.total)
        assertEquals(2, s.count)
        assertEquals(
            listOf(
                CategoryTotal(Category.Food, Paise(10_000), 67),
                CategoryTotal(Category.Bills, Paise(5_000), 33),
            ),
            s.categories,
        )
    }

    @Test
    fun sumsPerCategoryAndSortsByAmountThenEnumOrder() {
        val s = summarizeMonth(
            listOf(
                e("a", 100, Category.Bills),
                e("b", 100, Category.Food),
                e("c", 50, Category.Food),
                e("d", 150, Category.Transport),
            ),
        )
        assertEquals(
            listOf(Category.Food, Category.Transport, Category.Bills),
            s.categories.map { it.category },
        )
        assertEquals(Paise(150), s.categories[0].amount)
        assertEquals(Paise(400), s.total)
        assertEquals(4, s.count)
    }

    @Test
    fun emptyMonthHasZeroTotalAndNoCategories() {
        val s = summarizeMonth(emptyList())
        assertEquals(Paise(0), s.total)
        assertEquals(0, s.count)
        assertTrue(s.categories.isEmpty())
    }

    @Test
    fun singleCategoryIs100Percent() {
        val s = summarizeMonth(listOf(e("a", 1, Category.Home)))
        assertEquals(100, s.categories.single().percent)
    }

    @Test
    fun thirdsRoundTo33AndCanSumTo99() {
        val s = summarizeMonth(
            listOf(e("a", 100, Category.Food), e("b", 100, Category.Bills), e("c", 100, Category.Home)),
        )
        assertEquals(listOf(33, 33, 33), s.categories.map { it.percent })
    }

    @Test
    fun roundingHalfUpCanSumTo101() {
        // 1/8 = 12.5% each rounds to 13; four of them plus a larger one exceed 100.
        val s = summarizeMonth(
            listOf(
                e("a", 1, Category.Food),
                e("b", 1, Category.Bills),
                e("c", 1, Category.Home),
                e("d", 5, Category.Other),
            ),
        )
        assertEquals(listOf(63, 13, 13, 13), s.categories.map { it.percent })
        assertEquals(102, s.categories.sumOf { it.percent })
    }

    @Test
    fun largeAmountsDoNotOverflow() {
        val max = MoneyLimits.MAX_ENTRY_PAISE
        val s = summarizeMonth(List(1_000) { e("e$it", max, Category.Food) })
        assertEquals(Paise(max * 1_000), s.total)
        assertEquals(100, s.categories.single().percent)
    }
}
