package com.deepak.paisapilot.feature.personal.form

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepak.paisapilot.core.data.IdGenerator
import com.deepak.paisapilot.core.data.repo.PersonalExpenseRepository
import com.deepak.paisapilot.core.data.repo.SaveResult
import com.deepak.paisapilot.core.data.repo.UpdateResult
import com.deepak.paisapilot.domain.money.AmountResult
import com.deepak.paisapilot.domain.money.formatInr
import com.deepak.paisapilot.domain.money.parseInrAmount
import com.deepak.paisapilot.domain.personal.Category
import com.deepak.paisapilot.domain.personal.MAX_NOTE_LENGTH
import com.deepak.paisapilot.domain.personal.MAX_TITLE_LENGTH
import com.deepak.paisapilot.domain.personal.PersonalExpense
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val KEY_EXPENSE_ID = "expenseId"
private const val KEY_RECORD_ID = "recordId"
private const val KEY_STARTED = "draftStarted"
private const val KEY_AMOUNT = "draftAmount"
private const val KEY_TITLE = "draftTitle"
private const val KEY_CATEGORY = "draftCategory"
private const val KEY_DAY = "draftDay"
private const val KEY_NOTE = "draftNote"

private data class Draft(
    val amountText: String,
    val title: String,
    val category: Category,
    val date: LocalDate,
    val note: String,
)

