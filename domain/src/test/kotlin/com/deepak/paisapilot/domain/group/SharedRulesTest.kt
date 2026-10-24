package com.deepak.paisapilot.domain.group

import com.deepak.paisapilot.domain.money.MoneyLimits
import com.deepak.paisapilot.domain.money.Paise
import com.deepak.paisapilot.domain.personal.Category
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedRulesTest {
    private val me = "00000000-0000-0000-0000-00000000000a"
    private val alex = "00000000-0000-0000-0000-00000000000b"
    private val sam = "00000000-0000-0000-0000-00000000000c"

    private fun share(id: String, paise: Long) = Share(id, Paise(paise))

    private fun expense(
        amount: Long = 20_000,
        payer: String = me,
        shares: List<Share> = listOf(share(me, 10_000), share(alex, 10_000)),
        title: String? = null,
        note: String? = null,
        date: LocalDate = LocalDate.of(2026, 10, 24),
    ) = SharedExpense(
        id = "e1",
        groupId = "g1",
        amount = Paise(amount),
        title = title,
        category = Category.Food,
        date = date,
        payerMemberId = payer,
        note = note,
        shares = shares,
        createdAt = 1L,
        updatedAt = 1L,
    )

    // AC-SHR-01
    @Test
    fun validEqualSplitIsAcceptedAndNetsAreCorrect() {
        val valid = validateSharedExpense(expense()) as SharedValidation.Valid
        val nets = computeNets(listOf(valid.expense))
        assertEquals(10_000L, nets[me])
        assertEquals(-10_000L, nets[alex])
    }

    // AC-SHR-03
    @Test
    fun payerNotBeneficiaryGetsFullCredit() {
        val e = expense(amount = 1_000, shares = listOf(share(alex, 1_000)))
        val valid = validateSharedExpense(e) as SharedValidation.Valid
        val nets = computeNets(listOf(valid.expense))
        assertEquals(1_000L, nets[me])
        assertEquals(-1_000L, nets[alex])
        assertEquals(2, nets.size)
    }

    // AC-SHR-04
    @Test
    fun exactUnderAssignedIsBlockedWithRemaining() {
        val e = expense(amount = 10_000, shares = listOf(share(me, 6_000), share(alex, 3_000)))
        assertEquals(SharedValidation.SharesMismatch(1_000), validateSharedExpense(e))
    }

    @Test
    fun exactOverAssignedHasNegativeRemaining() {
        val e = expense(amount = 10_000, shares = listOf(share(me, 6_000), share(alex, 5_000)))
        assertEquals(SharedValidation.SharesMismatch(-1_000), validateSharedExpense(e))
    }

    // AC-SHR-05
    @Test
    fun exactMatchingSumIsValid() {
        val e = expense(amount = 10_000, shares = listOf(share(alex, 4_000), share(me, 6_000)))
        val valid = validateSharedExpense(e) as SharedValidation.Valid
        assertEquals(listOf(me, alex), valid.expense.shares.map { it.memberId })
    }

    // AC-SHR-06
    @Test
    fun changingTotalKeepsSharesAndShowsRemaining() {
        val e = expense(amount = 11_000, shares = listOf(share(me, 6_000), share(alex, 4_000)))
        assertEquals(SharedValidation.SharesMismatch(1_000), validateSharedExpense(e))
    }

    // AC-SHR-09
    @Test
    fun onePaiseAmongThreeKeepsZeroRows() {
        val seeded = seedExactFromEqual(Paise(1), listOf(sam, alex, me))
        assertEquals(listOf(1L, 0L, 0L), seeded.map { it.paise.value })
        val e = expense(amount = 1, shares = seeded)
        assertTrue(validateSharedExpense(e) is SharedValidation.Valid)
    }

    @Test
    fun seedFromEqualIsEmptyWhenNoSplitPossible() {
        assertTrue(seedExactFromEqual(Paise(0), listOf(me)).isEmpty())
        assertTrue(seedExactFromEqual(Paise(100), emptyList()).isEmpty())
    }

    @Test
    fun rejectsBadAmounts() {
        assertEquals(SharedValidation.AmountNotPositive, validateSharedExpense(expense(amount = 0)))
        assertEquals(
            SharedValidation.AmountOverLimit,
            validateSharedExpense(expense(amount = MoneyLimits.MAX_ENTRY_PAISE + 1)),
        )
    }

    @Test
    fun rejectsStructuralShareProblems() {
        assertEquals(SharedValidation.NoPayer, validateSharedExpense(expense(payer = " ")))
        assertEquals(SharedValidation.NoBeneficiaries, validateSharedExpense(expense(shares = emptyList())))
        assertEquals(
            SharedValidation.DuplicateMember,
            validateSharedExpense(expense(shares = listOf(share(me, 10_000), share(me, 10_000)))),
        )
        assertEquals(
            SharedValidation.NegativeShare,
            validateSharedExpense(expense(shares = listOf(share(me, 30_000), share(alex, -10_000)))),
        )
        assertEquals(
            SharedValidation.Overflow,
            validateSharedExpense(
                expense(shares = listOf(share(me, Long.MAX_VALUE), share(alex, Long.MAX_VALUE))),
            ),
        )
    }

    @Test
    fun trimsTitleAndNoteAndBlanksBecomeNull() {
        val valid = validateSharedExpense(expense(title = "  Dinner ", note = "   ")) as SharedValidation.Valid
        assertEquals("Dinner", valid.expense.title)
        assertNull(valid.expense.note)
    }

    @Test
    fun rejectsLongTextAndBadDate() {
        assertEquals(SharedValidation.TitleTooLong, validateSharedExpense(expense(title = "x".repeat(81))))
        assertEquals(SharedValidation.NoteTooLong, validateSharedExpense(expense(note = "x".repeat(301))))
        assertEquals(
            SharedValidation.DateOutOfRange,
            validateSharedExpense(expense(date = LocalDate.of(1999, 12, 31))),
        )
    }

    @Test
    fun netsSumToZeroAcrossExpenses() {
        val one = expense()
        val two = expense(
            amount = 10_000,
            payer = sam,
            shares = listOf(share(me, 3_334), share(alex, 3_333), share(sam, 3_333)),
        )
        val nets = computeNets(listOf(one, two))
        assertEquals(0L, nets.values.sum())
        assertEquals(listOf(me, alex, sam), nets.keys.toList())
    }
}
