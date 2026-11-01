package sg.smartwaste.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sg.smartwaste.app.ui.theme.SW

data class BarItem(val route: String, val label: String, val icon: ImageVector)

/**
 * Bottom padding for scrolling tab screens. Above the navigation-bar inset, the bar takes
 * 92 dp (68 dp pill + 2 × 12 dp margin) and the AI button reaches 160 dp, so the last item
 * has to clear the inset plus 170 dp.
 */
@Composable
fun bottomContentPadding(): Dp =
    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 170.dp

/**
 * Floating charcoal pill with icon-only tabs. The selected tab sits in a white
 * circle (the reference bottom bar).
 */
@Composable
fun PillBottomBar(
    items: List<BarItem>,
    current: String?,
    onSelect: (BarItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 12.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(68.dp)
                .shadow(20.dp, RoundedCornerShape(34.dp), ambientColor = Color.Black.copy(alpha = 0.25f), spotColor = Color.Black.copy(alpha = 0.25f))
                .clip(RoundedCornerShape(34.dp))
                .background(SW.Ink)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                val selected = item.route == current
                val bg by animateColorAsState(if (selected) SW.Surface else Color.Transparent, label = "tabBg")
                val fg by animateColorAsState(if (selected) SW.Ink else SW.OnInk, label = "tabFg")
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(bg)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelect(item) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(item.icon, item.label, tint = fg, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

/** Small floating AI support button for the bottom-right corner. */
@Composable
fun AiSupportFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.size(58.dp)) {
        Box(
            Modifier
                .size(52.dp)
                .align(Alignment.Center)
                .shadow(14.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.2f), spotColor = Color.Black.copy(alpha = 0.2f))
                .clip(CircleShape)
                .background(SW.Surface)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Forum, contentDescription = "AI support chat", tint = SW.Ink, modifier = Modifier.size(22.dp))
        }
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .clip(RoundedCornerShape(50))
                .background(SW.Ink)
                .padding(horizontal = 6.dp, vertical = 1.dp),
        ) {
            Text("AI", color = SW.OnInk, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}
