package com.deepak.paisapilot.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class AmountTextStyleTest {
    @Test
    fun amountStylesUseTabularFigures() {
        assertEquals(AMOUNT_FONT_FEATURE_SETTINGS, AmountTextStyle.fontFeatureSettings)
        assertEquals(AMOUNT_FONT_FEATURE_SETTINGS, Typography.displaySmall.fontFeatureSettings)
        assertEquals(AMOUNT_FONT_FEATURE_SETTINGS, Typography.headlineSmall.fontFeatureSettings)
    }
}
