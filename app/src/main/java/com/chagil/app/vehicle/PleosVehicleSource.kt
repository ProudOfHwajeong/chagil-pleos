package com.chagil.app.vehicle

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.WindowArea
import ai.pleos.playground.vehicle.constant.area.WindowValue
import ai.pleos.playground.vehicle.constant.control.LightState
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.pleos.playground.vehicle.constant.control.WiperState
import ai.pleos.playground.vehicle.listener.GearSelectionListener
import ai.pleos.playground.vehicle.listener.HvacDefrosterStateListener
import ai.pleos.playground.vehicle.listener.HvacSteeringWheelHeatStateListener
import ai.pleos.playground.vehicle.listener.LightStateListener
import ai.pleos.playground.vehicle.listener.WiperStateListener
import android.content.Context
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Reads the car through Pleos Vehicle SDK 2.3.2.
 *
 * - Listeners where the SDK has them (gear, wiper state, low-beam state, defrosters, steering heat),
 *   plus a 1 s refresh of those in case a listener goes quiet.
 * - Wiper switch and low-beam switch have no listener → polled every 250 ms.
 * - A signal becomes [Reading.Unreadable] after 2 failures in a row; one success resets it.
 * - Every SDK call is wrapped; nothing here throws except [start] when `initialize()` fails.
 * - Read-only: this class never calls any SDK `set*` function.
 *
 * Normally used through [SwitchingVehicleHub], which falls back to simulation.
 */
