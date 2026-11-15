package com.chagil.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic color tokens. Contrast ratios (WCAG 2.1) are listed in docs/design/DESIGN.md.
 * Rules: primary = "do this now", success = "the car confirmed it", attention = "look again"
 * (wrong operation, time out — never blame), sim = "not a real car signal". Red is reserved for
 * the L0 driving state only and is never used for a quest result.
 */
@Immutable
data class ChagilColors(
    val isNight: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val outline: Color,
    val text: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val attention: Color,
    val onAttention: Color,
    val attentionContainer: Color,
    val sim: Color,
    val onSim: Color,
    val simContainer: Color,
    val rest: Color,
    val restContainer: Color,
    val cockpitLine: Color,
    val cockpitFill: Color,
    val cockpitPart: Color,
    /** Dim layer behind dialogs and the side panel. Dark in both themes so a white card stands out. */
    val scrim: Color,
)

val NightColors = ChagilColors(
    isNight = true,
    background = Color(0xFF0B0F14),
    surface = Color(0xFF151B23),
    surfaceRaised = Color(0xFF1F2731),
    outline = Color(0xFF2E3946),
    text = Color(0xFFF2F5F8),
    textSecondary = Color(0xFFA7B2BF),
    textTertiary = Color(0xFF8A95A3),
    primary = Color(0xFF5CB8FF),
    onPrimary = Color(0xFF04223D),
    primaryContainer = Color(0xFF0F2B45),
    success = Color(0xFF3DDC97),
    onSuccess = Color(0xFF032617),
    successContainer = Color(0xFF0D2E22),
    attention = Color(0xFFFFC24D),
    onAttention = Color(0xFF2E1F00),
    attentionContainer = Color(0xFF33260A),
    sim = Color(0xFFC9A2FF),
    onSim = Color(0xFF1E0B3A),
    simContainer = Color(0xFF2A1C42),
    rest = Color(0xFFFF8A7A),
    restContainer = Color(0xFF3A1A16),
    cockpitLine = Color(0xFF4E5B6B),
    cockpitFill = Color(0xFF121820),
    cockpitPart = Color(0xFF26303C),
    scrim = Color(0xD1060A0E),
)

val DayColors = ChagilColors(
    isNight = false,
    background = Color(0xFFF3F5F8),
    surface = Color(0xFFFFFFFF),
    surfaceRaised = Color(0xFFE8ECF1),
    outline = Color(0xFFD3DAE2),
    text = Color(0xFF0E1621),
    textSecondary = Color(0xFF4A5664),
    textTertiary = Color(0xFF5F6B79),
    primary = Color(0xFF0B63CE),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCEBFF),
    success = Color(0xFF0B7A52),
    onSuccess = Color(0xFFFFFFFF),
    successContainer = Color(0xFFD6F5E7),
    attention = Color(0xFF8F5300),
    onAttention = Color(0xFFFFFFFF),
    attentionContainer = Color(0xFFFFEDCC),
    sim = Color(0xFF7A3FD1),
    onSim = Color(0xFFFFFFFF),
    simContainer = Color(0xFFEEE3FF),
    rest = Color(0xFFB3261E),
    restContainer = Color(0xFFFDE4E1),
    cockpitLine = Color(0xFF8C98A7),
    cockpitFill = Color(0xFFE9EDF2),
    cockpitPart = Color(0xFFD5DCE4),
    scrim = Color(0x8C0E1621),
)

val LocalChagilColors = staticCompositionLocalOf { NightColors }
