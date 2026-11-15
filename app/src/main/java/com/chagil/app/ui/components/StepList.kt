package com.chagil.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chagil.app.ui.theme.ChagilMotion
import com.chagil.app.ui.theme.ChagilSpacing
import com.chagil.app.ui.theme.ChagilTheme

/** ✓ done, ● active, ○ pending, ! attention (not found in time — shown calmly, not as an error). */
enum class StepState { DONE, ACTIVE, PENDING, ATTENTION }

data class StepItem(val label: String, val state: StepState)

private fun StepState.spoken() = when (this) {
    StepState.DONE -> "완료"
    StepState.ACTIVE -> "지금 할 단계"
    StepState.PENDING -> "남은 단계"
    StepState.ATTENTION -> "다시 볼 단계"
}

@Composable
fun StepList(steps: List<StepItem>, modifier: Modifier = Modifier) {
    Column(modifier) {
        steps.forEachIndexed { i, s ->
            StepRow(
                label = s.label,
                state = s.state,
                connectAbove = i > 0,
                connectBelow = i < steps.lastIndex,
                aboveDone = i > 0 && steps[i - 1].state == StepState.DONE,
            )
        }
    }
}

@Composable
fun StepRow(
    label: String,
    state: StepState,
    modifier: Modifier = Modifier,
    connectAbove: Boolean = false,
    connectBelow: Boolean = false,
    aboveDone: Boolean = false,
) {
    val c = ChagilTheme.colors
    val t = ChagilTheme.type
    val pulse by rememberInfiniteTransition(label = "step").animateFloat(
        0f, 1f, infiniteRepeatable(tween(ChagilMotion.pulse), RepeatMode.Restart), label = "p",
    )
    Row(
        modifier
            .height(ChagilSpacing.stepRowHeight)
            .semantics(mergeDescendants = true) { contentDescription = "$label, ${state.spoken()}" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(56.dp).fillMaxHeight(), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(56.dp, ChagilSpacing.stepRowHeight)) {
                val cx = size.width / 2
                val cy = size.height / 2
                val r = 20.dp.toPx()
                val lineW = 4.dp.toPx()
                if (connectAbove) drawLine(if (aboveDone) c.success else c.outline, Offset(cx, 0f), Offset(cx, cy - r - 4.dp.toPx()), lineW)
                if (connectBelow) drawLine(if (state == StepState.DONE) c.success else c.outline, Offset(cx, cy + r + 4.dp.toPx()), Offset(cx, size.height), lineW)
                when (state) {
                    StepState.DONE -> drawCircle(c.success, r, Offset(cx, cy))
                    StepState.ACTIVE -> {
                        drawCircle(c.primary.copy(alpha = 0.35f * (1 - pulse)), r + 10.dp.toPx() * pulse, Offset(cx, cy))
                        drawCircle(c.primary, r, Offset(cx, cy), style = Stroke(4.dp.toPx()))
                        drawCircle(c.primary, r * 0.5f, Offset(cx, cy))
                    }
                    StepState.PENDING -> drawCircle(c.outline, r - 2.dp.toPx(), Offset(cx, cy), style = Stroke(3.dp.toPx()))
                    StepState.ATTENTION -> drawCircle(c.attention, r, Offset(cx, cy))
                }
            }
            when (state) {
                StepState.DONE -> ChagilIconView(ChagilIcon.CHECK, c.onSuccess, size = 26.dp)
                StepState.ATTENTION -> Text("!", style = t.title.copy(fontWeight = FontWeight.Black), color = c.onAttention)
                else -> Unit
            }
        }
        Spacer(Modifier.width(ChagilSpacing.m))
        val (style, color) = when (state) {
            StepState.ACTIVE -> t.title to c.text
            StepState.ATTENTION -> t.title to c.attention
            StepState.DONE -> t.bodyLarge to c.textSecondary
            StepState.PENDING -> t.bodyLarge to c.textTertiary
        }
        Text(label, style = style, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Preview(widthDp = 560, heightDp = 380, showBackground = true, backgroundColor = 0xFF0B0F14)
@Composable
private fun StepListPreview() = ChagilTheme(night = true) {
    StepList(
        listOf(
            StepItem("전조등 켜기", StepState.DONE),
            StepItem("와이퍼 한 단계", StepState.ACTIVE),
            StepItem("앞유리 김서림 제거", StepState.ATTENTION),
            StepItem("와이퍼 끄기", StepState.PENDING),
        ),
        Modifier.padding(24.dp),
    )
}
