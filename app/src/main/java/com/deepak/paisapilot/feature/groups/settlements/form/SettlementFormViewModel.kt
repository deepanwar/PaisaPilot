package com.deepak.paisapilot.feature.groups.settlements.form

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepak.paisapilot.core.data.IdGenerator
import com.deepak.paisapilot.core.data.prefs.SettingsRepository
import com.deepak.paisapilot.core.data.repo.GroupRepository
import com.deepak.paisapilot.core.data.repo.SettlementRepository
import com.deepak.paisapilot.core.data.repo.SettlementWriteResult
import com.deepak.paisapilot.core.data.repo.SharedExpenseRepository
import com.deepak.paisapilot.domain.group.Member
import com.deepak.paisapilot.domain.group.Settlement
import com.deepak.paisapilot.domain.group.SettlementValidation
import com.deepak.paisapilot.domain.group.SharedExpense
import com.deepak.paisapilot.domain.group.computeNets
import com.deepak.paisapilot.domain.group.suggestSettlements
import com.deepak.paisapilot.domain.group.validateSettlement
import com.deepak.paisapilot.domain.money.AmountResult
import com.deepak.paisapilot.domain.money.Paise
import com.deepak.paisapilot.domain.money.formatInr
import com.deepak.paisapilot.domain.money.parseInrAmount
import com.deepak.paisapilot.domain.personal.MAX_NOTE_LENGTH
import com.deepak.paisapilot.feature.groups.shared.form.MemberChoice
import com.deepak.paisapilot.feature.personal.form.FormLoadState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val KEY_GROUP_ID = "groupId"
private const val KEY_SETTLEMENT_ID = "settlementId"
private const val KEY_PREFILL_FROM = "fromId"
private const val KEY_PREFILL_TO = "toId"
private const val KEY_PREFILL_AMOUNT = "amountPaise"
private const val KEY_RECORD_ID = "recordId"
private const val KEY_STARTED = "started"
private const val KEY_FROM = "draftFrom"
private const val KEY_TO = "draftTo"
private const val KEY_AMOUNT = "draftAmount"
private const val KEY_DAY = "draftDay"
private const val KEY_NOTE = "draftNote"

private data class Draft(
    val fromId: String?,
    val toId: String?,
    val amountText: String,
    val date: LocalDate,
    val note: String,
)

