package com.deepak.paisapilot.domain.profile

import org.junit.Assert.assertEquals
import org.junit.Test

class DisplayNameRulesTest {
    @Test
    fun trimsAValidName() {
        assertEquals(
            DisplayNameResult.Valid("Sam"),
            resolveDisplayName("  Sam  ", NameSubmit.Start),
        )
    }

    @Test
    fun allows40CharactersAndRejects41() {
        val forty = "a".repeat(40)
        val fortyOne = "a".repeat(41)
        assertEquals(DisplayNameResult.Valid(forty), resolveDisplayName(forty, NameSubmit.Start))
        assertEquals(DisplayNameResult.TooLong, resolveDisplayName(fortyOne, NameSubmit.Start))
    }

    @Test
    fun blankStartIsAnErrorAndBlankSkipStoresMe() {
        assertEquals(DisplayNameResult.Blank, resolveDisplayName("   ", NameSubmit.Start))
        assertEquals(DisplayNameResult.Valid("Me"), resolveDisplayName("   ", NameSubmit.Skip))
    }
}
