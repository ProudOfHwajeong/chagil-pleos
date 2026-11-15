package com.chagil.app.engine

import com.chagil.app.vehicle.Signal
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Minimum safety level a quest needs (SPEC §5 `level`). */
@Serializable
enum class QuestLevel { L1, L2 }

@Serializable
enum class Availability {
    @SerialName("open") OPEN,
    @SerialName("coming_soon") COMING_SOON,
}

/** Where a control sits; the designer draws one location picture per key (SPEC §5 `control`). */
enum class ControlPoint {
    LIGHT_STALK, WIPER_STALK, HVAC_FRONT_DEFROST, HVAC_REAR_DEFROST, STEERING_HEAT_BUTTON, GEAR_SELECTOR, SEAT_BELT;

    companion object {
        fun fromKey(key: String?): ControlPoint? = entries.firstOrNull { it.name.equals(key?.trim(), ignoreCase = true) }
    }
}

/** Text for the screen plus an optional separate sentence for TTS. Accepts `"..."` or `{"text","voice"}` in JSON. */
@Serializable(with = SpeechSerializer::class)
data class Speech(val text: String, val voice: String? = null) {
    val spoken: String get() = voice?.takeIf { it.isNotBlank() } ?: text
}

/** A quest file, `assets/quests/<id>.json` (SPEC §5). Unknown keys are ignored. */
@Serializable
data class Quest(
    val id: String,
    val title: String = "",
    val subtitle: String? = null,
    val level: QuestLevel = QuestLevel.L1,
    val limitSec: Int = 60,
    val zone: String? = null,
    val availability: Availability = Availability.OPEN,
    /** e.g. "현대차 안내" for manufacturer quests. */
    val label: String? = null,
    /** Optional sort key for the home screen (lower first). */
    val order: Int = 100,
    val intro: Speech? = null,
    val steps: List<Step> = emptyList(),
    val guards: List<Guard> = emptyList(),
    val done: Speech? = null,
) {
    val isOpen: Boolean get() = availability == Availability.OPEN
    val limitMs: Long get() = limitSec.coerceAtLeast(1) * 1000L

    /** Problems a content author should fix. Empty = fine. */
    fun validate(): List<String> = buildList {
        if (id.isBlank()) add("id가 비어 있음")
        if (isOpen && steps.isEmpty()) add("$id: 단계가 없음")
        steps.forEachIndexed { i, s ->
            if (s.signal == null && s.ifUnreadable != Step.SELF_CHECK) add("$id 단계 ${i + 1}: 알 수 없는 read '${s.read}'")
            if (s.to == null && s.toAnyOf.isNullOrEmpty() && s.notAnyOf.isNullOrEmpty()) {
                add("$id 단계 ${i + 1}: to / toAnyOf / notAnyOf 중 하나가 필요함")
            }
        }
        guards.forEachIndexed { i, g -> if (g.signal == null) add("$id guard ${i + 1}: 알 수 없는 read '${g.read}'") }
    }
}