@HiltViewModel
class ExpenseFormViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: PersonalExpenseRepository,
    private val clock: Clock,
    idGenerator: IdGenerator,
) : ViewModel() {
    private val expenseId: String? = savedStateHandle[KEY_EXPENSE_ID]
    private val recordId: String = savedStateHandle.get<String>(KEY_RECORD_ID)
        ?: (expenseId ?: idGenerator.newId()).also { savedStateHandle[KEY_RECORD_ID] = it }
    private var baseline = Draft("", "", Category.Other, LocalDate.now(clock), "")
    private var attemptedSave = false

    private val _state = MutableStateFlow(
        derive(
            ExpenseFormUiState(
                recordId = recordId,
                isEdit = expenseId != null,
                loadState = if (expenseId != null) FormLoadState.Loading else FormLoadState.Ready,
                date = baseline.date,
            ).withDraft(restoredDraft() ?: baseline),
        ),
    )
    val state: StateFlow<ExpenseFormUiState> = _state.asStateFlow()

    init {
        if (expenseId != null) loadForEdit(expenseId)
    }

    fun onRetryLoad() {
        val id = expenseId ?: return
        _state.value = derive(_state.value.copy(loadState = FormLoadState.Loading))
        loadForEdit(id)
    }

    fun onAmountChange(value: String) = edit { it.copy(amountText = value) }

    fun onTitleChange(value: String) = edit { it.copy(title = value) }

    fun onCategoryChange(value: Category) = edit { it.copy(category = value) }

    fun onDateChange(value: LocalDate) = edit { it.copy(date = value) }

    fun onNoteChange(value: String) = edit { it.copy(note = value) }

    fun onBackRequested() {
        _state.value = derive(_state.value.copy(showDiscardDialog = true))
    }

    fun onKeepEditing() {
        _state.value = derive(_state.value.copy(showDiscardDialog = false))
    }

    fun onSavedHandled() {
        _state.value = derive(_state.value.copy(saved = false))
    }

    fun onSave() {
        val current = _state.value
        if (current.isSaving || current.saved || current.loadState != FormLoadState.Ready) return
        val amount = parseInrAmount(current.amountText)
        if (amount !is AmountResult.Success) {
            attemptedSave = true
            _state.value = derive(current)
            return
        }
        if (current.titleTooLong || current.noteTooLong) return
        _state.value = derive(current.copy(isSaving = true, saveFailed = false))
        val expense = PersonalExpense(
            id = recordId,
            amount = amount.paise,
            title = current.title,
            category = current.category,
            date = current.date,
            note = current.note,
            createdAt = 0L,
            updatedAt = 0L,
        )
        viewModelScope.launch {
            try {
                val success = if (current.isEdit) {
                    repository.update(expense) == UpdateResult.Updated
                } else {
                    when (repository.create(expense)) {
                        SaveResult.Created, SaveResult.AlreadySaved -> true
                        is SaveResult.Invalid -> false
                    }
                }
                _state.value = derive(
                    if (success) {
                        _state.value.copy(saved = true)
                    } else {
                        _state.value.copy(isSaving = false, saveFailed = true)
                    },
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.value = derive(_state.value.copy(isSaving = false, saveFailed = true))
            }
        }
    }

    private fun loadForEdit(id: String) {
        viewModelScope.launch {
            try {
                val stored = repository.observe(id).first()
                if (stored == null) {
                    _state.value = derive(_state.value.copy(loadState = FormLoadState.NotFound))
                    return@launch
                }
                baseline = Draft(
                    amountText = formatInr(stored.amount).removePrefix("₹"),
                    title = stored.title.orEmpty(),
                    category = stored.category,
                    date = stored.date,
                    note = stored.note.orEmpty(),
                )
                _state.value = derive(
                    _state.value.copy(loadState = FormLoadState.Ready)
                        .withDraft(restoredDraft() ?: baseline),
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.value = derive(_state.value.copy(loadState = FormLoadState.LoadFailed))
            }
        }
    }

    private fun edit(change: (Draft) -> Draft) {
        val current = _state.value
        if (current.loadState != FormLoadState.Ready) return
        val next = change(current.toDraft())
        persist(next)
        _state.value = derive(current.copy(saveFailed = false).withDraft(next))
    }

    private fun persist(draft: Draft) {
        savedStateHandle[KEY_STARTED] = true
        savedStateHandle[KEY_AMOUNT] = draft.amountText
        savedStateHandle[KEY_TITLE] = draft.title
        savedStateHandle[KEY_CATEGORY] = draft.category.key
        savedStateHandle[KEY_DAY] = draft.date.toEpochDay()
        savedStateHandle[KEY_NOTE] = draft.note
    }

    private fun restoredDraft(): Draft? {
        if (savedStateHandle.get<Boolean>(KEY_STARTED) != true) return null
        return Draft(
            amountText = savedStateHandle[KEY_AMOUNT] ?: "",
            title = savedStateHandle[KEY_TITLE] ?: "",
            category = Category.fromKey(savedStateHandle[KEY_CATEGORY] ?: ""),
            date = savedStateHandle.get<Long>(KEY_DAY)?.let(LocalDate::ofEpochDay)
                ?: baseline.date,
            note = savedStateHandle[KEY_NOTE] ?: "",
        )
    }

    private fun ExpenseFormUiState.toDraft() = Draft(amountText, title, category, date, note)

    private fun ExpenseFormUiState.withDraft(draft: Draft) = copy(
        amountText = draft.amountText,
        title = draft.title,
        category = draft.category,
        date = draft.date,
        note = draft.note,
    )

    private fun derive(state: ExpenseFormUiState): ExpenseFormUiState {
        val parsed = parseInrAmount(state.amountText)
        val amountError = when {
            parsed is AmountResult.Success -> null
            parsed is AmountResult.Empty -> if (attemptedSave) parsed else null
            else -> parsed
        }
        val titleTooLong = state.title.trim().length > MAX_TITLE_LENGTH
        val noteTooLong = state.note.trim().length > MAX_NOTE_LENGTH
        return state.copy(
            amountError = amountError,
            titleTooLong = titleTooLong,
            noteTooLong = noteTooLong,
            canSave = state.loadState == FormLoadState.Ready &&
                parsed is AmountResult.Success &&
                !titleTooLong &&
                !noteTooLong &&
                !state.isSaving &&
                !state.saved,
            isDirty = state.toDraft() != baseline,
        )
    }
}
