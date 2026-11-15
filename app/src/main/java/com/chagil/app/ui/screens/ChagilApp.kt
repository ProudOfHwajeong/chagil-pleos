package com.chagil.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chagil.app.engine.DrillState
import com.chagil.app.safety.SafetyLevel
import com.chagil.app.ui.AppViewModel
import com.chagil.app.ui.Nav
import com.chagil.app.ui.theme.ChagilMotion
import com.chagil.app.ui.theme.ChagilTheme
import com.chagil.app.vehicle.Raw
import com.chagil.app.vehicle.Signal
import com.chagil.app.vehicle.SourceMode
import com.chagil.app.vehicle.rawOrNull
import androidx.compose.animation.core.tween

/** Root: theme, canvas scale, navigation, safety overlays, log/simulator panel. */
@Composable
fun ChagilApp(vm: AppViewModel) {
    val c = vm.c
    val nightOverride by vm.nightOverride.collectAsStateWithLifecycle()
    val night = nightOverride ?: isSystemInDarkTheme()

    val nav by vm.nav.collectAsStateWithLifecycle()
    val panelOpen by vm.panelOpen.collectAsStateWithLifecycle()
    val prompt by vm.prompt.collectAsStateWithLifecycle()
    val mode by c.hub.mode.collectAsStateWithLifecycle()
    val forced by c.hub.forcedSimulation.collectAsStateWithLifecycle()
    val readings by c.hub.readings.collectAsStateWithLifecycle()
    val diagnostics by c.hub.diagnostics.collectAsStateWithLifecycle()
    val safety by c.safety.status.collectAsStateWithLifecycle()
    val drill by c.engine.state.collectAsStateWithLifecycle()
    val quest by c.engine.quest.collectAsStateWithLifecycle()
    val resumable by c.engine.resumable.collectAsStateWithLifecycle()
    val log by c.engine.log.collectAsStateWithLifecycle()
    val best by c.progress.bestTimes.collectAsStateWithLifecycle()
    val lit by c.progress.litControls.collectAsStateWithLifecycle()
    val saved by c.progress.pausedSnapshot.collectAsStateWithLifecycle()
    val muted by c.voice.muted.collectAsStateWithLifecycle()

    val simulation = mode == SourceMode.SIMULATION
    val openPanel = { vm.setPanel(true) }
    val gearNeutral = readings[Signal.GEAR].rawOrNull == Raw.GEAR_NEUTRAL

    BackHandler(enabled = panelOpen || prompt != null || nav != Nav.HOME) {
        when {
            panelOpen -> vm.setPanel(false)
            prompt != null -> vm.dismissPrompt()
            nav == Nav.MAP -> vm.closeMap()
            drill is DrillState.Running -> c.engine.pause()
            drill is DrillState.Paused -> Unit
            else -> vm.goHome()
        }
    }

    ChagilTheme(night = night) {
        Box(Modifier.fillMaxSize().background(ChagilTheme.colors.background)) {
            CanvasScale {
                Box(Modifier.fillMaxSize()) {
                    if (safety.level == SafetyLevel.L0_DRIVING) {
                        RestScreen(simulation, openPanel)
                    } else {
                        val q = quest
                        when {
                            nav == Nav.MAP -> MapScreen(simulation, lit, vm::closeMap, openPanel)
                            nav == Nav.DRILL && q != null && drill !is DrillState.Idle -> DrillScreen(
                                DrillUi(
                                    quest = q,
                                    state = drill,
                                    safety = safety,
                                    resumable = resumable,
                                    simulation = simulation,
                                    wiperOn = readings[Signal.WIPER_SWITCH].rawOrNull.let { it != null && it != Raw.OFF },
                                    litControls = lit,
                                    gearNeutral = gearNeutral,
                                ),
                                DrillActions(
                                    onPause = c.engine::pause,
                                    onResume = vm::resume,
                                    onQuit = vm::goHome,
                                    onSelfCheck = { c.engine.selfCheck() },
                                    onRetry = vm::retry,
                                    onMap = vm::openMap,
                                    onConfirmPark = { c.safety.confirmParkedByDriver() },
                                    onSource = openPanel,
                                ),
                            )
                            else -> {
                                HomeScreen(vm.quests, safety, simulation, best, lit, vm::onCardTap, vm::openMap, openPanel)
                                val snap = saved
                                val sq = vm.quest(snap?.questId)
                                if (snap != null && sq != null && drill is DrillState.Idle && prompt == null) {
                                    Scrim {
                                        com.chagil.app.ui.components.PauseCard(
                                            "${sq.title} ${snap.stepIndex + 1}/${sq.steps.size}단계에서 멈췄어요.",
                                            onResume = vm::resumeSaved,
                                            onQuit = vm::discardSaved,
                                        )
                                    }
                                }
                            }
                        }
                        prompt?.let { kind ->
                            Scrim(onTap = vm::dismissPrompt) { SafetyPromptCard(kind, vm::onConfirmPark, vm::dismissPrompt) }
                        }
                    }
                    AnimatedVisibility(
                        visible = panelOpen,
                        enter = fadeIn(tween(ChagilMotion.medium)) + slideInHorizontally(tween(ChagilMotion.medium)) { it / 3 },
                        exit = fadeOut(tween(ChagilMotion.fast)) + slideOutHorizontally(tween(ChagilMotion.fast)) { it / 3 },
                    ) {
                        SidePanelOverlay(
                            PanelUi(
                                simulation = simulation,
                                forcedSimulation = forced,
                                log = log.map { it.toLine() },
                                diagnostics = diagnostics + c.quests.errors,
                                readings = readings,
                                nightOverride = nightOverride,
                                muted = muted,
                            ),
                            PanelActions(
                                onClose = { vm.setPanel(false) },
                                onSimSet = { signal, raw ->
                                    if (raw == UNREADABLE) c.simulator.setUnreadable(signal) else c.simulator.set(signal, raw)
                                },
                                onSimReset = { c.simulator.reset() },
                                onUseSimulation = { c.hub.useSimulation(it) },
                                onNight = vm::setNightOverride,
                                onMute = { c.voice.setMuted(it) },
                                onResetProgress = { c.progress.resetAll() },
                            ),
                        )
                    }
                }
            }
        }
    }
}
