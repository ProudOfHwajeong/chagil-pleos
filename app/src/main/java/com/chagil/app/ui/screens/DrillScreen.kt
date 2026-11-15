package com.chagil.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chagil.app.engine.DrillState
import com.chagil.app.engine.HintKind
import com.chagil.app.engine.LogFormat
import com.chagil.app.engine.PauseReason
import com.chagil.app.engine.Quest
import com.chagil.app.engine.StepStatus
import com.chagil.app.safety.SafetyLevel
import com.chagil.app.safety.SafetyStatus
import com.chagil.app.ui.SafetyPrompt
import com.chagil.app.ui.components.ChagilIcon
import com.chagil.app.ui.components.CockpitMap
import com.chagil.app.ui.components.ControlLocator
import com.chagil.app.ui.components.ControlPoint
import com.chagil.app.ui.components.CountdownBar
import com.chagil.app.ui.components.CountdownRing
import com.chagil.app.ui.components.DrillTopBar
import com.chagil.app.ui.components.InstructionPanel
import com.chagil.app.ui.components.InstructionTone
import com.chagil.app.ui.components.LocatorTone
import com.chagil.app.ui.components.PauseCard
import com.chagil.app.ui.components.PrimaryButton
import com.chagil.app.ui.components.ResultHero
import com.chagil.app.ui.components.SecondaryButton
import com.chagil.app.ui.components.StatusChip
import com.chagil.app.ui.components.StepItem
import com.chagil.app.ui.components.StepList
import com.chagil.app.ui.components.StepState
import com.chagil.app.ui.components.TopBarButton
import com.chagil.app.ui.components.VoiceLine
import com.chagil.app.ui.theme.ChagilShapes
import com.chagil.app.ui.theme.ChagilSpacing
import com.chagil.app.ui.theme.ChagilTheme
import kotlinx.coroutines.delay

/** Everything the drill screen needs, already read from the engine / safety / hub. */
data class DrillUi(
    val quest: Quest,
    val state: DrillState,
    val safety: SafetyStatus,
    val resumable: Boolean,
    val simulation: Boolean,
    /** Wiper switch reads something other than OFF (domain review: remind to turn it off at the end). */
    val wiperOn: Boolean,
    val litControls: Set<String>,
    /** Car reads GEAR_NEUTRAL (move to P, do not ask for a confirmation). */
    val gearNeutral: Boolean,
)

class DrillActions(
    val onPause: () -> Unit,
    val onResume: () -> Unit,
    val onQuit: () -> Unit,
    val onSelfCheck: () -> Unit,
    val onRetry: () -> Unit,
    val onMap: () -> Unit,
    val onConfirmPark: () -> Unit,
    val onSource: () -> Unit,
)

@Composable
fun DrillScreen(ui: DrillUi, a: DrillActions) {
    when (val s = ui.state) {
        is DrillState.Running -> RunningView(ui, s, a, paused = false)
        is DrillState.Paused -> Box(Modifier.fillMaxSize()) {
            RunningView(ui, s.snapshot, a, paused = true)
            PausedOverlay(ui, s, a)
        }
        is DrillState.TimedOut -> TimedOutView(ui, s, a)
        is DrillState.Completed -> CompletedView(ui, s, a)
        DrillState.Idle -> Unit
    }
}

private val sidePanelWidth: Dp
    @Composable get() = if (LocalWidthClass.current == WidthClass.COMPACT) 400.dp else 460.dp

@Composable
private fun SidePanel(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val compact = LocalWidthClass.current == WidthClass.COMPACT
    Column(
        Modifier
            .width(sidePanelWidth)
            .fillMaxHeight()
            .clip(ChagilShapes.large)
            .background(ChagilTheme.colors.surface)
            .padding(if (compact) ChagilSpacing.l else ChagilSpacing.xl),
        content = content,
    )
}

// ---- §3 / §4 running (+ hint, self-check) ------------------------------------------------------------

