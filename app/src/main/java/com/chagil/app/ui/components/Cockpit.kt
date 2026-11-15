package com.chagil.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chagil.app.ui.theme.ChagilColors
import com.chagil.app.ui.theme.ChagilMotion
import com.chagil.app.ui.theme.ChagilTheme

/** How the locator highlights its target: NORMAL = blue "여기", ATTENTION = amber (time-out/hint), SUCCESS = green. */
enum class LocatorTone { NORMAL, ATTENTION, SUCCESS }

/** Progress-map cell state. QUIZ = the app cannot read this control; taught by a quiz. */
enum class ZoneState { LIT, UNLIT, QUIZ, JUST_LIT }

/**
 * Driver's-eye cockpit with one control pulsing. Generic left-hand-drive layout, no brand marks.
 * Lever direction differs by model, so no arrows are drawn here (SPEC: draw per car later).
 */
@Composable
fun ControlLocator(
    point: ControlPoint?,
    modifier: Modifier = Modifier,
    tone: LocatorTone = LocatorTone.NORMAL,
    showLabel: Boolean = true,
    animate: Boolean = true,
) {
    val c = ChagilTheme.colors
    val looks = remember(point, tone, c) {
        buildMap {
            ControlPoint.entries.forEach { put(it, if (it == point) c.highlight(tone) else c.baseLook()) }
        }
    }
    CockpitCanvas(
        looks = looks,
        labels = if (showLabel && point != null) mapOf(point to point.label) else emptyMap(),
        modifier = modifier.semantics { contentDescription = point?.let { "${it.label}: ${it.where}" } ?: "운전석 그림" },
        animate = animate,
    )
}

/** Progress map ("내 차 도면"): every control lit / unlit / quiz. */
@Composable
fun CockpitMap(
    zones: Map<ControlPoint, ZoneState>,
    modifier: Modifier = Modifier,
    showLabels: Boolean = false,
    animate: Boolean = true,
) {
    val c = ChagilTheme.colors
    val looks = remember(zones, c) {
        buildMap {
            ControlPoint.entries.forEach {
                put(
                    it,
                    when (zones[it] ?: ZoneState.UNLIT) {
                        ZoneState.LIT -> c.lit(pulse = false)
                        ZoneState.JUST_LIT -> c.lit(pulse = true)
                        ZoneState.UNLIT -> c.baseLook()
                        ZoneState.QUIZ -> c.quiz()
                    },
                )
            }
        }
    }
    val labels = if (showLabels) zones.filterValues { it != ZoneState.UNLIT }.mapValues { (k, v) -> if (v == ZoneState.QUIZ) "퀴즈 · ${k.label}" else k.label } else emptyMap()
    val lit = zones.count { it.value == ZoneState.LIT || it.value == ZoneState.JUST_LIT }
    CockpitCanvas(
        looks = looks,
        labels = labels,
        modifier = modifier.semantics { contentDescription = "내 차 도면, ${zones.size}칸 중 ${lit}칸 점등" },
        animate = animate,
        quizMarks = zones.filterValues { it == ZoneState.QUIZ }.keys,
    )
}

internal data class PartLook(
    val fill: Color,
    val stroke: Color,
    val icon: Color,
    val glow: Color? = null,
    val pulse: Color? = null,
    val dashed: Boolean = false,
    val labelBg: Color = fill,
    val labelFg: Color = icon,
)

private fun ChagilColors.baseLook() = PartLook(cockpitPart, cockpitLine, textSecondary)
private fun ChagilColors.highlight(t: LocatorTone): PartLook {
    val (col, on) = when (t) {
        LocatorTone.NORMAL -> primary to onPrimary
        LocatorTone.ATTENTION -> attention to onAttention
        LocatorTone.SUCCESS -> success to onSuccess
    }
    return PartLook(col, col, on, glow = col, pulse = col, labelBg = col, labelFg = on)
}
private fun ChagilColors.lit(pulse: Boolean) =
    PartLook(success, success, onSuccess, glow = success, pulse = if (pulse) success else null, labelBg = success, labelFg = onSuccess)
