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

/**
 * SmartWaste design tokens: ONE palette used on every screen.
 * Light grey canvas, white cards and sheets, and a single charcoal accent for
 * selected chips, primary buttons and the floating bottom bar. Photo-like cover
 * art supplies the colour (see CoverArt in ui/components).
 */
object SW {
    // Surfaces
    val Bg = Color(0xFFF2F2F2)          // app background
    val Surface = Color(0xFFFFFFFF)     // cards, sheets, search pill, round buttons
    val SurfaceAlt = Color(0xFFF4F4F5)  // grey cards inside white sheets, idle tabs, fields
    val Line = Color(0xFFE6E6E9)        // hairline borders & dividers
    val Track = Color(0xFFE9E9EC)       // progress-bar tracks

    // The single dark accent
    val Ink = Color(0xFF1F2023)
    val InkSoft = Color(0xFF3A3B40)
    val OnInk = Color(0xFFFFFFFF)

    // Text
    val Text = Color(0xFF17181B)
    val Subtle = Color(0xFF55575D)
    val Muted = Color(0xFF9A9BA1)

    // Overlays used on top of photos / cover art
    val Frost = Color(0x33FFFFFF)       // frosted circles and tags
    val FrostLine = Color(0x59FFFFFF)   // their hairline
    val Smoke = Color(0x66000000)       // the dark translucent "See more" pill

    // Semantic
    val Success = Color(0xFF2E9B5F)
    val SuccessSoft = Color(0xFFE6F4EC)
    val Warn = Color(0xFFE0921C)
    val Danger = Color(0xFFE5484D)
    val DangerSoft = Color(0xFFFDECEC)
    val Info = Color(0xFF2F6FED)

    // Map pin tones (buyers and SmartWaste smart bins); regular bins use Ink
    val Sunset = Color(0xFFD27D4E)
    val Lagoon = Color(0xFF2A9DA8)
}

private val colors = lightColorScheme(
    primary = SW.Ink,
    onPrimary = SW.OnInk,
    primaryContainer = SW.SurfaceAlt,
    onPrimaryContainer = SW.Text,
    secondary = SW.InkSoft,
    onSecondary = SW.OnInk,
    tertiary = SW.Lagoon,
    onTertiary = SW.OnInk,
    background = SW.Bg,
    onBackground = SW.Text,
    surface = SW.Surface,
    onSurface = SW.Text,
    surfaceVariant = SW.SurfaceAlt,
    onSurfaceVariant = SW.Subtle,
    surfaceContainerLowest = SW.Surface,
    surfaceContainerLow = SW.Surface,
    surfaceContainer = SW.Surface,
    surfaceContainerHigh = SW.Surface,
    surfaceContainerHighest = SW.SurfaceAlt,
    outline = SW.Line,
    outlineVariant = SW.Line,
    error = SW.Danger,
)

private val base = FontFamily.Default

private val typography = Typography(
    displaySmall = TextStyle(fontFamily = base, fontWeight = FontWeight.SemiBold, fontSize = 38.sp, lineHeight = 44.sp, letterSpacing = (-1).sp),
    headlineMedium = TextStyle(fontFamily = base, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.6).sp),
    headlineSmall = TextStyle(fontFamily = base, fontWeight = FontWeight.Medium, fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.5).sp),
    titleLarge = TextStyle(fontFamily = base, fontWeight = FontWeight.Medium, fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.3).sp),
    titleMedium = TextStyle(fontFamily = base, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = (-0.1).sp),
    titleSmall = TextStyle(fontFamily = base, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = base, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = base, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = base, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontFamily = base, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = base, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp),
    labelSmall = TextStyle(fontFamily = base, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp),
)

private val shapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
)

@Composable
fun SmartWasteTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = typography, shapes = shapes, content = content)
}