@HiltViewModel
class SettlementFormViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: SettlementRepository,
    expenseRepository: SharedExpenseRepository,
    groupRepository: GroupRepository,
    settings: SettingsRepository,
    private val clock: Clock,
    idGenerator: IdGenerator,
) : ViewModel() {
    private val groupId: String = checkNotNull(savedStateHandle[KEY_GROUP_ID])
    private val settlementId: String? = savedStateHandle[KEY_SETTLEMENT_ID]
    private val recordId: String = savedStateHandle.get<String>(KEY_RECORD_ID)
        ?: (settlementId ?: idGenerator.newId()).also { savedStateHandle[KEY_RECORD_ID] = it }

    private var members: List<Member> = emptyList()
    private var membersLoaded = false
    private var groupArchived = false

    /** G7: the owner shows the live profile name, not the stored copy. */
    private var ownerName = ""
    private var expenses: List<SharedExpense> = emptyList()
    private var settlements: List<Settlement> = emptyList()
    private var ledgerLoaded = false

    private var stored: Settlement? = null
    private var storedLoaded = settlementId == null
    private var storedLoadFailed = false
    private var storedMissing = false

    private var baseline = Draft(null, null, "", LocalDate.now(clock), "")
    private var draft: Draft = restoredDraft() ?: baseline
    private var attemptedSave = false
    private var saveError: SettlementSaveError? = null
    private var isSaving = false
    private var saved = false
    private var showDiscardDialog = false

    private val _state = MutableStateFlow(compute())
    val state: StateFlow<SettlementFormUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            groupRepository.observeMembers(groupId).collect { list ->
                members = list
                membersLoaded = true
                applyDefaultsIfNeeded()
                publish()
            }
        }
        viewModelScope.launch {
            settings.settings.collect {
                ownerName = it.displayName
                publish()
            }
        }
        viewModelScope.launch {
            groupRepository.observeGroup(groupId).collect { group ->
                groupArchived = group?.isArchived == true
                publish()
            }
        }
        viewModelScope.launch {
            combine(expenseRepository.observeGroup(groupId), repository.observeGroup(groupId)) { e, s -> e to s }
                .collect { (e, s) ->
                    expenses = e
                    settlements = s
                    ledgerLoaded = true
                    applyDefaultsIfNeeded()
                    publish()
                }
        }
        if (settlementId != null) loadForEdit(settlementId)
    }

    fun onRetryLoad() {
        val id = settlementId ?: return
        storedLoadFailed = false
        publish()
        loadForEdit(id)
    }

    fun onFromChange(id: String) = edit { it.copy(fromId = id) }

    fun onToChange(id: String) = edit { it.copy(toId = id) }

    fun onAmountChange(value: String) = edit { it.copy(amountText = value) }

    fun onDateChange(value: LocalDate) = edit { it.copy(date = value) }

    fun onNoteChange(value: String) = edit { it.copy(note = value) }

    fun onBackRequested() {
        showDiscardDialog = true
        publish()
    }

    fun onKeepEditing() {
        showDiscardDialog = false
        publish()
    }

    fun onSavedHandled() {
        saved = false
        publish()
    }

    fun onSave() {
        val current = _state.value
        if (isSaving || saved || current.loadState != FormLoadState.Ready) return
        if (!current.canSave) {
            attemptedSave = true
            publish()
            return
        }
        val settlement = Settlement(
            id = recordId,
            groupId = groupId,
            fromMemberId = checkNotNull(draft.fromId),
            toMemberId = checkNotNull(draft.toId),
            amount = (parseInrAmount(draft.amountText) as AmountResult.Success).paise,
            occurredEpochDay = draft.date.toEpochDay(),
            note = draft.note,
            createdAt = 0L,
            updatedAt = 0L,
        )
        isSaving = true
        saveError = null
        publish()
        viewModelScope.launch {
            try {
                val result = if (settlementId != null) repository.update(settlement) else repository.create(settlement)
                isSaving = false
                when (result) {
                    SettlementWriteResult.Saved, SettlementWriteResult.AlreadySaved -> saved = true
                    SettlementWriteResult.MemberInvalid, SettlementWriteResult.NotFound ->
                        saveError = SettlementSaveError.MemberChanged
                    SettlementWriteResult.GroupArchived -> groupArchived = true
                    is SettlementWriteResult.Invalid -> saveError = when (val error = result.error) {
                        is SettlementValidation.ExceedsSuggestion -> SettlementSaveError.BalanceChanged(error.max)
                        SettlementValidation.PairNotSuggested -> SettlementSaveError.BalanceChanged(null)
                        else -> SettlementSaveError.Failed
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                isSaving = false
                saveError = SettlementSaveError.Failed
            }
            publish()
        }
    }

    private fun loadForEdit(id: String) {
        viewModelScope.launch {
            try {
                val record = repository.observe(id).first()
                if (record == null || record.groupId != groupId) {
                    storedMissing = true
                } else {
                    stored = record
                    baseline = Draft(
                        fromId = record.fromMemberId,
                        toId = record.toMemberId,
                        amountText = plainAmount(record.amount),
                        date = LocalDate.ofEpochDay(record.occurredEpochDay),
                        note = record.note.orEmpty(),
                    )
                    draft = restoredDraft() ?: baseline
                }
                storedLoaded = true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                storedLoadFailed = true
            }
            publish()
        }
    }

    /** Create: prefill from the tapped suggestion, else the first suggestion, unless restored. */
    private fun applyDefaultsIfNeeded() {
        if (settlementId != null || !membersLoaded || !ledgerLoaded) return
        if (savedStateHandle.get<Boolean>(KEY_STARTED) == true) return
        if (draft.fromId != null || draft.toId != null) return
        val fromArg = savedStateHandle.get<String>(KEY_PREFILL_FROM)
        val toArg = savedStateHandle.get<String>(KEY_PREFILL_TO)
        val amountArg = savedStateHandle.get<Long>(KEY_PREFILL_AMOUNT)
        val defaults = if (fromArg != null && toArg != null) {
            draft.copy(fromId = fromArg, toId = toArg, amountText = amountArg?.let { plainAmount(Paise(it)) }.orEmpty())
        } else {
            val first = suggestSettlements(computeNets(expenses, settlements)).firstOrNull() ?: return
            draft.copy(
                fromId = first.fromMemberId,
                toId = first.toMemberId,
                amountText = plainAmount(first.amount),
            )
        }
        baseline = defaults
        draft = defaults
    }

    private fun edit(change: (Draft) -> Draft) {
        if (_state.value.loadState != FormLoadState.Ready || isSaving) return
        draft = change(draft)
        saveError = null
        persist(draft)
        publish()
    }

    private fun persist(draft: Draft) {
        savedStateHandle[KEY_STARTED] = true
        savedStateHandle[KEY_FROM] = draft.fromId
        savedStateHandle[KEY_TO] = draft.toId
        savedStateHandle[KEY_AMOUNT] = draft.amountText
        savedStateHandle[KEY_DAY] = draft.date.toEpochDay()
        savedStateHandle[KEY_NOTE] = draft.note
    }

    private fun restoredDraft(): Draft? {
        if (savedStateHandle.get<Boolean>(KEY_STARTED) != true) return null
        return Draft(
            fromId = savedStateHandle[KEY_FROM],
            toId = savedStateHandle[KEY_TO],
            amountText = savedStateHandle[KEY_AMOUNT] ?: "",
            date = savedStateHandle.get<Long>(KEY_DAY)?.let(LocalDate::ofEpochDay) ?: baseline.date,
            note = savedStateHandle[KEY_NOTE] ?: "",
        )
    }

    private fun publish() {
        _state.value = compute()
    }

    private fun plainAmount(paise: Paise): String = formatInr(paise).removePrefix("₹").replace(",", "")

    private fun choice(member: Member) = MemberChoice(
        id = member.id,
        name = if (member.isLocalOwner) ownerName.ifBlank { member.name } else member.name,
        isOwner = member.isLocalOwner,
    )

    private fun compute(): SettlementFormUiState {
        val loadState = when {
            storedMissing -> FormLoadState.NotFound
            storedLoadFailed -> FormLoadState.LoadFailed
            !storedLoaded || !membersLoaded || !ledgerLoaded -> FormLoadState.Loading
            else -> FormLoadState.Ready
        }
        val parsed = parseInrAmount(draft.amountText)
        val amount = (parsed as? AmountResult.Success)?.paise
        val amountError = when {
            parsed is AmountResult.Success -> null
            parsed is AmountResult.Empty -> if (attemptedSave) parsed else null
            else -> parsed
        }
        val noteTooLong = draft.note.trim().length > MAX_NOTE_LENGTH
        val readOnly = groupArchived
        // The record being edited is left out of the balances it is checked against.
        val others = settlements.filter { it.id != recordId }
        val validation = if (amount != null && draft.fromId != null && draft.toId != null) {
            validateSettlement(
                settlement = Settlement(
                    id = recordId,
                    groupId = groupId,
                    fromMemberId = draft.fromId!!,
                    toMemberId = draft.toId!!,
                    amount = amount,
                    occurredEpochDay = draft.date.toEpochDay(),
                    note = draft.note,
                    createdAt = 0L,
                    updatedAt = 0L,
                ),
                expenses = expenses,
                otherSettlements = others,
                existing = stored,
            )
        } else {
            null
        }
        val isDirty = draft != baseline
        val canSave = loadState == FormLoadState.Ready &&
            !readOnly &&
            !isSaving &&
            !noteTooLong &&
            validation is SettlementValidation.Valid &&
            (settlementId == null || isDirty)
        val retained = setOfNotNull(stored?.fromMemberId, stored?.toMemberId)
        return SettlementFormUiState(
            recordId = recordId,
            groupId = groupId,
            isEdit = settlementId != null,
            loadState = loadState,
            readOnly = readOnly,
            members = members.filter { it.active || it.id in retained }.map(::choice),
            fromId = draft.fromId,
            toId = draft.toId,
            amountText = draft.amountText,
            amountError = amountError,
            date = draft.date,
            note = draft.note,
            noteTooLong = noteTooLong,
            validation = validation,
            isDirty = isDirty,
            canSave = canSave,
            isSaving = isSaving,
            saved = saved,
            saveError = saveError,
            showDiscardDialog = showDiscardDialog,
        )
    }
}
