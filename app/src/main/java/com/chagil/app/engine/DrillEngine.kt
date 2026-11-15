package com.chagil.app.engine

import com.chagil.app.safety.SafetyMonitor
import com.chagil.app.safety.SafetyStatus
import com.chagil.app.vehicle.Reading
import com.chagil.app.vehicle.VehicleSignalHub
import com.chagil.app.vehicle.rawOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * The single judgement engine. A quest is a list of "the car value must become X" steps; the car decides.
 *
 * Rules (SPEC §5):
 * - A step passes when its condition (`to` | `toAnyOf` | `notAnyOf`) holds for `stableMs` without interruption.
 * - Already satisfied at step start → passes after `stableMs` (log "이미 켜져 있음"), unless `alreadyOn` says otherwise.
 * - Reading changes to a key in `wrong` → hint immediately + log "다른 칸".
 * - Unreadable signal with `ifUnreadable: "selfCheck"` → [StepStatus.SELF_CHECK_NEEDED]; [selfCheck] passes it.
 *   With `ifNoChange: "selfCheck"` (opt-in, only for controls whose mapping is not verified, never Wiper.switch)
 *   the same happens when a readable value has not changed at all for [NO_CHANGE_SELF_CHECK_MS]. The car value
 *   stays the primary pass: if it later matches, the step passes normally.
 * - Guards: hint while violated, never fail.
 * - `limitSec` runs out → [DrillState.TimedOut]. Paused time does not count.
 * - Safety below the quest level → [DrillState.Paused] (SAFETY); [resume] continues with the same remaining time.
 *
 * Threading: give it a single-threaded scope (Main in the app, the test dispatcher in tests).
 * [clock] must be monotonic (SystemClock.elapsedRealtime in the app, virtual time in tests).
 */
