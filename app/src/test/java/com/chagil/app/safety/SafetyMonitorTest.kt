package com.chagil.app.safety

import com.chagil.app.engine.QuestLevel
import com.chagil.app.vehicle.Signal
import com.chagil.app.vehicle.SimulatedVehicleSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SafetyMonitorTest {

    private class Rig(val test: TestScope) {
        val car = SimulatedVehicleSource()
        val monitor = SafetyMonitor(car, test.backgroundScope)
        init { test.runCurrent() }
        fun gear(raw: String) { car.set(Signal.GEAR, raw); test.runCurrent() }
        val level get() = monitor.level.value
    }

    @Test
    fun `park is L2 and allows every quest`() = runTest {
        val rig = Rig(this)
        assertEquals(SafetyLevel.L2_PARKED, rig.level)
        assertTrue(rig.monitor.allows(QuestLevel.L1))
        assertTrue(rig.monitor.allows(QuestLevel.L2))
        assertFalse(rig.monitor.status.value.needsConfirmation)
    }

    @Test
    fun `moving gears are L0`() = runTest {
        val rig = Rig(this)
        for (g in listOf("GEAR_DRIVE", "GEAR_REVERSE", "GEAR_1", "GEAR_9")) {
            rig.gear(g)
            assertEquals(g, SafetyLevel.L0_DRIVING, rig.level)
            assertFalse(rig.monitor.allows(QuestLevel.L1))
        }
    }

    @Test
    fun `neutral and unknown are L1 and need the driver's confirmation`() = runTest {
        val rig = Rig(this)
        for (g in listOf("GEAR_NEUTRAL", "GEAR_UNKNOWN")) {
            rig.gear(g)
            assertEquals(SafetyLevel.L1_STOPPED_UNCONFIRMED, rig.level)
        }
        assertTrue(rig.monitor.status.value.needsConfirmation)
        assertFalse(rig.monitor.allows(QuestLevel.L1))

        rig.monitor.confirmParkedByDriver()
        assertTrue(rig.monitor.driverConfirmed.value)
        assertFalse(rig.monitor.status.value.needsConfirmation)
        assertTrue(rig.monitor.allows(QuestLevel.L1))
        assertFalse("L2 quests need the car to report P", rig.monitor.allows(QuestLevel.L2))
    }

    @Test
    fun `unreadable gear is L1`() = runTest {
        val rig = Rig(this)
        rig.car.setUnreadable(Signal.GEAR)
        runCurrent()
        assertEquals(SafetyLevel.L1_STOPPED_UNCONFIRMED, rig.level)
    }

    @Test
    fun `P to D clears the driver's confirmation`() = runTest {
        val rig = Rig(this)
        rig.monitor.confirmParkedByDriver()
        assertTrue(rig.monitor.driverConfirmed.value)
        rig.gear("GEAR_DRIVE")
        assertEquals(SafetyLevel.L0_DRIVING, rig.level)
        assertFalse(rig.monitor.driverConfirmed.value)

        rig.gear("GEAR_NEUTRAL")
        assertTrue(rig.monitor.status.value.needsConfirmation)
    }

    @Test
    fun `confirmation is ignored while driving`() = runTest {
        val rig = Rig(this)
        rig.gear("GEAR_DRIVE")
        rig.monitor.confirmParkedByDriver()
        assertFalse(rig.monitor.driverConfirmed.value)
    }

    @Test
    fun `ux restriction is L0 even in park`() = runTest {
        val rig = Rig(this)
        rig.car.setUxRestricted(true)
        runCurrent()
        assertEquals(SafetyLevel.L0_DRIVING, rig.level)
        rig.car.setUxRestricted(false)
        runCurrent()
        assertEquals(SafetyLevel.L2_PARKED, rig.level)
    }

    @Test
    fun `clearDriverConfirmation resets it`() = runTest {
        val rig = Rig(this)
        rig.gear("GEAR_NEUTRAL")
        rig.monitor.confirmParkedByDriver()
        rig.monitor.clearDriverConfirmation()
        assertFalse(rig.monitor.allows(QuestLevel.L1))
    }
}
