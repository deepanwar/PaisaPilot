package com.deepak.paisapilot.domain.activity

import com.deepak.paisapilot.domain.personal.Category
import java.time.LocalDate

enum class ActivityScope { All, Personal }

enum class ActivityType { All, Expenses }

data class ActivityFilter(
    val query: String = "",
    /** First day of the chosen month, or null for all months. */
    val month: LocalDate? = null,
    val scope: ActivityScope = ActivityScope.All,
    val type: ActivityType = ActivityType.All,
    val category: Category? = null,
) {
    val isDefault: Boolean get() = this == ActivityFilter()

    fun cleared(): ActivityFilter = ActivityFilter()
}
