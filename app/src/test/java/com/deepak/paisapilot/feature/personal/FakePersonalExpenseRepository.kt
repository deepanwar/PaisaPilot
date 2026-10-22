package com.deepak.paisapilot.feature.personal

import com.deepak.paisapilot.core.data.repo.PersonalExpenseRepository
import com.deepak.paisapilot.core.data.repo.SaveResult
import com.deepak.paisapilot.core.data.repo.UpdateResult
import com.deepak.paisapilot.domain.money.Paise
import com.deepak.paisapilot.domain.personal.PersonalExpense
import com.deepak.paisapilot.domain.time.MonthRange
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakePersonalExpenseRepository : PersonalExpenseRepository {
    val rows = MutableStateFlow<Map<String, PersonalExpense>>(emptyMap())
    var creates = 0
    var updates = 0
    var throwOnWrite = false
    var throwOnRead = false

    override suspend fun create(expense: PersonalExpense): SaveResult {
        creates += 1
        if (throwOnWrite) throw IOException("write failed")
        if (rows.value.containsKey(expense.id)) return SaveResult.AlreadySaved
        rows.value = rows.value + (expense.id to expense)
        return SaveResult.Created
    }

    override suspend fun update(expense: PersonalExpense): UpdateResult {
        updates += 1
        if (throwOnWrite) throw IOException("write failed")
        if (!rows.value.containsKey(expense.id)) return UpdateResult.NotFound
        rows.value = rows.value + (expense.id to expense)
        return UpdateResult.Updated
    }

    override suspend fun delete(id: String): Boolean {
        val had = rows.value.containsKey(id)
        rows.value = rows.value - id
        return had
    }

    override suspend fun restore(expense: PersonalExpense): SaveResult = create(expense)

    override fun observe(id: String): Flow<PersonalExpense?> {
        if (throwOnRead) throw IOException("read failed")
        return rows.map { it[id] }
    }

    override fun observeMonth(range: MonthRange): Flow<List<PersonalExpense>> =
        rows.map { all ->
            all.values.filter { it.date in range.start..range.endInclusive }
        }

    override fun observeMonthTotal(range: MonthRange): Flow<Paise> =
        observeMonth(range).map { list -> Paise(list.sumOf { it.amount.value }) }
}
