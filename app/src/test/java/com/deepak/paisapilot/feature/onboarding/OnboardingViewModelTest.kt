package com.deepak.paisapilot.feature.onboarding

import app.cash.turbine.test
import com.deepak.paisapilot.domain.profile.DisplayNameResult
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {
    @Test
    fun initialNameIsMe() = onboardingTest {
        val viewModel = viewModel()
        viewModel.state.test {
            val initial = awaitItem()
            assertEquals("Me", initial.name)
            assertNull(initial.nameError)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun startWritesOnceThenCompleted() = onboardingTest {
        val repository = FakeSettingsRepository()
        val viewModel = viewModel(repository)
        viewModel.state.test {
            awaitItem()
            viewModel.onNameChange("Sam")
            assertEquals("Sam", awaitItem().name)
            viewModel.onStart()
            assertTrue(awaitItem().saving)
            viewModel.onStart()
            advanceUntilIdle()
            assertTrue(awaitItem().completed)
            assertEquals(1, repository.writes)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun failedWriteKeepsInputAndRetrySucceeds() = onboardingTest {
        val repository = FakeSettingsRepository()
        val viewModel = viewModel(repository)
        repository.throwOnWrite = true
        viewModel.onNameChange("Sam")
        viewModel.onStart()
        advanceUntilIdle()
        assertTrue(viewModel.state.value.saveFailed)
        assertEquals("Sam", viewModel.state.value.name)
        assertFalse(viewModel.state.value.completed)
        repository.throwOnWrite = false
        viewModel.onRetry()
        advanceUntilIdle()
        assertTrue(viewModel.state.value.completed)
        assertEquals("Sam", repository.lastName)
        assertEquals(2, repository.writes)
    }

    @Test
    fun skipBlankStoresMe() = onboardingTest {
        val repository = FakeSettingsRepository()
        val viewModel = viewModel(repository)
        viewModel.onNameChange("   ")
        viewModel.onSkip()
        advanceUntilIdle()
        assertEquals("Me", repository.lastName)
        assertTrue(viewModel.state.value.completed)
        assertEquals(1, repository.writes)
    }

    @Test
    fun startBlankShowsErrorAndDoesNotWrite() = onboardingTest {
        val repository = FakeSettingsRepository()
        val viewModel = viewModel(repository)
        viewModel.onNameChange(" ")
        viewModel.onStart()
        assertEquals(DisplayNameResult.Blank, viewModel.state.value.nameError)
        assertEquals(0, repository.writes)
    }

    @Test
    fun startTooLongShowsErrorAndDoesNotWrite() = onboardingTest {
        val repository = FakeSettingsRepository()
        val viewModel = viewModel(repository)
        viewModel.onNameChange("a".repeat(41))
        viewModel.onStart()
        assertEquals(DisplayNameResult.TooLong, viewModel.state.value.nameError)
        assertEquals("a".repeat(41), viewModel.state.value.name)
        assertEquals(0, repository.writes)
    }

    private fun onboardingTest(block: suspend TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            block()
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun TestScope.viewModel(
        repository: FakeSettingsRepository = FakeSettingsRepository(),
    ): OnboardingViewModel = OnboardingViewModel(repository)
}
