package com.deepak.paisapilot.feature.groups.form

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepak.paisapilot.core.data.IdGenerator
import com.deepak.paisapilot.core.data.prefs.SettingsRepository
import com.deepak.paisapilot.core.data.repo.CreateGroupResult
import com.deepak.paisapilot.core.data.repo.GroupRepository
import com.deepak.paisapilot.domain.group.GroupNameResult
import com.deepak.paisapilot.domain.group.Member
import com.deepak.paisapilot.domain.group.MemberNameResult
import com.deepak.paisapilot.domain.group.validateGroupName
import com.deepak.paisapilot.domain.group.validateMemberName
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val KEY_RECORD_ID = "recordId"
private const val KEY_NAME = "draftName"
private const val KEY_MEMBERS = "draftMembers"
private const val KEY_NEW_MEMBER = "draftNewMember"

@HiltViewModel
class GroupFormViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: GroupRepository,
    private val settings: SettingsRepository,
    idGenerator: IdGenerator,
) : ViewModel() {
    private val recordId: String = savedStateHandle.get<String>(KEY_RECORD_ID)
        ?: idGenerator.newId().also { savedStateHandle[KEY_RECORD_ID] = it }

    private val _state = MutableStateFlow(
        derive(
            GroupFormUiState(
                name = savedStateHandle[KEY_NAME] ?: "",
                members = savedStateHandle.get<ArrayList<String>>(KEY_MEMBERS) ?: emptyList(),
                newMember = savedStateHandle[KEY_NEW_MEMBER] ?: "",
            ),
        ),
    )
    val state: StateFlow<GroupFormUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val owner = settings.settings.first().displayName
            _state.update { derive(it.copy(ownerName = owner)) }
        }
    }

    fun onNameChange(value: String) = edit {
        it.copy(name = value, nameError = null, saveFailed = false)
    }

    fun onNewMemberChange(value: String) = edit {
        it.copy(newMember = value, memberError = null)
    }

    fun onAddMember() {
        val current = _state.value
        if (current.newMember.isBlank()) return
        val existing = existingMembers(current)
        when (val result = validateMemberName(current.newMember, existing)) {
            is MemberNameResult.Valid -> edit {
                it.copy(members = it.members + result.name, newMember = "", memberError = null, tooFewMembers = false)
            }
            else -> _state.update { it.copy(memberError = result) }
        }
    }

    fun onRemoveMember(name: String) = edit {
        it.copy(members = it.members - name, memberError = null)
    }

    fun onBackRequested() {
        _state.update { it.copy(showDiscardDialog = true) }
    }

    fun onKeepEditing() {
        _state.update { it.copy(showDiscardDialog = false) }
    }

    fun onCreatedHandled() {
        _state.update { it.copy(createdGroupId = null) }
    }

    fun onSave() {
        val current = _state.value
        if (current.isSaving || current.createdGroupId != null) return
        val nameError = when (validateGroupName(current.name)) {
            GroupNameResult.Blank -> NameError.Blank
            GroupNameResult.TooLong -> NameError.TooLong
            is GroupNameResult.Valid -> null
        }
        val tooFew = current.members.isEmpty()
        if (nameError != null || tooFew || current.ownerName.isEmpty()) {
            _state.update { it.copy(nameError = nameError, tooFewMembers = tooFew) }
            return
        }
        _state.update { it.copy(isSaving = true, saveFailed = false) }
        viewModelScope.launch {
            try {
                val result = repository.createGroup(recordId, current.name, current.ownerName, current.members)
                _state.update {
                    when (result) {
                        CreateGroupResult.Created, CreateGroupResult.AlreadySaved ->
                            it.copy(createdGroupId = recordId)
                        is CreateGroupResult.Invalid -> it.copy(isSaving = false, saveFailed = true)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.update { it.copy(isSaving = false, saveFailed = true) }
            }
        }
    }

    private fun existingMembers(state: GroupFormUiState): List<Member> =
        (listOf(state.ownerName) + state.members).mapIndexed { index, name ->
            Member(id = "draft-$index", groupId = recordId, name = name, isLocalOwner = index == 0, active = true)
        }

    private fun edit(change: (GroupFormUiState) -> GroupFormUiState) {
        val next = change(_state.value)
        savedStateHandle[KEY_NAME] = next.name
        savedStateHandle[KEY_MEMBERS] = ArrayList(next.members)
        savedStateHandle[KEY_NEW_MEMBER] = next.newMember
        _state.value = derive(next)
    }

    private fun derive(state: GroupFormUiState): GroupFormUiState = state.copy(
        isDirty = state.name.isNotBlank() || state.members.isNotEmpty() || state.newMember.isNotBlank(),
    )
}
