package com.project.speakmate.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = DesignColor.Primary,
    onPrimary = DesignColor.OnPrimary,
    primaryContainer = DesignColor.PrimaryContainer,
    onPrimaryContainer = DesignColor.OnPrimaryContainer,
    secondary = DesignColor.Secondary,
    onSecondary = DesignColor.OnSecondary,
    secondaryContainer = DesignColor.SecondaryContainer,
    onSecondaryContainer = DesignColor.OnSecondaryContainer,
    tertiary = DesignColor.Tertiary,
    onTertiary = DesignColor.OnTertiary,
    tertiaryContainer = DesignColor.TertiaryContainer,
    onTertiaryContainer = DesignColor.OnTertiaryContainer,
    background = DesignColor.Background,
    onBackground = DesignColor.Text,
    surface = DesignColor.Background,
    onSurface = DesignColor.Text,
    surfaceVariant = DesignColor.SurfaceContainerHighest,
    onSurfaceVariant = DesignColor.Muted,
    surfaceTint = DesignColor.Primary,
    surfaceBright = DesignColor.Surface,
    surfaceDim = DesignColor.SurfaceContainerHighest,
    surfaceContainerLowest = DesignColor.Surface,
    surfaceContainerLow = DesignColor.SurfaceContainerLow,
    surfaceContainer = DesignColor.SurfaceContainer,
    surfaceContainerHigh = DesignColor.SurfaceContainerHigh,
    surfaceContainerHighest = DesignColor.SurfaceContainerHighest,
    error = DesignColor.Error,
    onError = DesignColor.OnError,
    errorContainer = DesignColor.ErrorSurface,
    onErrorContainer = DesignColor.OnErrorContainer,
    outline = DesignColor.Outline,
    outlineVariant = DesignColor.Border,
    inverseSurface = DesignColor.Text,
    inverseOnSurface = DesignColor.Background,
    inversePrimary = DesignColor.PrimaryContainer,
    scrim = DesignColor.Scrim
)

private val SpeakMateShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SpeakMateTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightColorScheme, typography = Typography, shapes = SpeakMateShapes, content = content)
}
