package com.deepak.paisapilot.feature.groups

import androidx.lifecycle.SavedStateHandle
import com.deepak.paisapilot.core.data.IdGenerator
import com.deepak.paisapilot.domain.group.MemberNameResult
import com.deepak.paisapilot.feature.groups.form.GroupFormViewModel
import com.deepak.paisapilot.feature.groups.form.NameError
import com.deepak.paisapilot.feature.onboarding.FakeSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GroupFormViewModelTest {
    private fun formTest(block: suspend TestScope.() -> Unit) {
        val dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
        try {
            runTest(dispatcher) { block() }
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun vm(repo: FakeGroupRepository, handle: SavedStateHandle = SavedStateHandle()) =
        GroupFormViewModel(handle, repo, FakeSettingsRepository(), IdGenerator { "group-1" })

    // AC-GRP-02
    @Test
    fun createsGroupWithOwnerAndMember() = formTest {
        val repo = FakeGroupRepository()
        val vm = vm(repo)
        advanceUntilIdle()
        vm.onNameChange("Flat")
        vm.onNewMemberChange("Alex")
        vm.onAddMember()
        vm.onSave()
        advanceUntilIdle()
        assertEquals("group-1", vm.state.value.createdGroupId)
        assertEquals(listOf("Me", "Alex"), repo.members.value.map { it.name })
        assertEquals(1, repo.groups.value.size)
    }

    // AC-GRP-03
    @Test
    fun duplicateMemberIsRejectedInline() = formTest {
        val vm = vm(FakeGroupRepository())
        advanceUntilIdle()
        vm.onNewMemberChange("Alex")
        vm.onAddMember()
        vm.onNewMemberChange(" alex ")
        vm.onAddMember()
        assertEquals(MemberNameResult.Duplicate, vm.state.value.memberError)
        assertEquals(listOf("Alex"), vm.state.value.members)
    }

    @Test
    fun ownerNameBlocksSameMemberName() = formTest {
        val vm = vm(FakeGroupRepository())
        advanceUntilIdle()
        vm.onNewMemberChange("me")
        vm.onAddMember()
        assertEquals(MemberNameResult.Duplicate, vm.state.value.memberError)
    }

    // AC-GRP-04
    @Test
    fun saveBlockedWithoutOtherMembersAndNoGroupWritten() = formTest {
        val repo = FakeGroupRepository()
        val vm = vm(repo)
        advanceUntilIdle()
        vm.onNameChange("Flat")
        vm.onSave()
        advanceUntilIdle()
        assertTrue(vm.state.value.tooFewMembers)
        assertEquals(0, repo.creates)
    }

    @Test
    fun blankNameShowsError() = formTest {
        val vm = vm(FakeGroupRepository())
        advanceUntilIdle()
        vm.onNewMemberChange("Alex")
        vm.onAddMember()
        vm.onSave()
        assertEquals(NameError.Blank, vm.state.value.nameError)
    }

    @Test
    fun thirtyFirstMemberRejected() = formTest {
        val vm = vm(FakeGroupRepository())
        advanceUntilIdle()
        repeat(29) {
            vm.onNewMemberChange("p$it")
            vm.onAddMember()
        }
        assertEquals(30, vm.state.value.memberCount)
        vm.onNewMemberChange("extra")
        vm.onAddMember()
        assertEquals(MemberNameResult.GroupFull, vm.state.value.memberError)
    }

    @Test
    fun doubleSaveCreatesOneGroup() = formTest {
        val repo = FakeGroupRepository()
        val vm = vm(repo)
        advanceUntilIdle()
        vm.onNameChange("Flat")
        vm.onNewMemberChange("Alex")
        vm.onAddMember()
        vm.onSave()
        vm.onSave()
        advanceUntilIdle()
        assertEquals(1, repo.creates)
        assertEquals(1, repo.groups.value.size)
    }

    @Test
    fun writeFailureKeepsFormAndAllowsRetry() = formTest {
        val repo = FakeGroupRepository().apply { throwOnWrite = true }
        val vm = vm(repo)
        advanceUntilIdle()
        vm.onNameChange("Flat")
        vm.onNewMemberChange("Alex")
        vm.onAddMember()
        vm.onSave()
        advanceUntilIdle()
        assertTrue(vm.state.value.saveFailed)
        assertFalse(vm.state.value.isSaving)
        assertNull(vm.state.value.createdGroupId)
        assertEquals("Flat", vm.state.value.name)
        repo.throwOnWrite = false
        vm.onSave()
        advanceUntilIdle()
        assertNotNull(vm.state.value.createdGroupId)
    }

    @Test
    fun draftSurvivesRecreation() = formTest {
        val handle = SavedStateHandle()
        val first = vm(FakeGroupRepository(), handle)
        advanceUntilIdle()
        first.onNameChange("Flat")
        first.onNewMemberChange("Alex")
        first.onAddMember()
        val second = vm(FakeGroupRepository(), handle)
        assertEquals("Flat", second.state.value.name)
        assertEquals(listOf("Alex"), second.state.value.members)
    }
}
