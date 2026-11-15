package com.chagil.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chagil.app.ui.theme.ChagilShapes
import com.chagil.app.ui.theme.ChagilSpacing
import com.chagil.app.ui.theme.ChagilTheme

/** Last 10 seconds turn amber (calm), never red. */
private const val LOW_SEC = 10

/** Ring timer: big number + "초", caption "남은 시간". [paused] dims it and shows "멈춤". */
@Composable
fun CountdownRing(
    remainingSec: Int,
    totalSec: Int,
    modifier: Modifier = Modifier,
    size: Dp = 176.dp,
    paused: Boolean = false,
) {
    val c = ChagilTheme.colors
    val t = ChagilTheme.type
    val frac by animateFloatAsState(
        if (totalSec <= 0) 0f else (remainingSec.coerceIn(0, totalSec) / totalSec.toFloat()),
        tween(900), label = "ring",
    )
    val color by animateColorAsState(
        when {
            paused -> c.textTertiary
            remainingSec <= LOW_SEC -> c.attention
            else -> c.primary
        },
        label = "ringColor",
    )
    Box(
        modifier.size(size).semantics { contentDescription = "남은 시간 ${remainingSec}초" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = 14.dp.toPx()
            val inset = w / 2
            val arcSize = Size(this.size.width - w, this.size.height - w)
            drawArc(c.surfaceRaised, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(w))
            drawArc(color, -90f, 360f * frac, false, Offset(inset, inset), arcSize, style = Stroke(w, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$remainingSec", style = t.number, color = c.text)
                Text("초", style = t.title, color = c.textSecondary, modifier = Modifier.padding(bottom = 6.dp, start = 2.dp))
            }
            Text(if (paused) "멈춤" else "남은 시간", style = t.label, color = c.textSecondary)
        }
    }
}

/** Compact bar variant for narrow layouts (1280dp): "남은 시간 31초" + bar. */
@Composable
fun CountdownBar(remainingSec: Int, totalSec: Int, modifier: Modifier = Modifier) {
    val c = ChagilTheme.colors
    val frac by animateFloatAsState(
        if (totalSec <= 0) 0f else remainingSec.coerceIn(0, totalSec) / totalSec.toFloat(), tween(900), label = "bar",
    )
    val color = if (remainingSec <= LOW_SEC) c.attention else c.primary
    Column(modifier.semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(ChagilSpacing.s)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("남은 시간", style = ChagilTheme.type.label, color = c.textSecondary)
            Spacer(Modifier.width(ChagilSpacing.s))
            Text("${remainingSec}초", style = ChagilTheme.type.title, color = c.text)
        }
        Box(Modifier.fillMaxWidth().height(12.dp).clip(ChagilShapes.pill).background(c.surfaceRaised)) {
            Box(Modifier.fillMaxWidth(frac).height(12.dp).clip(ChagilShapes.pill).background(color))
        }
    }
}

@Preview(widthDp = 700, heightDp = 260, showBackground = true, backgroundColor = 0xFF0B0F14)
@Composable
private fun CountdownPreview() = ChagilTheme(night = true) {
    Row(Modifier.padding(24.dp), horizontalArrangement = Arrangement.spacedBy(32.dp), verticalAlignment = Alignment.CenterVertically) {
        CountdownRing(31, 45)
        CountdownRing(8, 45)
        CountdownBar(31, 45, Modifier.width(260.dp))
    }
}
