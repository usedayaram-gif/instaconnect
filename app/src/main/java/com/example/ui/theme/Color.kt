package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Core White & Black Editorial Surfaces
val PureWhite = Color(0xFFFFFFFF)
val SoftSnow = Color(0xFFF7F7F9)
val LightBorder = Color(0xFFE6E6EA)
val MutedSlate = Color(0xFF71717A)

val PureBlack = Color(0xFF000000)
val ObsidianSurface = Color(0xFF111115)
val CharcoalCard = Color(0xFF1A1A20)
val DarkBorder = Color(0xFF2B2B33)
val SilverText = Color(0xFFA1A1AA)

// InstaConnect Signature Gradient Palette
val BrandMagenta = Color(0xFFE1306C)
val BrandCoral = Color(0xFFFF5858)
val BrandOrange = Color(0xFFF77737)
val BrandAmber = Color(0xFFFCAF45)
val BrandViolet = Color(0xFF833AB4)

val VerifiedBlue = Color(0xFF0095F6)
val DangerRed = Color(0xFFEF4444)
val SuccessEmerald = Color(0xFF10B981)

val BrandGradient =
  Brush.linearGradient(
    colors = listOf(BrandViolet, BrandMagenta, BrandOrange, BrandAmber)
  )

val ButtonGradient =
  Brush.horizontalGradient(
    colors = listOf(BrandCoral, BrandMagenta, BrandOrange)
  )

val StoryRingBrush =
  Brush.sweepGradient(
    colors = listOf(BrandAmber, BrandOrange, BrandMagenta, BrandViolet, BrandMagenta, BrandAmber)
  )
