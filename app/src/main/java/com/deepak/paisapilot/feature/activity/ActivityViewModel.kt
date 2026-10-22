package com.deepak.paisapilot.feature.activity

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepak.paisapilot.core.ActivityFilterRequests
import com.deepak.paisapilot.core.data.repo.ActivityRepository
import com.deepak.paisapilot.domain.activity.ActivityFilter
import com.deepak.paisapilot.domain.activity.ActivityItem
import com.deepak.paisapilot.domain.activity.ActivityScope
import com.deepak.paisapilot.domain.activity.ActivityType
import com.deepak.paisapilot.domain.activity.applyActivityFilter
import com.deepak.paisapilot.domain.money.Paise
import com.deepak.paisapilot.domain.personal.Category
import com.deepak.paisapilot.feature.personal.CategoryLabelProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val KEY_QUERY = "activityQuery"
private const val KEY_MONTH = "activityMonth"
private const val KEY_SCOPE = "activityScope"
private const val KEY_TYPE = "activityType"
private const val KEY_CATEGORY = "activityCategory"

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    repository: ActivityRepository,
    private val labels: CategoryLabelProvider,
    private val filterRequests: ActivityFilterRequests,
) : ViewModel() {
    private val filter = MutableStateFlow(restoreFilter())
    private val reloads = MutableStateFlow(0)

    val state: StateFlow<ActivityUiState> = reloads
        .flatMapLatest {
            combine(filter, repository.observeAll()) { f, items -> build(f, items) }
                .catch { emit(ActivityUiState.Error) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ActivityUiState.Loading)

    init {
        viewModelScope.launch {
            filterRequests.pending.filterNotNull().collect {
                filterRequests.take()?.let { requested -> update { requested } }
            }
        }
    }

    fun onQueryChange(value: String) = update { it.copy(query = value) }

    fun onScopeChange(value: ActivityScope) = update { it.copy(scope = value) }

    fun onTypeChange(value: ActivityType) = update { it.copy(type = value) }

    fun onMonthChange(value: LocalDate?) = update { it.copy(month = value?.withDayOfMonth(1)) }

    fun onCategoryChange(value: Category?) = update { it.copy(category = value) }

    fun onClearFilters() = update { it.cleared() }

    fun onRetry() {
        reloads.value += 1
    }

    private fun update(change: (ActivityFilter) -> ActivityFilter) {
        val next = change(filter.value)
        persist(next)
        filter.value = next
    }

    private fun build(f: ActivityFilter, items: List<ActivityItem>): ActivityUiState {
        val matches = applyActivityFilter(items, f, labels::label)
        val rows = matches.filterIsInstance<ActivityItem.Personal>().map {
            ActivityRowUi(it.id, it.expense.title, it.expense.category, it.date, it.amount)
        }
        return ActivityUiState.Content(
            filter = f,
            rows = rows,
            resultCount = matches.size,
            hasAnyData = items.isNotEmpty(),
            availableMonths = items.map { it.date.withDayOfMonth(1) }.distinct()
                .sortedDescending(),
            scopeTotal = if (f.scope == ActivityScope.Personal) {
                Paise(matches.sumOf { it.amount.value })
            } else {
                null
            },
        )
    }

    private fun persist(f: ActivityFilter) {
        savedStateHandle[KEY_QUERY] = f.query
        savedStateHandle[KEY_MONTH] = f.month?.toEpochDay() ?: NO_MONTH
        savedStateHandle[KEY_SCOPE] = f.scope.name
        savedStateHandle[KEY_TYPE] = f.type.name
        savedStateHandle[KEY_CATEGORY] = f.category?.key ?: ""
    }

    private fun restoreFilter(): ActivityFilter {
        val month = savedStateHandle.get<Long>(KEY_MONTH)
        val categoryKey = savedStateHandle.get<String>(KEY_CATEGORY).orEmpty()
        return ActivityFilter(
            query = savedStateHandle.get<String>(KEY_QUERY).orEmpty(),
            month = month?.takeIf { it != NO_MONTH }?.let(LocalDate::ofEpochDay),
            scope = savedStateHandle.get<String>(KEY_SCOPE)
                ?.let { runCatching { ActivityScope.valueOf(it) }.getOrNull() }
                ?: ActivityScope.All,
            type = savedStateHandle.get<String>(KEY_TYPE)
                ?.let { runCatching { ActivityType.valueOf(it) }.getOrNull() }
                ?: ActivityType.All,
            category = categoryKey.takeIf { it.isNotEmpty() }?.let(Category::fromKey),
        )
    }

    private companion object {
        const val NO_MONTH = Long.MIN_VALUE
    }
}
