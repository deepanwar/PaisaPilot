package com.deepak.paisapilot.feature.groups

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.deepak.paisapilot.core.data.repo.GroupRepository
import com.deepak.paisapilot.domain.group.Group
import com.deepak.paisapilot.domain.group.Member
import com.deepak.paisapilot.domain.group.MemberNameResult
import com.deepak.paisapilot.feature.groups.detail.DetailStatus
import com.deepak.paisapilot.feature.groups.detail.GroupDetailUiState
import com.deepak.paisapilot.feature.groups.detail.GroupDetailViewModel
import com.deepak.paisapilot.feature.groups.list.GroupListUiState
import com.deepak.paisapilot.feature.groups.list.GroupListViewModel
import com.deepak.paisapilot.feature.onboarding.FakeSettingsRepository
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
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
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GroupDetailViewModelTest {
    private val clock = Clock.fixed(Instant.parse("2026-10-20T10:00:00Z"), ZoneOffset.UTC)

    private fun detailTest(block: suspend TestScope.() -> Unit) {
        val dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
        try {
            runTest(dispatcher) { block() }
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun seeded() = FakeGroupRepository().apply {
        groups.value = listOf(Group("g", "Flat", 0L, null))
        members.value = listOf(
            Member("o", "g", "stored-name", isLocalOwner = true, active = true),
            Member("a", "g", "Alex", isLocalOwner = false, active = true),
        )
    }

    private fun vm(repo: GroupRepository) =
        GroupDetailViewModel(SavedStateHandle(mapOf("groupId" to "g")), repo, FakeSettingsRepository(), clock)

    private suspend fun app.cash.turbine.ReceiveTurbine<GroupDetailUiState>.content(
        check: (GroupDetailUiState) -> Boolean = { true },
    ): GroupDetailUiState {
        var item = awaitItem()
        while (item.status != DetailStatus.Content || !check(item)) item = awaitItem()
        return item
    }

    @Test
    fun ownerShowsLiveProfileName() = detailTest {
        val vm = vm(seeded())
        vm.state.test {
            val state = content()
            assertEquals(listOf("Me", "Alex"), state.members.map { it.name })
            assertTrue(state.canRecordShared)
            assertTrue(state.canDelete)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // AC-GRP-05 / AC-GRP-06
    @Test
    fun deactivatedMemberStaysListedAndInactive() = detailTest {
        val repo = seeded()
        val vm = vm(repo)
        vm.state.test {
            content()
            vm.onSetMemberActive("a", false)
            val state = content { s -> s.members.any { !it.active } }
            assertEquals(listOf(true, false), state.members.map { it.active })
            assertFalse(state.canRecordShared)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun ownerCannotBeDeactivated() = detailTest {
        val vm = vm(seeded())
        vm.state.test {
            content()
            vm.onSetMemberActive("o", false)
            val state = content { it.actionFailed }
            assertTrue(state.members.first { it.isOwner }.active)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun addingDuplicateMemberShowsError() = detailTest {
        val vm = vm(seeded())
        vm.state.test {
            content()
            vm.onAddMemberRequested()
            vm.onNewMemberChange(" alex ")
            vm.onAddMemberConfirmed()
            val state = content { it.memberError != null }
            assertEquals(MemberNameResult.Duplicate, state.memberError)
            assertTrue(state.showAddMember)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun addMemberClosesSheetAndListsNewcomer() = detailTest {
        val vm = vm(seeded())
        vm.state.test {
            content()
            vm.onAddMemberRequested()
            vm.onNewMemberChange("Bea")
            vm.onAddMemberConfirmed()
            val state = content { s -> s.members.size == 3 && !s.showAddMember }
            assertEquals("Bea", state.members.last().name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // AC-GRP-07
    @Test
    fun archiveMakesGroupReadOnly() = detailTest {
        val vm = vm(seeded())
        vm.state.test {
            content()
            vm.onArchiveRequested()
            assertTrue(content { it.showArchiveDialog }.showArchiveDialog)
            vm.onArchiveConfirmed()
            val state = content { it.archived }
            assertFalse(state.canRecordShared)
            assertFalse(state.showArchiveDialog)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun deleteHiddenWhenGroupHasHistory() = detailTest {
        val repo = seeded().apply { hasHistory.value = true }
        val vm = vm(repo)
        vm.state.test {
            assertFalse(content().canDelete)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun deleteEmptyGroupClosesScreen() = detailTest {
        val repo = seeded()
        val vm = vm(repo)
        vm.state.test {
            content()
            vm.onDeleteConfirmed()
            advanceUntilIdle()
            var item = awaitItem()
            while (!item.closed) item = awaitItem()
            assertTrue(repo.groups.value.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun listSeparatesArchivedGroups() = detailTest {
        val repo = seeded().apply {
            groups.value = groups.value + Group("g2", "Trip", 0L, 5L)
        }
        val vm = GroupListViewModel(repo)
        vm.state.test {
            var item = awaitItem()
            while (item !is GroupListUiState.Content) item = awaitItem()
            assertEquals(listOf("Flat"), item.active.map { it.name })
            assertEquals(listOf("Trip"), item.archived.map { it.name })
            assertEquals(2, item.active.first().memberCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun missingGroupShowsNotFound() = detailTest {
        val vm = vm(FakeGroupRepository())
        vm.state.test {
            var item = awaitItem()
            while (item.status == DetailStatus.Loading) item = awaitItem()
            assertEquals(DetailStatus.NotFound, item.status)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
