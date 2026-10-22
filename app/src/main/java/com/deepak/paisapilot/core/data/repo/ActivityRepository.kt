package com.deepak.paisapilot.core.data.repo

import com.deepak.paisapilot.domain.activity.ActivityItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Single source for the Activity feed. M3 and M4 add shared-expense and settlement flows to the
 * combine here without changing callers.
 */
interface ActivityRepository {
    fun observeAll(): Flow<List<ActivityItem>>
}

class DefaultActivityRepository(
    private val personal: PersonalExpenseRepository,
) : ActivityRepository {
    override fun observeAll(): Flow<List<ActivityItem>> =
        personal.observeAll().map { rows -> rows.map { ActivityItem.Personal(it) } }
}
