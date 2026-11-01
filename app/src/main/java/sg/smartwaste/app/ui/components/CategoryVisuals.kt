package sg.smartwaste.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Kitchen
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Store
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import sg.smartwaste.app.data.model.EWasteCategory
import sg.smartwaste.app.data.model.Place
import sg.smartwaste.app.data.model.PlaceType
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sin

val EWasteCategory.icon: ImageVector
    get() = when (this) {
        EWasteCategory.DEVICE_WITH_BATTERY -> Icons.Rounded.Smartphone
        EWasteCategory.APPLIANCE_NO_BATTERY -> Icons.Rounded.Print
        EWasteCategory.SMALL_BATTERY -> Icons.Rounded.BatteryChargingFull
        EWasteCategory.COMPONENT -> Icons.Rounded.Memory
        EWasteCategory.LAMP -> Icons.Rounded.Lightbulb
        EWasteCategory.LARGE_APPLIANCE -> Icons.Rounded.Kitchen
    }

/** Colour stops for a photo-like cover: sky, mid tone, ground, and a warm glow. */
data class CoverTones(val top: Color, val mid: Color, val bottom: Color, val glow: Color)

/** Photographic palettes (alpine sunset, lagoon, golden hour…) used for every cover. */
object Tones {
    val Alpine = CoverTones(Color(0xFF8FC3CF), Color(0xFF4D8597), Color(0xFF8A4B32), Color(0xFFF2A35E))
    val Harbor = CoverTones(Color(0xFFBCD3E3), Color(0xFF5F7F99), Color(0xFF2C3A4A), Color(0xFFFFD9A8))
    val Golden = CoverTones(Color(0xFFF6D49A), Color(0xFFDD8E52), Color(0xFF6E3B2A), Color(0xFFFFE3B0))
    val Lagoon = CoverTones(Color(0xFF9ED9D6), Color(0xFF2E9AA6), Color(0xFF1C5A55), Color(0xFFE4F6F2))
    val Dusk = CoverTones(Color(0xFFB9B6D9), Color(0xFF7D6E9E), Color(0xFF3B3354), Color(0xFFF4B48C))
    val Forest = CoverTones(Color(0xFFC5DDC0), Color(0xFF5E9466), Color(0xFF26442E), Color(0xFFF3E7B0))

    val all = listOf(Alpine, Lagoon, Golden, Harbor, Dusk, Forest)

    /** Stable pick for anything with an id, so each card keeps its own look. */
    fun forKey(key: String): CoverTones = all[abs(key.hashCode() % all.size)]
}

val EWasteCategory.tones: CoverTones
    get() = when (this) {
        EWasteCategory.DEVICE_WITH_BATTERY -> Tones.Alpine
        EWasteCategory.APPLIANCE_NO_BATTERY -> Tones.Harbor
        EWasteCategory.SMALL_BATTERY -> Tones.Golden
        EWasteCategory.COMPONENT -> Tones.Lagoon
        EWasteCategory.LAMP -> Tones.Dusk
        EWasteCategory.LARGE_APPLIANCE -> Tones.Forest
    }

/** Category accent for icons, progress bars and chart rows (mid tone of its cover). */
val EWasteCategory.tint: Color
    get() = tones.mid

fun placeTones(p: Place): CoverTones = if (p.isSmartBin) Tones.Lagoon else Tones.forKey(p.id)

fun placeIcon(p: Place): ImageVector = when {
    p.type == PlaceType.BUYER -> Icons.Rounded.Store
    p.isSmartBin -> Icons.Rounded.VerifiedUser
    else -> (p.accepts.firstOrNull() ?: EWasteCategory.DEVICE_WITH_BATTERY).icon
}

/**
 * Photo-style cover art: a sky-to-ground gradient, a soft sun glow and two ridge
 * silhouettes, with an optional white glyph. Stands in for the destination photos
 * of the reference design. [seed] varies the glow and ridges between cards.
 */
@Composable
fun CoverArt(
    tones: CoverTones,
    modifier: Modifier = Modifier,
    seed: Int = 0,
    icon: ImageVector? = null,
    iconSize: Dp = 72.dp,
    iconAlignment: Alignment = Alignment.Center,
    scrim: Boolean = false,
) {
    val k = seed and 0x7fffffff
    val glowX = 0.2f + (k % 7) / 10f
    val glowY = 0.22f + (k / 7 % 3) / 10f
    val phaseBack = (k % 11) * 0.57f
    val phaseFront = (k / 11 % 13) * 0.43f
    Box(modifier.background(Brush.verticalGradient(listOf(tones.top, tones.mid, tones.bottom)))) {
        Canvas(Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val center = Offset(w * glowX, h * glowY)
            val radius = max(w, h) * 0.62f
            drawRect(Brush.radialGradient(listOf(tones.glow.copy(alpha = 0.8f), tones.glow.copy(alpha = 0f)), center, radius))
            drawPath(ridge(w, h, base = 0.64f, amp = 0.11f, phase = phaseBack), tones.bottom.copy(alpha = 0.5f))
            drawPath(ridge(w, h, base = 0.8f, amp = 0.07f, phase = phaseFront), tones.bottom.copy(alpha = 0.92f))
        }
        if (icon != null) {
            Icon(icon, null, tint = Color.White.copy(alpha = 0.92f), modifier = Modifier.align(iconAlignment).size(iconSize))
        }
        if (scrim) {
            Box(
                Modifier.matchParentSize().background(
                    Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.08f), 0.45f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.6f))
                )
            )
        }
    }
}

private fun ridge(w: Float, h: Float, base: Float, amp: Float, phase: Float): Path {
    val path = Path()
    val y0 = h * base
    val a = h * amp
    val steps = 4
    path.moveTo(0f, h)
    path.lineTo(0f, y0 + a * sin(phase))
    for (i in 1..steps) {
        val xa = w * (i - 1) / steps
        val xb = w * i / steps
        val ya = y0 + a * sin(phase + (i - 1) * 1.7f)
        val yb = y0 + a * sin(phase + i * 1.7f)
        val cx = (xa + xb) / 2f
        path.cubicTo(cx, ya, cx, yb, xb, yb)
    }
    path.lineTo(w, h)
    path.close()
    return path
}
