package sg.smartwaste.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sg.smartwaste.app.R
import sg.smartwaste.app.ui.theme.SW

// ====================================================================== buttons

/** Round icon button. White on the grey canvas by default (back, save, call…). */
@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    container: Color = SW.Surface,
    content: Color = SW.Text,
    size: Dp = 44.dp,
    iconSize: Dp = 20.dp,
    bordered: Boolean = false,
) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(container)
            .then(if (bordered) Modifier.border(1.dp, SW.Line, CircleShape) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = content, modifier = Modifier.size(iconSize))
    }
}

/** Frosted-glass round button for use on top of photos and cover art. */
@Composable
fun FrostedCircleButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 20.dp,
) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(SW.Frost)
            .border(1.dp, SW.FrostLine, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = Color.White, modifier = Modifier.size(iconSize))
    }
}

/** Charcoal pill button (the "Book a tour" style). */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    container: Color = SW.Ink,
    contentColor: Color = SW.OnInk,
    leading: ImageVector? = null,
    height: Dp = 56.dp,
) {
    val bg = if (enabled) container else SW.Track
    val fg = if (enabled) contentColor else SW.Muted
    Row(
        modifier
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(bg)
            .clickable(enabled = enabled && !loading, onClick = onClick)
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(color = fg, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
        } else if (leading != null) {
            Icon(leading, null, tint = fg, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = fg, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** White pill button with a hairline border. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = 56.dp,
    leadingPainter: Int? = null,
    container: Color = SW.Surface,
) {
    Row(
        modifier
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(container)
            .border(1.dp, SW.Line, RoundedCornerShape(height / 2))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        val fg = if (enabled) SW.Text else SW.Muted
        if (leadingPainter != null) {
            Image(painterResource(leadingPainter), null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        } else if (leading != null) {
            Icon(leading, null, tint = fg, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = fg, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Dark round arrow button used at the corner of listing cards. */
@Composable
fun ArrowCircle(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    container: Color = SW.Ink,
    content: Color = SW.OnInk,
    icon: ImageVector = Icons.AutoMirrored.Rounded.ArrowForward,
    contentDescription: String? = null,
    onClick: (() -> Unit)? = null,
) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(container)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = content, modifier = Modifier.size(size * 0.48f))
    }
}

/** Translucent dark pill with a white chevron button ("See more" on hero cards). */
@Composable
fun SeeMorePill(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(29.dp))
            .background(SW.Smoke)
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(29.dp))
            .clickable(onClick = onClick),
    ) {
        Text(
            text, color = Color.White, style = MaterialTheme.typography.labelLarge, maxLines = 1,
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 60.dp),
        )
        Box(
            Modifier.align(Alignment.CenterEnd).padding(end = 6.dp).size(46.dp).clip(CircleShape).background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = SW.Ink, modifier = Modifier.size(26.dp))
        }
    }
}

// ====================================================================== chips

/** Rounded chip. Selected chips are charcoal; idle chips are white (or grey inside sheets). */
@Composable
fun PillChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: ImageVector? = null,
    idleColor: Color = SW.Surface,
    small: Boolean = false,
) {
    val bg by animateColorAsState(if (selected) SW.Ink else idleColor, label = "chipBg")
    val fg by animateColorAsState(if (selected) SW.OnInk else SW.Subtle, label = "chipFg")
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = if (small) 14.dp else 18.dp, vertical = if (small) 8.dp else 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            Icon(leading, null, tint = fg, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text, color = fg, maxLines = 1,
            style = if (small) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium,
        )
    }
}

/** Scrollable row of tab chips ("Tour schedule · Accommodation · Booking details" style). */
@Composable
fun <T> TabChips(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    idleColor: Color = SW.SurfaceAlt,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    small: Boolean = true,
) {
    Row(
        modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { o ->
            PillChip(label(o), selected = o == selected, onClick = { onSelect(o) }, idleColor = idleColor, small = small)
        }
    }
}

