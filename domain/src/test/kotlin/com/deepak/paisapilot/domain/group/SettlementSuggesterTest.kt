package com.deepak.paisapilot.domain.group

import com.deepak.paisapilot.domain.money.Paise
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettlementSuggesterTest {
    private val a = "00000000-0000-0000-0000-00000000000a"
    private val b = "00000000-0000-0000-0000-00000000000b"
    private val c = "00000000-0000-0000-0000-00000000000c"
    private val d = "00000000-0000-0000-0000-00000000000d"

    // AC-BAL-01
    @Test
    fun debtorsPayCreditor() {
        val result = suggestSettlements(mapOf(a to 10_000L, b to -6_000L, c to -4_000L))
        assertEquals(
            listOf(
                SuggestedSettlement(b, a, Paise(6_000)),
                SuggestedSettlement(c, a, Paise(4_000)),
            ),
            result,
        )
    }

    @Test
    fun balancedGroupHasNoSuggestions() {
        assertTrue(suggestSettlements(mapOf(a to 0L, b to 0L)).isEmpty())
    }

    @Test
    fun tiesBreakByStableId() {
        val result = suggestSettlements(mapOf(d to 500L, c to 500L, a to -500L, b to -500L))
        assertEquals(
            listOf(
                SuggestedSettlement(a, c, Paise(500)),
                SuggestedSettlement(b, d, Paise(500)),
            ),
            result,
        )
    }

    // AC-BAL-07
    @Test
    fun orderIndependentAndZeroesNets() {
        val random = Random(7)
        val ids = listOf(a, b, c, d)
        repeat(200) {
            val raw = ids.dropLast(1).map { it to random.nextLong(-5_000, 5_000) }
            val nets = raw.toMap() + (ids.last() to -raw.sumOf { it.second })
            val expected = suggestSettlements(nets)
            val shuffled = LinkedHashMap<String, Long>().apply { nets.entries.shuffled(random).forEach { put(it.key, it.value) } }
            assertEquals(expected, suggestSettlements(shuffled))
            val after = HashMap(nets)
            for (s in expected) {
                after.merge(s.fromMemberId, s.amount.value, Long::plus)
                after.merge(s.toMemberId, -s.amount.value, Long::plus)
            }
            assertTrue(after.values.all { it == 0L })
        }
    }
}
