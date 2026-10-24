package com.deepak.paisapilot.domain.group

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewDetectorTest {
    private val a = "00000000-0000-0000-0000-00000000000a"
    private val b = "00000000-0000-0000-0000-00000000000b"

    private fun review(expenses: List<SharedExpense>, settlements: List<Settlement>) =
        needsBalanceReview(computeExpenseNets(expenses), computeNets(expenses, settlements))

    @Test
    fun settlementWithinDebtDoesNotFlag() {
        val e = balanceExpense("e1", 10_000, a, listOf(b to 10_000))
        assertFalse(review(listOf(e), listOf(balanceSettlement("s1", b, a, 10_000))))
        assertFalse(review(listOf(e), listOf(balanceSettlement("s1", b, a, 4_000))))
    }

    // AC-BAL-08
    @Test
    fun expenseEditLeavingSettlementExcessFlagsAndKeepsSumZero() {
        val settlements = listOf(balanceSettlement("s1", b, a, 10_000))
        val edited = balanceExpense("e1", 6_000, a, listOf(b to 6_000))
        val nets = computeNets(listOf(edited), settlements)
        assertEquals(0L, nets.values.sum())
        assertTrue(review(listOf(edited), settlements))
        assertEquals(10_000L, settlements.single().amount.value)
    }

    @Test
    fun noExpenseButSettlementFlags() {
        assertTrue(review(emptyList(), listOf(balanceSettlement("s1", b, a, 100))))
    }

    @Test
    fun emptyLedgerDoesNotFlag() {
        assertFalse(review(emptyList(), emptyList()))
    }
}