@Serializable
data class Step(
    val id: String = "",
    val label: String = "",
    val instruction: String = "",
    val voice: String? = null,
    val control: String? = null,
    val read: String = "",
    /** Only for the short form `"read": "HVAC.defroster", "area": "FRONT"`. */
    val area: String? = null,
    val to: String? = null,
    val toAnyOf: List<String>? = null,
    val notAnyOf: List<String>? = null,
    val stableMs: Long = 1000,
    /** "pass" (default when absent): already satisfied at step start → pass after stableMs. Any other value: needs a change. */
    val alreadyOn: String? = null,
    /** "selfCheck": when the car cannot report the value, the driver taps "켰어요". */
    val ifUnreadable: String? = null,
    /**
     * "selfCheck": also offer "켰어요" when a readable value has not changed for 8 s. Only for controls whose
     * console/car mapping is not verified (the button may not move this signal). Never on Wiper.switch.
     */
    val ifNoChange: String? = null,
    /** Raw value → hint shown immediately when the reading changes to that value. */
    val wrong: Map<String, Speech> = emptyMap(),
    /** Shown on the time-out screen: where this control is. */
    val where: Speech? = null,
    /** "restore": put things back at the end. */
    val role: String? = null,
) {
    val signal: Signal? get() = Signal.fromKey(read, area)
    val controlPoint: ControlPoint? get() = ControlPoint.fromKey(control)
    val isRestore: Boolean get() = role.equals("restore", ignoreCase = true)
    val passIfAlreadyOn: Boolean get() = alreadyOn == null || alreadyOn.equals("pass", ignoreCase = true)
    val selfCheckIfUnreadable: Boolean get() = ifUnreadable.equals(SELF_CHECK, ignoreCase = true)
    val selfCheckIfNoChange: Boolean get() = ifNoChange.equals(SELF_CHECK, ignoreCase = true)

    fun matches(raw: String): Boolean = Condition.matches(raw, to, toAnyOf, notAnyOf)

    fun wrongHintFor(raw: String): Speech? =
        wrong.entries.firstOrNull { Condition.normalize(it.key) == Condition.normalize(raw) }?.value

    /** Short target text for logs, e.g. "ON", "INTERMITTENT_LEVEL_1/CONTINUOUS_LEVEL_1", "OFF 아님". */
    val targetText: String
        get() = to ?: toAnyOf?.joinToString("/") ?: notAnyOf?.let { "${it.joinToString("/")} 아님" } ?: "?"

    companion object {
        const val SELF_CHECK = "selfCheck"
    }
}

/**
 * Something that must not happen during the whole quest. The condition describes the VIOLATION:
 * `{"read": "Wiper.switch", "notAnyOf": ["OFF"]}` = "violated while the wiper is not OFF".
 * While violated the engine shows [text] as a hint; it never fails the drill.
 */
@Serializable
data class Guard(
    val read: String = "",
    val area: String? = null,
    val to: String? = null,
    val toAnyOf: List<String>? = null,
    val notAnyOf: List<String>? = null,
    val text: String = "",
    val voice: String? = null,
) {
    val signal: Signal? get() = Signal.fromKey(read, area)
    fun isViolatedBy(raw: String): Boolean = Condition.matches(raw, to, toAnyOf, notAnyOf)
}

internal object Condition {
    fun matches(raw: String, to: String?, toAnyOf: List<String>?, notAnyOf: List<String>?): Boolean {
        val v = normalize(raw)
        return when {
            to != null -> v == normalize(to)
            !toAnyOf.isNullOrEmpty() -> toAnyOf.any { normalize(it) == v }
            !notAnyOf.isNullOrEmpty() -> notAnyOf.none { normalize(it) == v }
            else -> false
        }
    }

    /** Case-insensitive; "true"/"false" from content files mean "ON"/"OFF". */
    fun normalize(value: String): String = when (val u = value.trim().uppercase()) {
        "TRUE" -> "ON"
        "FALSE" -> "OFF"
        else -> u
    }
}

@Serializable
internal data class SpeechSurrogate(val text: String = "", val voice: String? = null)

internal object SpeechSerializer : KSerializer<Speech> {
    override val descriptor: SerialDescriptor = SpeechSurrogate.serializer().descriptor

    override fun deserialize(decoder: Decoder): Speech {
        if (decoder !is JsonDecoder) {
            val s = decoder.decodeSerializableValue(SpeechSurrogate.serializer())
            return Speech(s.text, s.voice)
        }
        return when (val element = decoder.decodeJsonElement()) {
            is JsonPrimitive -> Speech(element.content)
            is JsonObject -> decoder.json.decodeFromJsonElement(SpeechSurrogate.serializer(), element).let { Speech(it.text, it.voice) }
            else -> Speech("")
        }
    }

    override fun serialize(encoder: Encoder, value: Speech) {
        encoder.encodeSerializableValue(SpeechSurrogate.serializer(), SpeechSurrogate(value.text, value.voice))
    }
}
