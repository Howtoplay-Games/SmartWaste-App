package sg.smartwaste.app.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import sg.smartwaste.app.R
import sg.smartwaste.app.ui.components.PrimaryButton
import sg.smartwaste.app.ui.components.appContainer
import sg.smartwaste.app.ui.theme.SW

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val prefs = appContainer().prefs
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SW.Ink, SW.Navy)))
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Box(
            Modifier.size(64.dp).clip(RoundedCornerShape(22.dp)).background(SW.Emerald),
            contentAlignment = Alignment.Center,
        ) {
            Image(painterResource(R.drawable.ic_recycle), null, colorFilter = ColorFilter.tint(Color.White), modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(28.dp))
        Text("SmartWaste", color = SW.Lime, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            "Smart recycling for a sustainable future",
            color = Color.White, fontSize = 36.sp, lineHeight = 42.sp, fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Singapore generates over 60,000 tonnes of e-waste every year. Let's make every item count.",
            color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(28.dp))
        Feature(Icons.Rounded.CameraAlt, "AI e-waste scanner", "Identify items, hazards & resale value from one photo", SW.Electric)
        Feature(Icons.Rounded.Map, "Live recycling map", "Nearest non-full bins & buyers with turn-by-turn routes", SW.Emerald)
        Feature(Icons.Rounded.EmojiEvents, "Verified rewards", "Earn points for verified deposits and redeem vouchers", SW.Amber)
        Spacer(Modifier.height(28.dp))
        Text("What should we call you?", color = Color.White, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color.White.copy(alpha = 0.08f)).padding(horizontal = 18.dp, vertical = 18.dp),
        ) {
            if (name.isEmpty()) Text("Your first name", color = Color.White.copy(alpha = 0.4f), style = MaterialTheme.typography.bodyLarge)
            BasicTextField(
                value = name, onValueChange = { name = it.take(24) }, singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                cursorBrush = SolidColor(SW.Lime), modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(24.dp))
        PrimaryButton(
            "Get started", {
                scope.launch {
                    prefs.completeOnboarding(name.ifBlank { "Recycler" })
                    onDone()
                }
            },
            Modifier.fillMaxWidth(), container = SW.Lime, contentColor = SW.Ink,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "We'll ask for camera and location access when you use those features.",
            color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun Feature(icon: ImageVector, title: String, subtitle: String, tint: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Start) {
        Box(Modifier.size(46.dp).clip(CircleShape).background(tint.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, color = Color.White, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.bodySmall)
        }
    }
}
