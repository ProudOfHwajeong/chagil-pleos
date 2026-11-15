package com.chagil.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Type scale. System font only (Noto Sans CJK on AAOS) — no bundled fonts.
 * Korean wraps at word (어절) boundaries: WordBreak.Phrase (= keep-all for Korean on API 33+).
 * Sizes: instruction ≥ 48sp, primary text 32sp, body ≥ 24sp (Android for Cars minimums).
 */
private val KoreanHeading = LineBreak(
    strategy = LineBreak.Strategy.Balanced,
    strictness = LineBreak.Strictness.Strict,
    wordBreak = LineBreak.WordBreak.Phrase,
)
private val KoreanParagraph = LineBreak(
    strategy = LineBreak.Strategy.HighQuality,
    strictness = LineBreak.Strictness.Strict,
    wordBreak = LineBreak.WordBreak.Phrase,
)
/** Korean locale so the platform applies Korean line-break rules even on an en-US system. */
private val Korean = LocaleList("ko-KR")

private val Trim = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

private fun style(size: Int, line: Int, weight: FontWeight, heading: Boolean, tracking: Double = 0.0) = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.em,
    lineBreak = if (heading) KoreanHeading else KoreanParagraph,
    lineHeightStyle = Trim,
    localeList = Korean,
)

@Immutable
data class ChagilTypography(
    /** Result time, e.g. "11.2초". Tabular numbers. */
    val hero: TextStyle = style(128, 136, FontWeight.Bold, true, -0.02).copy(fontFeatureSettings = "tnum"),
    /** The one instruction line on the drill screen. */
    val instruction: TextStyle = style(60, 74, FontWeight.Bold, true, -0.01),
    /** Screen titles, card titles, dialog questions. */
    val headline: TextStyle = style(40, 52, FontWeight.Bold, true),
    /** Primary text used for decisions (32sp). Active step label, eyebrow lines. */
    val title: TextStyle = style(32, 42, FontWeight.SemiBold, true),
    /** Voice line, secondary explanations. */
    val bodyLarge: TextStyle = style(28, 40, FontWeight.Medium, false),
    /** Minimum readable text (24sp). */
    val body: TextStyle = style(24, 34, FontWeight.Normal, false),
    /** Chips, small labels (24sp, never smaller). */
    val label: TextStyle = style(24, 30, FontWeight.SemiBold, true),
    /** Button text. */
    val button: TextStyle = style(30, 36, FontWeight.Bold, true),
    /** Log lines and SDK raw values. */
    val mono: TextStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 34.sp,
        fontFeatureSettings = "tnum",
    ),
    /** Big numbers inside rings/bars. */
    val number: TextStyle = style(56, 60, FontWeight.Bold, true, -0.02).copy(fontFeatureSettings = "tnum"),
)

val LocalChagilTypography = staticCompositionLocalOf { ChagilTypography() }
