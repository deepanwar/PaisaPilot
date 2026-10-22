package com.deepak.paisapilot.core.undo

import com.deepak.paisapilot.domain.money.Paise
import com.deepak.paisapilot.domain.personal.Category
import com.deepak.paisapilot.domain.personal.PersonalExpense
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class UndoDeleteCoordinatorTest {
    private fun expense(id: String) =
        PersonalExpense(id, Paise(100), null, Category.Other, LocalDate.of(2026, 10, 20), null, 1L, 2L)

    @Test
    fun offerThenTakeReturnsSameSnapshotAndClears() {
        val coordinator = UndoDeleteCoordinator()
        val e = expense("a")
        coordinator.offer(e)
        assertSame(e, coordinator.take())
        assertNull(coordinator.pending.value)
        assertNull(coordinator.take())
    }

    @Test
    fun newerOfferReplacesOlder() {
        val coordinator = UndoDeleteCoordinator()
        coordinator.offer(expense("a"))
        coordinator.offer(expense("b"))
        assertEquals("b", coordinator.pending.value?.id)
    }

    @Test
    fun staleClearDoesNotDropNewerSnapshot() {
        val coordinator = UndoDeleteCoordinator()
        val old = expense("a")
        coordinator.offer(old)
        coordinator.offer(expense("b"))
        coordinator.clear(old)
        assertEquals("b", coordinator.pending.value?.id)
        coordinator.clear(coordinator.pending.value!!)
        assertNull(coordinator.pending.value)
    }
}
