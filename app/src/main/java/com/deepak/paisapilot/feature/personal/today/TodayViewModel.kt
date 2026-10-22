package com.deepak.paisapilot.feature.personal.today

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepak.paisapilot.core.data.repo.PersonalExpenseRepository
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

const val RECENT_LIMIT: Int = 10
private const val KEY_MONTH_START = "monthStart"
private val EARLIEST_MONTH: LocalDate = LocalDate.of(2000, 1, 1)
private val LATEST_MONTH: LocalDate = LocalDate.of(2100, 12, 1)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TodayViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: PersonalExpenseRepository,
    private val clock: Clock,
) : ViewModel() {
    private val currentMonthStart: LocalDate = monthContaining(LocalDate.now(clock)).start
    private val selectedMonthStart = MutableStateFlow(
        savedStateHandle.get<Long>(KEY_MONTH_START)?.let(LocalDate::ofEpochDay)
            ?: currentMonthStart,
    )
    private val reloads = MutableStateFlow(0)

    val state: StateFlow<TodayUiState> = combine(selectedMonthStart, reloads) { month, _ -> month }
        .flatMapLatest { start -> observe(monthContaining(start)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState.Loading)

    fun onPreviousMonth() = moveTo(selectedMonthStart.value.minusMonths(1))

    fun onNextMonth() = moveTo(selectedMonthStart.value.plusMonths(1))

    fun onRetry() {
        reloads.value += 1
    }

    private fun moveTo(start: LocalDate) {
        if (start < EARLIEST_MONTH || start > LATEST_MONTH) return
        savedStateHandle[KEY_MONTH_START] = start.toEpochDay()
        selectedMonthStart.value = start
    }

    private fun observe(range: MonthRange) = combine(
        repository.observeMonth(range),
        repository.observeMonthTotal(range),
    ) { all, total ->
        TodayUiState.Content(
            month = range,
            total = total,
            rows = all.take(RECENT_LIMIT).map {
                TodayRowUi(it.id, it.title, it.category, it.date, it.amount)
            },
            hasMore = all.size > RECENT_LIMIT,
            isCurrentMonth = range.start == currentMonthStart,
            canGoPrevious = range.start > EARLIEST_MONTH,
            canGoNext = range.start < LATEST_MONTH,
        ) as TodayUiState
    }.catch { emit(TodayUiState.Error(range)) }
}
