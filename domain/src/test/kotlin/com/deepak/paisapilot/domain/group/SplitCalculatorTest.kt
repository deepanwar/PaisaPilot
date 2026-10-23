package com.deepak.paisapilot.domain.group

import com.deepak.paisapilot.domain.money.MoneyLimits
import com.deepak.paisapilot.domain.money.Paise
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SplitCalculatorTest {
    private val a = "00000000-0000-0000-0000-00000000000a"
    private val b = "00000000-0000-0000-0000-00000000000b"
    private val c = "00000000-0000-0000-0000-00000000000c"

    private fun equal(total: Long, vararg ids: String): List<Share> {
        val result = splitEqually(Paise(total), ids.toList())
        return (result as SplitResult.Success).shares
    }

    private fun share(id: String, paise: Long) = Share(id, Paise(paise))

    // Fixture 1 / AC-SHR-02
    @Test
    fun equalTenThousandByThree() {
        val shares = equal(10_000, a, b, c)
        assertEquals(listOf(a, b, c), shares.map { it.memberId })
        assertEquals(listOf(3334L, 3333L, 3333L), shares.map { it.paise.value })
    }

    @Test
    fun equalResultIndependentOfInputOrder() {
        assertEquals(equal(10_000, a, b, c), equal(10_000, c, a, b))
        assertEquals(equal(10_000, a, b, c), equal(10_000, b, c, a))
    }

    // Fixture 2 / AC-SHR-09
    @Test
    fun equalOnePaiseByThree() {
        val shares = equal(1, c, b, a)
        assertEquals(listOf(1L, 0L, 0L), shares.map { it.paise.value })
        assertEquals(a, shares.first().memberId)
    }

    @Test
    fun equalKeepsZeroRows() {
        val shares = equal(2, a, b, c, "d", "e")
        assertEquals(5, shares.size)
        assertEquals(listOf(1L, 1L, 0L, 0L, 0L), shares.map { it.paise.value })
    }

    // Fixture 4 / AC-SHR-03
    @Test
    fun singleBeneficiaryGetsFullAmount() {
        assertEquals(listOf(share(b, 10_000)), equal(10_000, b))
    }

    @Test
    fun equalAtMaxLimitSumsExactly() {
        val shares = equal(MoneyLimits.MAX_ENTRY_PAISE, a, b, c)
        assertEquals(MoneyLimits.MAX_ENTRY_PAISE, shares.sumOf { it.paise.value })
    }

    @Test
    fun equalRejectsInvalidInput() {
        assertEquals(SplitResult.AmountNotPositive, splitEqually(Paise(0), listOf(a)))
        assertEquals(SplitResult.AmountNotPositive, splitEqually(Paise(-5), listOf(a)))
        assertEquals(
            SplitResult.AmountOverLimit,
            splitEqually(Paise(MoneyLimits.MAX_ENTRY_PAISE + 1), listOf(a)),
        )
        assertEquals(SplitResult.NoBeneficiaries, splitEqually(Paise(100), emptyList()))
        assertEquals(SplitResult.DuplicateMember, splitEqually(Paise(100), listOf(a, a)))
        assertEquals(SplitResult.DuplicateMember, splitEqually(Paise(100), listOf("X", "x")))
    }

    @Test
    fun equalInvariantsHoldForManyInputs() {
        val random = Random(42)
        repeat(500) {
            val total = random.nextLong(1, 10_001)
            val n = random.nextInt(1, 13)
            val ids = List(n) { "m$it" }.shuffled(random)
            val shares = (splitEqually(Paise(total), ids) as SplitResult.Success).shares
            val values = shares.map { it.paise.value }
            assertEquals(total, values.sum())
            assertTrue(values.all { it >= 0 })
            assertTrue(values.max() - values.min() <= 1)
            assertEquals(ids.sorted(), shares.map { it.memberId })
        }
    }

    // Fixture 3 / AC-SHR-05
    @Test
    fun exactTwelveThreeFortyFive() {
        val result = validateExact(Paise(12_345), listOf(share(b, 4_345), share(a, 8_000)))
        assertEquals(
            ExactResult.Valid(listOf(share(a, 8_000), share(b, 4_345))),
            result,
        )
    }

    @Test
    fun exactSixtyForty() {
        val result = validateExact(Paise(10_000), listOf(share(a, 6_000), share(b, 4_000)))
        assertTrue(result is ExactResult.Valid)
    }

    // AC-SHR-04
    @Test
    fun exactUnderAssignedShowsRemaining() {
        val shares = listOf(share(a, 6_000), share(b, 3_000))
        assertEquals(ExactResult.Mismatch(1_000), validateExact(Paise(10_000), shares))
        assertEquals(1_000L, remaining(Paise(10_000), shares))
    }

    // AC-SHR-06: shares are never rescaled when total changes
    @Test
    fun exactTotalChangeKeepsSharesAndShowsRemaining() {
        val shares = listOf(share(a, 6_000), share(b, 4_000))
        assertEquals(ExactResult.Mismatch(1_000), validateExact(Paise(11_000), shares))
        assertEquals(1_000L, remaining(Paise(11_000), shares))
    }

    @Test
    fun exactOverAssignedIsNegativeRemaining() {
        val shares = listOf(share(a, 6_000), share(b, 5_000))
        assertEquals(ExactResult.Mismatch(-1_000), validateExact(Paise(10_000), shares))
    }

    @Test
    fun exactRejectsInvalidInput() {
        assertEquals(ExactResult.NoBeneficiaries, validateExact(Paise(100), emptyList()))
        assertEquals(
            ExactResult.DuplicateMember,
            validateExact(Paise(100), listOf(share(a, 50), share(a, 50))),
        )
        assertEquals(
            ExactResult.NegativeShare,
            validateExact(Paise(100), listOf(share(a, 150), share(b, -50))),
        )
        assertEquals(
            ExactResult.AmountNotPositive,
            validateExact(Paise(0), listOf(share(a, 0))),
        )
        assertEquals(
            ExactResult.AmountOverLimit,
            validateExact(Paise(MoneyLimits.MAX_ENTRY_PAISE + 1), listOf(share(a, 1))),
        )
    }

    @Test
    fun exactAllowsZeroShare() {
        val result = validateExact(Paise(100), listOf(share(a, 100), share(b, 0)))
        assertTrue(result is ExactResult.Valid)
    }

    @Test
    fun exactOverflowIsReported() {
        val shares = listOf(share(a, Long.MAX_VALUE), share(b, Long.MAX_VALUE))
        assertEquals(ExactResult.Overflow, validateExact(Paise(100), shares))
    }
}
