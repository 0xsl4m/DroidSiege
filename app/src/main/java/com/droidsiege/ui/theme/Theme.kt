package com.droidsiege.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors =
    lightColorScheme(
        primary = NavyPrimaryLight,
        onPrimary = NavyOnPrimaryLight,
        primaryContainer = NavyPrimaryContainerLight,
        onPrimaryContainer = NavyOnPrimaryContainerLight,
        secondary = NavySecondaryLight,
        onSecondary = NavyOnSecondaryLight,
        secondaryContainer = NavySecondaryContainerLight,
        onSecondaryContainer = NavyOnSecondaryContainerLight,
        tertiary = NavyTertiaryLight,
        onTertiary = NavyOnTertiaryLight,
        tertiaryContainer = NavyTertiaryContainerLight,
        onTertiaryContainer = NavyOnTertiaryContainerLight,
        error = NavyErrorLight,
        onError = NavyOnErrorLight,
        errorContainer = NavyErrorContainerLight,
        onErrorContainer = NavyOnErrorContainerLight,
        background = NavyBackgroundLight,
        onBackground = NavyOnBackgroundLight,
        surface = NavySurfaceLight,
        onSurface = NavyOnSurfaceLight,
        surfaceVariant = NavySurfaceVariantLight,
        onSurfaceVariant = NavyOnSurfaceVariantLight,
        outline = NavyOutlineLight,
    )

private val DarkColors =
    darkColorScheme(
        primary = NavyPrimaryDark,
        onPrimary = NavyOnPrimaryDark,
        primaryContainer = NavyPrimaryContainerDark,
        onPrimaryContainer = NavyOnPrimaryContainerDark,
        secondary = NavySecondaryDark,
        onSecondary = NavyOnSecondaryDark,
        secondaryContainer = NavySecondaryContainerDark,
        onSecondaryContainer = NavyOnSecondaryContainerDark,
        tertiary = NavyTertiaryDark,
        onTertiary = NavyOnTertiaryDark,
        tertiaryContainer = NavyTertiaryContainerDark,
        onTertiaryContainer = NavyOnTertiaryContainerDark,
        error = NavyErrorDark,
        onError = NavyOnErrorDark,
        errorContainer = NavyErrorContainerDark,
        onErrorContainer = NavyOnErrorContainerDark,
        background = NavyBackgroundDark,
        onBackground = NavyOnBackgroundDark,
        surface = NavySurfaceDark,
        onSurface = NavyOnSurfaceDark,
        surfaceVariant = NavySurfaceVariantDark,
        onSurfaceVariant = NavyOnSurfaceVariantDark,
        outline = NavyOutlineDark,
    )

@Composable
fun DroidSiegeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
