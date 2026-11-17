package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DarkInstaColorScheme =
  darkColorScheme(
    primary = BrandMagenta,
    onPrimary = PureWhite,
    primaryContainer = CharcoalCard,
    onPrimaryContainer = PureWhite,
    secondary = BrandOrange,
    onSecondary = PureWhite,
    secondaryContainer = DarkBorder,
    onSecondaryContainer = PureWhite,
    tertiary = BrandAmber,
    onTertiary = PureBlack,
    background = PureBlack,
    onBackground = PureWhite,
    surface = ObsidianSurface,
    onSurface = PureWhite,
    surfaceVariant = CharcoalCard,
    onSurfaceVariant = SilverText,
    outline = DarkBorder,
    error = DangerRed,
    onError = PureWhite,
  )

private val LightInstaColorScheme =
  lightColorScheme(
    primary = PureBlack,
    onPrimary = PureWhite,
    primaryContainer = SoftSnow,
    onPrimaryContainer = PureBlack,
    secondary = BrandMagenta,
    onSecondary = PureWhite,
    secondaryContainer = SoftSnow,
    onSecondaryContainer = PureBlack,
    tertiary = BrandOrange,
    onTertiary = PureWhite,
    background = PureWhite,
    onBackground = PureBlack,
    surface = PureWhite,
    onSurface = PureBlack,
    surfaceVariant = SoftSnow,
    onSurfaceVariant = MutedSlate,
    outline = LightBorder,
    error = DangerRed,
    onError = PureWhite,
  )

val InstaShapes =
  Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
  )

@Composable
fun InstaConnectTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkInstaColorScheme else LightInstaColorScheme
  MaterialTheme(
    colorScheme = colorScheme,
    typography = InstaTypography,
    shapes = InstaShapes,
    content = content,
  )
}
