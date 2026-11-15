package com.chagil.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chagil.app.ui.theme.ChagilTheme

/** Small line icons drawn on a 24×24 grid. No icon font / extended icon dependency. */
enum class ChagilIcon { SPEAKER, PAUSE, PLAY, CHECK, CLOSE, RETRY, LOCK, MAP, ARROW, PARK, INFO }

@Composable
fun ChagilIconView(icon: ChagilIcon, tint: Color, modifier: Modifier = Modifier, size: Dp = 32.dp) {
    Canvas(modifier.size(size)) { drawChagilIcon(icon, tint) }
}

internal fun DrawScope.drawChagilIcon(icon: ChagilIcon, tint: Color) {
    val u = size.minDimension / 24f
    fun p(x: Float, y: Float) = Offset(x * u, y * u)
    val stroke = Stroke(width = 2.4f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
    fun path(block: Path.() -> Unit) = Path().apply(block)
    when (icon) {
        ChagilIcon.SPEAKER -> {
            drawPath(path {
                moveTo(3f * u, 9f * u); lineTo(7f * u, 9f * u); lineTo(12f * u, 4.5f * u)
                lineTo(12f * u, 19.5f * u); lineTo(7f * u, 15f * u); lineTo(3f * u, 15f * u); close()
            }, tint)
            drawArc(tint, -50f, 100f, false, p(9f, 7.5f), Size(9f * u, 9f * u), style = stroke)
            drawArc(tint, -55f, 110f, false, p(8f, 3.5f), Size(15f * u, 17f * u), style = stroke)
        }
        ChagilIcon.PAUSE -> {
            drawRoundRect(tint, p(6f, 4.5f), Size(4f * u, 15f * u), androidx.compose.ui.geometry.CornerRadius(1.2f * u))
            drawRoundRect(tint, p(14f, 4.5f), Size(4f * u, 15f * u), androidx.compose.ui.geometry.CornerRadius(1.2f * u))
        }
        ChagilIcon.PLAY -> drawPath(path {
            moveTo(7f * u, 4.5f * u); lineTo(19.5f * u, 12f * u); lineTo(7f * u, 19.5f * u); close()
        }, tint)
        ChagilIcon.CHECK -> drawPath(path {
            moveTo(4.5f * u, 12.5f * u); lineTo(9.5f * u, 17.5f * u); lineTo(19.5f * u, 6.5f * u)
        }, tint, style = Stroke(3f * u, cap = StrokeCap.Round, join = StrokeJoin.Round))
        ChagilIcon.CLOSE -> {
            drawLine(tint, p(6f, 6f), p(18f, 18f), 2.6f * u, StrokeCap.Round)
            drawLine(tint, p(18f, 6f), p(6f, 18f), 2.6f * u, StrokeCap.Round)
        }
        ChagilIcon.RETRY -> {
            drawArc(tint, -60f, 300f, false, p(4.5f, 4.5f), Size(15f * u, 15f * u), style = stroke)
            drawPath(path {
                moveTo(15.5f * u, 2.5f * u); lineTo(19.5f * u, 5.8f * u); lineTo(15f * u, 8.5f * u)
            }, tint, style = stroke)
        }
        ChagilIcon.LOCK -> {
            drawRoundRect(tint, p(5f, 10.5f), Size(14f * u, 10f * u), androidx.compose.ui.geometry.CornerRadius(2f * u))
            drawArc(tint, 180f, 180f, false, p(8f, 4f), Size(8f * u, 9f * u), style = stroke)
            drawLine(tint, p(8f, 8.5f), p(8f, 10.5f), 2.4f * u)
            drawLine(tint, p(16f, 8.5f), p(16f, 10.5f), 2.4f * u)
        }
        ChagilIcon.MAP -> {
            drawPath(path {
                moveTo(3.5f * u, 6f * u); lineTo(9f * u, 4f * u); lineTo(15f * u, 6f * u); lineTo(20.5f * u, 4f * u)
                lineTo(20.5f * u, 18f * u); lineTo(15f * u, 20f * u); lineTo(9f * u, 18f * u); lineTo(3.5f * u, 20f * u); close()
            }, tint, style = stroke)
            drawLine(tint, p(9f, 4f), p(9f, 18f), 2f * u)
            drawLine(tint, p(15f, 6f), p(15f, 20f), 2f * u)
        }
        ChagilIcon.ARROW -> {
            drawLine(tint, p(4.5f, 12f), p(19f, 12f), 2.6f * u, StrokeCap.Round)
            drawPath(path {
                moveTo(13f * u, 6f * u); lineTo(19f * u, 12f * u); lineTo(13f * u, 18f * u)
            }, tint, style = Stroke(2.6f * u, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        ChagilIcon.PARK -> {
            drawRoundRect(tint, p(3.5f, 3.5f), Size(17f * u, 17f * u), androidx.compose.ui.geometry.CornerRadius(4f * u), style = stroke)
            drawPath(path {
                moveTo(9.5f * u, 17f * u); lineTo(9.5f * u, 7f * u); lineTo(13f * u, 7f * u)
                cubicTo(16.5f * u, 7f * u, 16.5f * u, 12.5f * u, 13f * u, 12.5f * u); lineTo(9.5f * u, 12.5f * u)
            }, tint, style = stroke)
        }
        ChagilIcon.INFO -> {
            drawCircle(tint, 9f * u, p(12f, 12f), style = stroke)
            drawLine(tint, p(12f, 11f), p(12f, 16.5f), 2.6f * u, StrokeCap.Round)
            drawCircle(tint, 1.5f * u, p(12f, 7.5f))
        }
    }
}

@Preview(widthDp = 600, heightDp = 80, backgroundColor = 0xFF0B0F14, showBackground = true)
@Composable
private fun IconsPreview() = ChagilTheme(night = true) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        ChagilIcon.entries.forEach { ChagilIconView(it, ChagilTheme.colors.text, size = 40.dp) }
    }
}