class DrillEngine(
    private val hub: VehicleSignalHub,
    private val safety: SafetyMonitor,
    private val clock: () -> Long,
    private val scope: CoroutineScope,
    private val records: DrillRecords = DrillRecords.None,
    private val tickMs: Long = 100,
) {
    private val _state = MutableStateFlow<DrillState>(DrillState.Idle)
    val state: StateFlow<DrillState> = _state.asStateFlow()

    private val _log = MutableStateFlow<List<LogEntry>>(emptyList())
    val log: StateFlow<List<LogEntry>> = _log.asStateFlow()

    private val _events = MutableSharedFlow<DrillEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<DrillEvent> = _events.asSharedFlow()

    /** The quest being run (also kept while paused / timed out / completed, cleared by [abort]). */
    private val _quest = MutableStateFlow<Quest?>(null)
    val quest: StateFlow<Quest?> = _quest.asStateFlow()

    /** true when paused and safety allows continuing → show "이어서 할까요?". */
    private val _resumable = MutableStateFlow(false)
    val resumable: StateFlow<Boolean> = _resumable.asStateFlow()

    private var session: Session? = null
    private var job: Job? = null

    // ---- public API ----------------------------------------------------------------------------

    /** Starts (or restarts) a quest from step 1. If safety does not allow it, it starts paused (SAFETY). */
    fun start(quest: Quest) = begin(quest, stepIndex = 0, elapsedMs = 0, continued = false)

    /** Continues a drill saved before the app was closed ([DrillRecords.savePausedSnapshot]). */
    fun resumeFrom(quest: Quest, snapshot: DrillSnapshot) =
        begin(quest, snapshot.stepIndex.coerceIn(0, (quest.steps.size - 1).coerceAtLeast(0)), snapshot.elapsedMs, continued = true)

    /** The driver taps "켰어요". Works only while the step is [StepStatus.SELF_CHECK_NEEDED]. */
    fun selfCheck(): Boolean {
        val s = session ?: return false
        if (s.runningSince == null || s.status != StepStatus.SELF_CHECK_NEEDED) return false
        pass(s, selfChecked = true)
        return true
    }

    fun pause() {
        val s = session ?: return
        if (s.runningSince != null) pauseInternal(s, PauseReason.USER)
    }

    /** Continues a paused drill if safety allows. Returns false (and stays paused) otherwise. */
    fun resume(): Boolean {
        val s = session ?: return false
        if (s.runningSince != null) return false
        if (!safety.allows(s.quest.level)) return false
        s.runningSince = clock()
        s.stableSince = null
        records.clearPausedSnapshot()
        addLog(s, "이어서 시작")
        _events.tryEmit(DrillEvent.Resumed)
        _resumable.value = false
        evaluate(s)
        return true
    }

    /** 그만하기 / 나중에: back to Idle. */
    fun abort() {
        session?.let { addLog(it, "그만하기") }
        finish()
        records.clearPausedSnapshot()
        _quest.value = null
        _state.value = DrillState.Idle
    }

    // ---- lifecycle -----------------------------------------------------------------------------

    private fun begin(quest: Quest, stepIndex: Int, elapsedMs: Long, continued: Boolean) {
        finish()
        _log.value = emptyList()
        _quest.value = quest
        if (quest.steps.isEmpty()) {
            _state.value = DrillState.Idle
            return
        }
        val s = Session(quest, elapsedMs)
        session = s
        records.clearPausedSnapshot()
        s.runningSince = clock()
        addLog(s, if (continued) "${quest.title} 이어서 시작 · ${stepIndex + 1}/${quest.steps.size}단계" else "${quest.title} 시작")
        enterStep(s, stepIndex)
        if (session !== s) return // finished instantly
        if (!safety.allows(quest.level)) pauseInternal(s, PauseReason.SAFETY)
        job = scope.launch {
            launch { hub.readings.collect { if (session === s && s.runningSince != null) evaluate(s) } }
            launch { safety.status.collect { if (session === s) onSafety(s, it) } }
            launch {
                while (isActive) {
                    delay(tickMs)
                    if (session === s) onTick(s)
                }
            }
        }
    }

    private fun finish() {
        session = null
        _resumable.value = false
        job?.cancel()
        job = null
    }

    private fun onSafety(s: Session, status: SafetyStatus) {
        val allowed = status.allows(s.quest.level)
        when (val st = _state.value) {
            is DrillState.Running -> if (!allowed) pauseInternal(s, PauseReason.SAFETY)
            is DrillState.Paused -> if (!allowed && st.reason == PauseReason.USER) {
                _state.value = st.copy(reason = PauseReason.SAFETY)
            }
            else -> Unit
        }
        _resumable.value = s.runningSince == null && allowed
    }

    private fun onTick(s: Session) {
        if (s.runningSince == null) return
        val since = s.stableSince
        if (since != null && clock() - since >= s.step.stableMs) {
            val raw = s.step.signal?.let { hub.readings.value[it] }.rawOrNull
            if (raw != null && s.step.matches(raw)) {
                pass(s, selfChecked = false)
                return
            }
        }
        if (elapsed(s) >= s.quest.limitMs) {
            timeout(s)
            return
        }
        evaluate(s) // refreshes remaining time and time-based self-check
    }

    private fun pauseInternal(s: Session, reason: PauseReason) {
        val snapshot = running(s)
        addLog(s, if (reason == PauseReason.SAFETY) "일시정지 · 주차 확인이 풀림" else "일시정지")
        s.accumulatedMs = elapsed(s)
        s.runningSince = null
        s.stableSince = null
        _state.value = DrillState.Paused(reason, snapshot)
        records.savePausedSnapshot(DrillSnapshot(s.quest.id, s.stepIndex, snapshot.remainingMs, s.accumulatedMs))
        _events.tryEmit(DrillEvent.Paused(reason))
        _resumable.value = safety.allows(s.quest.level)
    }

    // ---- steps ---------------------------------------------------------------------------------

    private fun enterStep(s: Session, index: Int) {
        s.stepIndex = index
        s.stepEnteredAt = elapsed(s)
        s.lastValue = null
        s.prevValue = null
        s.changed = false
        s.noChangeLogged = false
        s.selfCheckForNoChange = false
        s.stableSince = null
        s.hint = null
        s.status = StepStatus.WAITING
        val step = s.step
        _events.tryEmit(DrillEvent.StepStarted(index, step))
        // A wrong value already present at step start gets no hint: hints answer a driver action (SPEC: "바뀌면").
        s.lastValue = step.signal?.let { hub.readings.value[it] }.rawOrNull
        evaluate(s)
    }

    /** Re-reads the current step and guards, then publishes Running. Idempotent. */
    private fun evaluate(s: Session) {
        if (session !== s || s.runningSince == null) return
        evaluateGuards(s)
        val step = s.step
        val reading = step.signal?.let { hub.readings.value[it] ?: Reading.Unknown } ?: Reading.Unreadable
        when (reading) {
            is Reading.Value -> {
                val raw = reading.raw
                val previous = s.lastValue
                if (previous == null) {
                    s.lastValue = raw // first value seen in this step: not a driver action
                } else if (raw != previous) {
                    s.prevValue = previous
                    s.lastValue = raw
                    s.changed = true
                    onValueChanged(s, previous, raw)
                }
                if (step.matches(raw) && (s.changed || step.passIfAlreadyOn)) {
                    if (s.stableSince == null) s.stableSince = clock()
                    s.status = StepStatus.STABILIZING
                    s.hint = null
                    if (step.stableMs <= 0) {
                        pass(s, selfChecked = false)
                        return
                    }
                } else {
                    s.stableSince = null
                    val noChange = step.selfCheckIfNoChange && !s.changed &&
                        elapsed(s) - s.stepEnteredAt >= NO_CHANGE_SELF_CHECK_MS
                    if (noChange && !s.noChangeLogged) {
                        s.noChangeLogged = true
                        addLog(s, "${step.signal?.label ?: step.read} $raw · 값이 바뀌지 않음 · 운전자 확인")
                    }
                    s.status = if (noChange) StepStatus.SELF_CHECK_NEEDED else StepStatus.WAITING
                    s.selfCheckForNoChange = noChange
                }
            }
            Reading.Unreadable, Reading.Unknown -> {
                s.stableSince = null
                val unreadable = reading == Reading.Unreadable ||
                    elapsed(s) - s.stepEnteredAt >= UNKNOWN_AS_UNREADABLE_MS
                s.status = if (step.selfCheckIfUnreadable && unreadable) StepStatus.SELF_CHECK_NEEDED else StepStatus.WAITING
                s.selfCheckForNoChange = false
            }
        }
        _state.value = running(s)
    }

    private fun onValueChanged(s: Session, from: String, to: String) {
        val step = s.step
        if (step.matches(to)) return
        val wrongHint = step.wrongHintFor(to)
        if (wrongHint != null) wrong(s, to, wrongHint, from) else s.hint = null
    }

    private fun wrong(s: Session, raw: String, speech: Speech, from: String) {
        val hint = Hint(speech.text, speech.voice, HintKind.WRONG_VALUE)
        s.hint = hint
        val label = s.step.signal?.label ?: s.step.read
        addLog(s, "$label $from → $raw · 다른 칸: \"${speech.text}\"")
        _events.tryEmit(DrillEvent.WrongValue(hint))
    }

    private fun evaluateGuards(s: Session) {
        var shown: Hint? = null
        s.quest.guards.forEachIndexed { i, guard ->
            val raw = guard.signal?.let { hub.readings.value[it] }.rawOrNull
            val violated = raw != null && guard.isViolatedBy(raw)
            if (violated) {
                val hint = Hint(guard.text, guard.voice, HintKind.GUARD)
                if (s.violatedGuards.add(i)) {
                    addLog(s, "${guard.signal?.label ?: guard.read} $raw · 주의: \"${guard.text}\"")
                    _events.tryEmit(DrillEvent.GuardViolated(hint))
                }
                if (shown == null) shown = hint
            } else {
                s.violatedGuards.remove(i)
            }
        }
        s.guardHint = shown
    }

    private fun pass(s: Session, selfChecked: Boolean) {
        val step = s.step
        val label = step.signal?.label ?: step.read
        val value = s.lastValue ?: step.targetText
        val stable = if (step.stableMs > 0) " · ${LogFormat.shortSeconds(step.stableMs)}초 안정" else ""
        val alreadyOn = !selfChecked && !s.changed
        addLog(
            s,
            when {
                selfChecked && s.noChangeLogged -> "$label 값이 바뀌지 않음 · 운전자 확인 · 통과"
                selfChecked -> "$label 읽을 수 없음 · 운전자 확인 · 통과"
                step.isRestore && alreadyOn -> "$label $value · 되돌리기 · 이미 되어 있음 · 통과"
                step.isRestore -> "$label → $value · 되돌리기 · 통과"
                alreadyOn -> "$label $value · 이미 켜져 있음 · 통과"
                else -> "$label ${s.prevValue} → $value$stable · 통과"
            },
        )
        s.status = StepStatus.PASSED
        if (selfChecked) s.selfChecked += s.stepIndex
        _events.tryEmit(DrillEvent.StepPassed(s.stepIndex, step, alreadyOn, selfChecked))
        if (s.stepIndex + 1 < s.quest.steps.size) enterStep(s, s.stepIndex + 1) else complete(s)
    }

    private fun complete(s: Session) {
        val elapsed = elapsed(s)
        val quest = s.quest
        val previousBest = records.bestMs(quest.id)
        val lit = quest.steps.filterIndexed { i, _ -> i !in s.selfChecked }.mapNotNull { it.control }.toSet()
        records.saveResult(quest.id, elapsed, lit)
        addLog(s, "드릴 완료 · ${LogFormat.seconds(elapsed)}초 · ${LogFormat.comparison(elapsed, previousBest)}")
        finish()
        _state.value = DrillState.Completed(quest.id, elapsed, previousBest, lit)
        _events.tryEmit(DrillEvent.Completed(elapsed, previousBest, quest.done))
    }

    private fun timeout(s: Session) {
        val quest = s.quest
        addLog(s, "시간 종료 · ${s.stepIndex}/${quest.steps.size}단계 통과")
        finish()
        _state.value = DrillState.TimedOut(quest.id, s.stepIndex, quest.steps.indices.map { it < s.stepIndex })
        _events.tryEmit(DrillEvent.TimedOut(s.stepIndex, s.step.where))
    }

    // ---- helpers -------------------------------------------------------------------------------

    private fun running(s: Session) = DrillState.Running(
        questId = s.quest.id,
        stepIndex = s.stepIndex,
        remainingMs = (s.quest.limitMs - elapsed(s)).coerceAtLeast(0),
        stepStatus = s.status,
        lastRead = s.lastValue,
        hint = s.guardHint ?: s.hint,
        elapsedMs = elapsed(s),
        stepCount = s.quest.steps.size,
        selfCheckForNoChange = s.status == StepStatus.SELF_CHECK_NEEDED && s.selfCheckForNoChange,
    )

    private fun elapsed(s: Session): Long = s.accumulatedMs + (s.runningSince?.let { clock() - it } ?: 0L)

    private fun addLog(s: Session, text: String) {
        val entry = LogEntry(elapsed(s), text)
        _log.update { (it + entry).takeLast(MAX_LOG) }
    }

    private class Session(val quest: Quest, var accumulatedMs: Long) {
        var runningSince: Long? = null
        var stepIndex = 0
        var stepEnteredAt = 0L
        var lastValue: String? = null
        var prevValue: String? = null
        var changed = false
        var noChangeLogged = false
        var selfCheckForNoChange = false
        var stableSince: Long? = null
        var status = StepStatus.WAITING
        var hint: Hint? = null
        var guardHint: Hint? = null
        val violatedGuards = mutableSetOf<Int>()
        val selfChecked = mutableSetOf<Int>()
        val step: Step get() = quest.steps[stepIndex]
    }

    companion object {
        /** A selfCheck step whose value never arrives is treated as unreadable after this long. */
        const val UNKNOWN_AS_UNREADABLE_MS = 3_000L

        /** An `ifNoChange: "selfCheck"` step whose readable value never changes offers "켰어요" after this long. */
        const val NO_CHANGE_SELF_CHECK_MS = 8_000L
        private const val MAX_LOG = 200
    }
}
