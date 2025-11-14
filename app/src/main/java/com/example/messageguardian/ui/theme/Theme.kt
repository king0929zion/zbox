package com.example.messageguardian.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = DustyRose,
    onPrimary = Color.White,
    secondary = MintLeaf,
    onSecondary = DeepBrown,
    background = IvoryMist,
    onBackground = DeepBrown,
    surface = SoftPeach,
    onSurface = DeepBrown,
    surfaceVariant = WarmSand,
    onSurfaceVariant = DeepBrown.copy(alpha = 0.7f)
)

private val DarkColors = darkColorScheme(
    primary = DustyRose,
    onPrimary = DeepBrown,
    secondary = MintLeaf,
    onSecondary = DeepBrown,
    background = DeepBrown,
    onBackground = IvoryMist,
    surface = DeepBrown,
    onSurface = IvoryMist,
    surfaceVariant = DustyRose,
    onSurfaceVariant = IvoryMist.copy(alpha = 0.8f)
)

@Composable
fun MessageGuardianTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
