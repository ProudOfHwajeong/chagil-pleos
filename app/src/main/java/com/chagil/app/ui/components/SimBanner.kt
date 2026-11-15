package com.chagil.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chagil.app.ui.theme.ChagilShapes
import com.chagil.app.ui.theme.ChagilSpacing
import com.chagil.app.ui.theme.ChagilTheme

const val SIM_BANNER_TEXT = "시뮬레이션 — 실제 차 신호가 아닙니다"

/**
 * Always-visible label while the signal source is SIMULATION. Diagonal stripes = "not real",
 * readable at a glance in demo videos. Place at the very top of any panel/screen in sim mode.
 */
@Composable
fun SimBanner(modifier: Modifier = Modifier, text: String = SIM_BANNER_TEXT) {
    val c = ChagilTheme.colors
    Row(
        modifier
            .heightIn(min = 56.dp)
            .clip(ChagilShapes.small)
            .background(c.simContainer)
            .drawBehind {
                val gap = 28.dp.toPx()
                var x = -size.height
                while (x < size.width) {
                    drawLine(c.sim.copy(alpha = 0.14f), Offset(x, size.height), Offset(x + size.height, 0f), 10.dp.toPx())
                    x += gap
                }
            }
            .border(2.dp, c.sim.copy(alpha = 0.6f), ChagilShapes.small)
            .padding(horizontal = ChagilSpacing.l)
            .semantics(mergeDescendants = true) { contentDescription = text },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChagilIconView(ChagilIcon.INFO, c.sim, size = 28.dp)
        Spacer(Modifier.width(ChagilSpacing.s))
        Text(text, style = ChagilTheme.type.label, color = c.sim, maxLines = 1)
    }
}

/**
 * One row of the simulator control panel (sim mode only): label + segmented options, 76dp targets.
 * e.g. SimSwitchRow("와이퍼", listOf("OFF","MIST","INTERMITTENT_LEVEL_1","AUTO"), "OFF") { hub.set(...) }
 * [display] maps a raw value to a short on-screen label.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SimSwitchRow(
    label: String,
    options: List<String>,
    selected: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    display: (String) -> String = { it },
) {
    val c = ChagilTheme.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(ChagilSpacing.xs)) {
        Text(label, style = ChagilTheme.type.label, color = c.textSecondary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(ChagilSpacing.targetGap), verticalArrangement = Arrangement.spacedBy(ChagilSpacing.s)) {
            options.forEach { opt ->
                val on = opt == selected
                Row(
                    Modifier
                        .heightIn(min = ChagilSpacing.touchMin)
                        .widthIn(min = ChagilSpacing.touchMin)
                        .clip(ChagilShapes.medium)
                        .background(if (on) c.sim else c.surfaceRaised)
                        .clickable(role = Role.RadioButton) { onSelect(opt) }
                        .semantics { this.selected = on }
                        .padding(horizontal = 22.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(display(opt), style = ChagilTheme.type.label, color = if (on) c.onSim else c.text, maxLines = 1)
                }
            }
        }
    }
}

@Preview(widthDp = 900, heightDp = 300, showBackground = true, backgroundColor = 0xFF0B0F14)
@Composable
private fun SimPreview() = ChagilTheme(night = true) {
    Column(Modifier.padding(24.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SimBanner(Modifier.fillMaxWidth())
        SimSwitchRow("와이퍼 스위치", listOf("OFF", "MIST", "INTERMITTENT_LEVEL_1", "AUTO"), "MIST", {})
    }
}
