package com.deepak.paisapilot.core.data

import java.util.UUID

fun interface IdGenerator {
    fun newId(): String
}

/** Lowercase canonical UUID string, the form StableIdOrder relies on. */
object UuidIdGenerator : IdGenerator {
    override fun newId(): String = UUID.randomUUID().toString()
}
