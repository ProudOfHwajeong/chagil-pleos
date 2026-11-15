package com.chagil.app.safety

import com.chagil.app.engine.QuestLevel
import com.chagil.app.vehicle.Raw
import com.chagil.app.vehicle.Reading
import com.chagil.app.vehicle.Signal
import com.chagil.app.vehicle.VehicleSignalHub
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * L0 driving: no app screen, no sound.
 * L1 stopped, gear not confirmed by the car: operation drills only, after the driver says "P에 두었어요".
 * L2 parked: the car reports GEAR_PARK.
 */
enum class SafetyLevel { L0_DRIVING, L1_STOPPED_UNCONFIRMED, L2_PARKED }

data class SafetyStatus(val level: SafetyLevel, val driverConfirmed: Boolean) {
    /** L1 and the driver has not answered "P에 두셨나요?" yet → show the confirmation dialog. */
    val needsConfirmation: Boolean get() = level == SafetyLevel.L1_STOPPED_UNCONFIRMED && !driverConfirmed

    /** Whether a quest needing [required] may run now. */
    fun allows(required: QuestLevel): Boolean = when (level) {
        SafetyLevel.L0_DRIVING -> false
        SafetyLevel.L2_PARKED -> true
        SafetyLevel.L1_STOPPED_UNCONFIRMED -> required == QuestLevel.L1 && driverConfirmed
    }
}

/**
 * Rules (SPEC §4):
 * - uxRestricted == true, or gear ∈ {DRIVE, REVERSE, 1..9} → L0.
 * - gear == PARK → L2.
 * - anything else (UNKNOWN / NEUTRAL / unreadable / not yet read) → L1; the driver may confirm.
 * - Entering L0 (e.g. P → D) clears the driver's confirmation.
 */
class SafetyMonitor(hub: VehicleSignalHub, scope: CoroutineScope) {

    private val _confirmed = MutableStateFlow(false)
    val driverConfirmed: StateFlow<Boolean> = _confirmed.asStateFlow()

    private val _level = MutableStateFlow(levelFor(hub.readings.value[Signal.GEAR], hub.uxRestricted.value))
    val level: StateFlow<SafetyLevel> = _level.asStateFlow()

    private val _status = MutableStateFlow(SafetyStatus(_level.value, false))
    val status: StateFlow<SafetyStatus> = _status.asStateFlow()

    init {
        scope.launch {
            combine(hub.readings, hub.uxRestricted) { readings, ux -> levelFor(readings[Signal.GEAR], ux) }
                .collect { level ->
                    if (level == SafetyLevel.L0_DRIVING) _confirmed.value = false
                    _level.value = level
                    publish()
                }
        }
    }

    /** Driver answered "네, P에 두었어요". Ignored while driving. */
    fun confirmParkedByDriver() {
        if (_level.value == SafetyLevel.L0_DRIVING) return
        _confirmed.value = true
        publish()
    }

    fun clearDriverConfirmation() {
        _confirmed.value = false
        publish()
    }

    fun allows(required: QuestLevel): Boolean = _status.value.allows(required)

    private fun publish() {
        _status.value = SafetyStatus(_level.value, _confirmed.value)
    }

    companion object {
        fun levelFor(gear: Reading?, uxRestricted: Boolean?): SafetyLevel {
            val raw = (gear as? Reading.Value)?.raw
            return when {
                uxRestricted == true || Raw.isMovingGear(raw) -> SafetyLevel.L0_DRIVING
                raw == Raw.GEAR_PARK -> SafetyLevel.L2_PARKED
                else -> SafetyLevel.L1_STOPPED_UNCONFIRMED
            }
        }
    }
}
