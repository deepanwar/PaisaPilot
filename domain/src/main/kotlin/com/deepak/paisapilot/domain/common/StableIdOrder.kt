package com.deepak.paisapilot.domain.common

/**
 * Single definition of "stable ID order": compare the lowercase canonical UUID string.
 * Not `UUID.compareTo`, which compares signed 64-bit halves and can disagree with string order.
 */
object StableIdOrder : Comparator<String> {
    override fun compare(a: String, b: String): Int = a.lowercase().compareTo(b.lowercase())
}

fun Iterable<String>.sortedStable(): List<String> = sortedWith(StableIdOrder)
