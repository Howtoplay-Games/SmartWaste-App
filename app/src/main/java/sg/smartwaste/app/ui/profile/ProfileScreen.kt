package sg.smartwaste.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import sg.smartwaste.app.BuildConfig
import sg.smartwaste.app.data.Impact
import sg.smartwaste.app.data.db.DepositEntity
import sg.smartwaste.app.data.model.EWasteCategory
import sg.smartwaste.app.data.rewards.RewardsCatalog
import sg.smartwaste.app.ui.components.Avatar
import sg.smartwaste.app.ui.components.IconBadge
import sg.smartwaste.app.ui.components.SectionHeader
import sg.smartwaste.app.ui.components.StatTile
import sg.smartwaste.app.ui.components.Tag
import sg.smartwaste.app.ui.components.appContainer
import sg.smartwaste.app.ui.components.icon
import sg.smartwaste.app.ui.components.tint
import sg.smartwaste.app.ui.theme.SW
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfileScreen() {
    val c = appContainer()
    val scope = rememberCoroutineScope()
    val name by c.prefs.userName.collectAsStateWithLifecycle(initialValue = "")
    val apiKey by c.prefs.apiKeyOverride.collectAsStateWithLifecycle(initialValue = "")
    val impact by c.history.impact.collectAsStateWithLifecycle(initialValue = Impact())
    val lifetime by c.history.lifetimePoints.collectAsStateWithLifecycle(initialValue = 0)
    val deposits by c.history.deposits.collectAsStateWithLifecycle(initialValue = emptyList())

    var editName by remember { mutableStateOf(false) }
    var editKey by remember { mutableStateOf(false) }
    var showTransparency by remember { mutableStateOf(false) }
    var showAllHistory by remember { mutableStateOf(false) }
    val tier = RewardsCatalog.tierFor(lifetime)

    LazyColumn(Modifier.fillMaxSize().background(SW.Bg), contentPadding = PaddingValues(bottom = 150.dp)) {
        item {
            Column(
                Modifier.fillMaxWidth().statusBarsPadding().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Avatar(name, size = 84.dp)
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name.ifBlank { "Recycler" }, style = MaterialTheme.typography.headlineSmall, color = SW.Ink)
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Rounded.Edit, "Edit name", tint = SW.Muted, modifier = Modifier.size(18.dp).clickable { editName = true })
                }
                Spacer(Modifier.height(6.dp))
                Tag("${tier.name} · $lifetime lifetime pts", SW.Ink, Color.White)
            }
        }
        item {
            SectionHeader("Recycling impact")
            Spacer(Modifier.height(12.dp))
            Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(Icons.Rounded.Devices, "${impact.items}", "Items", Modifier.weight(1f), tint = SW.Electric)
                StatTile(Icons.Rounded.Eco, String.format(Locale.US, "%.1f kg", impact.kg), "Diverted", Modifier.weight(1f))
                StatTile(Icons.Rounded.Cloud, String.format(Locale.US, "%.1f kg", impact.co2Kg), "CO₂e saved", Modifier.weight(1f), tint = SW.Amber)
            }
            Spacer(Modifier.height(10.dp))
            CategoryBreakdown(impact)
        }
        item {
            Spacer(Modifier.height(22.dp))
            SectionHeader(
                "Deposit history",
                action = if (deposits.size > 4) (if (showAllHistory) "Less" else "See all") else null,
                onAction = { showAllHistory = !showAllHistory },
            )
            Spacer(Modifier.height(10.dp))
            if (deposits.isEmpty()) {
                Text(
                    "No deposits yet. Scan an item and drop it in a bin to start earning.",
                    color = SW.Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        }
        items(if (showAllHistory) deposits else deposits.take(4), key = { it.id }) { d -> DepositRow(d) }
        item {
            Spacer(Modifier.height(22.dp))
            SectionHeader("Settings")
            Spacer(Modifier.height(10.dp))
            SettingRow(
                Icons.Rounded.VpnKey, "AI API key",
                when {
                    apiKey.isNotBlank() -> "Custom key set (…${apiKey.takeLast(4)})"
                    BuildConfig.OPENROUTER_API_KEY.isNotBlank() -> "Using build key from local.properties"
                    else -> "Not set. Using on-device backup AI only"
                },
            ) { editKey = true }
            SettingRow(Icons.Rounded.Security, "Data & transparency", "What we collect and how points are calculated", trailing = if (showTransparency) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown) {
                showTransparency = !showTransparency
            }
            if (showTransparency) TransparencyCard()
            SettingRow(Icons.Rounded.Info, "About SmartWaste", "v${BuildConfig.VERSION_NAME} · Every recycled item becomes a data point for a cleaner Singapore.") {}
        }
    }

    if (editName) {
        TextDialog("Your name", name, onDismiss = { editName = false }) { v ->
            scope.launch { c.prefs.setName(v) }; editName = false
        }
    }
    if (editKey) {
        TextDialog(
            "OpenRouter API key", apiKey, secret = true,
            hint = "Overrides the key from local.properties. Leave empty to use the build key.",
            onDismiss = { editKey = false },
        ) { v -> scope.launch { c.prefs.setApiKey(v) }; editKey = false }
    }
}