@Composable
fun Tag(text: String, background: Color, foreground: Color, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Row(
        modifier.clip(RoundedCornerShape(50)).background(background).padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = foreground, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(text, color = foreground, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Convenience: tinted tag using a palette colour. */
@Composable
fun TintTag(text: String, tint: Color, modifier: Modifier = Modifier, icon: ImageVector? = null) =
    Tag(text, tint.copy(alpha = 0.12f), tint, modifier, icon)

/** Frosted tag for use on top of photos and cover art. */
@Composable
fun FrostTag(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(SW.Frost)
            .border(1.dp, SW.FrostLine, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(text, color = Color.White, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Outlined "☆ 5.0" chip. [onImage] switches to white-on-frost for photos. */
@Composable
fun RatingChip(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.StarBorder,
    onImage: Boolean = false,
) {
    val fg = if (onImage) Color.White else SW.Text
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .then(if (onImage) Modifier.background(SW.Frost) else Modifier)
            .border(1.dp, if (onImage) SW.FrostLine else SW.Line, RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, color = fg, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

/** Underlined text link ("See all", "Read more", "143 reviews"). */
@Composable
fun UnderlinedLink(
    text: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    color: Color = SW.Text,
    style: TextStyle = MaterialTheme.typography.labelMedium,
) {
    Text(
        text,
        color = color,
        style = style,
        textDecoration = TextDecoration.Underline,
        maxLines = 1,
        modifier = modifier
            .then(if (onClick != null) Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onClick) else Modifier)
            .padding(vertical = 2.dp),
    )
}

// ====================================================================== layout

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null,
    horizontalPadding: Dp = 20.dp,
) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = horizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = SW.Text, modifier = Modifier.weight(1f, fill = false))
        if (action != null && onAction != null) {
            Spacer(Modifier.width(12.dp))
            UnderlinedLink(action, onAction)
        }
    }
}

/** Small grey grabber at the top of sheets. */
@Composable
fun DragHandle(modifier: Modifier = Modifier) {
    Box(modifier.size(width = 36.dp, height = 4.dp).clip(RoundedCornerShape(2.dp)).background(SW.Line))
}

/**
 * Centered title bar with round buttons on both sides (the "Iconic Brazil" header).
 * [trailing] fills the right-hand slot.
 */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
            if (onBack != null) CircleIconButton(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Back", onBack, iconSize = 24.dp)
        }
        Column(Modifier.weight(1f).padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = SW.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = SW.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Box(Modifier.sizeIn(minWidth = 44.dp, minHeight = 44.dp), contentAlignment = Alignment.Center) {
            trailing?.invoke()
        }
    }
}

/**
 * The itinerary "Day" card: thumbnail, grey overline and title, with an optional
 * chevron that expands labelled detail blocks (see [DetailBlock]).
 */
@Composable
fun DayCard(
    overline: String,
    title: String,
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
    onToggle: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    thumbnail: @Composable BoxScope.() -> Unit,
    details: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(22.dp)
    val open = expanded && details != null
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (open) SW.SurfaceAlt else SW.Surface)
            .border(1.dp, if (open) SW.SurfaceAlt else SW.Line, shape)
            .then(if (onToggle != null) Modifier.clickable(onClick = onToggle) else Modifier)
            .animateContentSize()
            .padding(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(width = 72.dp, height = 58.dp).clip(RoundedCornerShape(14.dp)), content = thumbnail)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(overline, style = MaterialTheme.typography.bodySmall, color = SW.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(title, style = MaterialTheme.typography.titleSmall, color = SW.Text, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            when {
                trailing != null -> {
                    Spacer(Modifier.width(8.dp))
                    trailing()
                }
                onToggle != null && details != null -> Icon(
                    if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown, null,
                    tint = SW.Text, modifier = Modifier.padding(horizontal = 6.dp).size(22.dp),
                )
            }
        }
        if (open) {
            Spacer(Modifier.height(14.dp))
            Column(Modifier.padding(horizontal = 6.dp).padding(bottom = 6.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                details?.invoke(this)
            }
        }
    }
}

/** "Morning / Arrive in Rio…" style label + text pair. */
@Composable
fun DetailBlock(label: String, text: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = SW.Muted)
        Spacer(Modifier.height(2.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = SW.Text)
    }
}

/**
 * The "Upcoming tours" card: rounded cover with a corner action, title, meta line,
 * a rating-style chip and a dark arrow button.
 */
@Composable
fun ListingCard(
    title: String,
    meta: String,
    chip: String,
    chipNote: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    chipIcon: ImageVector = Icons.Rounded.StarBorder,
    width: Dp = 220.dp,
    coverHeight: Dp = 140.dp,
    description: String? = null,
    coverAction: (@Composable BoxScope.() -> Unit)? = null,
    cover: @Composable BoxScope.() -> Unit,
) {
    Column(
        modifier
            .width(width)
            .clip(RoundedCornerShape(26.dp))
            .background(SW.Surface)
            .clickable(onClick = onClick)
            .padding(8.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(coverHeight).clip(RoundedCornerShape(20.dp))) {
            cover()
            if (coverAction != null) {
                Box(Modifier.align(Alignment.TopEnd).padding(10.dp)) { coverAction() }
            }
        }
        Column(Modifier.padding(start = 8.dp, end = 4.dp, top = 12.dp, bottom = 4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = SW.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(meta, style = MaterialTheme.typography.bodySmall, color = SW.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (description != null) {
                Spacer(Modifier.height(4.dp))
                Text(description, style = MaterialTheme.typography.bodySmall, color = SW.Subtle, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Icon(chipIcon, null, tint = SW.Text, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        chip, style = MaterialTheme.typography.labelMedium, color = SW.Text, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false),
                    )
                    if (chipNote != null) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            chipNote, style = MaterialTheme.typography.bodySmall, color = SW.Muted, maxLines = 1,
                            overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false),
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                ArrowCircle(size = 42.dp, contentDescription = "Open $title", onClick = onClick)
            }
        }
    }
}

/** Label / value columns separated by hairlines, on a grey rounded strip. */
@Composable
fun StatsStrip(items: List<Triple<String, String, String>>, modifier: Modifier = Modifier, color: Color = SW.SurfaceAlt) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(color)
            .padding(vertical = 14.dp),
    ) {
        items.forEachIndexed { i, (label, value, sub) ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(label, color = SW.Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                Spacer(Modifier.height(4.dp))
                Text(value, color = SW.Text, style = MaterialTheme.typography.titleLarge, maxLines = 1)
                Spacer(Modifier.height(2.dp))
                Text(sub, color = SW.Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (i < items.lastIndex) Box(Modifier.width(1.dp).height(46.dp).background(SW.Line).align(Alignment.CenterVertically))
        }
    }
}

/** Smooth area chart in charcoal. */
@Composable
fun TrendChart(values: List<Float>, modifier: Modifier = Modifier, color: Color = SW.Ink) {
    Canvas(modifier) {
        if (values.size < 2) return@Canvas
        val max = (values.maxOrNull() ?: 0f).coerceAtLeast(1f)
        val w = size.width
        val h = size.height
        val stepX = w / (values.size - 1)
        val pts = values.mapIndexed { i, v -> Offset(i * stepX, h - (v / max) * h * 0.82f - h * 0.06f) }

        repeat(3) { k ->
            val gy = h * (k + 1) / 4f
            drawLine(SW.Line, Offset(0f, gy), Offset(w, gy), strokeWidth = 1f)
        }

        val line = Path().apply {
            moveTo(pts[0].x, pts[0].y)
            for (i in 1 until pts.size) {
                val p0 = pts[i - 1]
                val p1 = pts[i]
                val cx = (p0.x + p1.x) / 2
                cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
            }
        }
        val area = Path().apply {
            addPath(line)
            lineTo(pts.last().x, h)
            lineTo(pts.first().x, h)
            close()
        }
        drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = 0.14f), Color.Transparent)))
        drawPath(line, color, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
        drawCircle(Color.White, 6.dp.toPx(), pts.last())
        drawCircle(color, 4.dp.toPx(), pts.last())
    }
}

