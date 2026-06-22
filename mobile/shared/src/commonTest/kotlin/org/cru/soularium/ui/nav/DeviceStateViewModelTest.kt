package org.cru.soularium.ui.nav

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.cru.soularium.domain.DeviceState
import org.cru.soularium.domain.ports.DeviceStateRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceStateViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `first launch routes to intro`() =
        runTest(dispatcher) {
            val vm = DeviceStateViewModel(FakeDeviceStateRepository())
            advanceUntilIdle()
            assertEquals(Routes.INTRO, vm.startRoute.value)
        }

    @Test
    fun `intro seen but terms not accepted routes to terms`() =
        runTest(dispatcher) {
            val vm = DeviceStateViewModel(FakeDeviceStateRepository(DeviceState(hasSeenIntro = true)))
            advanceUntilIdle()
            assertEquals(Routes.TERMS, vm.startRoute.value)
        }

    @Test
    fun `intro seen and terms accepted routes to home`() =
        runTest(dispatcher) {
            val vm =
                DeviceStateViewModel(
                    FakeDeviceStateRepository(DeviceState(hasSeenIntro = true, agreedToTos = true)),
                )
            advanceUntilIdle()
            assertEquals(Routes.HOME, vm.startRoute.value)
        }

    @Test
    fun `agreeing to terms advances the start route to home`() =
        runTest(dispatcher) {
            val vm = DeviceStateViewModel(FakeDeviceStateRepository())
            advanceUntilIdle()
            assertEquals(Routes.INTRO, vm.startRoute.value)

            vm.markTosAgreed()
            advanceUntilIdle()
            assertEquals(Routes.HOME, vm.startRoute.value)
        }

    @Test
    fun `setLocale persists the chosen locale`() =
        runTest(dispatcher) {
            val vm = DeviceStateViewModel(FakeDeviceStateRepository())
            advanceUntilIdle()

            vm.setLocale("fr")
            advanceUntilIdle()
            assertEquals("fr", vm.deviceState.value.locale)
        }
}

private class FakeDeviceStateRepository(
    initial: DeviceState = DeviceState(),
) : DeviceStateRepository {
    private val state = MutableStateFlow(initial)

    override val deviceState: Flow<DeviceState> = state

    override suspend fun markIntroSeen() {
        state.update { it.copy(hasSeenIntro = true) }
    }

    override suspend fun markTosAgreed() {
        state.update { it.copy(hasSeenIntro = true, agreedToTos = true) }
    }

    override suspend fun setLocale(locale: String) {
        state.update { it.copy(locale = locale) }
    }
}
