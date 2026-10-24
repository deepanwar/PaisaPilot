package com.deepak.paisapilot.domain.group

import com.deepak.paisapilot.domain.money.MoneyLimits
import com.deepak.paisapilot.domain.money.Paise
import com.deepak.paisapilot.domain.personal.MAX_NOTE_LENGTH
import com.deepak.paisapilot.domain.time.isAllowedReportingDate
import java.time.DateTimeException
import java.time.LocalDate

sealed interface SettlementValidation {
    data class Valid(val settlement: Settlement) : SettlementValidation

    data object SameMember : SettlementValidation

    data object AmountNotPositive : SettlementValidation

    data object AmountOverLimit : SettlementValidation

    data object NoteTooLong : SettlementValidation

    data object DateOutOfRange : SettlementValidation

    /** The pair is not a current suggested debtor to creditor pair. */
    data object PairNotSuggested : SettlementValidation

    /** Amount is above the suggested debt for the pair. [max] is that suggestion in paise. */
    data class ExceedsSuggestion(val max: Long) : SettlementValidation

    data object Overflow : SettlementValidation
}

/**
 * Trims the note (blank becomes null), then requires the pair and amount to fit the suggestions
 * derived from [expenses] and [otherSettlements]. [otherSettlements] must exclude the record being
 * edited, so an edit is checked against the balances without its own old amount. Membership in the
 * group is checked by the repository.
 *
 * A legacy edit stays valid when [existing] has the same pair and the new amount is not above its
 * old amount, even if later expense edits mean the pair is no longer suggested.
 */
fun validateSettlement(
    settlement: Settlement,
    expenses: List<SharedExpense>,
    otherSettlements: List<Settlement>,
    existing: Settlement? = null,
): SettlementValidation {
    if (settlement.fromMemberId == settlement.toMemberId) return SettlementValidation.SameMember
    val amount = settlement.amount.value
    if (amount <= 0L) return SettlementValidation.AmountNotPositive
    if (amount > MoneyLimits.MAX_ENTRY_PAISE) return SettlementValidation.AmountOverLimit
    val note = settlement.note?.trim()?.takeIf { it.isNotEmpty() }
    if (note != null && note.length > MAX_NOTE_LENGTH) return SettlementValidation.NoteTooLong
    val date = try {
        LocalDate.ofEpochDay(settlement.occurredEpochDay)
    } catch (_: DateTimeException) {
        return SettlementValidation.DateOutOfRange
    }
    if (!isAllowedReportingDate(date)) return SettlementValidation.DateOutOfRange

    val suggested = try {
        suggestSettlements(computeNets(expenses, otherSettlements))
            .firstOrNull { it.fromMemberId == settlement.fromMemberId && it.toMemberId == settlement.toMemberId }
    } catch (_: ArithmeticException) {
        return SettlementValidation.Overflow
    }
    val legacyOk = existing != null &&
        existing.fromMemberId == settlement.fromMemberId &&
        existing.toMemberId == settlement.toMemberId &&
        amount <= existing.amount.value
    if (!legacyOk) {
        if (suggested == null) return SettlementValidation.PairNotSuggested
        if (amount > suggested.amount.value) return SettlementValidation.ExceedsSuggestion(suggested.amount.value)
    }
    return SettlementValidation.Valid(settlement.copy(note = note, amount = Paise(amount)))
}