@Composable
private fun RunningView(ui: DrillUi, r: DrillState.Running, a: DrillActions, paused: Boolean) {
    val c = ChagilTheme.colors
    val t = ChagilTheme.type
    val q = ui.quest
    val step = q.steps.getOrNull(r.stepIndex) ?: return
    val selfCheck = r.stepStatus == StepStatus.SELF_CHECK_NEEDED

    // §3: show the green "통과" locator for 600ms before moving to the next control.
    var lastIndex by remember { mutableIntStateOf(r.stepIndex) }
    var flash by remember { mutableStateOf<ControlPoint?>(null) }
    LaunchedEffect(r.stepIndex) {
        if (r.stepIndex > lastIndex) {
            flash = q.steps.getOrNull(lastIndex)?.point
            lastIndex = r.stepIndex
            delay(600)
        }
        lastIndex = r.stepIndex
        flash = null
    }

    // §3: "차가 읽은 값" only after the value changed at least once in this step.
    val initialRead = remember(r.stepIndex) { r.lastRead }
    var changed by remember(r.stepIndex) { mutableStateOf(false) }
    LaunchedEffect(r.stepIndex, r.lastRead) { if (r.lastRead != initialRead) changed = true }

    val hint = r.hint
    val part = step.signal?.partName()
    val readValue = r.lastRead?.takeIf { (changed || hint?.kind == HintKind.WRONG_VALUE) && part != null }?.let { "$part $it" }

    PageFrame(
        simulation = ui.simulation,
        topBar = {
            DrillTopBar(q.title, progress = "${r.stepIndex + 1}/${q.steps.size}단계") {
                TappableSourceChip(ui.simulation, a.onSource)
                if (!selfCheck && !paused) TopBarButton("일시정지", a.onPause, icon = ChagilIcon.PAUSE)
                TopBarButton("그만하기", a.onQuit, icon = ChagilIcon.CLOSE)
            }
        },
    ) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(ChagilSpacing.xl)) {
            Column(Modifier.weight(1f).fillMaxHeight()) {
                when {
                    hint != null -> {
                        val full = hint.text.startsWith("지금은")
                        InstructionPanel(
                            headline = hint.text,
                            eyebrow = if (full || hint.kind == HintKind.GUARD) null else "지금은",
                            voice = hint.voice,
                            readValue = readValue,
                            tone = InstructionTone.HINT,
                        )
                    }
                    selfCheck -> InstructionPanel(
                        headline = step.instruction,
                        eyebrow = "이 차는 이 신호를 읽을 수 없어요",
                        voice = step.voice,
                        checking = false,
                    )
                    else -> InstructionPanel(
                        headline = step.instruction,
                        voice = step.voice,
                        readValue = readValue,
                        checking = !paused,
                    )
                }
                Spacer(Modifier.height(ChagilSpacing.l))
                ControlLocator(
                    point = flash ?: step.point,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    tone = when {
                        flash != null -> LocatorTone.SUCCESS
                        hint != null -> LocatorTone.ATTENTION
                        else -> LocatorTone.NORMAL
                    },
                    animate = !paused,
                )
            }
            val compact = LocalWidthClass.current == WidthClass.COMPACT
            SidePanel {
                Text("할 일", style = t.label, color = c.textSecondary)
                Spacer(Modifier.height(ChagilSpacing.s))
                StepList(q.steps.mapIndexed { i, s -> StepItem(s.label, if (i < r.stepIndex) StepState.DONE else if (i == r.stepIndex) StepState.ACTIVE else StepState.PENDING) })
                val sec = ((r.remainingMs + 999) / 1000).toInt()
                if (compact && !selfCheck) {
                    Spacer(Modifier.height(ChagilSpacing.l))
                    CountdownBar(sec, q.limitSec)
                }
                Spacer(Modifier.weight(1f))
                if (selfCheck) {
                    PrimaryButton("켰어요", a.onSelfCheck, Modifier.fillMaxWidth(), icon = ChagilIcon.CHECK)
                } else if (!compact) {
                    CountdownRing(sec, q.limitSec, Modifier.align(Alignment.CenterHorizontally), paused = paused)
                }
            }
        }
    }
}

// ---- §8 paused -------------------------------------------------------------------------------------

@Composable
private fun PausedOverlay(ui: DrillUi, p: DrillState.Paused, a: DrillActions) {
    val q = ui.quest
    val where = "${q.title} ${p.snapshot.stepIndex + 1}/${q.steps.size}단계에서 멈췄어요."
    Scrim {
        when {
            p.reason == PauseReason.USER && ui.resumable -> PauseCard(where, a.onResume, a.onQuit)
            ui.resumable -> PauseCard(
                if (ui.safety.level == SafetyLevel.L2_PARKED) "$where 차가 다시 P에 있어요." else "$where 다시 멈춰 있어요.",
                a.onResume, a.onQuit,
            )
            ui.safety.level == SafetyLevel.L1_STOPPED_UNCONFIRMED -> SafetyPromptCard(
                if (ui.gearNeutral) SafetyPrompt.MOVE_TO_PARK else SafetyPrompt.CONFIRM_PARK,
                onConfirm = a.onConfirmPark,
                onCancel = a.onQuit,
                cancelText = "그만하기",
            )
            else -> Unit // L0: the rest screen covers everything.
        }
    }
}

@Composable
fun Scrim(onTap: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(ChagilTheme.colors.scrim)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onTap?.invoke() },
        contentAlignment = Alignment.Center,
    ) { content() }
}

// ---- §5 timed out ----------------------------------------------------------------------------------

