package com.deepak.paisapilot.feature.groups.shared.form

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepak.paisapilot.core.data.IdGenerator
import com.deepak.paisapilot.core.data.prefs.SettingsRepository
import com.deepak.paisapilot.core.data.repo.GroupRepository
import com.deepak.paisapilot.core.data.repo.SharedExpenseRepository
import com.deepak.paisapilot.core.data.repo.SharedWriteResult
import com.deepak.paisapilot.domain.common.StableIdOrder
import com.deepak.paisapilot.domain.group.Member
import com.deepak.paisapilot.domain.group.Share
import com.deepak.paisapilot.domain.group.SharedExpense
import com.deepak.paisapilot.domain.group.SplitResult
import com.deepak.paisapilot.domain.group.computeNets
import com.deepak.paisapilot.domain.group.seedExactFromEqual
import com.deepak.paisapilot.domain.group.splitEqually
import com.deepak.paisapilot.domain.money.AmountResult
import com.deepak.paisapilot.domain.money.Paise
import com.deepak.paisapilot.domain.money.formatInr
import com.deepak.paisapilot.domain.money.parseInrAmount
import com.deepak.paisapilot.domain.personal.Category
import com.deepak.paisapilot.domain.personal.MAX_NOTE_LENGTH
import com.deepak.paisapilot.domain.personal.MAX_TITLE_LENGTH
import com.deepak.paisapilot.feature.personal.form.FormLoadState
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

private const val KEY_GROUP_ID = "groupId"
private const val KEY_EXPENSE_ID = "expenseId"
private const val KEY_RECORD_ID = "recordId"
private const val KEY_STARTED = "draftStarted"
private const val KEY_AMOUNT = "draftAmount"
private const val KEY_TITLE = "draftTitle"
private const val KEY_CATEGORY = "draftCategory"
private const val KEY_DAY = "draftDay"
private const val KEY_NOTE = "draftNote"
private const val KEY_PAYER = "draftPayer"
private const val KEY_SELECTED = "draftSelected"
private const val KEY_MODE = "draftMode"
private const val KEY_EXACT_IDS = "draftExactIds"
private const val KEY_EXACT_TEXTS = "draftExactTexts"

private data class Draft(
    val amountText: String,
    val title: String,
    val category: Category,
    val date: LocalDate,
    val note: String,
    val payerId: String?,
    val selected: Set<String>,
    val mode: SplitMode,
    val exactTexts: Map<String, String>,
)

/** Result of working out the shares for the current draft. */
private class SplitView(
    val shares: List<Share>?,
    val shareTexts: Map<String, String>,
    val shareErrors: Map<String, AmountResult>,
    val assigned: Long,
    val remaining: Long?,
)

