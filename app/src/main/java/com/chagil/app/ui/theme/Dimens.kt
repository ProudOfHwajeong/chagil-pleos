package com.chagil.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.dp

/** 8dp grid. Touch targets ≥ 76dp (Android for Cars), ≥ 24dp gap between targets. */
object ChagilSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val s = 12.dp
    val m = 16.dp
    val l = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
    val xxxl = 64.dp

    /** Outer page padding at 1707dp width. Use [pageGutterCompact] when width < 1400dp. */
    val pageGutter = 48.dp
    val pageGutterCompact = 32.dp

    /** Gap between two adjacent touch targets. */
    val targetGap = 24.dp
    val touchMin = 76.dp
    val buttonHeight = 88.dp
    val chipHeight = 56.dp
    val topBarHeight = 104.dp
    val stepRowHeight = 80.dp
}

@Immutable
object ChagilShapes {
    val small = RoundedCornerShape(12.dp)
    val medium = RoundedCornerShape(20.dp)
    val large = RoundedCornerShape(28.dp)
    val xl = RoundedCornerShape(36.dp)
    val pill = RoundedCornerShape(percent = 50)
}

/** Motion tokens (ms). Pulses are slow and soft: no flashing faster than 3 Hz (WCAG 2.3.1). */
object ChagilMotion {
    const val fast = 150
    const val medium = 250
    const val slow = 400
    const val pulse = 1400
    const val celebrate = 700
}
