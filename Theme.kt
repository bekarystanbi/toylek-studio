package com.toylek.studio.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.toylek.studio.R

object Toylek {
    val Gold = Color(0xFFD4AF6A)
    val GoldLight = Color(0xFFF3DFB0)
    val GoldDark = Color(0xFF9C7A3C)
    val Ink = Color(0xFF0E0B10)
    val Surface = Color(0xFF1B1620)
    val SurfaceHigh = Color(0xFF272030)
    val Outline = Color(0xFF3B3245)
    val TextPrimary = Color(0xFFF7F1E8)
    val TextSecondary = Color(0xFFB9AFC2)
    val Rec = Color(0xFFE53946)
    val Success = Color(0xFF4CAF7D)

    val Display = FontFamily(Font(R.font.poiret_one))
    val Elegant = FontFamily(Font(R.font.lora_italic))
    val Ui = FontFamily(Font(R.font.fira_semibold, FontWeight.SemiBold))
    val UiLight = FontFamily(Font(R.font.montserrat))

    val GoldBrush = Brush.linearGradient(listOf(GoldLight, Gold, GoldDark))
    val BackgroundBrush = Brush.linearGradient(
        listOf(Color(0xFF1E1426), Color(0xFF0E0B10), Color(0xFF171019))
    )
}

private val Colors = darkColorScheme(
    primary = Toylek.Gold,
    onPrimary = Color(0xFF241A08),
    secondary = Toylek.GoldLight,
    onSecondary = Color(0xFF241A08),
    background = Toylek.Ink,
    onBackground = Toylek.TextPrimary,
    surface = Toylek.Surface,
    onSurface = Toylek.TextPrimary,
    surfaceVariant = Toylek.SurfaceHigh,
    onSurfaceVariant = Toylek.TextSecondary,
    outline = Toylek.Outline,
    error = Toylek.Rec,
)

private val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = Toylek.Display, fontSize = 64.sp, letterSpacing = 6.sp),
    displayMedium = TextStyle(fontFamily = Toylek.Display, fontSize = 48.sp, letterSpacing = 4.sp),
    headlineLarge = TextStyle(fontFamily = Toylek.Elegant, fontSize = 40.sp),
    headlineMedium = TextStyle(fontFamily = Toylek.Ui, fontSize = 30.sp),
    titleLarge = TextStyle(fontFamily = Toylek.Ui, fontSize = 26.sp),
    titleMedium = TextStyle(fontFamily = Toylek.Ui, fontSize = 22.sp),
    bodyLarge = TextStyle(fontFamily = Toylek.UiLight, fontSize = 20.sp),
    bodyMedium = TextStyle(fontFamily = Toylek.UiLight, fontSize = 18.sp),
    labelLarge = TextStyle(fontFamily = Toylek.Ui, fontSize = 22.sp),
    labelMedium = TextStyle(fontFamily = Toylek.Ui, fontSize = 18.sp),
)

@Composable
fun ToylekTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, typography = AppTypography, content = content)
}
