package sg.smartwaste.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sg.smartwaste.app.ui.theme.SW

data class BarItem(val route: String, val label: String, val icon: ImageVector, val accent: Boolean = false)

/** Floating dark pill navigation bar with a white active bubble, as in the reference designs. */
@Composable
fun PillBottomBar(
    items: List<BarItem>,
    current: String?,
    onSelect: (BarItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .navigationBarsPadding()
            .padding(horizontal = 22.dp, vertical = 12.dp)
            .fillMaxWidth()
            .height(70.dp)
            .shadow(20.dp, RoundedCornerShape(35.dp), ambientColor = Color.Black.copy(alpha = 0.3f), spotColor = Color.Black.copy(alpha = 0.3f))
            .clip(RoundedCornerShape(35.dp))
            .background(SW.Ink)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { item ->
            val selected = item.route == current
            val bg by animateColorAsState(
                when {
                    selected -> Color.White
                    item.accent -> SW.Emerald
                    else -> Color.Transparent
                }, label = "barBg"
            )
            val size by animateDpAsState(if (selected || item.accent) 54.dp else 48.dp, label = "barSize")
            Box(
                Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(bg)
                    .clickable { onSelect(item) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    item.icon,
                    contentDescription = item.label,
                    tint = if (selected) SW.Ink else Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

/** Small floating AI support button for the bottom-right corner. */
@Composable
fun AiSupportFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.size(56.dp)) {
        Box(
            Modifier
                .size(52.dp)
                .align(Alignment.Center)
                .shadow(14.dp, CircleShape, spotColor = SW.Emerald)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(SW.Emerald, SW.Electric)))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Forum, contentDescription = "AI support chat", tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .clip(RoundedCornerShape(50))
                .background(SW.Lime)
                .padding(horizontal = 5.dp, vertical = 1.dp),
        ) {
            Text("AI", color = SW.Ink, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}
