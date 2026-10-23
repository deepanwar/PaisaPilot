package com.deepak.paisapilot.domain.common

import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StableIdOrderTest {
    // High bit set: UUID.compareTo (signed halves) ranks it BEFORE the low one; string order ranks it after.
    private val low = "10000000-0000-0000-0000-000000000000"
    private val high = "f0000000-0000-0000-0000-000000000000"

    @Test
    fun ordersByCanonicalString_notUuidCompareTo() {
        assertTrue(UUID.fromString(high) < UUID.fromString(low))
        assertEquals(listOf(low, high), listOf(high, low).sortedStable())
    }

    @Test
    fun ignoresCase() {
        assertEquals(0, StableIdOrder.compare(high, high.uppercase()))
        assertEquals(listOf(low, high.uppercase()), listOf(high.uppercase(), low).sortedStable())
    }

    @Test
    fun deterministicForAnyInputOrder() {
        val ids = listOf("c", "a", "b")
        assertEquals(listOf("a", "b", "c"), ids.sortedStable())
        assertEquals(listOf("a", "b", "c"), ids.reversed().sortedStable())
    }
}
