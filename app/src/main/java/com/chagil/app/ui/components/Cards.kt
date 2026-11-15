package com.chagil.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chagil.app.ui.theme.ChagilShapes
import com.chagil.app.ui.theme.ChagilSpacing
import com.chagil.app.ui.theme.ChagilTheme

/**
 * Centered prompt card with up to two buttons. Used for: safety confirm ("P에 두셨나요?"),
 * pause/resume ([PauseCard]), the L0 rest screen (no buttons).
 */
@Composable
fun PromptCard(
    icon: ChagilIcon,
    title: String,
    body: String?,
    modifier: Modifier = Modifier,
    iconTint: Color = ChagilTheme.colors.primary,
    iconContainer: Color = ChagilTheme.colors.primaryContainer,
    primaryText: String? = null,
    onPrimary: () -> Unit = {},
    secondaryText: String? = null,
    onSecondary: () -> Unit = {},
) {
    val c = ChagilTheme.colors
    Column(
        modifier
            .widthIn(max = 880.dp)
            .clip(ChagilShapes.xl)
            .background(c.surface)
            .padding(horizontal = ChagilSpacing.xxxl, vertical = ChagilSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(104.dp).clip(CircleShape).background(iconContainer), contentAlignment = Alignment.Center) {
            ChagilIconView(icon, iconTint, size = 52.dp)
        }
        Spacer(Modifier.height(ChagilSpacing.xl))
        Text(title, style = ChagilTheme.type.headline.copy(fontSize = ChagilTheme.type.instruction.fontSize, lineHeight = ChagilTheme.type.instruction.lineHeight), color = c.text, textAlign = TextAlign.Center)
        if (body != null) {
            Spacer(Modifier.height(ChagilSpacing.m))
            Text(body, style = ChagilTheme.type.bodyLarge, color = c.textSecondary, textAlign = TextAlign.Center)
        }
        if (primaryText != null || secondaryText != null) {
            Spacer(Modifier.height(ChagilSpacing.xxl))
            Row(horizontalArrangement = Arrangement.spacedBy(ChagilSpacing.targetGap)) {
                if (secondaryText != null) SecondaryButton(secondaryText, onSecondary)
                if (primaryText != null) PrimaryButton(primaryText, onPrimary)
            }
        }
    }
}

/** "이어서 할까요?" — shown when the car is parked again after a safety pause. */
@Composable
fun PauseCard(
    progressText: String,
    onResume: () -> Unit,
    onQuit: () -> Unit,
    modifier: Modifier = Modifier,
) = PromptCard(
    icon = ChagilIcon.PLAY,
    title = "이어서 할까요?",
    body = progressText,
    modifier = modifier,
    primaryText = "이어서 하기",
    onPrimary = onResume,
    secondaryText = "그만하기",
    onSecondary = onQuit,
)

/**
 * Completion hero: "통과" + big time + delta pill.
 * @param timeText e.g. "11.2초". @param deltaText e.g. "지난번보다 3.1초 빨라요" / "첫 기록이에요".
 * @param improved true → green pill, false/null → neutral pill (never negative wording).
 */
@Composable
fun ResultHero(
    timeText: String,
    modifier: Modifier = Modifier,
    deltaText: String? = null,
    improved: Boolean? = null,
    eyebrow: String = "통과",
) {
    val c = ChagilTheme.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(ChagilSpacing.s)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).clip(CircleShape).background(c.success), contentAlignment = Alignment.Center) {
                ChagilIconView(ChagilIcon.CHECK, c.onSuccess, size = 32.dp)
            }
            Spacer(Modifier.size(ChagilSpacing.m))
            Text(eyebrow, style = ChagilTheme.type.headline, color = c.success)
        }
        Text(timeText, style = ChagilTheme.type.hero, color = c.text)
        if (deltaText != null) {
            if (improved == true) StatusChip(deltaText, c.success, c.successContainer)
            else StatusChip(deltaText, c.textSecondary, c.surfaceRaised)
        }
    }
}

@Preview(widthDp = 1707, heightDp = 700, showBackground = true, backgroundColor = 0xFF0B0F14)
@Composable
private fun CardsPreview() = ChagilTheme(night = true) {
    Row(Modifier.padding(32.dp), horizontalArrangement = Arrangement.spacedBy(48.dp), verticalAlignment = Alignment.CenterVertically) {
        PauseCard("폭우 드릴 2/4단계에서 멈췄어요. 차가 다시 P에 있어요.", {}, {})
        ResultHero("11.2초", deltaText = "지난번보다 3.1초 빨라요", improved = true)
    }
}
