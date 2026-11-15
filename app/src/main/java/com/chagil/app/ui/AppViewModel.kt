package com.chagil.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chagil.app.core.AppContainer
import com.chagil.app.engine.DrillEvent
import com.chagil.app.engine.DrillState
import com.chagil.app.engine.LogFormat
import com.chagil.app.engine.Quest
import com.chagil.app.safety.SafetyLevel
import com.chagil.app.vehicle.Raw
import com.chagil.app.vehicle.Signal
import com.chagil.app.vehicle.rawOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Top-level destinations. The drill screen shows whatever [DrillState] the engine is in. */
enum class Nav { HOME, DRILL, MAP }

/** Which safety prompt is on screen. */
enum class SafetyPrompt {
    /** "P에 두셨나요?" — the car cannot read the gear (UNKNOWN / unreadable). */
    CONFIRM_PARK,

    /** The car reads N: the driver must move to P; we do not ask for a confirmation (domain review). */
    MOVE_TO_PARK,
}

/**
 * Screen-layer state and actions. All judging lives in feat/core ([AppContainer]); this class only
 * routes taps to the engine, keeps navigation, and turns engine events into voice.
 */
class AppViewModel(app: Application) : AndroidViewModel(app) {

    val c: AppContainer = AppContainer.get(app)

    /** Home sort: the plan's order (rain → car wash → frost → departure), then the JSON `order`. */
    val quests: List<Quest> = c.quests.all().sortedWith(
        compareBy<Quest>({ PREFERRED_ORDER.indexOf(it.id).let { i -> if (i < 0) Int.MAX_VALUE else i } }, { it.order }, { it.id }),
    )

    private val _nav = MutableStateFlow(Nav.HOME)
    val nav: StateFlow<Nav> = _nav.asStateFlow()

    /** Where the map was opened from, so 닫기 goes back there. */
    private var mapReturn = Nav.HOME

    private val _panelOpen = MutableStateFlow(false)
    val panelOpen: StateFlow<Boolean> = _panelOpen.asStateFlow()

    /** null = follow the system day/night mode (AAOS), true = night, false = day (demo toggle). */
    private val _nightOverride = MutableStateFlow<Boolean?>(null)
    val nightOverride: StateFlow<Boolean?> = _nightOverride.asStateFlow()

    private val _prompt = MutableStateFlow<SafetyPrompt?>(null)
    val prompt: StateFlow<SafetyPrompt?> = _prompt.asStateFlow()

    /** Quest to start once the driver answers "P에 뒀어요". null = the prompt is for resuming a paused drill. */
    private var pendingQuest: Quest? = null

    init {
        viewModelScope.launch { c.engine.events.collect(::speakFor) }
        // Leaving the drill state machine (abort) always returns home.
        viewModelScope.launch {
            c.engine.state.collect { if (it is DrillState.Idle && _nav.value == Nav.DRILL) _nav.value = Nav.HOME }
        }
        // Once the car itself reads P, an open confirmation is no longer needed.
        viewModelScope.launch {
            c.safety.level.collect { if (it != SafetyLevel.L1_STOPPED_UNCONFIRMED) _prompt.value = null }
        }
    }

    fun quest(id: String?): Quest? = quests.firstOrNull { it.id == id }

    // ---- navigation ------------------------------------------------------------------------------

    fun openMap() {
        mapReturn = if (_nav.value == Nav.MAP) mapReturn else _nav.value
        _nav.value = Nav.MAP
    }

    fun closeMap() {
        _nav.value = if (mapReturn == Nav.DRILL && c.engine.state.value !is DrillState.Idle) Nav.DRILL else Nav.HOME
    }

    fun goHome() {
        if (c.engine.state.value !is DrillState.Idle) c.engine.abort()
        _nav.value = Nav.HOME
    }

    fun setPanel(open: Boolean) {
        _panelOpen.value = open
    }

    fun setNightOverride(night: Boolean?) {
        _nightOverride.value = night
    }

