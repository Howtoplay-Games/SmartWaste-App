package sg.smartwaste.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * SmartWaste design tokens: ONE palette used on every screen.
 * Near-black olive surfaces + a single lime accent (from the login reference),
 * with harmonised green/yellow tints for categories and charts.
 */
object SW {
    // Surfaces
    val Bg = Color(0xFF0A0B07)          // app background
    val Sheet = Color(0xFF101209)       // content sheet under hero headers
    val Card = Color(0xFF171A10)        // cards
    val Card2 = Color(0xFF20241A)       // raised elements, chips, icon buttons
    val Field = Color(0xFF0E1009)       // text fields
    val Line = Color(0xFF2A2E21)        // borders & dividers

    // Text
    val Text = Color(0xFFF4F6EC)
    val Subtle = Color(0xFFB3B8A6)
    val Muted = Color(0xFF7B8070)

    // Accent
    val Lime = Color(0xFFD8F65C)
    val LimeDeep = Color(0xFFA6C437)
    val LimeSoft = Color(0x24D8F65C)     // 14% lime wash
    val OnLime = Color(0xFF0A0B07)
    val Olive = Color(0xFF3B4715)
    val OliveDark = Color(0xFF1A2008)

    // Harmonised tints (categories, charts, place types)
    val Mint = Color(0xFF7FD8C4)
    val Sage = Color(0xFF9FE0A8)
    val Amber = Color(0xFFF2C14E)
    val Butter = Color(0xFFF2E27A)
    val Moss = Color(0xFFB5C77A)

    // Semantic
    val Warn = Color(0xFFF2B544)
    val Danger = Color(0xFFF0705F)
    val DangerSoft = Color(0x29F0705F)

    /** Olive glow used behind hero headers on every screen. */
    val heroBrush: Brush
        get() = Brush.verticalGradient(listOf(Color(0xFF2C3610), Color(0xFF151A07), Bg))
}

private val colors = darkColorScheme(
    primary = SW.Lime,
    onPrimary = SW.OnLime,
    secondary = SW.Mint,
    onSecondary = SW.OnLime,
    tertiary = SW.Amber,
    background = SW.Bg,
    onBackground = SW.Text,
    surface = SW.Card,
    onSurface = SW.Text,
    surfaceVariant = SW.Card2,
    onSurfaceVariant = SW.Subtle,
    surfaceContainerHigh = SW.Card,
    outline = SW.Line,
    error = SW.Danger,
)

private val base = FontFamily.Default

private val typography = Typography(
    displaySmall = TextStyle(fontFamily = base, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 46.sp, letterSpacing = (-1).sp),
    headlineMedium = TextStyle(fontFamily = base, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.6).sp),
    headlineSmall = TextStyle(fontFamily = base, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.4).sp),
    titleLarge = TextStyle(fontFamily = base, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontFamily = base, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = base, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = base, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = base, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = base, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontFamily = base, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = base, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp),
    labelSmall = TextStyle(fontFamily = base, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp),
)

private val shapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
)

@Composable
fun SmartWasteTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = typography, shapes = shapes, content = content)
}
