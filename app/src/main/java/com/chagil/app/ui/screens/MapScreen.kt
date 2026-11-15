package com.chagil.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.chagil.app.ui.components.ChagilIcon
import com.chagil.app.ui.components.CockpitMap
import com.chagil.app.ui.components.ControlPoint
import com.chagil.app.ui.components.DrillTopBar
import com.chagil.app.ui.components.StatusChip
import com.chagil.app.ui.components.StatusDot
import com.chagil.app.ui.components.TopBarButton
import com.chagil.app.ui.components.ZoneState
import com.chagil.app.ui.theme.ChagilShapes
import com.chagil.app.ui.theme.ChagilSpacing
import com.chagil.app.ui.theme.ChagilTheme

private data class MapRow(val no: String, val name: String, val cells: List<ControlPoint>, val soon: Boolean = false)

private val Rows = listOf(
    MapRow("1", "라이트 레버", listOf(ControlPoint.LIGHT_STALK)),
    MapRow("2", "와이퍼 레버", listOf(ControlPoint.WIPER_STALK)),
    MapRow("3", "공조·김서림", listOf(ControlPoint.HVAC_FRONT_DEFROST)),
    MapRow("4", "열선 (핸들·뒷유리)", listOf(ControlPoint.STEERING_HEAT_BUTTON, ControlPoint.HVAC_REAR_DEFROST)),
    MapRow("5", "벨트·좌석", listOf(ControlPoint.SEAT_BELT), soon = true),
    MapRow("Q", "운전 보조 퀴즈", emptyList(), soon = true),
)

/** SCREENS §7 "내 차 도면". */
@Composable
fun MapScreen(simulation: Boolean, litControls: Set<String>, onClose: () -> Unit, onSource: () -> Unit) {
    val c = ChagilTheme.colors
    val t = ChagilTheme.type
    val zones = mapZones(litControls, emptySet())
    val lit = CountedCells.count { zones[it] == ZoneState.LIT }
    PageFrame(
        simulation = simulation,
        topBar = {
            DrillTopBar("내 차 도면") {
                TappableSourceChip(simulation, onSource)
                TopBarButton("닫기", onClose, icon = ChagilIcon.CLOSE)
            }
        },
    ) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(ChagilSpacing.xl)) {
            Box(Modifier.weight(1f).fillMaxHeight().clip(ChagilShapes.large).background(c.surface).padding(ChagilSpacing.l)) {
                CockpitMap(zones, Modifier.fillMaxSize(), showLabels = true)
            }
            Column(
                Modifier.width(if (LocalWidthClass.current == WidthClass.COMPACT) 400.dp else 460.dp).fillMaxHeight()
                    .clip(ChagilShapes.large).background(c.surface).padding(ChagilSpacing.xl),
            ) {
                Text("$lit / ${CountedCells.size}개 기능", style = t.headline, color = c.text)
                Text("확인할 수 있는 기능 기준", style = t.body, color = c.textSecondary)
                Spacer(Modifier.height(ChagilSpacing.l))
                Rows.forEach { row ->
                    val n = row.cells.count { zones[it] == ZoneState.LIT }
                    val quiz = row.cells.isNotEmpty() && row.cells.all { zones[it] == ZoneState.QUIZ }
                    Row(Modifier.fillMaxWidth().heightIn(min = 76.dp), verticalAlignment = Alignment.CenterVertically) {
                        StatusDot(if (n > 0) c.success else c.outline)
                        Spacer(Modifier.width(ChagilSpacing.m))
                        Text("${row.no}  ${row.name}", style = t.title.copy(fontSize = t.bodyLarge.fontSize), color = c.text, modifier = Modifier.weight(1f), maxLines = 1)
                        when {
                            row.soon -> StatusChip("준비 중", c.textSecondary, c.surfaceRaised)
                            quiz -> StatusChip("퀴즈", c.textSecondary, c.surfaceRaised)
                            else -> Text("$n/${row.cells.size}", style = t.label, color = c.textSecondary)
                        }
                    }
                }
            }
        }
    }
}
