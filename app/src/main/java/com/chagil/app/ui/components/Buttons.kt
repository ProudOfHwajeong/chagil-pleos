package com.chagil.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chagil.app.ui.theme.ChagilShapes
import com.chagil.app.ui.theme.ChagilSpacing
import com.chagil.app.ui.theme.ChagilTheme

/** Rule: at most two big buttons per screen — one Primary, one Secondary. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ChagilIcon? = null,
    enabled: Boolean = true,
) {
    val c = ChagilTheme.colors
    BigButton(text, onClick, modifier, icon, enabled, container = c.primary, content = c.onPrimary, border = null)
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ChagilIcon? = null,
    enabled: Boolean = true,
) {
    val c = ChagilTheme.colors
    BigButton(text, onClick, modifier, icon, enabled, container = c.surfaceRaised, content = c.text, border = c.cockpitLine) // cockpitLine: ≥2.5:1 boundary vs surface (outline was 1.4:1)
}

/** Compact 76dp button for top bars (e.g. 그만하기 / 닫기). Counts toward the 2-button rule. */
@Composable
fun TopBarButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ChagilIcon? = null) {
    val c = ChagilTheme.colors
    Row(
        modifier
            .heightIn(min = ChagilSpacing.touchMin)
            .clip(ChagilShapes.pill)
            .background(c.surfaceRaised)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 28.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            ChagilIconView(icon, c.text, size = 28.dp)
            Spacer(Modifier.width(ChagilSpacing.s))
        }
        Text(text, style = ChagilTheme.type.label.copy(fontSize = ChagilTheme.type.bodyLarge.fontSize), color = c.text, maxLines = 1)
    }
}

@Composable
private fun BigButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    icon: ChagilIcon?,
    enabled: Boolean,
    container: Color,
    content: Color,
    border: Color?,
) {
    Row(
        modifier
            .defaultMinSize(minWidth = 240.dp)
            .heightIn(min = ChagilSpacing.buttonHeight)
            .alpha(if (enabled) 1f else 0.45f)
            .clip(ChagilShapes.pill)
            .background(container)
            .then(if (border != null) Modifier.border(2.dp, border, ChagilShapes.pill) else Modifier)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 40.dp, vertical = ChagilSpacing.m),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            ChagilIconView(icon, content, size = 34.dp)
            Spacer(Modifier.width(ChagilSpacing.m))
        }
        Text(
            text,
            style = ChagilTheme.type.button,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(widthDp = 900, heightDp = 260, showBackground = true, backgroundColor = 0xFF0B0F14)
@Composable
private fun ButtonsPreview() = ChagilTheme(night = true) {
    Row(Modifier.padding(32.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        PrimaryButton("다시", onClick = {}, icon = ChagilIcon.RETRY)
        SecondaryButton("나중에", onClick = {})
        TopBarButton("그만하기", onClick = {}, icon = ChagilIcon.CLOSE)
    }
}
