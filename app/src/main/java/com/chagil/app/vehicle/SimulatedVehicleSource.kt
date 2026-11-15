package com.chagil.app.vehicle

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * In-app stand-in for the car, driven by the simulator panel (or tests).
 * Starts parked with everything off. Setting a switch also updates its matching state signal
 * (wiper switch → wiper state, low-beam switch → low-beam state), like a real car would.
 */
class SimulatedVehicleSource(
    private val clock: () -> Long = System::currentTimeMillis,
) : VehicleSignalHub {

    private val _mode = MutableStateFlow(SourceMode.SIMULATION)
    override val mode: StateFlow<SourceMode> = _mode.asStateFlow()

    private val _readings = MutableStateFlow(initialReadings())
    override val readings: StateFlow<Map<Signal, Reading>> = _readings.asStateFlow()

    private val _uxRestricted = MutableStateFlow<Boolean?>(false)
    override val uxRestricted: StateFlow<Boolean?> = _uxRestricted.asStateFlow()

    override fun start() = Unit
    override fun stop() = Unit

    /** Sets one value, e.g. `set(Signal.WIPER_SWITCH, "MIST")`. Unchanged values keep their timestamp. */
    fun set(signal: Signal, raw: String) {
        _readings.update { current ->
            var next = current.with(signal, raw)
            derivedState(signal, raw)?.let { (s, v) -> next = next.with(s, v) }
            next
        }
    }

    /** Makes a signal behave like a car that cannot report it (for the "켰어요" self-check path). */
    fun setUnreadable(signal: Signal) {
        _readings.update { it + (signal to Reading.Unreadable) }
    }

    /** Simulates the platform driving restriction. */
    fun setUxRestricted(restricted: Boolean?) {
        _uxRestricted.value = restricted
    }

    fun reset() {
        _readings.value = initialReadings()
        _uxRestricted.value = false
    }

    private fun Map<Signal, Reading>.with(signal: Signal, raw: String): Map<Signal, Reading> {
        val old = this[signal]
        if (old is Reading.Value && old.raw == raw) return this
        return this + (signal to Reading.Value(raw, clock()))
    }

    private fun derivedState(signal: Signal, raw: String): Pair<Signal, String>? = when (signal) {
        Signal.WIPER_SWITCH -> Signal.WIPER_STATE to if (raw == Raw.OFF || raw == "AUTO") Raw.OFF else Raw.ON
        Signal.LOW_BEAM_SWITCH -> Signal.LOW_BEAM_STATE to when (raw) {
            Raw.ON -> Raw.ON
            "DAYTIME_RUNNING" -> "DAYTIME_RUNNING"
            else -> Raw.OFF // OFF, or AUTOMATIC in daylight
        }
        else -> null
    }

    private fun initialReadings(): Map<Signal, Reading> {
        val now = clock()
        return INITIAL.mapValues { (_, raw) -> Reading.Value(raw, now) }
    }

    companion object {
        val INITIAL: Map<Signal, String> = linkedMapOf(
            Signal.GEAR to Raw.GEAR_PARK,
            Signal.WIPER_SWITCH to Raw.OFF,
            Signal.WIPER_STATE to Raw.OFF,
            Signal.LOW_BEAM_SWITCH to "AUTOMATIC",
            Signal.LOW_BEAM_STATE to Raw.OFF,
            Signal.DEFROST_FRONT to Raw.OFF,
            Signal.DEFROST_REAR to Raw.OFF,
            Signal.STEERING_HEAT to "0",
        )

        /**
         * Values the simulator panel offers per signal. Wiper options mirror the Pleos Virtual Console card
         * (Off / Auto / Low / High) plus MIST so the "wrong value" hint can be demonstrated.
         */
        fun optionsFor(signal: Signal): List<String> = when (signal) {
            Signal.GEAR -> listOf(Raw.GEAR_PARK, Raw.GEAR_REVERSE, Raw.GEAR_NEUTRAL, Raw.GEAR_DRIVE)
            Signal.WIPER_SWITCH -> listOf(
                Raw.OFF, "MIST", "INTERMITTENT_LEVEL_1", "CONTINUOUS_LEVEL_1", "CONTINUOUS_LEVEL_2", "AUTO",
            )
            Signal.WIPER_STATE -> listOf(Raw.OFF, Raw.ON)
            Signal.LOW_BEAM_SWITCH -> listOf(Raw.OFF, Raw.ON, "DAYTIME_RUNNING", "AUTOMATIC")
            Signal.LOW_BEAM_STATE -> listOf(Raw.OFF, Raw.ON, "DAYTIME_RUNNING")
            Signal.DEFROST_FRONT, Signal.DEFROST_REAR -> listOf(Raw.OFF, Raw.ON)
            Signal.STEERING_HEAT -> listOf("0", "1", "2", "3")
        }
    }
}
