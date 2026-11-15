package com.chagil.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chagil.app.ui.theme.ChagilShapes
import com.chagil.app.ui.theme.ChagilSpacing
import com.chagil.app.ui.theme.ChagilTheme

/** PASS 통과, HINT 오조작 안내, INFO 이미 켜져 있음/값 변화, DONE 드릴 완료. */
enum class LogKind { PASS, HINT, INFO, DONE }

data class LogLine(val time: String, val text: String, val kind: LogKind = LogKind.INFO)

/** "00:02.1 · LowBeam 스위치 AUTOMATIC → ON · 1초 안정 · 통과" split as time | dot | text. */
@Composable
fun LogRow(time: String, text: String, kind: LogKind, modifier: Modifier = Modifier) {
    val c = ChagilTheme.colors
    val dot = when (kind) {
        LogKind.PASS -> c.success
        LogKind.HINT -> c.attention
        LogKind.INFO -> c.textTertiary
        LogKind.DONE -> c.primary
    }
    Row(modifier.heightIn(min = 44.dp), verticalAlignment = Alignment.Top) {
        Text(time, style = ChagilTheme.type.mono, color = c.textTertiary)
        Spacer(Modifier.width(ChagilSpacing.m))
        StatusDot(dot, Modifier.padding(top = 10.dp))
        Spacer(Modifier.width(ChagilSpacing.m))
        Text(text, style = ChagilTheme.type.body, color = if (kind == LogKind.INFO) c.textSecondary else c.text)
    }
}

/** Judge log. In simulation mode the SimBanner is pinned on top (SPEC §6-9). */
@Composable
fun LogPanel(lines: List<LogLine>, simulation: Boolean, modifier: Modifier = Modifier, title: String = "판정 로그") {
    val c = ChagilTheme.colors
    Column(
        modifier.clip(ChagilShapes.large).background(c.surface).padding(ChagilSpacing.l),
        verticalArrangement = Arrangement.spacedBy(ChagilSpacing.s),
    ) {
        if (simulation) SimBanner(Modifier.fillMaxWidth())
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = ChagilTheme.type.title, color = c.text, modifier = Modifier.weight(1f))
            SourceChip(simulation)
        }
        if (lines.isEmpty()) Text("아직 기록이 없어요", style = ChagilTheme.type.body, color = c.textTertiary)
        lines.forEach { LogRow(it.time, it.text, it.kind) }
    }
}

@Preview(widthDp = 1200, heightDp = 520, showBackground = true, backgroundColor = 0xFF0B0F14)
@Composable
private fun LogPreview() = ChagilTheme(night = true) {
    LogPanel(
        listOf(
            LogLine("00:02.1", "LowBeam 스위치 AUTOMATIC → ON · 1초 안정 · 통과", LogKind.PASS),
            LogLine("00:04.0", "Wiper 스위치 OFF → MIST · 오조작: 지금은 한 번 닦기예요", LogKind.HINT),
            LogLine("00:05.6", "Wiper 스위치 OFF → INTERMITTENT_LEVEL_1 · 1초 안정 · 통과", LogKind.PASS),
            LogLine("00:11.2", "드릴 완료 · 11.2초 · 지난 기록보다 3.1초 빠름", LogKind.DONE),
        ),
        simulation = true,
        modifier = Modifier.padding(24.dp),
    )
}
