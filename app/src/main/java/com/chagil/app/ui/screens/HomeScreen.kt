package com.chagil.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chagil.app.engine.LogFormat
import com.chagil.app.engine.Quest
import com.chagil.app.safety.SafetyStatus
import com.chagil.app.ui.components.ChagilIcon
import com.chagil.app.ui.components.CockpitMap
import com.chagil.app.ui.components.ControlPoint
import com.chagil.app.ui.components.DrillCard
import com.chagil.app.ui.components.DrillCardState
import com.chagil.app.ui.components.SafetyChip
import com.chagil.app.ui.components.TopBarButton
import com.chagil.app.ui.components.ZoneState
import com.chagil.app.ui.theme.ChagilShapes
import com.chagil.app.ui.theme.ChagilSpacing
import com.chagil.app.ui.theme.ChagilTheme

/** "전조등 · 와이퍼, 45초" → "전조등 · 와이퍼": the card meta already shows the time. */
private val TrailingSeconds = Regex(",\\s*\\d+초\\s*$")

/** Legal review: one-line data notice on home. */
const val DATA_NOTICE = "연습 판정을 위해 기어·와이퍼·전조등·공조 상태를 읽기만 해요. 기기 밖으로 보내지 않아요."

/** SCREENS §1. */
@Composable
fun HomeScreen(
    quests: List<Quest>,
    safety: SafetyStatus,
    simulation: Boolean,
    bestTimes: Map<String, Long>,
    litControls: Set<String>,
    onCard: (Quest) -> Unit,
    onMap: () -> Unit,
    onSource: () -> Unit,
) {
    val c = ChagilTheme.colors
    val t = ChagilTheme.type
    PageFrame(
        simulation = simulation,
        bodyTopGap = ChagilSpacing.l,
        topBar = {
            val compact = LocalWidthClass.current == WidthClass.COMPACT
            Row(Modifier.fillMaxWidth().height(ChagilSpacing.topBarHeight), verticalAlignment = Alignment.CenterVertically) {
                Text("차 길들이기", style = t.headline, color = c.text)
                Spacer(Modifier.width(ChagilSpacing.l))
                if (!compact) {
                    Text(
                        "차를 세워 두고, 진짜 레버로 연습해요",
                        style = t.bodyLarge, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
                SafetyChip(safety.level.chip(), confirmedByDriver = safety.driverConfirmed)
                Spacer(Modifier.width(ChagilSpacing.s))
                TappableSourceChip(simulation, onSource)
            }
        },
    ) {
        val compact = LocalWidthClass.current == WidthClass.COMPACT
        Row(
            Modifier.fillMaxWidth().height(if (compact) 260.dp else 330.dp),
            horizontalArrangement = Arrangement.spacedBy(ChagilSpacing.l),
        ) {
            quests.take(4).forEach { q ->
                val state = when {
                    !q.isOpen -> DrillCardState.COMING_SOON
                    !safety.allows(q.level) -> DrillCardState.LOCKED_BY_SAFETY
                    else -> DrillCardState.OPEN
                }
                DrillCard(
                    title = q.title.ifBlank { q.id },
                    subtitle = q.subtitle?.replace(TrailingSeconds, "") ?: "",
                    meta = if (q.isOpen) "${q.limitSec}초 · ${q.steps.size}단계" else "곧 열려요",
                    state = state,
                    glyph = q.glyph(),
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    label = q.label,
                    best = bestTimes[q.id]?.let { "최고 ${LogFormat.seconds(it)}초" },
                    onClick = if (q.isOpen) ({ onCard(q) }) else null,
                )
            }
        }
        Spacer(Modifier.height(ChagilSpacing.l))
        val zones = mapZones(litControls, emptySet())
        val lit = CountedCells.count { zones[it] == ZoneState.LIT }
        Row(
            Modifier.fillMaxWidth().weight(1f).clip(ChagilShapes.large).background(c.surface).padding(ChagilSpacing.xl),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.width(if (compact) 320.dp else 420.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(ChagilSpacing.s)) {
                Text("내 차 도면", style = t.headline, color = c.text)
                Text(
                    if (lit == 0) "첫 드릴을 해 보세요" else "$lit / ${CountedCells.size}칸에 불이 들어왔어요",
                    style = t.bodyLarge, color = c.textSecondary,
                )
                Spacer(Modifier.height(ChagilSpacing.s))
                TopBarButton("도면 보기", onMap, icon = ChagilIcon.MAP)
            }
            Spacer(Modifier.width(ChagilSpacing.l))
            CockpitMap(zones, Modifier.weight(1f).fillMaxHeight())
        }
        Spacer(Modifier.height(ChagilSpacing.m))
        Text(DATA_NOTICE, style = t.body, color = c.textSecondary, maxLines = 2)
    }
}

/** Progress-map cells: lit (car-confirmed), just lit, quiz, unlit. */
fun mapZones(litKeys: Set<String>, justLit: Set<String>): Map<ControlPoint, ZoneState> =
    ControlPoint.entries.associateWith {
        when {
            it.name in justLit -> ZoneState.JUST_LIT
            it.name in litKeys -> ZoneState.LIT
            it in QuizCells -> ZoneState.QUIZ
            else -> ZoneState.UNLIT
        }
    }
