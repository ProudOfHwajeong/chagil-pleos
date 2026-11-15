package com.chagil.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.chagil.app.ui.SafetyPrompt
import com.chagil.app.ui.components.ChagilIcon
import com.chagil.app.ui.components.PromptCard
import com.chagil.app.ui.components.SafetyChip
import com.chagil.app.ui.components.SafetyChipLevel
import com.chagil.app.ui.components.SimBanner
import com.chagil.app.ui.theme.ChagilSpacing
import com.chagil.app.ui.theme.ChagilTheme

/** SCREENS §2-a. [SafetyPrompt.MOVE_TO_PARK] is the domain-review variant for a car that reads N. */
@Composable
fun SafetyPromptCard(
    kind: SafetyPrompt,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    cancelText: String = "아직이에요",
) {
    val c = ChagilTheme.colors
    when (kind) {
        SafetyPrompt.CONFIRM_PARK -> PromptCard(
            icon = ChagilIcon.PARK,
            title = "P에 두셨나요?",
            body = "기어를 P에 두고 시작해요. 차가 P를 읽을 수 없어서 한 번만 확인할게요.",
            iconTint = c.attention,
            iconContainer = c.attentionContainer,
            secondaryText = cancelText,
            onSecondary = onCancel,
            primaryText = "P에 뒀어요",
            onPrimary = onConfirm,
        )
        SafetyPrompt.MOVE_TO_PARK -> PromptCard(
            icon = ChagilIcon.PARK,
            title = "지금은 N이에요",
            body = "기어를 P로 옮기면 열려요.",
            iconTint = c.attention,
            iconContainer = c.attentionContainer,
            secondaryText = if (cancelText == "아직이에요") "알겠어요" else cancelText,
            onSecondary = onCancel,
        )
    }
}

/**
 * SCREENS §2-b. No buttons, no sound. In SIMULATION the banner and source chip stay visible
 * (legal: the simulation label is on every screen; the presenter needs the panel to put the gear back to P).
 */
@Composable
fun RestScreen(simulation: Boolean, onSource: () -> Unit) {
    val c = ChagilTheme.colors
    Box(Modifier.fillMaxSize()) {
        Row(
            Modifier.align(Alignment.TopEnd).fillMaxWidth().padding(ChagilSpacing.pageGutter),
            horizontalArrangement = Arrangement.spacedBy(ChagilSpacing.s, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (simulation) {
                SimBanner(Modifier.weight(1f))
                TappableSourceChip(true, onSource)
            }
            SafetyChip(SafetyChipLevel.L0_DRIVING)
        }
        PromptCard(
            icon = ChagilIcon.PAUSE,
            title = "주행 중에는 쉬어요",
            body = "차를 세우고 P에 두면 이어서 할 수 있어요.",
            iconTint = c.rest,
            iconContainer = c.restContainer,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}
