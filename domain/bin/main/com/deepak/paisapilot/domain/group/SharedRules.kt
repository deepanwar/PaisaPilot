package com.deepak.paisapilot.domain.group

import com.deepak.paisapilot.domain.money.MoneyLimits
import com.deepak.paisapilot.domain.money.Paise
import com.deepak.paisapilot.domain.personal.MAX_NOTE_LENGTH
import com.deepak.paisapilot.domain.personal.MAX_TITLE_LENGTH
import com.deepak.paisapilot.domain.time.isAllowedReportingDate

sealed interface SharedValidation {
    data class Valid(val expense: SharedExpense) : SharedValidation

    data object AmountNotPositive : SharedValidation

    data object AmountOverLimit : SharedValidation

    data object TitleTooLong : SharedValidation

    data object NoteTooLong : SharedValidation

    data object DateOutOfRange : SharedValidation

    data object NoPayer : SharedValidation

    data object NoBeneficiaries : SharedValidation

    data object DuplicateMember : SharedValidation

    data object NegativeShare : SharedValidation

    data object Overflow : SharedValidation

    /** Signed: positive = still unassigned, negative = over-assigned. */
    data class SharesMismatch(val remaining: Long) : SharedValidation
}

/**
 * Trims title and note (blank becomes null) and requires the shares to sum exactly to the amount.
 * Shares come back sorted by [com.deepak.paisapilot.domain.common.StableIdOrder]. Membership of the
 * payer and beneficiaries in the group is checked by the repository, not here.
 */
fun validateSharedExpense(expense: SharedExpense): SharedValidation {
    val amount = expense.amount.value
    if (amount <= 0L) return SharedValidation.AmountNotPositive
    if (amount > MoneyLimits.MAX_ENTRY_PAISE) return SharedValidation.AmountOverLimit
    val title = expense.title?.trim()?.takeIf { it.isNotEmpty() }
    if (title != null && title.length > MAX_TITLE_LENGTH) return SharedValidation.TitleTooLong
    val note = expense.note?.trim()?.takeIf { it.isNotEmpty() }
    if (note != null && note.length > MAX_NOTE_LENGTH) return SharedValidation.NoteTooLong
    if (!isAllowedReportingDate(expense.date)) return SharedValidation.DateOutOfRange
    if (expense.payerMemberId.isBlank()) return SharedValidation.NoPayer
    return when (val exact = validateExact(expense.amount, expense.shares)) {
        is ExactResult.Valid ->
            SharedValidation.Valid(expense.copy(title = title, note = note, shares = exact.shares))
        is ExactResult.Mismatch -> SharedValidation.SharesMismatch(exact.remaining)
        ExactResult.AmountNotPositive -> SharedValidation.AmountNotPositive
        ExactResult.AmountOverLimit -> SharedValidation.AmountOverLimit
        ExactResult.NoBeneficiaries -> SharedValidation.NoBeneficiaries
        ExactResult.DuplicateMember -> SharedValidation.DuplicateMember
        ExactResult.NegativeShare -> SharedValidation.NegativeShare
        ExactResult.Overflow -> SharedValidation.Overflow
    }
}

/** Starting point when the form switches to Exact: the current equal allocation, or empty if none. */
fun seedExactFromEqual(total: Paise, beneficiaryIds: List<String>): List<Share> =
    (splitEqually(total, beneficiaryIds) as? SplitResult.Success)?.shares.orEmpty()

/** Expense-only nets; see [computeExpenseNets]. Settlement-aware nets: [computeNets]. */
fun computeNets(expenses: List<SharedExpense>): Map<String, Long> = computeExpenseNets(expenses)