@Composable
private fun CategoryBreakdown(impact: Impact) {
    val max = (impact.byCategory.values.maxOrNull() ?: 0).coerceAtLeast(1)
    Column(
        Modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color.White).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("By category", style = MaterialTheme.typography.titleSmall, color = SW.Ink)
        EWasteCategory.entries.forEach { cat ->
            val count = impact.byCategory[cat] ?: 0
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(cat.icon, null, tint = cat.tint, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(cat.shortTitle, style = MaterialTheme.typography.labelMedium, color = SW.Subtle, modifier = Modifier.width(110.dp))
                Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(SW.Field)) {
                    if (count > 0) {
                        Box(Modifier.fillMaxWidth(count.toFloat() / max).height(8.dp).clip(RoundedCornerShape(4.dp)).background(cat.tint))
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text("$count", style = MaterialTheme.typography.labelMedium, color = SW.Ink, modifier = Modifier.width(28.dp))
            }
        }
    }
}

@Composable
private fun DepositRow(d: DepositEntity) {
    val cat = EWasteCategory.fromKey(d.category)
    val verified = d.verification == "ONLINE_VERIFIED" || d.verification == "CHECKSUM_VALID" || d.verification == "PLAUSIBLE"
    Row(
        Modifier.padding(horizontal = 20.dp, vertical = 5.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color.White).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(cat?.icon ?: Icons.Rounded.Devices, cat?.tint ?: SW.Emerald, size = 42.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("${d.itemName}${if (d.quantity > 1) " × ${d.quantity}" else ""}", style = MaterialTheme.typography.titleSmall, color = SW.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${d.placeName} · ${SimpleDateFormat("d MMM, h:mm a", Locale.getDefault()).format(Date(d.timestamp))}",
                style = MaterialTheme.typography.labelSmall, color = SW.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (verified) Tag("Verified", SW.Mint, SW.EmeraldDeep, icon = Icons.Rounded.VerifiedUser) else Tag("Unverified", SW.Field, SW.Subtle)
                if (d.binProof == "QR") Tag("Smart bin", SW.Sky, SW.Electric)
            }
        }
        Text("+${d.points}", style = MaterialTheme.typography.titleMedium, color = SW.EmeraldDeep)
    }
}

@Composable
private fun SettingRow(icon: ImageVector, title: String, subtitle: String, trailing: ImageVector? = null, onClick: () -> Unit) {
    Row(
        Modifier.padding(horizontal = 20.dp, vertical = 5.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color.White)
            .clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon, SW.Ink, size = 40.dp, container = SW.Field)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = SW.Ink)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = SW.Muted)
        }
        trailing?.let { Icon(it, null, tint = SW.Muted) }
    }
}

@Composable
private fun TransparencyCard() {
    Column(
        Modifier.padding(horizontal = 20.dp, vertical = 5.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(SW.Ink).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val white = Color.White.copy(alpha = 0.85f)
        Text("Data we keep (on this device)", style = MaterialTheme.typography.titleSmall, color = SW.Lime)
        Text("• Bin ID, timestamp, item category, quantity, points\n• Your proof photo and a one-way hash of scanned codes (to stop re-use)\n• No account, email or phone number is required", color = white, style = MaterialTheme.typography.bodySmall)
        Text("Sent to third parties", style = MaterialTheme.typography.titleSmall, color = SW.Lime)
        Text("• Photos are sent to the AI provider (OpenRouter) only for identification and proof checks\n• Barcodes go to UPCitemdb / Open Products Facts for product lookup\n• Your location is used for nearby places and routing (OpenStreetMap services)", color = white, style = MaterialTheme.typography.bodySmall)
        Text("How points work", style = MaterialTheme.typography.titleSmall, color = SW.Lime)
        Text(
            "Base points per category × quantity\n× 1.2 if you scan a SmartWaste bin QR\n× 0.6 if AI can't confirm the bin photo\n+ verification bonus: online-verified barcode +100%, IMEI/barcode checksum +80%, plausible serial +50%, none 0%\nCodes already used earn no bonus · max 10 deposits per day",
            color = white, style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun TextDialog(
    title: String,
    initial: String,
    secret: Boolean = false,
    hint: String? = null,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(28.dp),
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = value, onValueChange = { value = it }, singleLine = true,
                    visualTransformation = if (secret) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
                    shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(),
                )
                hint?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, style = MaterialTheme.typography.labelSmall, color = SW.Muted)
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(value) }) { Text("Save", color = SW.EmeraldDeep) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = SW.Subtle) } },
    )
}
