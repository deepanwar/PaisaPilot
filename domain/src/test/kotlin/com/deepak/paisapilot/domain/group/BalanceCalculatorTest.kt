package com.deepak.paisapilot.domain.group

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BalanceCalculatorTest {
    private val a = "00000000-0000-0000-0000-00000000000a"
    private val b = "00000000-0000-0000-0000-00000000000b"
    private val c = "00000000-0000-0000-0000-00000000000c"

    @Test
    fun expenseNetsCreditPayerAndDebitShares() {
        val e = balanceExpense("e1", 10_000, a, listOf(a to 3_334, b to 3_333, c to 3_333))
        val nets = computeExpenseNets(listOf(e))
        assertEquals(6_666L, nets[a])
        assertEquals(-3_333L, nets[b])
        assertEquals(-3_333L, nets[c])
    }

    @Test
    fun settlementMovesNetsTowardZero() {
        val e = balanceExpense("e1", 10_000, a, listOf(b to 10_000))
        val nets = computeNets(listOf(e), listOf(balanceSettlement("s1", b, a, 4_000)))
        assertEquals(6_000L, nets[a])
        assertEquals(-6_000L, nets[b])
    }

    @Test
    fun editReplayUsesCurrentExpenseOnly() {
        val before = balanceExpense("e1", 10_000, a, listOf(b to 10_000))
        val after = balanceExpense("e1", 6_000, a, listOf(b to 6_000))
        val settlements = listOf(balanceSettlement("s1", b, a, 4_000))
        assertEquals(6_000L, computeNets(listOf(before), settlements)[a])
        assertEquals(2_000L, computeNets(listOf(after), settlements)[a])
    }

    @Test
    fun overflowThrows() {
        val big = balanceExpense("e1", Long.MAX_VALUE, a, listOf(b to Long.MAX_VALUE))
        assertThrows(ArithmeticException::class.java) {
            computeNets(listOf(big), listOf(balanceSettlement("s1", a, b, Long.MAX_VALUE)))
        }
    }

    @Test
    fun randomLedgersSumToZeroAndIgnoreOrder() {
        val members = listOf(a, b, c, "00000000-0000-0000-0000-00000000000d")
        val random = Random(42)
        repeat(200) {
            val expenses = List(random.nextInt(1, 8)) { i ->
                val shares = members.shuffled(random).take(random.nextInt(1, 5)).map { it to random.nextLong(0, 5_000) }
                balanceExpense("e$i", shares.sumOf { it.second }, members.random(random), shares)
            }
            val settlements = List(random.nextInt(0, 5)) { i ->
                val (from, to) = members.shuffled(random).take(2)
                balanceSettlement("s$i", from, to, random.nextLong(1, 5_000))
            }
            val nets = computeNets(expenses, settlements)
            assertEquals(0L, nets.values.sum())
            assertEquals(nets, computeNets(expenses.shuffled(random), settlements.shuffled(random)))
            assertEquals(0L, computeExpenseNets(expenses).values.sum())
        }
    }
}
