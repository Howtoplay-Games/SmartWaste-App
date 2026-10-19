package sg.smartwaste.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** SmartWaste design tokens: soft neutral canvas, ink pills, emerald accent, navy scanner. */
object SW {
    val Ink = Color(0xFF111316)
    val Ink2 = Color(0xFF23262B)
    val Bg = Color(0xFFF2F3F5)
    val Card = Color(0xFFFFFFFF)
    val Muted = Color(0xFF8A8F98)
    val Subtle = Color(0xFF5B616B)
    val Line = Color(0xFFE6E8EC)
    val Field = Color(0xFFEDEFF2)

    val Emerald = Color(0xFF12B76A)
    val EmeraldDeep = Color(0xFF067647)
    val Mint = Color(0xFFDCFCE7)
    val Lime = Color(0xFFCDF463)

    val Navy = Color(0xFF071339)
    val Navy2 = Color(0xFF13245C)
    val Navy3 = Color(0xFF243672)
    val Electric = Color(0xFF2F6BFF)
    val Sky = Color(0xFFE3ECFF)

    val Amber = Color(0xFFF59E0B)
    val AmberSoft = Color(0xFFFEF3C7)
    val Coral = Color(0xFFEF4444)
    val CoralSoft = Color(0xFFFEE2E2)
}

private val colors = lightColorScheme(
    primary = SW.Ink,
    onPrimary = Color.White,
    secondary = SW.Emerald,
    onSecondary = Color.White,
    tertiary = SW.Electric,
    background = SW.Bg,
    onBackground = SW.Ink,
    surface = SW.Card,
    onSurface = SW.Ink,
    surfaceVariant = SW.Field,
    onSurfaceVariant = SW.Subtle,
    outline = SW.Line,
    error = SW.Coral,
)

private val base = FontFamily.Default

private val typography = Typography(
    displaySmall = TextStyle(fontFamily = base, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, lineHeight = 40.sp, letterSpacing = (-0.8).sp),
    headlineMedium = TextStyle(fontFamily = base, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.6).sp),
    headlineSmall = TextStyle(fontFamily = base, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.4).sp),
    titleLarge = TextStyle(fontFamily = base, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontFamily = base, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = base, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = base, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = base, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = base, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = base, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = base, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp),
    labelSmall = TextStyle(fontFamily = base, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp),
)

private val shapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
)

@Composable
fun SmartWasteTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = typography, shapes = shapes, content = content)
}