private fun ChagilColors.quiz() =
    PartLook(cockpitFill, textTertiary, textTertiary, dashed = true, labelBg = surfaceRaised, labelFg = textSecondary)

// Virtual canvas 1000 × 600 (driver's eye view, landscape).
private const val VW = 1000f
private const val VY0 = 140f // top of the visible crop (windshield above is not drawn)
private const val VH = 600f - VY0

private val anchors = mapOf(
    ControlPoint.LIGHT_STALK to Offset(118f, 334f),
    ControlPoint.WIPER_STALK to Offset(582f, 334f),
    ControlPoint.HVAC_FRONT_DEFROST to Offset(712f, 398f),
    ControlPoint.HVAC_REAR_DEFROST to Offset(810f, 398f),
    ControlPoint.STEERING_HEAT_BUTTON to Offset(908f, 398f),
    ControlPoint.GEAR_SELECTOR to Offset(800f, 528f),
    ControlPoint.SEAT_BELT to Offset(104f, 492f),
)

@Composable
private fun CockpitCanvas(
    looks: Map<ControlPoint, PartLook>,
    labels: Map<ControlPoint, String>,
    modifier: Modifier,
    animate: Boolean,
    quizMarks: Set<ControlPoint> = emptySet(),
) {
    val c = ChagilTheme.colors
    val measurer = rememberTextMeasurer()
    val pulseAnim by rememberInfiniteTransition(label = "cockpit").animateFloat(
        0f, 1f, infiniteRepeatable(tween(ChagilMotion.pulse, easing = LinearEasing), RepeatMode.Restart), label = "pulse",
    )
    val pulse = if (animate) pulseAnim else 0.45f
    Canvas(modifier.clipToBounds().graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)) {
        val s = minOf(size.width / VW, size.height / VH)
        val ox = (size.width - VW * s) / 2
        val oy = (size.height - VH * s) / 2
        fun toScreen(p: Offset) = Offset(ox + p.x * s, oy + (p.y - VY0) * s)

        withTransform({
            translate(ox, oy - VY0 * s)
            scale(s, s, Offset.Zero)
        }) {
            // Glows first (behind everything)
            looks.forEach { (pt, look) ->
                val a = anchors.getValue(pt)
                look.glow?.let { drawCircle(it.copy(alpha = 0.16f), 92f, a) }
            }
            drawStatic(c)
            drawStalk(looks.getValue(ControlPoint.LIGHT_STALK), Offset(300f, 360f), anchors.getValue(ControlPoint.LIGHT_STALK))
            drawStalk(looks.getValue(ControlPoint.WIPER_STALK), Offset(400f, 360f), anchors.getValue(ControlPoint.WIPER_STALK))
            drawWheel(c, looks.getValue(ControlPoint.STEERING_HEAT_BUTTON))
            drawHvac(c, looks)
            drawGear(c, looks.getValue(ControlPoint.GEAR_SELECTOR))
            drawBelt(looks.getValue(ControlPoint.SEAT_BELT))
            // Pulse rings on top
            looks.forEach { (pt, look) ->
                val col = look.pulse ?: return@forEach
                val a = anchors.getValue(pt)
                drawCircle(col.copy(alpha = 0.7f * (1f - pulse)), 46f + 64f * pulse, a, style = Stroke(6f))
                val p2 = (pulse + 0.5f) % 1f
                drawCircle(col.copy(alpha = 0.45f * (1f - p2)), 46f + 64f * p2, a, style = Stroke(4f))
            }
        }
        // Screen-space text (keeps ≥ 24sp regardless of canvas scale)
        drawGearLetters(measurer, c, looks.getValue(ControlPoint.GEAR_SELECTOR), ::toScreen, s)
        quizMarks.forEach { pt -> drawQuizMark(measurer, c, toScreen(anchors.getValue(pt)), s) }
        labels.forEach { (pt, text) ->
            drawCallout(measurer, text, looks.getValue(pt), toScreen(anchors.getValue(pt)), s)
        }
        // Soft fade at the left/right edges so the dashboard blends into any layout.
        drawRect(
            Brush.horizontalGradient(0f to Color.Transparent, 0.05f to Color.Black, 0.95f to Color.Black, 1f to Color.Transparent),
            blendMode = BlendMode.DstIn,
        )
    }
}

