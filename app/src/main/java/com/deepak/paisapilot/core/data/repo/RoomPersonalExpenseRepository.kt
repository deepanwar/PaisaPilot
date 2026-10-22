package com.deepak.paisapilot.core.data.repo

import android.database.sqlite.SQLiteConstraintException
import com.deepak.paisapilot.core.data.db.dao.PersonalExpenseDao
import com.deepak.paisapilot.core.data.db.mapping.toDomain
import com.deepak.paisapilot.core.data.db.mapping.toEntity
import com.deepak.paisapilot.domain.money.Paise
import com.deepak.paisapilot.domain.personal.PersonalExpense
import com.deepak.paisapilot.domain.personal.PersonalValidation
import com.deepak.paisapilot.domain.personal.validatePersonalExpense
import com.deepak.paisapilot.domain.time.MonthRange
import java.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomPersonalExpenseRepository(
    private val dao: PersonalExpenseDao,
    private val clock: Clock,
) : PersonalExpenseRepository {

    override suspend fun create(expense: PersonalExpense): SaveResult {
        val now = clock.millis()
        return insert(expense.copy(createdAt = now, updatedAt = now))
    }

    override suspend fun restore(expense: PersonalExpense): SaveResult = insert(expense)

    private suspend fun insert(expense: PersonalExpense): SaveResult {
        val valid = when (val result = validatePersonalExpense(expense)) {
            is PersonalValidation.Valid -> result.expense
            else -> return SaveResult.Invalid(result)
        }
        return try {
            dao.insert(valid.toEntity())
            SaveResult.Created
        } catch (_: SQLiteConstraintException) {
            SaveResult.AlreadySaved
        }
    }

    override suspend fun update(expense: PersonalExpense): UpdateResult {
        val valid = when (val result = validatePersonalExpense(expense)) {
            is PersonalValidation.Valid -> result.expense
            else -> return UpdateResult.Invalid(result)
        }
        val existing = dao.getById(valid.id) ?: return UpdateResult.NotFound
        val changed = dao.update(
            valid.copy(createdAt = existing.createdAt, updatedAt = clock.millis()).toEntity(),
        )
        return if (changed > 0) UpdateResult.Updated else UpdateResult.NotFound
    }

    override suspend fun delete(id: String): Boolean = dao.delete(id) > 0

    override fun observe(id: String): Flow<PersonalExpense?> =
        dao.observeById(id).map { it?.toDomain() }

    override fun observeAll(): Flow<List<PersonalExpense>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeMonth(range: MonthRange): Flow<List<PersonalExpense>> =
        dao.observeBetween(range.start.toEpochDay(), range.endInclusive.toEpochDay())
            .map { rows -> rows.map { it.toDomain() } }

    override fun observeMonthTotal(range: MonthRange): Flow<Paise> =
        dao.observeTotalBetween(range.start.toEpochDay(), range.endInclusive.toEpochDay())
            .map { Paise(it) }
}
