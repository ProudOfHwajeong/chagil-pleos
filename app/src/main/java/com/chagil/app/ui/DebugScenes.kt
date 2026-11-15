package com.chagil.app.ui

import androidx.lifecycle.viewModelScope
import com.chagil.app.engine.DrillState
import com.chagil.app.vehicle.Raw
import com.chagil.app.vehicle.Signal
import com.chagil.app.vehicle.SourceMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Debug-only scenes for screenshots / rehearsal (MainActivity calls this only when BuildConfig.DEBUG):
 * `adb shell am start -n com.chagil.app/.MainActivity --es screen <name> [--es theme night|day|auto]`.
 * Every scene drives the real engine through the simulator, so what you see is what the engine judged.
 */
object DebugScenes {
    val names = listOf(
        "home", "confirm", "neutral", "drill", "hint", "selfcheck", "nochange", "pause", "resume", "rest",
        "timeout", "completed", "map", "sim",
    )

    private var job: Job? = null

    fun run(vm: AppViewModel, scene: String?, theme: String?) {
        when (theme) {
            "night" -> vm.setNightOverride(true)
            "day" -> vm.setNightOverride(false)
            "auto" -> vm.setNightOverride(null)
        }
        if (scene == null) return
        job?.cancel()
        job = vm.viewModelScope.launch { play(vm, scene) }
    }

    private suspend fun play(vm: AppViewModel, scene: String) {
        val c = vm.c
        val sim = c.simulator
        // Wait for the SDK probe (≤ 3 s) so a fallback does not reset what the scene sets up.
        withTimeoutOrNull(5_000) { c.hub.probing.first { !it } }
        if (c.hub.mode.value != SourceMode.SIMULATION) c.hub.useSimulation(true)
        if (c.engine.state.value !is DrillState.Idle) c.engine.abort()
        vm.setPanel(false)
        vm.dismissPrompt()
        sim.reset()
        vm.goHome()
        delay(400) // let SafetyMonitor see the reset values before a drill starts
        val rain = vm.quest("rain_drill") ?: return

        suspend fun drillAtStep2() {
            sim.set(Signal.LOW_BEAM_SWITCH, Raw.ON)
            delay(200)
            vm.startDrill(rain)
            delay(1_600)
        }

        when (scene) {
            "home" -> Unit
            "confirm" -> { sim.setUnreadable(Signal.GEAR); delay(200); vm.onCardTap(rain) }
            "neutral" -> { sim.set(Signal.GEAR, Raw.GEAR_NEUTRAL); delay(200); vm.onCardTap(rain) }
            "drill" -> drillAtStep2()
            "hint" -> { drillAtStep2(); sim.set(Signal.WIPER_SWITCH, "AUTO") }
            "selfcheck" -> { sim.setUnreadable(Signal.LOW_BEAM_SWITCH); vm.startDrill(rain) }
            "nochange" -> vm.startDrill(rain) // lowbeam stays OFF: after 8 s the "값이 아직 그대로예요" check
            "pause" -> { drillAtStep2(); c.engine.pause() }
            "rest" -> { drillAtStep2(); sim.set(Signal.GEAR, Raw.GEAR_DRIVE) }
            "resume" -> { drillAtStep2(); sim.set(Signal.GEAR, Raw.GEAR_DRIVE); delay(1_000); sim.set(Signal.GEAR, Raw.GEAR_PARK) }
            "timeout" -> { sim.set(Signal.LOW_BEAM_SWITCH, Raw.ON); vm.startDrill(rain.copy(limitSec = 4)) }
            "completed" -> {
                // Repeatable captures: drop records of earlier runs, then seed one "last time" of 14.3 s.
                c.progress.resetAll()
                c.progress.saveResult(rain.id, 14_300, emptySet())
                sim.set(Signal.LOW_BEAM_SWITCH, Raw.ON)
                vm.startDrill(rain)
                delay(1_500); sim.set(Signal.WIPER_SWITCH, "CONTINUOUS_LEVEL_1")
                delay(1_500); sim.set(Signal.DEFROST_FRONT, Raw.ON)
                delay(1_500); sim.set(Signal.WIPER_SWITCH, Raw.OFF)
            }
            "map" -> vm.openMap()
            "sim" -> { drillAtStep2(); sim.set(Signal.WIPER_SWITCH, "AUTO"); vm.setPanel(true) }
        }
    }
}