/** Boxed selectable tile used in category grids. Selected tiles turn charcoal. */
@Composable
fun CategoryTile(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = SW.Text,
    idleColor: Color = SW.Surface,
) {
    val bg by animateColorAsState(if (selected) SW.Ink else idleColor, label = "tile")
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = if (selected) SW.OnInk else tint, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(6.dp))
        Text(
            label, color = if (selected) SW.OnInk else SW.Subtle, style = MaterialTheme.typography.labelMedium,
            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
        )
    }
}

// ====================================================================== misc

/** Round avatar with the user's initial on a warm gradient. */
@Composable
fun Avatar(name: String, modifier: Modifier = Modifier, size: Dp = 48.dp, onClick: (() -> Unit)? = null) {
    val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "S"
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(Color(0xFFF2A35E), Color(0xFFD27D4E))))
            .border(2.dp, Color.White, CircleShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(initial, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = (size.value * 0.4f).sp)
    }
}

/** SmartWaste leaf mark in a charcoal circle. */
@Composable
fun BrandMark(modifier: Modifier = Modifier, size: Dp = 84.dp, container: Color = SW.Ink, content: Color = SW.OnInk) {
    Box(modifier.size(size).clip(CircleShape).background(container), contentAlignment = Alignment.Center) {
        Image(
            painterResource(R.drawable.ic_recycle), "SmartWaste",
            colorFilter = ColorFilter.tint(content),
            modifier = Modifier.size(size * 0.42f),
        )
    }
}

