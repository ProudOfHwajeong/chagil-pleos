package com.chagil.app.vehicle

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SwitchingVehicleHubTest {

    private class FakeSdk : VehicleSignalHub {
        override val mode = MutableStateFlow(SourceMode.PLEOS_SDK)
        override val readings = MutableStateFlow<Map<Signal, Reading>>(Signal.entries.associateWith { Reading.Unknown })
        override val uxRestricted: StateFlow<Boolean?> = MutableStateFlow(null)
        var stopped = false
        override fun start() = Unit
        override fun stop() { stopped = true }
        fun emit(signal: Signal, raw: String) = readings.update { it + (signal to Reading.Value(raw, 0)) }
    }

    private fun TestScope.hub(
        factory: () -> VehicleSignalHub,
        platformUx: MutableStateFlow<Boolean?> = MutableStateFlow(null),
    ) = SwitchingVehicleHub(
        sdkFactory = factory,
        simulation = SimulatedVehicleSource(),
        platformUx = platformUx,
        onStartPlatform = {},
        onStopPlatform = {},
        scope = backgroundScope,
        log = Diagnostics(clock = { 0 }),
    )

    @Test
    fun `initialize failure falls back to simulation`() = runTest {
        val hub = hub({ throw IllegalStateException("no vehicle service") })
        hub.start()
        runCurrent()
        assertEquals(SourceMode.SIMULATION, hub.mode.value)
        assertEquals("GEAR_PARK", hub.readings.value[Signal.GEAR].rawOrNull)
        assertTrue(hub.diagnostics.value.single().contains("시뮬레이션"))
    }

    @Test
    fun `no gear or wiper value within 3 seconds falls back to simulation`() = runTest {
        val sdk = FakeSdk()
        val hub = hub({ sdk })
        hub.start()
        runCurrent()
        assertEquals(SourceMode.PLEOS_SDK, hub.mode.value)
        assertTrue(hub.probing.value)
        advanceTimeBy(3_001)
        runCurrent()
        assertEquals(SourceMode.SIMULATION, hub.mode.value)
        assertTrue(sdk.stopped)
    }

    @Test
    fun `a real value keeps the SDK and is mirrored`() = runTest {
        val sdk = FakeSdk()
        val hub = hub({ sdk })
        hub.start()
        runCurrent()
        sdk.emit(Signal.GEAR, "GEAR_PARK")
        runCurrent()
        advanceTimeBy(5_000)
        runCurrent()
        assertEquals(SourceMode.PLEOS_SDK, hub.mode.value)
        sdk.emit(Signal.WIPER_SWITCH, "MIST")
        runCurrent()
        assertEquals("MIST", hub.readings.value[Signal.WIPER_SWITCH].rawOrNull)
    }

    @Test
    fun `manual switch to simulation and back`() = runTest {
        val sdk = FakeSdk()
        val hub = hub({ sdk })
        hub.start()
        sdk.emit(Signal.GEAR, "GEAR_PARK")
        runCurrent()
        hub.useSimulation(true)
        runCurrent()
        assertEquals(SourceMode.SIMULATION, hub.mode.value)
        hub.simulation.set(Signal.WIPER_SWITCH, "AUTO")
        runCurrent()
        assertEquals("AUTO", hub.readings.value[Signal.WIPER_SWITCH].rawOrNull)

        hub.useSimulation(false)
        runCurrent()
        assertEquals(SourceMode.PLEOS_SDK, hub.mode.value)
    }

    @Test
    fun `a real platform restriction wins even in simulation`() = runTest {
        val platform = MutableStateFlow<Boolean?>(null)
        val hub = hub({ throw IllegalStateException() }, platform)
        hub.start()
        runCurrent()
        assertEquals(false, hub.uxRestricted.value)
        platform.value = true
        runCurrent()
        assertEquals(true, hub.uxRestricted.value)
    }
}
