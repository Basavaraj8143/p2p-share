package com.basavaraj.dropshare.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DropShareColorScheme = darkColorScheme(
    background = DropShareColors.Background,
    surface = DropShareColors.Surface,
    surfaceVariant = DropShareColors.ElevatedSurface,
    outline = DropShareColors.Border,

    primary = DropShareColors.Primary,
    onPrimary = DropShareColors.OnPrimary,

    error = DropShareColors.Error,
    onError = DropShareColors.OnError,

    onBackground = DropShareColors.PrimaryText,
    onSurface = DropShareColors.PrimaryText,
    onSurfaceVariant = DropShareColors.SecondaryText,
)

@Composable
fun DropShareTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DropShareColorScheme,
        typography = AppTypography,
        content = content,
    )
}