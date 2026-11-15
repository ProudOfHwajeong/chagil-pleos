package com.chagil.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.chagil.app.ui.components.ChagilIcon
import com.chagil.app.ui.components.LogKind
import com.chagil.app.ui.components.LogLine
import com.chagil.app.ui.components.LogRow
import com.chagil.app.ui.components.SimBanner
import com.chagil.app.ui.components.SimSwitchRow
import com.chagil.app.ui.components.SourceChip
import com.chagil.app.ui.components.TopBarButton
import com.chagil.app.ui.theme.ChagilShapes
import com.chagil.app.ui.theme.ChagilSpacing
import com.chagil.app.ui.theme.ChagilTheme
import com.chagil.app.vehicle.Raw
import com.chagil.app.vehicle.Reading
import com.chagil.app.vehicle.Signal

/** Option key for "this signal cannot be read" in the simulator rows. */
const val UNREADABLE = "__UNREADABLE__"

class PanelActions(
    val onClose: () -> Unit,
    val onSimSet: (Signal, String) -> Unit,
    val onSimReset: () -> Unit,
    val onUseSimulation: (Boolean) -> Unit,
    val onNight: (Boolean?) -> Unit,
    val onMute: (Boolean) -> Unit,
    val onResetProgress: () -> Unit,
)

data class PanelUi(
    val simulation: Boolean,
    val forcedSimulation: Boolean,
    val log: List<LogLine>,
    val diagnostics: List<String>,
    val readings: Map<Signal, Reading>,
    val nightOverride: Boolean?,
    val muted: Boolean,
)

private data class SimRow(val label: String, val signal: Signal, val options: List<Pair<String, String>>, val unreadable: Boolean = false)

/** SCREENS §9 simulator rows. Wiper labels use the lever words the quests use (LO / HI). */
private val SimRows = listOf(
    SimRow("기어", Signal.GEAR, listOf(Raw.GEAR_PARK to "P", Raw.GEAR_NEUTRAL to "N", Raw.GEAR_DRIVE to "D", Raw.GEAR_REVERSE to "R"), unreadable = true),
    SimRow("전조등 스위치", Signal.LOW_BEAM_SWITCH, listOf("OFF" to "끔", "ON" to "켬", "AUTOMATIC" to "AUTO", "DAYTIME_RUNNING" to "주간등"), unreadable = true),
    SimRow(
        "와이퍼 스위치", Signal.WIPER_SWITCH,
        listOf("OFF" to "끔", "MIST" to "MIST", "INTERMITTENT_LEVEL_1" to "간헐 1", "CONTINUOUS_LEVEL_1" to "LO", "CONTINUOUS_LEVEL_2" to "HI", "AUTO" to "AUTO"),
    ),
    SimRow("앞유리 김서림", Signal.DEFROST_FRONT, listOf("OFF" to "끔", "ON" to "켬")),
    SimRow("뒷유리 열선", Signal.DEFROST_REAR, listOf("OFF" to "끔", "ON" to "켬")),
    SimRow("핸들 열선", Signal.STEERING_HEAT, listOf("0" to "끔", "1" to "1단", "2" to "2단")),
)

