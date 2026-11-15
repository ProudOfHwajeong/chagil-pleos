package com.chagil.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chagil.app.ui.theme.ChagilSpacing
import com.chagil.app.ui.theme.ChagilTheme

/** "폭우 드릴 · 2/4단계" on the left, chips/buttons on the right ([actions]). Height 104dp. */
@Composable
fun DrillTopBar(
    title: String,
    modifier: Modifier = Modifier,
    progress: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val c = ChagilTheme.colors
    Row(
        modifier.fillMaxWidth().height(ChagilSpacing.topBarHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = ChagilTheme.type.title.copy(fontWeight = FontWeight.Bold), color = c.text, modifier = Modifier.semantics { heading() })
        if (progress != null) {
            Text("  ·  ", style = ChagilTheme.type.title, color = c.textTertiary)
            Text(progress, style = ChagilTheme.type.title, color = c.textSecondary)
        }
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(ChagilSpacing.m), verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

@Preview(widthDp = 1707, heightDp = 120, showBackground = true, backgroundColor = 0xFF0B0F14)
@Composable
private fun TopBarPreview() = ChagilTheme(night = true) {
    DrillTopBar("폭우 드릴", Modifier.padding(horizontal = 48.dp), progress = "2/4단계") {
        SourceChip(simulation = true)
        Spacer(Modifier.width(8.dp))
        TopBarButton("일시정지", {}, icon = ChagilIcon.PAUSE)
        TopBarButton("그만하기", {}, icon = ChagilIcon.CLOSE)
    }
}
