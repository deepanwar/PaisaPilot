package com.deepak.paisapilot.core

import com.deepak.paisapilot.domain.activity.ActivityFilter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * One-shot handoff from another tab (Insights) to Activity. Navigation arguments would be
 * overridden by the tab's saved state, so the request is held here and applied once.
 */
@Singleton
class ActivityFilterRequests @Inject constructor() {
    private val _pending = MutableStateFlow<ActivityFilter?>(null)
    val pending: StateFlow<ActivityFilter?> = _pending.asStateFlow()

    fun request(filter: ActivityFilter) {
        _pending.value = filter
    }

    fun take(): ActivityFilter? = _pending.value.also { _pending.value = null }
}
