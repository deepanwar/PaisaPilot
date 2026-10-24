package com.deepak.paisapilot.feature.groups

import com.deepak.paisapilot.core.data.repo.GroupActionResult
import com.deepak.paisapilot.core.data.repo.SharedExpenseRepository
import com.deepak.paisapilot.core.data.repo.SharedWriteResult
import com.deepak.paisapilot.domain.group.SharedExpense
import com.deepak.paisapilot.domain.group.SharedValidation
import com.deepak.paisapilot.domain.group.validateSharedExpense
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeSharedExpenseRepository : SharedExpenseRepository {
    val expenses = MutableStateFlow<List<SharedExpense>>(emptyList())
    var creates = 0
    var updates = 0
    var throwOnWrite = false

    /** When set, create and update return this instead of touching the store. */
    var forcedResult: SharedWriteResult? = null
    var lastWritten: SharedExpense? = null

    override suspend fun create(expense: SharedExpense): SharedWriteResult {
        creates += 1
        if (throwOnWrite) throw IOException("write failed")
        forcedResult?.let { return it }
        val valid = when (val r = validateSharedExpense(expense)) {
            is SharedValidation.Valid -> r.expense
            else -> return SharedWriteResult.Invalid(r)
        }
        if (expenses.value.any { it.id == valid.id }) return SharedWriteResult.AlreadySaved
        lastWritten = valid
        expenses.value = expenses.value + valid
        return SharedWriteResult.Saved
    }

    override suspend fun update(expense: SharedExpense): SharedWriteResult {
        updates += 1
        if (throwOnWrite) throw IOException("write failed")
        forcedResult?.let { return it }
        val valid = when (val r = validateSharedExpense(expense)) {
            is SharedValidation.Valid -> r.expense
            else -> return SharedWriteResult.Invalid(r)
        }
        if (expenses.value.none { it.id == valid.id }) return SharedWriteResult.NotFound
        lastWritten = valid
        expenses.value = expenses.value.map { if (it.id == valid.id) valid else it }
        return SharedWriteResult.Saved
    }

    override suspend fun delete(id: String): GroupActionResult {
        if (throwOnWrite) throw IOException("write failed")
        if (expenses.value.none { it.id == id }) return GroupActionResult.NotFound
        expenses.value = expenses.value.filterNot { it.id == id }
        return GroupActionResult.Done
    }

    override fun observe(id: String): Flow<SharedExpense?> =
        expenses.map { list -> list.firstOrNull { it.id == id } }

    override fun observeGroup(groupId: String): Flow<List<SharedExpense>> =
        expenses.map { list -> list.filter { it.groupId == groupId } }
}