/** Right-edge panel: judgement log, then (SIMULATION only) the simulator, then demo settings. */
@Composable
fun SidePanelOverlay(ui: PanelUi, a: PanelActions) = BoxWithConstraints(Modifier.fillMaxSize()) {
    val c = ChagilTheme.colors
    val t = ChagilTheme.type
    val width = if (widthClassFor(maxWidth) == WidthClass.COMPACT) 560.dp else 720.dp
    Box(
        Modifier.fillMaxSize().background(c.scrim)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = a.onClose),
    )
    Column(
        Modifier
            .align(Alignment.CenterEnd)
            .width(width)
            .fillMaxHeight()
            .clip(RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp))
            .background(c.surface)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .padding(ChagilSpacing.l),
        verticalArrangement = Arrangement.spacedBy(ChagilSpacing.m),
    ) {
        if (ui.simulation) SimBanner(Modifier.fillMaxWidth())
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("판정 로그", style = t.title, color = c.text, modifier = Modifier.weight(1f))
            // In SIMULATION the banner right above already says so; the chip only names a real car signal.
            if (!ui.simulation) {
                SourceChip(false)
                Spacer(Modifier.width(ChagilSpacing.s))
            }
            TopBarButton("닫기", a.onClose, icon = ChagilIcon.CLOSE)
        }
        // Log: newest at the bottom, auto-scroll.
        val logScroll = rememberScrollState()
        LaunchedEffect(ui.log.size) { logScroll.animateScrollTo(logScroll.maxValue) }
        Column(
            Modifier.fillMaxWidth().heightIn(min = 120.dp).weight(if (ui.simulation) 0.3f else 0.6f)
                .clip(ChagilShapes.medium).background(c.background).padding(ChagilSpacing.m)
                .verticalScroll(logScroll),
            verticalArrangement = Arrangement.spacedBy(ChagilSpacing.xs),
        ) {
            // Only the latest connection note (e.g. "3초 안에 … → 시뮬레이션"), above the drill log.
            ui.diagnostics.lastOrNull()?.let { LogRow("연결", it.substringAfter(" · "), LogKind.INFO) }
            if (ui.log.isEmpty()) Text("아직 기록이 없어요", style = t.body, color = c.textTertiary)
            ui.log.forEach { LogRow(it.time, it.text, it.kind) }
        }
        Column(
            Modifier.fillMaxWidth().weight(if (ui.simulation) 0.7f else 0.4f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(ChagilSpacing.m),
        ) {
            if (ui.simulation) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("시뮬레이터", style = t.title, color = c.text, modifier = Modifier.weight(1f))
                    TopBarButton("처음 상태로", a.onSimReset, icon = ChagilIcon.RETRY)
                }
                SimRows.forEach { row ->
                    val reading = ui.readings[row.signal]
                    val selected = when (reading) {
                        is Reading.Value -> reading.raw
                        Reading.Unreadable -> UNREADABLE
                        else -> null
                    }
                    val options = row.options + if (row.unreadable) listOf(UNREADABLE to "읽기 불가") else emptyList()
                    val labels = options.toMap()
                    SimSwitchRow(row.label, options.map { it.first }, selected, { a.onSimSet(row.signal, it) }, display = { labels[it] ?: it })
                }
            }
            Text("시연 설정", style = t.title, color = c.text, modifier = Modifier.padding(top = ChagilSpacing.s))
            SettingRow("신호 출처", listOf(false to "차 신호", true to "시뮬레이션"), ui.forcedSimulation || ui.simulation, a.onUseSimulation)
            SettingRow("화면", listOf<Pair<Boolean?, String>>(null to "자동", false to "낮", true to "밤"), ui.nightOverride, a.onNight)
            SettingRow("음성 안내", listOf(false to "켬", true to "끔"), ui.muted, a.onMute)
            TopBarButton("기록 지우기", a.onResetProgress, icon = ChagilIcon.RETRY)
            Spacer(Modifier.height(ChagilSpacing.l))
        }
    }
}

/** Segmented setting row (primary color; the purple SimSwitchRow is reserved for simulated car values). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> SettingRow(label: String, options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    val c = ChagilTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(ChagilSpacing.xs)) {
        Text(label, style = ChagilTheme.type.label, color = c.textSecondary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(ChagilSpacing.s), verticalArrangement = Arrangement.spacedBy(ChagilSpacing.s)) {
            options.forEach { (value, text) ->
                val on = value == selected
                Row(
                    Modifier
                        .heightIn(min = ChagilSpacing.touchMin)
                        .clip(ChagilShapes.medium)
                        .background(if (on) c.primary else c.surfaceRaised)
                        .clickable(role = Role.RadioButton) { onSelect(value) }
                        .semantics { this.selected = on }
                        .padding(horizontal = 22.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) { Text(text, style = ChagilTheme.type.label, color = if (on) c.onPrimary else c.text, maxLines = 1) }
            }
        }
    }
}
