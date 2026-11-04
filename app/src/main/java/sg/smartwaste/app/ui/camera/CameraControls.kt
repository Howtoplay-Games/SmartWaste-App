package sg.smartwaste.app.ui.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cameraswitch
import androidx.compose.material.icons.outlined.NoPhotography
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import sg.smartwaste.app.ui.theme.SW

/** Shown over the preview when the device has no camera CameraX can open. */
@Composable
fun NoCameraMessage(text: String, modifier: Modifier = Modifier, action: @Composable () -> Unit = {}) {
    Box(modifier.background(SW.Ink), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Icon(Icons.Outlined.NoPhotography, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(10.dp))
            Text("No camera found", color = Color.White, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Text(text, color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            action()
        }
    }
}

/** Front/back switch, only worth showing when [CameraHandle.canSwitch]. */
@Composable
fun SwitchCameraButton(handle: CameraHandle, modifier: Modifier = Modifier, size: Dp = 52.dp) {
    Box(
        modifier.size(size).clip(CircleShape).background(SW.Frost).border(1.dp, SW.FrostLine, CircleShape)
            .clickable { handle.switchCamera() },
        contentAlignment = Alignment.Center,
    ) { Icon(Icons.Outlined.Cameraswitch, "Switch camera", tint = Color.White, modifier = Modifier.size(22.dp)) }
}
