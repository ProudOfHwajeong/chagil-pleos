package com.chagil.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chagil.app.ui.theme.ChagilShapes
import com.chagil.app.ui.theme.ChagilSpacing
import com.chagil.app.ui.theme.ChagilTheme

enum class DrillCardState { OPEN, COMING_SOON, LOCKED_BY_SAFETY }

/** Card illustration, keyed by quest "zone"/id in the screen layer. */
enum class DrillGlyph { RAIN, CAR_WASH, FROST, BELT }

/**
 * Home drill card. OPEN → whole card is the touch target (≥ 76dp) with a go-circle.
 * COMING_SOON → "준비 중", not clickable. LOCKED_BY_SAFETY → "P에 두면 열려요"; tap opens the
 * safety confirm (pass [onClick]); never says "잠김" in a scolding way.
 * @param label optional small pill, e.g. "현대차 안내".
 * @param meta e.g. "45초 · 4단계". @param best e.g. "최고 11.2초" (OPEN only, shown top-right).
 */
@Composable
fun DrillCard(
    title: String,
    subtitle: String,
    meta: String,
    state: DrillCardState,
    glyph: DrillGlyph,
    modifier: Modifier = Modifier,
    label: String? = null,
    best: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val c = ChagilTheme.colors
    val t = ChagilTheme.type
    val open = state == DrillCardState.OPEN
    val accent = when (glyph) {
        DrillGlyph.RAIN -> c.primary
        DrillGlyph.CAR_WASH -> c.primary
        DrillGlyph.FROST -> c.primary
        DrillGlyph.BELT -> c.textTertiary
    }
    val clickable = onClick != null && state != DrillCardState.COMING_SOON
    Column(
        modifier
            .heightIn(min = 300.dp)
            .clip(ChagilShapes.large)
            .background(c.surface)
            .border(2.dp, if (open) c.outline else c.outline.copy(alpha = 0.5f), ChagilShapes.large)
            .then(if (clickable) Modifier.clickable(role = Role.Button) { onClick?.invoke() } else Modifier)
            .semantics(mergeDescendants = true) { contentDescription = "$title. $subtitle. ${state.spoken()}" }
            .padding(ChagilSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(ChagilSpacing.m),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(76.dp).clip(ChagilShapes.medium).background(if (open) c.primaryContainer else c.surfaceRaised),
                contentAlignment = Alignment.Center,
            ) { DrillGlyphView(glyph, if (open) accent else c.textTertiary, 48.dp) }
            Spacer(Modifier.weight(1f))
            if (label != null) {
                StatusChip(label, c.textSecondary, c.surfaceRaised)
                Spacer(Modifier.width(ChagilSpacing.s))
            }
            when (state) {
                DrillCardState.COMING_SOON -> StatusChip("준비 중", c.textSecondary, c.surfaceRaised)
                DrillCardState.LOCKED_BY_SAFETY -> StatusChip("P에 두면 열려요", c.attention, c.attentionContainer, icon = ChagilIcon.LOCK)
                DrillCardState.OPEN -> if (best != null) StatusChip(best, c.success, c.successContainer)
            }
        }
        Column(Modifier.alpha(if (open) 1f else 0.7f), verticalArrangement = Arrangement.spacedBy(ChagilSpacing.xs)) {
            Text(title, style = t.headline, color = c.text, maxLines = 1)
            Text(subtitle, style = t.body, color = c.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth().heightIn(min = 76.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(meta, style = t.label, color = c.textSecondary, modifier = Modifier.weight(1f))
            if (open) {
                Box(
                    Modifier.size(76.dp).clip(CircleShape).background(c.primary),
                    contentAlignment = Alignment.Center,
                ) { ChagilIconView(ChagilIcon.ARROW, c.onPrimary, size = 36.dp) }
            }
        }
    }
}

private fun DrillCardState.spoken() = when (this) {
    DrillCardState.OPEN -> "시작할 수 있어요"
    DrillCardState.COMING_SOON -> "준비 중"
    DrillCardState.LOCKED_BY_SAFETY -> "P에 두면 열려요"
}

@Composable
fun DrillGlyphView(glyph: DrillGlyph, color: Color, size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val u = this.size.minDimension / 48f
        fun o(x: Float, y: Float) = Offset(x * u, y * u)
        val st = Stroke(3.5f * u, cap = StrokeCap.Round)
        when (glyph) {
            DrillGlyph.RAIN -> {
                // cloud
                drawCircle(color, 8f * u, o(17f, 19f))
                drawCircle(color, 11f * u, o(27f, 16f))
                drawCircle(color, 7f * u, o(35f, 21f))
                drawRoundRect(color, o(10f, 19f), Size(31f * u, 9f * u), CornerRadius(4.5f * u))
                listOf(15f, 24f, 33f).forEach { x -> drawLine(color, o(x, 33f), o(x - 3f, 42f), 3.5f * u, StrokeCap.Round) }
            }
            DrillGlyph.CAR_WASH -> {
                val body = Path().apply {
                    moveTo(6f * u, 34f * u); lineTo(6f * u, 27f * u); lineTo(12f * u, 19f * u); lineTo(36f * u, 19f * u)
                    lineTo(42f * u, 27f * u); lineTo(42f * u, 34f * u); close()
                }
                drawPath(body, color, style = st)
                drawCircle(color, 3.5f * u, o(14f, 36f)); drawCircle(color, 3.5f * u, o(34f, 36f))
                listOf(14f, 24f, 34f).forEach { x -> drawLine(color, o(x, 5f), o(x, 12f), 3.5f * u, StrokeCap.Round) }
            }
            DrillGlyph.FROST -> {
                val c0 = o(24f, 24f)
                repeat(3) { i ->
                    val ang = Math.toRadians(90.0 + i * 60.0)
                    val dx = (Math.cos(ang) * 17 * u).toFloat(); val dy = (Math.sin(ang) * 17 * u).toFloat()
                    drawLine(color, Offset(c0.x - dx, c0.y - dy), Offset(c0.x + dx, c0.y + dy), 3.5f * u, StrokeCap.Round)
                }
                drawCircle(color, 4f * u, c0)
            }
            DrillGlyph.BELT -> {
                drawLine(color, o(12f, 6f), o(34f, 42f), 6f * u, StrokeCap.Round)
                drawRoundRect(color, o(26f, 28f), Size(14f * u, 9f * u), CornerRadius(2f * u), style = st)
            }
        }
    }
}

@Preview(widthDp = 1707, heightDp = 400, showBackground = true, backgroundColor = 0xFF0B0F14)
@Composable
private fun DrillCardPreview() = ChagilTheme(night = true) {
    Row(Modifier.padding(32.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        DrillCard("폭우 드릴", "전조등, 와이퍼, 앞유리 김서림 제거", "45초 · 4단계", DrillCardState.OPEN, DrillGlyph.RAIN, Modifier.weight(1f), best = "최고 11.2초", onClick = {})
        DrillCard("세차 대비", "자동세차 전에 와이퍼 AUTO 끄기", "20초 · 2단계", DrillCardState.OPEN, DrillGlyph.CAR_WASH, Modifier.weight(1f), label = "현대차 안내", onClick = {})
        DrillCard("성에 낀 아침", "앞유리, 뒷유리 열선, 핸들 열선", "60초 · 4단계", DrillCardState.LOCKED_BY_SAFETY, DrillGlyph.FROST, Modifier.weight(1f), onClick = {})
        DrillCard("출발 준비", "탄 사람 모두 벨트 확인", "곧 열려요", DrillCardState.COMING_SOON, DrillGlyph.BELT, Modifier.weight(1f))
    }
}
