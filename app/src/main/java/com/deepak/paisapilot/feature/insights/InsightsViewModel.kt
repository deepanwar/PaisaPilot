package com.deepak.paisapilot.feature.insights

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepak.paisapilot.core.ActivityFilterRequests
import com.deepak.paisapilot.core.data.repo.PersonalExpenseRepository
import com.deepak.paisapilot.domain.activity.ActivityFilter
import com.deepak.paisapilot.domain.activity.ActivityScope
import com.deepak.paisapilot.domain.personal.Category
import com.deepak.paisapilot.domain.personal.summarizeMonth
import com.deepak.paisapilot.domain.time.MonthRange
import com.deepak.paisapilot.domain.time.monthContaining
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

const val INSIGHTS_RECENT_LIMIT: Int = 5
private const val KEY_MONTH_START = "insightsMonthStart"
private val EARLIEST_MONTH: LocalDate = LocalDate.of(2000, 1, 1)
private val LATEST_MONTH: LocalDate = LocalDate.of(2100, 12, 1)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: PersonalExpenseRepository,
    private val filterRequests: ActivityFilterRequests,
    clock: Clock,
) : ViewModel() {
    private val currentMonthStart: LocalDate = monthContaining(LocalDate.now(clock)).start
    private val selectedMonthStart = MutableStateFlow(
        savedStateHandle.get<Long>(KEY_MONTH_START)?.let(LocalDate::ofEpochDay)
            ?: currentMonthStart,
    )
    private val reloads = MutableStateFlow(0)

    val state: StateFlow<InsightsUiState> = combine(selectedMonthStart, reloads) { month, _ -> month }
        .flatMapLatest { start -> observe(monthContaining(start)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InsightsUiState.Loading)

    fun onPreviousMonth() = moveTo(selectedMonthStart.value.minusMonths(1))

    fun onNextMonth() = moveTo(selectedMonthStart.value.plusMonths(1))

    fun onRetry() {
        reloads.value += 1
    }

    /** Asks Activity to show this category in the month on screen. The caller then opens the tab. */
    fun onCategorySelected(category: Category) {
        filterRequests.request(
            ActivityFilter(
                month = selectedMonthStart.value,
                scope = ActivityScope.Personal,
                category = category,
            ),
        )
    }

    private fun moveTo(start: LocalDate) {
        if (start < EARLIEST_MONTH || start > LATEST_MONTH) return
        savedStateHandle[KEY_MONTH_START] = start.toEpochDay()
        selectedMonthStart.value = start
    }

    private fun observe(range: MonthRange) = repository.observeMonth(range)
        .map { expenses ->
            val summary = summarizeMonth(expenses)
            InsightsUiState.Content(
                month = range,
                isCurrentMonth = range.start == currentMonthStart,
                total = summary.total,
                count = summary.count,
                categories = summary.categories.map {
                    CategoryRowUi(
                        category = it.category,
                        amount = it.amount,
                        percent = it.percent,
                        fraction = it.amount.value.toFloat() / summary.total.value.toFloat(),
                    )
                },
                recent = expenses.take(INSIGHTS_RECENT_LIMIT).map {
                    RecentRowUi(it.id, it.title, it.category, it.date, it.amount)
                },
                canGoPrevious = range.start > EARLIEST_MONTH,
                canGoNext = range.start < LATEST_MONTH,
            ) as InsightsUiState
        }
        .catch { emit(InsightsUiState.Error(range)) }
}