private fun DrawScope.drawStatic(c: ChagilColors) {
    // Windshield tint + dashboard
    val dash = Path().apply {
        moveTo(-1600f, 300f)
        cubicTo(-400f, 170f, 200f, 152f, 500f, 152f)
        cubicTo(800f, 152f, 1400f, 170f, 2600f, 300f)
        lineTo(2600f, 600f); lineTo(-1600f, 600f); close()
    }
    drawPath(dash, c.cockpitFill)
    drawPath(Path().apply {
        moveTo(-1600f, 300f); cubicTo(-400f, 170f, 200f, 152f, 500f, 152f); cubicTo(800f, 152f, 1400f, 170f, 2600f, 300f)
    }, c.cockpitLine, style = Stroke(3f))
    // Instrument cluster
    drawRoundRect(c.cockpitPart, Offset(196f, 196f), Size(310f, 104f), CornerRadius(30f))
    drawRoundRect(c.cockpitLine, Offset(196f, 196f), Size(310f, 104f), CornerRadius(30f), style = Stroke(3f))
    drawArc(c.cockpitLine, 150f, 240f, false, Offset(236f, 214f), Size(68f, 68f), style = Stroke(5f, cap = StrokeCap.Round))
    drawArc(c.cockpitLine, 150f, 240f, false, Offset(398f, 214f), Size(68f, 68f), style = Stroke(5f, cap = StrokeCap.Round))
    // Center display
    drawRoundRect(c.cockpitPart, Offset(646f, 192f), Size(320f, 136f), CornerRadius(22f))
    drawRoundRect(c.cockpitLine, Offset(646f, 192f), Size(320f, 136f), CornerRadius(22f), style = Stroke(3f))
    // Steering column
    drawRoundRect(c.cockpitPart, Offset(322f, 340f), Size(56f, 260f), CornerRadius(14f))
    // Console
    val console = Path().apply { moveTo(700f, 462f); lineTo(910f, 462f); lineTo(944f, 600f); lineTo(666f, 600f); close() }
    drawPath(console, c.cockpitPart.copy(alpha = 0.7f))
    drawPath(console, c.cockpitLine, style = Stroke(3f, join = StrokeJoin.Round))
}

private fun DrawScope.drawStalk(l: PartLook, from: Offset, tip: Offset) {
    val effect = if (l.dashed) PathEffect.dashPathEffect(floatArrayOf(14f, 10f)) else null
    drawLine(l.stroke, from, tip, 28f, StrokeCap.Round, pathEffect = effect)
    if (!l.dashed) drawLine(l.fill, from, tip, 20f, StrokeCap.Round)
    drawCircle(if (l.dashed) l.stroke else l.fill, 24f, tip, style = if (l.dashed) Stroke(4f, pathEffect = effect) else androidx.compose.ui.graphics.drawscope.Fill)
    if (!l.dashed) drawCircle(l.stroke, 24f, tip, style = Stroke(4f))
}

private fun DrawScope.drawWheel(c: ChagilColors, heat: PartLook) {
    val center = Offset(350f, 432f)
    val heated = heat.glow != null && !heat.dashed
    val ring = if (heated) heat.fill.copy(alpha = 0.85f) else c.cockpitLine
    drawCircle(ring, 152f, center, style = Stroke(28f))
    drawLine(c.cockpitLine, Offset(212f, 446f), Offset(300f, 446f), 24f, StrokeCap.Round)
    drawLine(c.cockpitLine, Offset(400f, 446f), Offset(488f, 446f), 24f, StrokeCap.Round)
    drawLine(c.cockpitLine, Offset(350f, 486f), Offset(350f, 570f), 24f, StrokeCap.Round)
    drawCircle(c.cockpitPart, 56f, center)
    drawCircle(c.cockpitLine, 56f, center, style = Stroke(4f))
}

