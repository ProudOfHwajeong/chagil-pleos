package com.chagil.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle

/**
 * App theme. Night is the default look in the car; day follows the system (AAOS switches
 * day/night with the headlights / ambient light sensor through the system UI mode).
 */
@Composable
fun ChagilTheme(
    night: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (night) NightColors else DayColors
    val type = ChagilTypography()
    val m3 = if (night) {
        darkColorScheme(
            primary = colors.primary, onPrimary = colors.onPrimary,
            primaryContainer = colors.primaryContainer, onPrimaryContainer = colors.text,
            background = colors.background, onBackground = colors.text,
            surface = colors.surface, onSurface = colors.text,
            surfaceVariant = colors.surfaceRaised, onSurfaceVariant = colors.textSecondary,
            outline = colors.outline, error = colors.rest,
        )
    } else {
        lightColorScheme(
            primary = colors.primary, onPrimary = colors.onPrimary,
            primaryContainer = colors.primaryContainer, onPrimaryContainer = colors.text,
            background = colors.background, onBackground = colors.text,
            surface = colors.surface, onSurface = colors.text,
            surfaceVariant = colors.surfaceRaised, onSurfaceVariant = colors.textSecondary,
            outline = colors.outline, error = colors.rest,
        )
    }
    CompositionLocalProvider(
        LocalChagilColors provides colors,
        LocalChagilTypography provides type,
    ) {
        MaterialTheme(colorScheme = m3) {
            CompositionLocalProvider(
                LocalContentColor provides colors.text,
                LocalTextStyle provides type.body,
                content = content,
            )
        }
    }
}

/** Accessors: `ChagilTheme.colors.primary`, `ChagilTheme.type.instruction`. */
object ChagilTheme {
    val colors: ChagilColors
        @Composable @ReadOnlyComposable get() = LocalChagilColors.current
    val type: ChagilTypography
        @Composable @ReadOnlyComposable get() = LocalChagilTypography.current
}