@HiltViewModel
class SharedFormViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: SharedExpenseRepository,
    groupRepository: GroupRepository,
    settings: SettingsRepository,
    private val clock: Clock,
    idGenerator: IdGenerator,
) : ViewModel() {
    private val groupId: String = checkNotNull(savedStateHandle[KEY_GROUP_ID])
    private val expenseId: String? = savedStateHandle[KEY_EXPENSE_ID]
    private val recordId: String = savedStateHandle.get<String>(KEY_RECORD_ID)
        ?: (expenseId ?: idGenerator.newId()).also { savedStateHandle[KEY_RECORD_ID] = it }

    private var members: List<Member> = emptyList()
    private var membersLoaded = false
    private var groupArchived = false

    /** G7: the owner shows the live profile name, not the stored copy. */
    private var ownerName = ""
    private var expenseLoaded = expenseId == null
    private var expenseLoadFailed = false
    private var expenseMissing = false

    /** Edit only: the stored payer and beneficiaries, which stay valid even if now inactive. */
    private var retainedPayer: String? = null
    private var retainedShareIds: Set<String> = emptySet()

    private var baseline = Draft(
        amountText = "",
        title = "",
        category = Category.Other,
        date = LocalDate.now(clock),
        note = "",
        payerId = null,
        selected = emptySet(),
        mode = SplitMode.Equally,
        exactTexts = emptyMap(),
    )
    private var draft: Draft = restoredDraft() ?: baseline
    private var attemptedSave = false
    private var saveError: SharedSaveError? = null
    private var isSaving = false
    private var saved = false
    private var showDiscardDialog = false

    private val _state = MutableStateFlow(compute())
    val state: StateFlow<SharedFormUiState> = _state.asStateFlow()

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
        if (expenseId != null) loadForEdit(expenseId)
    }

    fun onRetryLoad() {
        val id = expenseId ?: return
        expenseLoadFailed = false
        publish()
        loadForEdit(id)
    }

    fun onAmountChange(value: String) = edit { it.copy(amountText = value) }

    fun onTitleChange(value: String) = edit { it.copy(title = value) }

    fun onCategoryChange(value: Category) = edit { it.copy(category = value) }

    fun onDateChange(value: LocalDate) = edit { it.copy(date = value) }

    fun onNoteChange(value: String) = edit { it.copy(note = value) }

    fun onPayerChange(memberId: String) = edit { it.copy(payerId = memberId) }

    /** Selection changes keep entered Exact values; the user reconciles any difference. */
    fun onToggleMember(memberId: String) = edit { current ->
        if (memberId in current.selected) {
            current.copy(selected = current.selected - memberId)
        } else {
            current.copy(selected = current.selected + memberId)
        }
    }

    fun onSplitModeChange(mode: SplitMode) = edit { current ->
        if (mode == current.mode) return@edit current
        when (mode) {
            SplitMode.Equally -> current.copy(mode = mode)
            SplitMode.Exact -> {
                val total = (parseInrAmount(current.amountText) as? AmountResult.Success)?.paise
                val seeded = if (total != null) {
                    seedExactFromEqual(total, current.selected.toList())
                        .associate { it.memberId to plainAmount(it.paise) }
                } else {
                    emptyMap()
                }
                current.copy(mode = mode, exactTexts = seeded)
            }
        }
    }

    fun onExactShareChange(memberId: String, text: String) = edit {
        it.copy(exactTexts = it.exactTexts + (memberId to text))
    }

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
        val amount = (parseInrAmount(draft.amountText) as AmountResult.Success).paise
        val shares = splitView(amount).shares ?: return
        val expense = SharedExpense(
            id = recordId,
            groupId = groupId,
            amount = amount,
            title = draft.title,
            category = draft.category,
            date = draft.date,
            payerMemberId = checkNotNull(draft.payerId),
            note = draft.note,
            shares = shares,
            createdAt = 0L,
            updatedAt = 0L,
        )
        isSaving = true
        saveError = null
        publish()
        viewModelScope.launch {
            try {
                val result = if (expenseId != null) {
                    repository.update(expense)
                } else {
                    repository.create(expense)
                }
                isSaving = false
                when (result) {
                    SharedWriteResult.Saved, SharedWriteResult.AlreadySaved -> saved = true
                    SharedWriteResult.MemberInvalid, SharedWriteResult.NotFound ->
                        saveError = SharedSaveError.MemberChanged
                    SharedWriteResult.GroupArchived -> groupArchived = true
                    is SharedWriteResult.Invalid -> saveError = SharedSaveError.Failed
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                isSaving = false
                saveError = SharedSaveError.Failed
            }
            publish()
        }
    }

    private fun loadForEdit(id: String) {
        viewModelScope.launch {
            try {
                val stored = repository.observe(id).first()
                if (stored == null || stored.groupId != groupId) {
                    expenseMissing = true
                } else {
                    retainedPayer = stored.payerMemberId
                    retainedShareIds = stored.shares.map { it.memberId }.toSet()
                    val ids = stored.shares.map { it.memberId }
                    val equal = (splitEqually(stored.amount, ids) as? SplitResult.Success)?.shares
                    val isEqual = equal != null && equal.sortedWith(compareBy(StableIdOrder) { it.memberId }) ==
                        stored.shares.sortedWith(compareBy(StableIdOrder) { it.memberId })
                    baseline = Draft(
                        amountText = plainAmount(stored.amount),
                        title = stored.title.orEmpty(),
                        category = stored.category,
                        date = stored.date,
                        note = stored.note.orEmpty(),
                        payerId = stored.payerMemberId,
                        selected = ids.toSet(),
                        mode = if (isEqual) SplitMode.Equally else SplitMode.Exact,
                        exactTexts = if (isEqual) {
                            emptyMap()
                        } else {
                            stored.shares.associate { it.memberId to plainAmount(it.paise) }
                        },
                    )
                    draft = restoredDraft() ?: baseline
                }
                expenseLoaded = true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                expenseLoadFailed = true
            }
            publish()
        }
    }

    /** Create: all active members start selected and the local owner pays, unless restored. */
    private fun applyDefaultsIfNeeded() {
        if (expenseId != null || !membersLoaded) return
        if (savedStateHandle.get<Boolean>(KEY_STARTED) == true) return
        if (draft.selected.isNotEmpty() || draft.payerId != null) return
        val active = members.filter { it.active }
        val defaults = draft.copy(
            selected = active.map { it.id }.toSet(),
            payerId = (active.firstOrNull { it.isLocalOwner } ?: active.firstOrNull())?.id,
        )
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
        savedStateHandle[KEY_AMOUNT] = draft.amountText
        savedStateHandle[KEY_TITLE] = draft.title
        savedStateHandle[KEY_CATEGORY] = draft.category.key
        savedStateHandle[KEY_DAY] = draft.date.toEpochDay()
        savedStateHandle[KEY_NOTE] = draft.note
        savedStateHandle[KEY_PAYER] = draft.payerId
        savedStateHandle[KEY_SELECTED] = ArrayList(draft.selected)
        savedStateHandle[KEY_MODE] = draft.mode.name
        savedStateHandle[KEY_EXACT_IDS] = ArrayList(draft.exactTexts.keys)
        savedStateHandle[KEY_EXACT_TEXTS] = ArrayList(draft.exactTexts.values)
    }

    private fun restoredDraft(): Draft? {
        if (savedStateHandle.get<Boolean>(KEY_STARTED) != true) return null
        val exactIds = savedStateHandle.get<ArrayList<String>>(KEY_EXACT_IDS).orEmpty()
        val exactTexts = savedStateHandle.get<ArrayList<String>>(KEY_EXACT_TEXTS).orEmpty()
        return Draft(
            amountText = savedStateHandle[KEY_AMOUNT] ?: "",
            title = savedStateHandle[KEY_TITLE] ?: "",
            category = Category.fromKey(savedStateHandle[KEY_CATEGORY] ?: ""),
            date = savedStateHandle.get<Long>(KEY_DAY)?.let(LocalDate::ofEpochDay) ?: baseline.date,
            note = savedStateHandle[KEY_NOTE] ?: "",
            payerId = savedStateHandle[KEY_PAYER],
            selected = savedStateHandle.get<ArrayList<String>>(KEY_SELECTED).orEmpty().toSet(),
            mode = savedStateHandle.get<String>(KEY_MODE)
                ?.let { name -> SplitMode.entries.firstOrNull { it.name == name } }
                ?: SplitMode.Equally,
            exactTexts = exactIds.zip(exactTexts).toMap(),
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

    private fun splitView(total: Paise?): SplitView {
        val selected = draft.selected.toList()
        if (total == null) {
            return SplitView(null, emptyMap(), emptyMap(), 0L, null)
        }
        return when (draft.mode) {
            SplitMode.Equally -> {
                val shares = (splitEqually(total, selected) as? SplitResult.Success)?.shares
                SplitView(
                    shares = shares,
                    shareTexts = shares.orEmpty().associate { it.memberId to formatInr(it.paise) },
                    shareErrors = emptyMap(),
                    assigned = shares.orEmpty().sumOf { it.paise.value },
                    remaining = if (shares != null) 0L else total.value,
                )
            }
            SplitMode.Exact -> {
                val errors = HashMap<String, AmountResult>()
                val entered = ArrayList<Share>()
                for (id in selected) {
                    when (val parsed = parseInrAmount(draft.exactTexts[id].orEmpty())) {
                        is AmountResult.Success -> entered += Share(id, parsed.paise)
                        AmountResult.Empty, AmountResult.NotPositive -> entered += Share(id, Paise(0))
                        else -> errors[id] = parsed
                    }
                }
                val assigned = entered.sumOf { it.paise.value }
                val remaining = total.value - assigned
                val valid = errors.isEmpty() && selected.isNotEmpty() && remaining == 0L
                SplitView(
                    shares = if (valid) entered.sortedWith(compareBy(StableIdOrder) { it.memberId }) else null,
                    shareTexts = emptyMap(),
                    shareErrors = errors,
                    assigned = assigned,
                    remaining = remaining,
                )
            }
        }
    }

    private fun compute(): SharedFormUiState {
        val parsed = parseInrAmount(draft.amountText)
        val total = (parsed as? AmountResult.Success)?.paise
        val amountError = when {
            parsed is AmountResult.Success -> null
            parsed is AmountResult.Empty -> if (attemptedSave) parsed else null
            else -> parsed
        }
        val titleTooLong = draft.title.trim().length > MAX_TITLE_LENGTH
        val noteTooLong = draft.note.trim().length > MAX_NOTE_LENGTH
        val split = splitView(total)

        // Payers: active members, plus the stored payer on edit. Split rows add stored beneficiaries.
        val payerChoices = members
            .filter { it.active || it.id == retainedPayer }
            .map(::choice)
        val rowMembers = members.filter { it.active || it.id in retainedShareIds }
        val rows = rowMembers.map { member ->
            val included = member.id in draft.selected
            ShareRowUi(
                member = choice(member),
                included = included,
                shareText = when {
                    !included -> ""
                    draft.mode == SplitMode.Equally -> split.shareTexts[member.id].orEmpty()
                    else -> draft.exactTexts[member.id].orEmpty()
                },
                shareError = split.shareErrors[member.id],
            )
        }

        val loadState = when {
            expenseMissing -> FormLoadState.NotFound
            expenseLoadFailed -> FormLoadState.LoadFailed
            !membersLoaded || !expenseLoaded -> FormLoadState.Loading
            else -> FormLoadState.Ready
        }
        val payerValid = draft.payerId != null && payerChoices.any { it.id == draft.payerId }
        val selectedValid = draft.selected.isNotEmpty() && draft.selected.all { id -> rowMembers.any { it.id == id } }
        val ready = loadState == FormLoadState.Ready
        val staleMembers = ready && ((draft.payerId != null && !payerValid) || (!selectedValid && draft.selected.isNotEmpty()))
        val canSave = ready &&
            !groupArchived &&
            total != null &&
            !titleTooLong &&
            !noteTooLong &&
            payerValid &&
            selectedValid &&
            split.shares != null &&
            !isSaving &&
            !saved

        val netPreview = if (canSave) {
            val preview = SharedExpense(
                id = recordId,
                groupId = groupId,
                amount = checkNotNull(total),
                title = null,
                category = draft.category,
                date = draft.date,
                payerMemberId = checkNotNull(draft.payerId),
                note = null,
                shares = checkNotNull(split.shares),
                createdAt = 0L,
                updatedAt = 0L,
            )
            val byId = members.associateBy { it.id }
            computeNets(listOf(preview)).mapNotNull { (id, net) ->
                byId[id]?.let { NetLineUi(choice(it), net) }
            }
        } else {
            emptyList()
        }

        return SharedFormUiState(
            recordId = recordId,
            groupId = groupId,
            isEdit = expenseId != null,
            loadState = loadState,
            readOnly = groupArchived,
            amountText = draft.amountText,
            amountError = amountError,
            title = draft.title,
            titleTooLong = titleTooLong,
            category = draft.category,
            date = draft.date,
            note = draft.note,
            noteTooLong = noteTooLong,
            payerChoices = payerChoices,
            payerId = draft.payerId,
            splitMode = draft.mode,
            rows = rows,
            totalPaise = total?.value,
            assignedPaise = split.assigned,
            remainingPaise = split.remaining,
            noBeneficiaries = ready && draft.selected.isEmpty(),
            netPreview = netPreview,
            canSave = canSave,
            isDirty = draft != baseline,
            isSaving = isSaving,
            saveError = saveError ?: SharedSaveError.MemberChanged.takeIf { staleMembers },
            saved = saved,
            showDiscardDialog = showDiscardDialog,
        )
    }
}
