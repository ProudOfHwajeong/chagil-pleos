package com.chagil.app.engine

import java.util.Locale
import kotlinx.serialization.Serializable

enum class StepStatus {
    /** Waiting for the driver to move the control. */
    WAITING,

    /** The value is right; holding it for `stableMs`. Show "차가 확인하는 중". */
    STABILIZING,

    /** Transient: the step just passed (the engine moves on immediately). */
    PASSED,

    /** The car cannot report this value: show the "켰어요" button → [DrillEngine.selfCheck]. */
    SELF_CHECK_NEEDED,
}

enum class HintKind {
    /** The reading changed to a value listed in the step's `wrong` map. */
    WRONG_VALUE,

    /** A quest guard is violated (e.g. wiper on a frozen windshield). */
    GUARD,
}

data class Hint(val text: String, val voice: String?, val kind: HintKind = HintKind.WRONG_VALUE) {
    val spoken: String get() = voice?.takeIf { it.isNotBlank() } ?: text
}

enum class PauseReason {
    /** Safety level dropped below the quest level (driving, or gear no longer confirmed). */
    SAFETY,

    /** The driver pressed 일시정지. */
    USER,
}

/** One judgement-log line. [atMs] is drill time (pauses excluded); [text] has the plan's format without the time. */
data class LogEntry(val atMs: Long, val text: String) {
    /** e.g. "00:05.6 · Wiper 스위치 OFF → INTERMITTENT_LEVEL_1 · 1초 안정 · 통과" */
    val line: String get() = "${LogFormat.clock(atMs)} · $text"
}

sealed interface DrillState {
    data object Idle : DrillState

    data class Running(
        val questId: String,
        /** Current step. Steps before it are passed. */
        val stepIndex: Int,
        val remainingMs: Long,
        val stepStatus: StepStatus,
        /** Raw value the car reports for the current step's signal (null = not read / unreadable). */
        val lastRead: String?,
        /** Show this instead of the instruction while non-null. */
        val hint: Hint?,
        val elapsedMs: Long = 0,
        val stepCount: Int = 0,
    ) : DrillState

    data class Paused(val reason: PauseReason, val snapshot: Running) : DrillState

    data class TimedOut(
        val questId: String,
        /** The step the driver could not find; show its `where`. */
        val stepIndex: Int,
        val passed: List<Boolean>,
    ) : DrillState

    data class Completed(
        val questId: String,
        val elapsedMs: Long,
        val previousBestMs: Long?,
        /** ControlPoint keys used in this drill, judged by the car (self-checked steps excluded). */
        val litControls: Set<String> = emptySet(),
    ) : DrillState
}

/** One-shot moments, mainly for voice. Collect [DrillEngine.events]. */
sealed interface DrillEvent {
    data class StepStarted(val index: Int, val step: Step) : DrillEvent
    data class WrongValue(val hint: Hint) : DrillEvent
    data class GuardViolated(val hint: Hint) : DrillEvent
    data class StepPassed(val index: Int, val step: Step, val alreadyOn: Boolean, val selfChecked: Boolean) : DrillEvent
    data class Paused(val reason: PauseReason) : DrillEvent
    data object Resumed : DrillEvent
    data class TimedOut(val stepIndex: Int, val where: Speech?) : DrillEvent
    data class Completed(val elapsedMs: Long, val previousBestMs: Long?, val done: Speech?) : DrillEvent
}

/** Saved when a drill pauses, so "이어서 할까요?" works even after the app was closed. */
@Serializable
data class DrillSnapshot(
    val questId: String,
    val stepIndex: Int,
    val remainingMs: Long,
    val elapsedMs: Long,
)

/** Persistence the engine needs. Implemented by `data.ProgressStore`. */
interface DrillRecords {
    fun bestMs(questId: String): Long?

    /** Stores the run; keeps the best time; lights the given ControlPoint keys. */
    fun saveResult(questId: String, elapsedMs: Long, litControls: Set<String>)

    fun savePausedSnapshot(snapshot: DrillSnapshot)
    fun clearPausedSnapshot()

    object None : DrillRecords {
        override fun bestMs(questId: String): Long? = null
        override fun saveResult(questId: String, elapsedMs: Long, litControls: Set<String>) = Unit
        override fun savePausedSnapshot(snapshot: DrillSnapshot) = Unit
        override fun clearPausedSnapshot() = Unit
    }
}

object LogFormat {
    /** 5600 → "00:05.6" (tenths, rounded down). */
    fun clock(ms: Long): String {
        val tenths = ms.coerceAtLeast(0) / 100
        val minutes = tenths / 600
        val seconds = (tenths / 10) % 60
        return String.format(Locale.US, "%02d:%02d.%d", minutes, seconds, tenths % 10)
    }

    /** 11234 → "11.2" */
    fun seconds(ms: Long): String {
        val tenths = ms.coerceAtLeast(0) / 100
        return "${tenths / 10}.${tenths % 10}"
    }

    /** 1000 → "1", 1500 → "1.5" */
    fun shortSeconds(ms: Long): String = if (ms % 1000 == 0L) "${ms / 1000}" else seconds(ms)

    /** "지난 기록보다 3.1초 빠름" / "지난 기록 14.3초" / "첫 기록" */
    fun comparison(elapsedMs: Long, previousBestMs: Long?): String = when {
        previousBestMs == null -> "첫 기록"
        elapsedMs < previousBestMs -> "지난 기록보다 ${seconds(previousBestMs - elapsedMs)}초 빠름"
        else -> "지난 기록 ${seconds(previousBestMs)}초"
    }
}
