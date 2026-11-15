package com.chagil.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chagil.app.engine.LogEntry
import com.chagil.app.engine.Quest
import com.chagil.app.engine.Step
import com.chagil.app.safety.SafetyLevel
import com.chagil.app.ui.components.ControlPoint
import com.chagil.app.ui.components.DrillGlyph
import com.chagil.app.ui.components.LogKind
import com.chagil.app.ui.components.LogLine
import com.chagil.app.ui.components.SafetyChipLevel
import com.chagil.app.ui.components.SimBanner
import com.chagil.app.ui.components.SourceChip
import com.chagil.app.ui.theme.ChagilShapes
import com.chagil.app.ui.theme.ChagilSpacing
import com.chagil.app.vehicle.Signal

// ---- layout canvas ---------------------------------------------------------------------------------

/** SCREENS.md canvas (1707×960dp). Every screen is scaled to it so the layout is identical everywhere. */
private val CanvasWidth = 1707.dp
private val CanvasHeight = 960.dp

/**
 * Draws the SCREENS.md layout at the same proportions on any display: the Pleos display
 * (2560×1440 at 160dpi = 2560dp) is drawn 1.5×, 1280×720dp at 0.75×. Fixed-size cards and panels
 * therefore never clip or overlap; the compact variants in SCREENS.md stay available via [LocalWidthClass]
 * but are not needed.
 */
@Composable
fun CanvasScale(content: @Composable () -> Unit) = BoxWithConstraints(Modifier.fillMaxSize()) {
    val d = LocalDensity.current
    val scale = minOf(maxWidth / CanvasWidth, maxHeight / CanvasHeight).coerceIn(0.5f, 3f)
    if (kotlin.math.abs(scale - 1f) < 0.02f) {
        content()
    } else {
        CompositionLocalProvider(LocalDensity provides Density(d.density * scale, d.fontScale), content = content)
    }
}

enum class WidthClass { COMPACT, REGULAR }

val LocalWidthClass = compositionLocalOf { WidthClass.REGULAR }

fun widthClassFor(width: Dp) = if (width < 1400.dp) WidthClass.COMPACT else WidthClass.REGULAR

val WidthClass.gutter: Dp get() = if (this == WidthClass.COMPACT) ChagilSpacing.pageGutterCompact else ChagilSpacing.pageGutter

/**
 * SCREENS §0 common frame: gutters, [topBar], SimBanner (simulation only), 32dp, body.
 * Wide screens keep the body at most 1824dp wide, centered.
 */
@Composable
fun PageFrame(
    simulation: Boolean,
    topBar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    bodyTopGap: Dp = ChagilSpacing.xl,
    body: @Composable ColumnScope.() -> Unit,
) = BoxWithConstraints(modifier.fillMaxSize()) {
    val wc = widthClassFor(maxWidth)
    CompositionLocalProvider(LocalWidthClass provides wc) {
        Column(
            Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 1824.dp)
                .fillMaxSize()
                .padding(horizontal = wc.gutter)
                .padding(bottom = wc.gutter),
        ) {
            topBar()
            if (simulation) SimBanner(Modifier.fillMaxWidth())
            Spacer(Modifier.height(bodyTopGap))
            body()
        }
    }
}

/** SourceChip with a 76dp touch area that opens the log/simulator panel (SCREENS §9). */
@Composable
fun TappableSourceChip(simulation: Boolean, onClick: () -> Unit) {
    SourceChip(
        simulation,
        tappable = true,
        modifier = Modifier
            .clip(ChagilShapes.pill)
            .clickable(role = Role.Button, onClickLabel = "판정 로그 열기", onClick = onClick)
            .padding(vertical = 10.dp),
    )
}

// ---- engine → component mapping (SCREENS appendix B) -------------------------------------------------

fun SafetyLevel.chip(): SafetyChipLevel = when (this) {
    SafetyLevel.L0_DRIVING -> SafetyChipLevel.L0_DRIVING
    SafetyLevel.L1_STOPPED_UNCONFIRMED -> SafetyChipLevel.L1_STOPPED_UNCONFIRMED
    SafetyLevel.L2_PARKED -> SafetyChipLevel.L2_PARKED
}

val Step.point: ControlPoint? get() = ControlPoint.fromKey(control)

fun Quest.glyph(): DrillGlyph = when {
    id.contains("rain") -> DrillGlyph.RAIN
    id.contains("wash") -> DrillGlyph.CAR_WASH
    id.contains("frost") || zone == "heating" -> DrillGlyph.FROST
    id.contains("depart") || zone == "seat" -> DrillGlyph.BELT
    else -> DrillGlyph.RAIN
}

/** Short part name used in "차가 읽은 값 와이퍼 MIST". */
fun Signal.partName(): String = when (this) {
    Signal.GEAR -> "기어"
    Signal.WIPER_SWITCH, Signal.WIPER_STATE -> "와이퍼"
    Signal.LOW_BEAM_SWITCH, Signal.LOW_BEAM_STATE -> "전조등"
    Signal.DEFROST_FRONT -> "앞유리 김서림"
    Signal.DEFROST_REAR -> "뒷유리 열선"
    Signal.STEERING_HEAT -> "핸들 열선"
}

fun LogEntry.toLine(): LogLine = LogLine(
    time = com.chagil.app.engine.LogFormat.clock(atMs),
    text = text,
    kind = when {
        text.startsWith("드릴 완료") -> LogKind.DONE
        text.contains("다른 칸") || text.contains("주의") -> LogKind.HINT
        text.contains("통과") -> LogKind.PASS
        else -> LogKind.INFO
    },
)

/** Cells of "내 차 도면" (SCREENS §7). The gear is drawn but is a safety signal, not a drill cell. */
val MapCells: List<ControlPoint> = ControlPoint.entries.filter { it != ControlPoint.GEAR_SELECTOR }

/**
 * Controls shown as quiz cells. Empty in round 1: the belt is "준비 중" everywhere (home card, map row),
 * so the cockpit draws it plainly instead of a "퀴즈" callout and "?" mark.
 */
val QuizCells: Set<ControlPoint> = emptySet()

/** Cells counted in "n / m칸": the belt is "준비 중" in round 1, so it is drawn but not counted. */
val CountedCells: List<ControlPoint> = MapCells.filter { it != ControlPoint.SEAT_BELT }

/** Cells of the zone a quest trains, for the completion caption. */
fun zoneCells(zone: String?): Pair<String, List<ControlPoint>> = when (zone) {
    "windshield" -> "앞유리·시야 구역" to listOf(ControlPoint.LIGHT_STALK, ControlPoint.WIPER_STALK, ControlPoint.HVAC_FRONT_DEFROST)
    "heating" -> "열선·공조 구역" to listOf(ControlPoint.HVAC_FRONT_DEFROST, ControlPoint.HVAC_REAR_DEFROST, ControlPoint.STEERING_HEAT_BUTTON)
    else -> "내 차 도면" to MapCells
}
