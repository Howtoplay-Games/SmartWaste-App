package sg.smartwaste.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Search
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sg.smartwaste.app.R
import sg.smartwaste.app.ui.theme.SW

// ====================================================================== buttons

@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    container: Color = SW.Card2,
    content: Color = SW.Text,
    size: Dp = 44.dp,
    iconSize: Dp = 20.dp,
    bordered: Boolean = true,
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

/** Lime pill button (the "Sign in" / "Send Code" style). */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    container: Color = SW.Lime,
    contentColor: Color = SW.OnLime,
    leading: ImageVector? = null,
    height: Dp = 54.dp,
) {
    val bg = if (enabled) container else container.copy(alpha = 0.3f)
    Row(
        modifier
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(bg)
            .clickable(enabled = enabled && !loading, onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(color = contentColor, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
        } else if (leading != null) {
            Icon(leading, null, tint = contentColor, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = contentColor, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

/** Dark outlined pill button (the "Google" / "Apple" style). */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = 54.dp,
    leadingPainter: Int? = null,
) {
    Row(
        modifier
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(SW.Card)
            .border(1.dp, SW.Line, RoundedCornerShape(height / 2))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (leadingPainter != null) {
            Image(painterResource(leadingPainter), null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        } else if (leading != null) {
            Icon(leading, null, tint = SW.Text, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = if (enabled) SW.Text else SW.Muted, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

@Composable
fun ArrowCircle(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    container: Color = SW.Lime,
    content: Color = SW.OnLime,
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
        Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = content, modifier = Modifier.size(size * 0.45f))
    }
}

// ====================================================================== chips

@Composable
fun PillChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: ImageVector? = null,
    selectedColor: Color = SW.Lime,
) {
    val bg by animateColorAsState(if (selected) selectedColor else SW.Card, label = "chipBg")
    val fg = if (selected) SW.OnLime else SW.Text
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .then(if (!selected) Modifier.border(1.dp, SW.Line, RoundedCornerShape(50)) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            Icon(leading, null, tint = fg, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, color = fg, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

/** Month / Quarter / Year style segmented control. */
@Composable
fun <T> SegmentedControl(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(SW.Card).border(1.dp, SW.Line, RoundedCornerShape(50)).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { o ->
            val sel = o == selected
            val bg by animateColorAsState(if (sel) SW.Lime else Color.Transparent, label = "seg")
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(50)).background(bg).clickable { onSelect(o) }.padding(vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label(o), color = if (sel) SW.OnLime else SW.Muted, style = MaterialTheme.typography.labelLarge, maxLines = 1)
            }
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
    Tag(text, tint.copy(alpha = 0.14f), tint, modifier, icon)

// ====================================================================== layout

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = SW.Text)
        if (action != null && onAction != null) {
            Text(
                action,
                style = MaterialTheme.typography.labelMedium,
                color = SW.Lime,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onAction).padding(6.dp),
            )
        }
    }
}

@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    color: Color = SW.Card,
    corner: Dp = 22.dp,
    onClick: (() -> Unit)? = null,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(corner))
            .background(color)
            .border(1.dp, SW.Line, RoundedCornerShape(corner))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

/**
 * Hero header used at the top of every main screen: olive glow gradient with a faint dot
 * texture, ending in the rounded top edge of the content sheet (second reference layout).
 */
@Composable
fun HeroHeader(
    modifier: Modifier = Modifier,
    sheetEdge: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier.fillMaxWidth().background(SW.heroBrush)) {
        DotTexture(Modifier.matchParentSize())
        Column(Modifier.fillMaxWidth().padding(bottom = if (sheetEdge) 40.dp else 16.dp), content = content)
        if (sheetEdge) {
            Box(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(26.dp)
                    .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)).background(SW.Sheet)
            )
        }
    }
}

@Composable
private fun DotTexture(modifier: Modifier) {
    Canvas(modifier) {
        val step = 16.dp.toPx()
        val r = 1.dp.toPx()
        var y = step / 2
        while (y < size.height * 0.7f) {
            var x = step / 2
            val alpha = 0.10f * (1f - y / (size.height * 0.7f))
            while (x < size.width) {
                drawCircle(SW.Lime.copy(alpha = alpha), r, Offset(x, y))
                x += step
            }
            y += step
        }
    }
}

/** The "Daily Average · Transactions · Largest Expense" strip from the second reference. */
@Composable
fun StatsStrip(items: List<Triple<String, String, String>>, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(22.dp))
            .padding(vertical = 14.dp),
    ) {
        items.forEachIndexed { i, (label, value, sub) ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(label, color = SW.Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                Spacer(Modifier.height(4.dp))
                Text(value, color = SW.Text, style = MaterialTheme.typography.titleLarge, maxLines = 1)
                Spacer(Modifier.height(2.dp))
                Text(sub, color = SW.Lime, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (i < items.lastIndex) Box(Modifier.width(1.dp).height(46.dp).background(Color.White.copy(alpha = 0.08f)).align(Alignment.CenterVertically))
        }
    }
}

/** Smooth area chart (spending-trend style) in lime. */
@Composable
fun TrendChart(values: List<Float>, modifier: Modifier = Modifier, color: Color = SW.Lime) {
    Canvas(modifier) {
        if (values.size < 2) return@Canvas
        val max = (values.maxOrNull() ?: 0f).coerceAtLeast(1f)
        val w = size.width
        val h = size.height
        val stepX = w / (values.size - 1)
        val pts = values.mapIndexed { i, v -> Offset(i * stepX, h - (v / max) * h * 0.82f - h * 0.06f) }

        // Faint guide lines
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
        drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = 0.32f), Color.Transparent)))
        drawPath(line, color, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
        drawCircle(SW.Bg, 6.dp.toPx(), pts.last())
        drawCircle(color, 4.dp.toPx(), pts.last())
    }
}