class PleosVehicleSource(
    context: Context,
    private val diagnostics: Diagnostics = Diagnostics(),
    override val uxRestricted: StateFlow<Boolean?> = MutableStateFlow(null),
) : VehicleSignalHub {

    private val appContext = context.applicationContext

    private val _mode = MutableStateFlow(SourceMode.PLEOS_SDK)
    override val mode: StateFlow<SourceMode> = _mode.asStateFlow()

    private val _readings = MutableStateFlow(unknownAll())
    override val readings: StateFlow<Map<Signal, Reading>> = _readings.asStateFlow()

    private var vehicle: Vehicle? = null
    private var scope: CoroutineScope? = null
    private val failures = ConcurrentHashMap<Signal, Int>()
    private val unregister = mutableListOf<() -> Unit>()

    private val front by lazy { WindowArea(WindowValue.FRONT_WINDSHIELD) }
    private val rear by lazy { WindowArea(WindowValue.REAR_WINDSHIELD) }

    /** @throws IllegalStateException when the SDK cannot initialize (no Vehicle Service, missing classes…). */
    override fun start() {
        if (vehicle != null) return
        val v = try {
            Vehicle(appContext).also { it.initialize() }
        } catch (t: Throwable) {
            diagnostics.add("Vehicle SDK 초기화 실패 · ${t.shortMessage()}")
            throw IllegalStateException("Vehicle SDK initialize failed", t)
        }
        vehicle = v
        failures.clear()
        _readings.value = unknownAll()
        registerListeners(v)
        val s = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        scope = s
        s.repeat(FAST_POLL_MS) { pollSwitches(v) }
        s.repeat(SLOW_POLL_MS) { refreshListened(v) }
    }

    override fun stop() {
        scope?.cancel()
        scope = null
        unregister.forEach { runCatching(it) }
        unregister.clear()
        vehicle?.let { v -> runCatching { v.release() }.onFailure { diagnostics.add("SDK release 실패 · ${it.shortMessage()}") } }
        vehicle = null
        _readings.value = unknownAll()
    }

    // ---- polling -------------------------------------------------------------------------------

    private fun pollSwitches(v: Vehicle) {
        sdk(Signal.WIPER_SWITCH) {
            v.getWiper().getCurrentWiperSwitch(front, { ok(Signal.WIPER_SWITCH, it?.name) }, { fail(Signal.WIPER_SWITCH, it) })
        }
        sdk(Signal.LOW_BEAM_SWITCH) {
            v.getLight().getLowBeamLightSwitch({ ok(Signal.LOW_BEAM_SWITCH, it?.name) }, { fail(Signal.LOW_BEAM_SWITCH, it) })
        }
    }

    private fun refreshListened(v: Vehicle) {
        sdk(Signal.GEAR) {
            v.getGear().getCurrentGearSelectionState({ ok(Signal.GEAR, it?.name) }, { fail(Signal.GEAR, it) })
        }
        sdk(Signal.WIPER_STATE) {
            v.getWiper().getCurrentWiperState(front, { ok(Signal.WIPER_STATE, it?.name) }, { fail(Signal.WIPER_STATE, it) })
        }
        sdk(Signal.LOW_BEAM_STATE) {
            v.getLight().getCurrentLowBeamLightState({ ok(Signal.LOW_BEAM_STATE, it?.name) }, { fail(Signal.LOW_BEAM_STATE, it) })
        }
        sdk(Signal.DEFROST_FRONT) {
            v.getHVAC().getHvacDefrosterState(front, { ok(Signal.DEFROST_FRONT, it?.let(Raw::of)) }, { fail(Signal.DEFROST_FRONT, it) })
        }
        sdk(Signal.DEFROST_REAR) {
            v.getHVAC().getHvacDefrosterState(rear, { ok(Signal.DEFROST_REAR, it?.let(Raw::of)) }, { fail(Signal.DEFROST_REAR, it) })
        }
        sdk(Signal.STEERING_HEAT) {
            v.getSteeringWheel().getHvacSteeringWheelHeatState({ ok(Signal.STEERING_HEAT, it?.toString()) }, { fail(Signal.STEERING_HEAT, it) })
        }
    }

    // ---- listeners -----------------------------------------------------------------------------

    private fun registerListeners(v: Vehicle) {
        val gear = object : GearSelectionListener {
            override fun onSelected(gear: VehicleGear?) = ok(Signal.GEAR, gear?.name)
            override fun onFailed(e: Exception) = fail(Signal.GEAR, e)
        }
        sdk(Signal.GEAR) {
            v.getGear().registerGearSelection(gear)
            unregister += { v.getGear().unregisterGearSelection(gear) }
        }

        val wiper = object : WiperStateListener {
            override fun onWiperStateUpdated(state: WiperState?) = ok(Signal.WIPER_STATE, state?.name)
            override fun onFailed(e: Exception) = fail(Signal.WIPER_STATE, e)
        }
        sdk(Signal.WIPER_STATE) {
            v.getWiper().registerWiperState(front, wiper)
            unregister += { v.getWiper().unregisterWiperState(front, wiper) }
        }

        val lowBeam = object : LightStateListener {
            override fun onLightStateUpdated(state: LightState?) = ok(Signal.LOW_BEAM_STATE, state?.name)
            override fun onFailed(e: Exception) = fail(Signal.LOW_BEAM_STATE, e)
        }
        sdk(Signal.LOW_BEAM_STATE) {
            v.getLight().registerLowBeamLightState(lowBeam)
            unregister += { v.getLight().unregisterLowBeamLightState(lowBeam) }
        }

        for ((signal, area) in listOf(Signal.DEFROST_FRONT to front, Signal.DEFROST_REAR to rear)) {
            val defrost = object : HvacDefrosterStateListener {
                override fun onHvacDefrosterStateUpdated(on: Boolean?) = ok(signal, on?.let(Raw::of))
                override fun onFailed(e: Exception) = fail(signal, e)
            }
            sdk(signal) {
                v.getHVAC().registerHvacDefrosterState(area, defrost)
                unregister += { v.getHVAC().unregisterHvacDefrosterState(area, defrost) }
            }
        }

        val heat = object : HvacSteeringWheelHeatStateListener {
            override fun onHvacSteeringWheelHeatStateUpdated(level: Int?) = ok(Signal.STEERING_HEAT, level?.toString())
            override fun onFailed(e: Exception) = fail(Signal.STEERING_HEAT, e)
        }
        sdk(Signal.STEERING_HEAT) {
            v.getSteeringWheel().registerHvacSteeringWheelHeatState(heat)
            unregister += { v.getSteeringWheel().unregisterHvacSteeringWheelHeatState(heat) }
        }
    }

    // ---- bookkeeping ---------------------------------------------------------------------------

    private inline fun sdk(signal: Signal, block: () -> Unit) {
        try {
            block()
        } catch (t: Throwable) {
            fail(signal, t)
        }
    }

    private fun ok(signal: Signal, raw: String?) {
        if (raw == null) {
            fail(signal, null)
            return
        }
        failures[signal] = 0
        _readings.update { current ->
            val old = current[signal]
            if (old is Reading.Value && old.raw == raw) current
            else current + (signal to Reading.Value(raw, System.currentTimeMillis()))
        }
    }

    private fun fail(signal: Signal, error: Throwable?) {
        val count = (failures[signal] ?: 0) + 1
        failures[signal] = count
        if (count == UNREADABLE_AFTER) {
            diagnostics.add("${signal.key} 읽기 실패 · ${error?.shortMessage() ?: "값 없음"}")
        }
        if (count >= UNREADABLE_AFTER) {
            _readings.update { if (it[signal] == Reading.Unreadable) it else it + (signal to Reading.Unreadable) }
        }
    }

    private fun CoroutineScope.repeat(periodMs: Long, action: () -> Unit): Job = launch {
        while (isActive) {
            action()
            delay(periodMs)
        }
    }

    private fun unknownAll(): Map<Signal, Reading> = Signal.entries.associateWith { Reading.Unknown }

    private companion object {
        const val FAST_POLL_MS = 250L
        const val SLOW_POLL_MS = 1_000L
        const val UNREADABLE_AFTER = 2
    }
}
