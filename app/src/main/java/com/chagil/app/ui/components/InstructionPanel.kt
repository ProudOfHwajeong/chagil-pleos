package com.chagil.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chagil.app.ui.theme.ChagilColors
import com.chagil.app.ui.theme.ChagilShapes
import com.chagil.app.ui.theme.ChagilSpacing
import com.chagil.app.ui.theme.ChagilTheme

/**
 * NORMAL = 지금 할 일, HINT = 다른 칸 안내 (amber, never "틀렸어요"),
 * CALM = 시간 초과 ("괜찮아요. 여기 있어요"), SUCCESS = 통과.
 */
enum class InstructionTone { NORMAL, HINT, CALM, SUCCESS }

internal fun ChagilColors.toneColor(t: InstructionTone) = when (t) {
    InstructionTone.NORMAL -> primary
    InstructionTone.HINT -> attention
    InstructionTone.CALM -> attention
    InstructionTone.SUCCESS -> success
}

internal fun ChagilColors.toneContainer(t: InstructionTone) = when (t) {
    InstructionTone.NORMAL -> primaryContainer
    InstructionTone.HINT -> attentionContainer
    InstructionTone.CALM -> attentionContainer
    InstructionTone.SUCCESS -> successContainer
}

/**
 * The single big instruction block of the drill screen.
 * @param eyebrow small line above the headline, e.g. "지금은" (hint) or "2/4단계".
 * @param headline instruction ≥ 48sp. Keep ≤ 14 Korean characters per line, max 2 lines.
 * @param voice what TTS says; shown with a speaker icon, quoted.
 * @param readValue e.g. "와이퍼 MIST" — rendered as "차가 읽은 값: 와이퍼 MIST".
 * @param checking shows "차가 확인하는 중" with a pulsing primary dot (car is judging; green is kept for "passed").
 * @param simulation the value comes from the in-app simulator: say "시뮬레이션 값", not "차가 읽은 값".
 */
@Composable
fun InstructionPanel(
    headline: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    voice: String? = null,
    readValue: String? = null,
    checking: Boolean = false,
    tone: InstructionTone = InstructionTone.NORMAL,
    simulation: Boolean = false,
) {
    val c = ChagilTheme.colors
    val t = ChagilTheme.type
    val accent by animateColorAsState(c.toneColor(tone), label = "accent")
    Row(modifier.height(IntrinsicSize.Min)) {
        Box(
            Modifier
                .width(8.dp)
                .fillMaxHeight()
                .clip(ChagilShapes.pill)
                .background(accent),
        )
        Spacer(Modifier.width(ChagilSpacing.xl))
        Column(
            Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            verticalArrangement = Arrangement.spacedBy(ChagilSpacing.m),
        ) {
            if (eyebrow != null) Text(eyebrow, style = t.title, color = accent)
            Text(headline, style = t.instruction, color = c.text, maxLines = 3)
            if (voice != null) VoiceLine(voice, accent, c.toneContainer(tone))
            if (checking || readValue != null) {
                Spacer(Modifier.height(ChagilSpacing.xxs))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (checking) {
                        StatusDot(c.primary, pulsing = true)
                        Spacer(Modifier.width(ChagilSpacing.s))
                        Text(if (simulation) "시뮬레이션 값 확인 중" else "차가 확인하는 중", style = t.label, color = c.primary)
                        if (readValue != null) Spacer(Modifier.width(ChagilSpacing.xl))
                    }
                    if (readValue != null) ReadValue(readValue, if (tone == InstructionTone.HINT) c.attention else c.textSecondary, simulation = simulation)
                }
            }
        }
    }
}

@Composable
fun VoiceLine(text: String, accent: Color, container: Color, modifier: Modifier = Modifier) {
    val c = ChagilTheme.colors
    Row(modifier, verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(container),
            contentAlignment = Alignment.Center,
        ) { ChagilIconView(ChagilIcon.SPEAKER, accent, size = 28.dp) }
        Spacer(Modifier.width(ChagilSpacing.m))
        Text(
            "“$text”",
            style = ChagilTheme.type.bodyLarge,
            color = c.textSecondary,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

/** "차가 읽은 값: 와이퍼 MIST" — raw SDK value in mono so judges see it is a real reading. */
@Composable
fun ReadValue(value: String, color: Color, modifier: Modifier = Modifier, simulation: Boolean = false) {
    val c = ChagilTheme.colors
    Row(
        modifier
            .clip(ChagilShapes.small)
            .background(c.surfaceRaised)
            .padding(horizontal = ChagilSpacing.m, vertical = ChagilSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(if (simulation) "시뮬레이션 값" else "차가 읽은 값", style = ChagilTheme.type.label, color = c.textSecondary)
        Spacer(Modifier.width(ChagilSpacing.s))
        Text(value, style = ChagilTheme.type.mono.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = color)
    }
}

@Preview(widthDp = 1000, heightDp = 420, showBackground = true, backgroundColor = 0xFF0B0F14)
@Composable
private fun InstructionPreview() = ChagilTheme(night = true) {
    InstructionPanel(
        headline = "와이퍼를 한 단계 올려 보세요",
        voice = "오른쪽 레버예요. 비 오는 날 쓰는 단계로 옮겨 보세요.",
        checking = true,
        modifier = Modifier.padding(32.dp),
    )
}

@Preview(widthDp = 1000, heightDp = 460, showBackground = true, backgroundColor = 0xFF0B0F14)
@Composable
private fun InstructionHintPreview() = ChagilTheme(night = true) {
    InstructionPanel(
        eyebrow = "지금은",
        headline = "“한 번 닦기”예요",
        voice = "반대쪽으로 한 칸 옮겨 보세요.",
        readValue = "와이퍼 MIST",
        tone = InstructionTone.HINT,
        modifier = Modifier.padding(32.dp),
    )
}
