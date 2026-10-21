package com.deepak.paisapilot.domain.personal

import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryTest {
    @Test
    fun keysRoundTrip() {
        Category.entries.forEach { assertEquals(it, Category.fromKey(it.key)) }
    }

    @Test
    fun keysAreUnique() {
        assertEquals(Category.entries.size, Category.entries.map { it.key }.toSet().size)
    }

    @Test
    fun unknownKeyFallsBackToOther() {
        assertEquals(Category.Other, Category.fromKey("crypto"))
        assertEquals(Category.Other, Category.fromKey(""))
    }

    @Test
    fun hasEightStarterCategories() {
        assertEquals(8, Category.entries.size)
    }
}
