package com.deepak.paisapilot.domain.group

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettlementRulesTest {
    private val a = "00000000-0000-0000-0000-00000000000a"
    private val b = "00000000-0000-0000-0000-00000000000b"
    private val c = "00000000-0000-0000-0000-00000000000c"

    // A paid 10_000 and only B and C benefit: nets A +10_000, B -6_000, C -4_000.
    private val expenses = listOf(balanceExpense("e1", 10_000, a, listOf(b to 6_000, c to 4_000)))

    private fun settle(from: String, to: String, amount: Long, id: String = "s1", note: String? = null) =
        balanceSettlement(id, from, to, amount).copy(
            occurredEpochDay = LocalDate.of(2026, 10, 24).toEpochDay(),
            note = note,
        )

    private fun validate(s: Settlement, others: List<Settlement> = emptyList(), existing: Settlement? = null) =
        validateSettlement(s, expenses, others, existing)

    @Test
    fun suggestedPairWithinAmountIsValid() {
        assertTrue(validate(settle(b, a, 6_000)) is SettlementValidation.Valid)
        assertTrue(validate(settle(b, a, 100)) is SettlementValidation.Valid)
    }

    // AC-BAL-04
    @Test
    fun overSuggestionAndReversedPairAreRejected() {
        assertEquals(SettlementValidation.ExceedsSuggestion(6_000), validate(settle(b, a, 6_001)))
        assertEquals(SettlementValidation.PairNotSuggested, validate(settle(a, b, 1_000)))
        assertEquals(SettlementValidation.PairNotSuggested, validate(settle(b, c, 1_000)))
    }

    @Test
    fun basicChecks() {
        assertEquals(SettlementValidation.SameMember, validate(settle(b, b, 1)))
        assertEquals(SettlementValidation.AmountNotPositive, validate(settle(b, a, 0)))
        assertEquals(SettlementValidation.NoteTooLong, validate(settle(b, a, 1, note = "x".repeat(301))))
        assertEquals(
            SettlementValidation.DateOutOfRange,
            validate(settle(b, a, 1).copy(occurredEpochDay = Long.MAX_VALUE)),
        )
    }

    @Test
    fun blankNoteBecomesNull() {
        val valid = validate(settle(b, a, 1, note = "   ")) as SettlementValidation.Valid
        assertEquals(null, valid.settlement.note)
    }

    // AC-BAL-05: B->A 20 edited to 25 stays valid because the check excludes the old record.
    @Test
    fun editChecksAgainstBalancesWithoutItself() {
        val old = settle(b, a, 2_000)
        assertTrue(validate(settle(b, a, 2_500), emptyList(), old) is SettlementValidation.Valid)
        assertEquals(
            SettlementValidation.ExceedsSuggestion(6_000),
            validate(settle(b, a, 6_001), emptyList(), old),
        )
    }

    @Test
    fun otherSettlementsReduceTheAllowance() {
        val others = listOf(settle(b, a, 4_000, id = "s0"))
        assertEquals(SettlementValidation.ExceedsSuggestion(2_000), validate(settle(b, a, 2_001), others))
        assertTrue(validate(settle(b, a, 2_000), others) is SettlementValidation.Valid)
    }

    @Test
    fun legacyEditKeepsPairAfterExpenseEdit() {
        // Expense edited down so the old 6_000 settlement is excess; nothing is suggested for B now.
        val edited = listOf(balanceExpense("e1", 4_000, a, listOf(b to 2_000, c to 2_000)))
        val old = settle(b, a, 6_000)
        assertEquals(
            SettlementValidation.PairNotSuggested,
            validateSettlement(settle(b, a, 1_000, id = "new"), edited, listOf(old)),
        )
        assertTrue(validateSettlement(settle(b, a, 5_000), edited, emptyList(), old) is SettlementValidation.Valid)
        assertEquals(
            SettlementValidation.ExceedsSuggestion(2_000),
            validateSettlement(settle(b, a, 6_001), edited, emptyList(), old),
        )
    }

    // Three-member sequence ending at all zero.
    @Test
    fun threeMemberSequenceSettlesToZero() {
        val s1 = settle(b, a, 2_500, id = "s1")
        val s2 = settle(b, a, 3_500, id = "s2")
        val s3 = settle(c, a, 4_000, id = "s3")
        assertTrue(validate(s1) is SettlementValidation.Valid)
        assertTrue(validate(s2, listOf(s1)) is SettlementValidation.Valid)
        assertTrue(validate(s3, listOf(s1, s2)) is SettlementValidation.Valid)
        assertTrue(computeNets(expenses, listOf(s1, s2, s3)).values.all { it == 0L })
        assertEquals(SettlementValidation.PairNotSuggested, validate(settle(b, a, 1, id = "s4"), listOf(s1, s2, s3)))
    }
}