private fun DrawScope.drawHvac(c: ChagilColors, looks: Map<ControlPoint, PartLook>) {
    drawRoundRect(c.cockpitPart, Offset(652f, 352f), Size(316f, 92f), CornerRadius(24f))
    drawRoundRect(c.cockpitLine, Offset(652f, 352f), Size(316f, 92f), CornerRadius(24f), style = Stroke(3f))
    listOf(ControlPoint.HVAC_FRONT_DEFROST, ControlPoint.HVAC_REAR_DEFROST, ControlPoint.STEERING_HEAT_BUTTON).forEach { pt ->
        val l = looks.getValue(pt)
        val a = anchors.getValue(pt)
        val tl = Offset(a.x - 42f, a.y - 32f)
        val effect = if (l.dashed) PathEffect.dashPathEffect(floatArrayOf(10f, 8f)) else null
        val bg = if (l.glow != null) l.fill else c.cockpitFill
        drawRoundRect(bg, tl, Size(84f, 64f), CornerRadius(16f))
        drawRoundRect(l.stroke, tl, Size(84f, 64f), CornerRadius(16f), style = Stroke(3f, pathEffect = effect))
        val ic = if (l.glow != null) l.icon else c.textSecondary
        when (pt) {
            ControlPoint.HVAC_FRONT_DEFROST -> {
                val w = Path().apply {
                    moveTo(a.x - 22f, a.y + 14f); lineTo(a.x + 22f, a.y + 14f)
                    lineTo(a.x + 16f, a.y - 14f); lineTo(a.x - 16f, a.y - 14f); close()
                }
                drawPath(w, ic, style = Stroke(3.5f, join = StrokeJoin.Round))
                heatWaves(ic, a.x, a.y + 8f, 9f, 16f)
            }
            ControlPoint.HVAC_REAR_DEFROST -> {
                drawRoundRect(ic, Offset(a.x - 22f, a.y - 14f), Size(44f, 28f), CornerRadius(4f), style = Stroke(3.5f))
                heatWaves(ic, a.x, a.y + 8f, 9f, 16f)
            }
            else -> {
                drawCircle(ic, 12f, Offset(a.x, a.y + 6f), style = Stroke(3.5f))
                drawCircle(ic, 3.5f, Offset(a.x, a.y + 6f))
                heatWaves(ic, a.x, a.y - 10f, 8f, 12f)
            }
        }
    }
}

/** Three small wavy "heat" lines centered at [cx], bottom at [bottom]. */
private fun DrawScope.heatWaves(col: Color, cx: Float, bottom: Float, gap: Float, h: Float) {
    for (i in -1..1) {
        val x = cx + i * gap
        val p = Path().apply {
            moveTo(x, bottom)
            quadraticTo(x - 3.5f, bottom - h * 0.25f, x, bottom - h * 0.5f)
            quadraticTo(x + 3.5f, bottom - h * 0.75f, x, bottom - h)
        }
        drawPath(p, col, style = Stroke(2.6f, cap = StrokeCap.Round))
    }
}

private fun DrawScope.drawGear(c: ChagilColors, l: PartLook) {
    val effect = if (l.dashed) PathEffect.dashPathEffect(floatArrayOf(10f, 8f)) else null
    val tl = Offset(772f, 486f)
    drawRoundRect(if (l.glow != null) l.fill else c.cockpitPart, tl, Size(56f, 84f), CornerRadius(24f))
    drawRoundRect(l.stroke, tl, Size(56f, 84f), CornerRadius(24f), style = Stroke(4f, pathEffect = effect))
    drawLine(if (l.glow != null) l.icon else c.cockpitLine, Offset(800f, 500f), Offset(800f, 520f), 4f, StrokeCap.Round)
}

private fun DrawScope.drawBelt(l: PartLook) {
    val effect = if (l.dashed) PathEffect.dashPathEffect(floatArrayOf(16f, 10f)) else null
    val strapAlpha = if (l.glow != null) 1f else 0.55f
    drawLine(l.stroke.copy(alpha = strapAlpha), Offset(8f, 376f), Offset(196f, 600f), if (l.dashed) 6f else 38f, StrokeCap.Butt, pathEffect = effect)
    if (!l.dashed) drawLine(l.fill.copy(alpha = strapAlpha), Offset(8f, 376f), Offset(196f, 600f), 30f, StrokeCap.Butt)
    // latch plate
    val a = anchors.getValue(ControlPoint.SEAT_BELT)
    withTransform({ rotate(40f, a) }) {
        drawRoundRect(l.stroke, Offset(a.x - 30f, a.y - 16f), Size(60f, 32f), CornerRadius(8f), style = Stroke(5f, pathEffect = effect))
    }
}