/** Boxed selectable tile used in category grids ("Food / Transport / Shopping" style). */
@Composable
fun CategoryTile(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = SW.Lime,
) {
    val borderColor = if (selected) tint else SW.Line
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) tint.copy(alpha = 0.14f) else SW.Card)
            .border(if (selected) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = if (selected) tint else SW.Subtle, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(6.dp))
        Text(
            label, color = if (selected) tint else SW.Subtle, style = MaterialTheme.typography.labelMedium,
            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
        )
    }
}

// ====================================================================== misc

@Composable
fun Avatar(name: String, modifier: Modifier = Modifier, size: Dp = 44.dp, onClick: (() -> Unit)? = null) {
    val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "S"
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(SW.Lime, SW.LimeDeep)))
            .border(2.dp, SW.Bg, CircleShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(initial, color = SW.OnLime, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.42f).sp)
    }
}

/** Glowing sphere logo from the login reference, with optional concentric rings. */
@Composable
fun GlowOrb(modifier: Modifier = Modifier, orbSize: Dp = 96.dp, rings: Boolean = false) {
    Box(modifier.size(if (rings) orbSize * 1.9f else orbSize), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = center
            val r = orbSize.toPx() / 2
            if (rings) {
                listOf(1.3f to 0.10f, 1.6f to 0.07f, 1.88f to 0.04f).forEach { (k, a) ->
                    drawCircle(SW.Lime.copy(alpha = a), r * k, c, style = Stroke(1.dp.toPx()))
                }
            }
            drawCircle(Brush.radialGradient(listOf(SW.Lime.copy(alpha = 0.35f), Color.Transparent), c, r * 1.5f), r * 1.5f, c)
            drawCircle(
                Brush.radialGradient(
                    listOf(Color(0xFFE9FF8A), SW.LimeDeep, Color(0xFF3F5212), Color(0xFF131806)),
                    Offset(c.x - r * 0.35f, c.y - r * 0.4f), r * 1.5f,
                ),
                r, c,
            )
            drawCircle(SW.Lime.copy(alpha = 0.55f), r, c, style = Stroke(1.2.dp.toPx()))
        }
        Image(
            painterResource(R.drawable.ic_recycle), null,
            colorFilter = ColorFilter.tint(SW.OnLime.copy(alpha = 0.55f)),
            modifier = Modifier.size(orbSize * 0.36f),
        )
    }
}

@Composable
fun SearchPill(
    placeholder: String,
    onClick: () -> Unit,
    onFilter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier
                .weight(1f)
                .height(52.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(SW.Card)
                .border(1.dp, SW.Line, RoundedCornerShape(26.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Search, null, tint = SW.Subtle, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(placeholder, color = SW.Muted, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
        }
        Spacer(Modifier.width(10.dp))
        CircleIconButton(Icons.Rounded.Tune, "Filters", onFilter, container = SW.Lime, content = SW.OnLime, size = 52.dp, iconSize = 22.dp, bordered = false)
    }
}

/** Labelled input in the login reference style: "Email address*" above a dark rounded field. */
@Composable
fun AuthField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    password: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: androidx.compose.ui.text.input.ImeAction = androidx.compose.ui.text.input.ImeAction.Next,
    onIme: (() -> Unit)? = null,
) {
    var visible by remember { mutableStateOf(false) }
    Column(modifier.fillMaxWidth()) {
        Text(label, color = SW.Text, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(SW.Field)
                .border(1.dp, SW.Line, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f)) {
                if (value.isEmpty()) Text(placeholder, color = SW.Muted, style = MaterialTheme.typography.bodyMedium)
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = SW.Text),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(SW.Lime),
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

/** Capacity bar for smart bins (lime → amber → red). Null means the bin doesn't report capacity. */
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
fun ProgressBar(fraction: Float, color: Color, modifier: Modifier = Modifier, height: Dp = 6.dp) {
    Box(modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(height / 2)).background(SW.Card2)) {
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
    else -> SW.Lime
}

@Composable
fun StatTile(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    tint: Color = SW.Lime,
) {
    Column(
        modifier.clip(RoundedCornerShape(20.dp)).background(SW.Card).border(1.dp, SW.Line, RoundedCornerShape(20.dp)).padding(14.dp),
    ) {
        Box(Modifier.size(34.dp).clip(CircleShape).background(tint.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, color = SW.Text, maxLines = 1)
        Text(label, style = MaterialTheme.typography.labelSmall, color = SW.Muted, maxLines = 1)
    }
}

@Composable
fun IconBadge(icon: ImageVector, tint: Color, modifier: Modifier = Modifier, size: Dp = 44.dp, container: Color = tint.copy(alpha = 0.14f)) {
    Box(modifier.size(size).clip(RoundedCornerShape(size * 0.34f)).background(container), contentAlignment = Alignment.Center) {
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