/** White search pill with the charcoal filter button inside it. */
@Composable
fun SearchPill(
    placeholder: String,
    onClick: () -> Unit,
    onFilter: () -> Unit,
    modifier: Modifier = Modifier,
    filterIcon: ImageVector = Icons.Rounded.Tune,
    filterDescription: String = "Filters",
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(SW.Surface)
            .clickable(onClick = onClick)
            .padding(start = 20.dp, end = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Search, null, tint = SW.Text, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(placeholder, color = SW.Muted, style = MaterialTheme.typography.bodyLarge, maxLines = 1, modifier = Modifier.weight(1f))
        CircleIconButton(filterIcon, filterDescription, onFilter, container = SW.Ink, content = SW.OnInk, size = 46.dp, iconSize = 20.dp)
    }
}

/** Labelled input: "Email address*" above a white rounded field. */
@Composable
fun AuthField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    password: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onIme: (() -> Unit)? = null,
) {
    var visible by remember { mutableStateOf(false) }
    Column(modifier.fillMaxWidth()) {
        Text(label, color = SW.Text, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(SW.Surface)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f)) {
                if (value.isEmpty()) Text(placeholder, color = SW.Muted, style = MaterialTheme.typography.bodyMedium)
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = SW.Text),
                    cursorBrush = SolidColor(SW.Ink),
                    visualTransformation = if (password && !visible) PasswordVisualTransformation() else VisualTransformation.None,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (password) KeyboardType.Password else keyboardType,
                        imeAction = imeAction,
                    ),
                    keyboardActions = KeyboardActions(onAny = { onIme?.invoke() ?: defaultKeyboardAction(imeAction) }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (password) {
                Icon(
                    if (visible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                    if (visible) "Hide password" else "Show password",
                    tint = SW.Muted,
                    modifier = Modifier.size(20.dp).clickable { visible = !visible },
                )
            }
        }
    }
}

/** Capacity bar for smart bins. Null means the bin doesn't report capacity. */
@Composable
fun FillLevelBar(percent: Int?, modifier: Modifier = Modifier, showLabel: Boolean = true) {
    Column(modifier) {
        if (showLabel) {
            Text(
                if (percent == null) "Capacity not reported" else "$percent% full",
                style = MaterialTheme.typography.labelSmall,
                color = if (percent == null) SW.Muted else fillColor(percent),
            )
            Spacer(Modifier.height(4.dp))
        }
        ProgressBar(if (percent == null) 0f else percent / 100f, if (percent == null) SW.Line else fillColor(percent))
    }
}

@Composable
fun ProgressBar(fraction: Float, color: Color, modifier: Modifier = Modifier, height: Dp = 6.dp, track: Color = SW.Track) {
    Box(modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(height / 2)).background(track)) {
        if (fraction > 0f) {
            Box(
                Modifier.fillMaxWidth(fraction.coerceIn(0.02f, 1f)).height(height)
                    .clip(RoundedCornerShape(height / 2)).background(color)
            )
        }
    }
}

fun fillColor(percent: Int): Color = when {
    percent >= 90 -> SW.Danger
    percent >= 65 -> SW.Warn
    else -> SW.Ink
}

@Composable
fun IconBadge(icon: ImageVector, tint: Color, modifier: Modifier = Modifier, size: Dp = 44.dp, container: Color = tint.copy(alpha = 0.12f)) {
    Box(modifier.size(size).clip(RoundedCornerShape(size * 0.32f)).background(container), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
fun OrDivider(text: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f).height(1.dp).background(SW.Line))
        Text(text, color = SW.Muted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 12.dp))
        Box(Modifier.weight(1f).height(1.dp).background(SW.Line))
    }
}
