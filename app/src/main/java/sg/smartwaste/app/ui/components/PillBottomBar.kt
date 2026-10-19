package sg.smartwaste.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.remember
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

/** [accent] marks the raised centre action (Scan). */
data class BarItem(val route: String, val label: String, val icon: ImageVector, val accent: Boolean = false)

/**
 * Bottom bar from the second reference: flat labelled tabs with a raised circular
 * centre button, styled in the shared dark/lime palette.
 */
@Composable
fun PillBottomBar(
    items: List<BarItem>,
    current: String?,
    onSelect: (BarItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .padding(top = 22.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(SW.Card)
                .border(1.dp, SW.Line, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .navigationBarsPadding()
                .height(68.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                if (item.accent) {
                    Spacer(Modifier.size(72.dp)) // room for the raised button
                } else {
                    val selected = item.route == current
                    val tint by animateColorAsState(if (selected) SW.Lime else SW.Muted, label = "tab")
                    Column(
                        Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelect(item) }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            Modifier.size(width = 44.dp, height = 28.dp).clip(RoundedCornerShape(14.dp))
                                .background(if (selected) SW.LimeSoft else Color.Transparent),
                            contentAlignment = Alignment.Center,
                        ) { Icon(item.icon, item.label, tint = tint, modifier = Modifier.size(22.dp)) }
                        Spacer(Modifier.height(3.dp))
                        Text(
                            item.label, color = tint, fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        )
                    }
                }
            }
        }
        items.firstOrNull { it.accent }?.let { scan ->
            val selected = scan.route == current
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .size(66.dp)
                    .shadow(16.dp, CircleShape, spotColor = SW.Lime, ambientColor = SW.Lime)
                    .clip(CircleShape)
                    .background(SW.Bg)
                    .padding(5.dp)
                    .clip(CircleShape)
                    .background(if (selected) SW.Text else SW.Lime)
                    .clickable { onSelect(scan) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(scan.icon, scan.label, tint = SW.OnLime, modifier = Modifier.size(28.dp))
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
                .size(50.dp)
                .align(Alignment.Center)
                .shadow(12.dp, CircleShape, spotColor = SW.Lime)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(SW.Card2, SW.Olive)))
                .border(1.dp, SW.Lime.copy(alpha = 0.5f), CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Forum, contentDescription = "AI support chat", tint = SW.Lime, modifier = Modifier.size(22.dp))
        }
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .clip(RoundedCornerShape(50))
                .background(SW.Lime)
                .padding(horizontal = 5.dp, vertical = 1.dp),
        ) {
            Text("AI", color = SW.OnLime, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}
