package com.deepak.paisapilot.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class DomainMarkerTest {
    @Test
    fun staysInTheDomainPackage() {
        assertEquals(
            "com.deepak.paisapilot.domain",
            DomainMarker::class.java.packageName,
        )
    }
}