private fun DrawScope.drawGearLetters(m: TextMeasurer, c: ChagilColors, l: PartLook, map: (Offset) -> Offset, s: Float) {
    val col = if (l.glow != null) l.fill else c.textTertiary
    val style = TextStyle(fontSize = (20f * s).toSp(), lineHeight = (20f * s).toSp(), fontWeight = FontWeight.Bold, color = col)
    "PRND".forEachIndexed { i, ch ->
        val r = m.measure(ch.toString(), style)
        val p = map(Offset(856f, 492f + i * 22f))
        drawText(r, topLeft = Offset(p.x, p.y - r.size.height / 2f))
    }
}

private fun DrawScope.drawQuizMark(m: TextMeasurer, c: ChagilColors, at: Offset, s: Float) {
    val r = m.measure("?", TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Black, color = c.text))
    val rad = 22.dp.toPx()
    val center = Offset(at.x + 34f * s, at.y - 34f * s)
    drawCircle(c.surfaceRaised, rad, center)
    drawCircle(c.textTertiary, rad, center, style = Stroke(2.dp.toPx()))
    drawText(r, topLeft = Offset(center.x - r.size.width / 2f, center.y - r.size.height / 2f))
}

private fun DrawScope.drawCallout(m: TextMeasurer, text: String, l: PartLook, at: Offset, s: Float) {
    val r = m.measure(text, TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = l.labelFg))
    val padH = 18.dp.toPx()
    val padV = 10.dp.toPx()
    val w = r.size.width + padH * 2
    val h = r.size.height + padV * 2
    val gap = 74f * s
    var top = at.y - gap - h
    var connectorFrom = Offset(at.x, top + h)
    if (top < 0f) {
        top = at.y + gap
        connectorFrom = Offset(at.x, top)
    }
    val left = (at.x - w / 2).coerceIn(0f, size.width - w)
    drawLine(l.labelBg, connectorFrom, Offset(at.x, if (top > at.y) at.y + 30f * s else at.y - 30f * s), 3.dp.toPx(), StrokeCap.Round)
    drawRoundRect(l.labelBg, Offset(left, top), Size(w, h), CornerRadius(h / 2))
    drawText(r, topLeft = Offset(left + padH, top + padV))
}

@Preview(widthDp = 1000, heightDp = 600, showBackground = true, backgroundColor = 0xFF0B0F14)
@Composable
private fun LocatorPreview() = ChagilTheme(night = true) {
    ControlLocator(ControlPoint.WIPER_STALK, Modifier.fillMaxSize().padding(24.dp), animate = false)
}

@Preview(widthDp = 1707, heightDp = 600, showBackground = true, backgroundColor = 0xFFF3F5F8)
@Composable
private fun CockpitMapPreview() = ChagilTheme(night = false) {
    Row(Modifier.padding(24.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        ControlLocator(ControlPoint.HVAC_FRONT_DEFROST, Modifier.weight(1f).aspectRatio(VW / VH), tone = LocatorTone.ATTENTION, animate = false)
        CockpitMap(
            mapOf(
                ControlPoint.LIGHT_STALK to ZoneState.LIT,
                ControlPoint.WIPER_STALK to ZoneState.JUST_LIT,
                ControlPoint.HVAC_FRONT_DEFROST to ZoneState.LIT,
                ControlPoint.HVAC_REAR_DEFROST to ZoneState.UNLIT,
                ControlPoint.STEERING_HEAT_BUTTON to ZoneState.UNLIT,
                ControlPoint.GEAR_SELECTOR to ZoneState.QUIZ,
                ControlPoint.SEAT_BELT to ZoneState.UNLIT,
            ),
            Modifier.weight(1f).aspectRatio(VW / VH),
            showLabels = true,
            animate = false,
        )
    }
}
