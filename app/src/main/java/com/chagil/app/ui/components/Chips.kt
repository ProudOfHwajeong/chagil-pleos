package com.chagil.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chagil.app.ui.theme.ChagilMotion
import com.chagil.app.ui.theme.ChagilShapes
import com.chagil.app.ui.theme.ChagilSpacing
import com.chagil.app.ui.theme.ChagilTheme

/** Mirrors com.chagil.app.safety.SafetyLevel without depending on it. Map 1:1 in the screen layer. */
enum class SafetyChipLevel { L0_DRIVING, L1_STOPPED_UNCONFIRMED, L2_PARKED }

/** A status pill (not a touch target). Height 56dp, text 24sp. */
@Composable
fun StatusChip(
    text: String,
    color: Color,
    container: Color,
    modifier: Modifier = Modifier,
    icon: ChagilIcon? = null,
    pulsingDot: Boolean = false,
) {
    Row(
        modifier
            .heightIn(min = ChagilSpacing.chipHeight)
            .clip(ChagilShapes.pill)
            .background(container)
            .padding(horizontal = 22.dp)
            .semantics(mergeDescendants = true) { contentDescription = text },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) ChagilIconView(icon, color, size = 26.dp) else StatusDot(color, pulsing = pulsingDot)
        Spacer(Modifier.width(ChagilSpacing.s))
        Text(text, style = ChagilTheme.type.label, color = color, maxLines = 1)
    }
}

@Composable
fun StatusDot(color: Color, modifier: Modifier = Modifier, pulsing: Boolean = false) {
    val anim by rememberInfiniteTransition(label = "dot").animateFloat(
        1f, 0.35f, infiniteRepeatable(tween(ChagilMotion.pulse / 2), RepeatMode.Reverse), label = "dotA",
    )
    val a = if (pulsing) anim else 1f
    Canvas(modifier.size(14.dp)) { drawCircle(color.copy(alpha = a)) }
}

/**
 * Safety state chip. Copy:
 * L0 "주행 중 · 쉬어요", L1 "정지 · P 확인 필요" / "정지 · P 확인함", L2 "주차 확인 · P".
 */
@Composable
fun SafetyChip(level: SafetyChipLevel, modifier: Modifier = Modifier, confirmedByDriver: Boolean = false) {
    val c = ChagilTheme.colors
    when (level) {
        SafetyChipLevel.L0_DRIVING -> StatusChip("주행 중 · 쉬어요", c.rest, c.restContainer, modifier, ChagilIcon.PAUSE)
        SafetyChipLevel.L1_STOPPED_UNCONFIRMED -> StatusChip(
            if (confirmedByDriver) "정지 · P 확인함" else "정지 · P 확인 필요",
            c.attention, c.attentionContainer, modifier, ChagilIcon.INFO,
        )
        SafetyChipLevel.L2_PARKED -> StatusChip("주차 확인 · P", c.success, c.successContainer, modifier, ChagilIcon.PARK)
    }
}

/** Signal source chip: "실차 신호" (Vehicle SDK) or "시뮬레이션". */
@Composable
fun SourceChip(simulation: Boolean, modifier: Modifier = Modifier) {
    val c = ChagilTheme.colors
    if (simulation) StatusChip("시뮬레이션", c.sim, c.simContainer, modifier)
    else StatusChip("실차 신호", c.primary, c.primaryContainer, modifier, pulsingDot = true)
}

@Preview(widthDp = 1100, heightDp = 300, showBackground = true, backgroundColor = 0xFF0B0F14)
@Composable
private fun ChipsPreview() = ChagilTheme(night = true) {
    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SafetyChip(SafetyChipLevel.L0_DRIVING)
            SafetyChip(SafetyChipLevel.L1_STOPPED_UNCONFIRMED)
            SafetyChip(SafetyChipLevel.L2_PARKED)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SourceChip(simulation = false)
            SourceChip(simulation = true)
        }
    }
}
