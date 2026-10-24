package com.deepak.paisapilot.domain.group

/**
 * "Review balances" trigger (ARCHITECTURE.md G4). [expenseNets] is E, [nets] is N including
 * settlements. True if any member has N != 0 with the opposite sign to E, or E == 0 and N != 0.
 * Recorded settlements are never changed; this only advises.
 */
fun needsBalanceReview(expenseNets: Map<String, Long>, nets: Map<String, Long>): Boolean =
    nets.any { (memberId, n) ->
        val e = expenseNets[memberId] ?: 0L
        n != 0L && (e == 0L || Integer.signum(n.compareTo(0L)) != Integer.signum(e.compareTo(0L)))
    }