    // ---- drills ----------------------------------------------------------------------------------

    /** Home card tap. Opens the safety prompt instead when the current level does not allow the quest. */
    fun onCardTap(quest: Quest) {
        if (!quest.isOpen) return
        val status = c.safety.status.value
        if (status.allows(quest.level)) {
            startDrill(quest)
            return
        }
        if (status.level == SafetyLevel.L0_DRIVING) return
        pendingQuest = quest
        showSafetyPrompt()
    }

    fun startDrill(quest: Quest) {
        c.voice.stop()
        c.engine.start(quest)
        _nav.value = Nav.DRILL
    }

    fun retry() {
        val q = c.engine.quest.value ?: return
        onCardTap(q)
    }

    /** Paused by safety and the car still cannot read P → ask the driver. */
    fun askToResume() {
        pendingQuest = null
        showSafetyPrompt()
    }

    private fun showSafetyPrompt() {
        val gear = c.hub.readings.value[Signal.GEAR].rawOrNull
        _prompt.value = if (gear == Raw.GEAR_NEUTRAL) SafetyPrompt.MOVE_TO_PARK else SafetyPrompt.CONFIRM_PARK
        if (_prompt.value == SafetyPrompt.CONFIRM_PARK) speak("기어를 P에 두셨나요?")
    }

    fun onConfirmPark() {
        c.safety.confirmParkedByDriver()
        _prompt.value = null
        val q = pendingQuest
        pendingQuest = null
        if (q != null) startDrill(q)
    }

    fun dismissPrompt() {
        _prompt.value = null
        pendingQuest = null
    }

    fun resume() {
        if (!c.engine.resume()) askToResume()
    }

    /** "이어서 할까요?" on home after the app was closed mid-drill. */
    fun resumeSaved() {
        val snap = c.progress.pausedSnapshot.value ?: return
        val q = quest(snap.questId) ?: run { c.progress.clearPausedSnapshot(); return }
        c.engine.resumeFrom(q, snap)
        _nav.value = Nav.DRILL
    }

    fun discardSaved() = c.progress.clearPausedSnapshot()

    // ---- voice -----------------------------------------------------------------------------------

    private fun speak(text: String?, interrupt: Boolean = true) {
        if (c.safety.level.value == SafetyLevel.L0_DRIVING) return // L0: no sound
        c.voice.speak(text, interrupt)
    }

    private fun speakFor(e: DrillEvent) {
        when (e) {
            is DrillEvent.StepStarted -> {
                if (e.index == 0) {
                    speak(c.engine.quest.value?.intro?.spoken)
                    speak(e.step.voice ?: e.step.instruction, interrupt = false)
                } else {
                    speak(e.step.voice ?: e.step.instruction)
                }
            }
            is DrillEvent.WrongValue -> speak(e.hint.spoken)
            is DrillEvent.GuardViolated -> speak(e.hint.spoken)
            is DrillEvent.TimedOut -> speak("괜찮아요. 여기 있어요. " + (e.where?.spoken ?: ""))
            is DrillEvent.Completed -> speak(listOfNotNull(e.done?.spoken, completionPhrase(e.elapsedMs, e.previousBestMs)).joinToString(" "))
            is DrillEvent.Paused, DrillEvent.Resumed, is DrillEvent.StepPassed -> Unit
        }
    }

    companion object {
        val PREFERRED_ORDER = listOf("rain_drill", "carwash_drill", "frost_drill", "departure_drill")

        /** SCREENS §6 delta copy. */
        fun completionPhrase(elapsedMs: Long, previousBestMs: Long?): String = when {
            previousBestMs == null -> "첫 기록이에요."
            elapsedMs < previousBestMs -> "지난번보다 ${LogFormat.seconds(previousBestMs - elapsedMs)}초 빨라요."
            else -> "지난 기록 ${LogFormat.seconds(previousBestMs)}초."
        }
    }
}
