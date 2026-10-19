package sg.smartwaste.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sg.smartwaste.app.ui.theme.SW

@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    container: Color = Color.White,
    content: Color = SW.Ink,
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

/** Rounded chip like the "Asia / Europe / South America" selector in the reference design. */
@Composable
fun PillChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: ImageVector? = null,
    selectedColor: Color = SW.Ink,
    dark: Boolean = false,
) {
    val bg by animateColorAsState(
        when {
            selected -> selectedColor
            dark -> Color.White.copy(alpha = 0.08f)
            else -> Color.White
        }, label = "chipBg"
    )
    val fg = when {
        selected -> Color.White
        dark -> Color.White.copy(alpha = 0.85f)
        else -> SW.Ink
    }
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .then(if (!selected && !dark) Modifier.border(1.dp, SW.Line, RoundedCornerShape(50)) else Modifier)
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
        Text(title, style = MaterialTheme.typography.titleLarge, color = SW.Ink)
        if (action != null && onAction != null) {
            Text(
                action,
                style = MaterialTheme.typography.labelMedium,
                color = SW.Subtle,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onAction).padding(6.dp),
            )
        }
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
                .height(56.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White)
                .clickable(onClick = onClick)
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Search, null, tint = SW.Ink, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text(placeholder, color = SW.Muted, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
        }
        Spacer(Modifier.width(10.dp))
        CircleIconButton(Icons.Rounded.Tune, "Filters", onFilter, container = SW.Ink, content = Color.White, size = 56.dp, iconSize = 22.dp)
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    container: Color = SW.Ink,
    contentColor: Color = Color.White,
    leading: ImageVector? = null,
    height: Dp = 58.dp,
) {
    val bg = if (enabled) container else container.copy(alpha = 0.35f)
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
            CircularProgressIndicator(color = contentColor, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
        } else if (leading != null) {
            Icon(leading, null, tint = contentColor, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(text, color = contentColor, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = 58.dp,
) {
    Row(
        modifier
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(Color.White)
            .border(1.dp, SW.Line, RoundedCornerShape(height / 2))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (leading != null) {
            Icon(leading, null, tint = SW.Ink, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = if (enabled) SW.Ink else SW.Muted, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    color: Color = SW.Card,
    corner: Dp = 24.dp,
    onClick: (() -> Unit)? = null,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(corner))
            .background(color)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

@Composable
fun ArrowCircle(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    container: Color = SW.Ink,
    content: Color = Color.White,
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

@Composable
fun Avatar(name: String, modifier: Modifier = Modifier, size: Dp = 46.dp, onClick: (() -> Unit)? = null) {
    val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "S"
    Box(
        modifier
            .size(size)
            .shadow(6.dp, CircleShape)
            .clip(CircleShape)
            .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(SW.Lime, SW.Emerald)))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(initial, color = SW.Ink, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.42f).sp)
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

/** Capacity bar for smart bins (green → amber → red). Null means the bin doesn't report capacity. */
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
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(SW.Field)) {
            if (percent != null) {
                Box(
                    Modifier
                        .fillMaxWidth(percent.coerceIn(2, 100) / 100f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(fillColor(percent))
                )
            }
        }
    }
}

fun fillColor(percent: Int): Color = when {
    percent >= 90 -> SW.Coral
    percent >= 65 -> SW.Amber
    else -> SW.Emerald
}

@Composable
fun StatTile(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    tint: Color = SW.Emerald,
    container: Color = Color.White,
    valueColor: Color = SW.Ink,
    labelColor: Color = SW.Muted,
) {
    Column(
        modifier.clip(RoundedCornerShape(22.dp)).background(container).padding(14.dp),
    ) {
        Box(
            Modifier.size(34.dp).clip(CircleShape).background(tint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp)) }
        Spacer(Modifier.height(10.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, color = valueColor, maxLines = 1)
        Text(label, style = MaterialTheme.typography.labelSmall, color = labelColor, maxLines = 1)
    }
}

@Composable
fun IconBadge(icon: ImageVector, tint: Color, modifier: Modifier = Modifier, size: Dp = 44.dp, container: Color = tint.copy(alpha = 0.12f)) {
    Box(modifier.size(size).clip(RoundedCornerShape(size * 0.36f)).background(container), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}
