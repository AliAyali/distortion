package com.aliayali.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DistortionDarkColorScheme = darkColorScheme(
    primary = DistortionTeal,
    onPrimary = OnDistortionTeal,
    primaryContainer = TealContainer,
    onPrimaryContainer = OnTealContainer,

    secondary = AnomalyPurple,
    onSecondary = VoidBlack,
    secondaryContainer = AnomalyContainer,
    onSecondaryContainer = Color(0xFFE4D5F5),
    tertiary = WarningAmber,
    onTertiary = VoidBlack,

    background = NightBackground,
    onBackground = TextPrimary,

    surface = NightSurface,
    onSurface = TextPrimary,
    surfaceVariant = RaisedSurface,
    onSurfaceVariant = TextSecondary,

    outline = OutlineDark,
    outlineVariant = OutlineSubtle,

    error = BloodRed,
    onError = OnBloodRed,
    errorContainer = RedContainer,
    onErrorContainer = OnRedContainer
)

private val DistortionLightColorScheme = lightColorScheme(
    primary = Color(0xFF286F69),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD0EAE5),
    onPrimaryContainer = Color(0xFF123B37),

    secondary = Color(0xFF695184),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEBDDFA),
    onSecondaryContainer = Color(0xFF302142),

    tertiary = Color(0xFF805C22),
    onTertiary = Color(0xFFFFFFFF),

    background = LightBackground,
    onBackground = LightOnBackground,

    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,

    outline = LightOutline,

    error = BloodRed,
    onError = OnBloodRed,
    errorContainer = RedContainer,
    onErrorContainer = OnRedContainer
)

@Composable
fun DistortionTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DistortionDarkColorScheme
    } else {
        DistortionLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = DistortionTypography,
        content = content
    )
}