@Composable
private fun TimedOutView(ui: DrillUi, s: DrillState.TimedOut, a: DrillActions) {
    val c = ChagilTheme.colors
    val t = ChagilTheme.type
    val q = ui.quest
    val step = q.steps.getOrNull(s.stepIndex)
    val point = step?.point
    PageFrame(
        simulation = ui.simulation,
        topBar = {
            DrillTopBar(q.title, progress = "시간 종료") { TappableSourceChip(ui.simulation, a.onSource) }
        },
    ) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(ChagilSpacing.xl)) {
            Column(Modifier.weight(1f).fillMaxHeight()) {
                InstructionPanel(
                    headline = "괜찮아요. 여기 있어요",
                    voice = step?.where?.spoken ?: point?.where,
                    tone = InstructionTone.CALM,
                )
                if (ui.wiperOn && q.steps.any { it.read.startsWith("Wiper") }) {
                    Spacer(Modifier.height(ChagilSpacing.m))
                    StatusChip("와이퍼가 아직 켜져 있어요. OFF로 돌려 두세요", c.attention, c.attentionContainer, icon = ChagilIcon.INFO)
                }
                Spacer(Modifier.height(ChagilSpacing.l))
                ControlLocator(point, Modifier.fillMaxWidth().weight(1f), tone = LocatorTone.ATTENTION)
            }
            SidePanel {
                Text("할 일", style = t.label, color = c.textSecondary)
                Spacer(Modifier.height(ChagilSpacing.s))
                StepList(
                    q.steps.mapIndexed { i, st ->
                        StepItem(
                            st.label,
                            when {
                                s.passed.getOrElse(i) { false } -> StepState.DONE
                                i == s.stepIndex -> StepState.ATTENTION
                                else -> StepState.PENDING
                            },
                        )
                    },
                )
                // Same place as the completion screen: the next action sits at the bottom of the side panel.
                Spacer(Modifier.weight(1f))
                PrimaryButton("다시", a.onRetry, Modifier.fillMaxWidth(), icon = ChagilIcon.RETRY)
                Spacer(Modifier.height(ChagilSpacing.l))
                SecondaryButton("나중에", a.onQuit, Modifier.fillMaxWidth())
            }
        }
    }
}

// ---- §6 completed ----------------------------------------------------------------------------------

@Composable
private fun CompletedView(ui: DrillUi, s: DrillState.Completed, a: DrillActions) {
    val c = ChagilTheme.colors
    val t = ChagilTheme.type
    val q = ui.quest
    val (delta, improved) = when {
        s.previousBestMs == null -> "첫 기록이에요" to null
        // Below 0.1s the rounded gap would read "0.0초 빨라요"; say it plainly instead.
        s.previousBestMs - s.elapsedMs in 0L..99L -> "지난번과 거의 같아요" to false
        s.elapsedMs < s.previousBestMs -> "지난번보다 ${LogFormat.seconds(s.previousBestMs - s.elapsedMs)}초 빨라요" to true
        else -> "지난 기록 ${LogFormat.seconds(s.previousBestMs)}초" to false
    }
    // JUST_LIT pulses twice, then settles to LIT (§6).
    var justLit by remember(s) { mutableStateOf(s.litControls) }
    LaunchedEffect(s) {
        delay(2L * com.chagil.app.ui.theme.ChagilMotion.pulse)
        justLit = emptySet()
    }
    val zones = mapZones(ui.litControls + s.litControls, justLit)
    val (zoneName, cells) = zoneCells(q.zone)
    val litInZone = cells.count { zones[it] == com.chagil.app.ui.components.ZoneState.LIT || zones[it] == com.chagil.app.ui.components.ZoneState.JUST_LIT }

    PageFrame(
        simulation = ui.simulation,
        topBar = { DrillTopBar(q.title, progress = "완료") { TappableSourceChip(ui.simulation, a.onSource) } },
    ) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(ChagilSpacing.xl)) {
            Column(Modifier.weight(1f).fillMaxHeight()) {
                ResultHero("${LogFormat.seconds(s.elapsedMs)}초", deltaText = delta, improved = improved)
                q.done?.spoken?.let {
                    Spacer(Modifier.height(ChagilSpacing.l))
                    VoiceLine(it, c.success, c.successContainer)
                }
                Spacer(Modifier.height(ChagilSpacing.xl))
                CockpitMap(zones, Modifier.fillMaxWidth().weight(1f))
                Spacer(Modifier.height(ChagilSpacing.s))
                Text("$zoneName $litInZone/${cells.size}", style = t.label, color = c.textSecondary)
            }
            SidePanel {
                Text("할 일", style = t.label, color = c.textSecondary)
                Spacer(Modifier.height(ChagilSpacing.s))
                StepList(q.steps.map { StepItem(it.label, StepState.DONE) })
                Spacer(Modifier.weight(1f))
                PrimaryButton("도면 보기", a.onMap, Modifier.fillMaxWidth(), icon = ChagilIcon.MAP)
                Spacer(Modifier.height(ChagilSpacing.l))
                SecondaryButton("다시", a.onRetry, Modifier.fillMaxWidth(), icon = ChagilIcon.RETRY)
            }
        }
    }
}
