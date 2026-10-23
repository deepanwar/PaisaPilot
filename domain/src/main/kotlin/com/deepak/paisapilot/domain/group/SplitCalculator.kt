package com.deepak.paisapilot.domain.group

import com.deepak.paisapilot.domain.common.StableIdOrder
import com.deepak.paisapilot.domain.money.MoneyLimits
import com.deepak.paisapilot.domain.money.Paise

data class Share(val memberId: String, val paise: Paise)

sealed interface SplitResult {
    data class Success(val shares: List<Share>) : SplitResult

    data object AmountNotPositive : SplitResult

    data object AmountOverLimit : SplitResult

    data object NoBeneficiaries : SplitResult

    data object DuplicateMember : SplitResult
}

sealed interface ExactResult {
    data class Valid(val shares: List<Share>) : ExactResult

    /** Signed: positive = still unassigned, negative = over-assigned. */
    data class Mismatch(val remaining: Long) : ExactResult

    data object AmountNotPositive : ExactResult

    data object AmountOverLimit : ExactResult

    data object NoBeneficiaries : ExactResult

    data object DuplicateMember : ExactResult

    data object NegativeShare : ExactResult

    data object Overflow : ExactResult
}

/**
 * Equal split among the selected beneficiaries only. The first `total % n` members in
 * [StableIdOrder] get one extra paisa. Zero-paise rows are kept.
 */
fun splitEqually(total: Paise, beneficiaryIds: List<String>): SplitResult {
    if (total.value <= 0L) return SplitResult.AmountNotPositive
    if (total.value > MoneyLimits.MAX_ENTRY_PAISE) return SplitResult.AmountOverLimit
    if (beneficiaryIds.isEmpty()) return SplitResult.NoBeneficiaries
    if (beneficiaryIds.map { it.lowercase() }.toSet().size != beneficiaryIds.size) {
        return SplitResult.DuplicateMember
    }
    val sorted = beneficiaryIds.sortedWith(StableIdOrder)
    val count = sorted.size
    val base = total.value / count
    val extra = (total.value % count).toInt()
    return SplitResult.Success(
        sorted.mapIndexed { index, id -> Share(id, Paise(if (index < extra) base + 1 else base)) },
    )
}

/** Sum of entered shares minus nothing: `total - assigned`. Throws [ArithmeticException] on overflow. */
fun remaining(total: Paise, shares: List<Share>): Long {
    var assigned = 0L
    for (share in shares) assigned = Math.addExact(assigned, share.paise.value)
    return Math.subtractExact(total.value, assigned)
}

/** Exact-amount split: shares must be non-negative and sum exactly to [total]. Never rescales. */
fun validateExact(total: Paise, shares: List<Share>): ExactResult {
    if (total.value <= 0L) return ExactResult.AmountNotPositive
    if (total.value > MoneyLimits.MAX_ENTRY_PAISE) return ExactResult.AmountOverLimit
    if (shares.isEmpty()) return ExactResult.NoBeneficiaries
    if (shares.map { it.memberId.lowercase() }.toSet().size != shares.size) {
        return ExactResult.DuplicateMember
    }
    if (shares.any { it.paise.value < 0L }) return ExactResult.NegativeShare
    val left = try {
        remaining(total, shares)
    } catch (_: ArithmeticException) {
        return ExactResult.Overflow
    }
    if (left != 0L) return ExactResult.Mismatch(left)
    return ExactResult.Valid(shares.sortedWith(compareBy(StableIdOrder) { it.memberId }))
}
