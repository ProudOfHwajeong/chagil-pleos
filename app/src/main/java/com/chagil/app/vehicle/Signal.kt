package com.chagil.app.vehicle

/**
 * Every vehicle value the app reads. [key] is the name used in quest JSON (`"read": "Wiper.switch"`),
 * [label] is the short name used in the judgement log.
 */
enum class Signal(val key: String, val label: String) {
    GEAR("Gear.selection", "기어"),
    WIPER_SWITCH("Wiper.switch", "Wiper 스위치"),
    WIPER_STATE("Wiper.state", "Wiper 상태"),
    LOW_BEAM_SWITCH("LowBeam.switch", "LowBeam 스위치"),
    LOW_BEAM_STATE("LowBeam.state", "LowBeam 상태"),
    DEFROST_FRONT("HVAC.defroster.FRONT", "HVAC 앞유리 디프로스터"),
    DEFROST_REAR("HVAC.defroster.REAR", "HVAC 뒷유리 디프로스터"),
    STEERING_HEAT("SteeringWheel.heat", "핸들 열선");

    companion object {
        /**
         * Resolves a quest `read` key. Also accepts the plan's short form
         * `"read": "HVAC.defroster", "area": "FRONT"`.
         */
        fun fromKey(key: String?, area: String? = null): Signal? {
            if (key.isNullOrBlank()) return null
            val full = if (!area.isNullOrBlank() && key.equals("HVAC.defroster", ignoreCase = true)) {
                "HVAC.defroster.${area.trim().uppercase()}"
            } else {
                key.trim()
            }
            return entries.firstOrNull { it.key.equals(full, ignoreCase = true) }
        }
    }
}

/**
 * Raw value strings. Enum values use the SDK enum name as-is ("MIST", "GEAR_PARK", "AUTOMATIC"),
 * Booleans are "ON"/"OFF", Ints are "0", "1", "2".
 */
object Raw {
    const val ON = "ON"
    const val OFF = "OFF"

    const val GEAR_PARK = "GEAR_PARK"
    const val GEAR_NEUTRAL = "GEAR_NEUTRAL"
    const val GEAR_REVERSE = "GEAR_REVERSE"
    const val GEAR_DRIVE = "GEAR_DRIVE"
    const val GEAR_UNKNOWN = "GEAR_UNKNOWN"

    fun of(value: Boolean): String = if (value) ON else OFF

    /** D, R and GEAR_1..GEAR_9 mean the car can move. */
    fun isMovingGear(raw: String?): Boolean =
        raw == GEAR_DRIVE || raw == GEAR_REVERSE || (raw != null && MANUAL_GEAR.matches(raw))

    private val MANUAL_GEAR = Regex("GEAR_[1-9]")
}
