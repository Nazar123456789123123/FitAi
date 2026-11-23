package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val FitAiLightColorScheme = lightColorScheme(
    primary = InkObsidian,
    onPrimary = VoltLime,
    primaryContainer = VoltLimeSoft,
    onPrimaryContainer = InkObsidian,
    secondary = RunningAccent,
    onSecondary = SurfaceWhite,
    secondaryContainer = RunningContainer,
    onSecondaryContainer = InkObsidian,
    tertiary = SwimmingAccent,
    onTertiary = SurfaceWhite,
    tertiaryContainer = SwimmingContainer,
    onTertiaryContainer = InkObsidian,
    background = CanvasStone,
    onBackground = InkObsidian,
    surface = SurfaceWhite,
    onSurface = InkObsidian,
    surfaceVariant = SurfaceMuted,
    onSurfaceVariant = InkCharcoal,
    outline = HairlineBorder,
    outlineVariant = SurfaceSubtle,
    error = ZoneHighCrimson,
    onError = SurfaceWhite,
    errorContainer = ZoneHighBg,
    onErrorContainer = ZoneHighCrimson
)

val FitAiShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

@Composable
fun FitAiTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = FitAiLightColorScheme,
        typography = FitAiTypography,
        shapes = FitAiShapes,
        content = content
    )
}
