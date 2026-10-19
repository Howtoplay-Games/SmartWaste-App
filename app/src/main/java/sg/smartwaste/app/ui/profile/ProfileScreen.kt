package sg.smartwaste.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEvents
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
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import sg.smartwaste.app.BuildConfig
import sg.smartwaste.app.data.auth.SessionState
import sg.smartwaste.app.data.db.DepositEntity
import sg.smartwaste.app.data.model.EWasteCategory
import sg.smartwaste.app.data.rewards.RewardsCatalog
import sg.smartwaste.app.ui.components.Avatar
import sg.smartwaste.app.ui.components.IconBadge
import sg.smartwaste.app.ui.components.ProgressBar
import sg.smartwaste.app.ui.components.SectionHeader
import sg.smartwaste.app.ui.components.SegmentedControl
import sg.smartwaste.app.ui.components.TintTag
import sg.smartwaste.app.ui.components.TrendChart
import sg.smartwaste.app.ui.components.appContainer
import sg.smartwaste.app.ui.components.icon
import sg.smartwaste.app.ui.components.tint
import sg.smartwaste.app.ui.theme.SW
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private enum class Period(val label: String, val days: Int) { MONTH("Month", 30), QUARTER("Quarter", 90), YEAR("Year", 365) }

@Composable
fun ProfileScreen() {
    val c = appContainer()
    val scope = rememberCoroutineScope()
    val session by c.auth.session.collectAsStateWithLifecycle(initialValue = SessionState.Loading)
    val user = (session as? SessionState.SignedIn)?.user
    val apiKey by c.prefs.apiKeyOverride.collectAsStateWithLifecycle(initialValue = "")
    val lifetime by c.history.lifetimePoints.collectAsStateWithLifecycle(initialValue = 0)
    val deposits by c.history.deposits.collectAsStateWithLifecycle(initialValue = emptyList())

    var period by remember { mutableStateOf(Period.MONTH) }
    var editName by remember { mutableStateOf(false) }
    var editKey by remember { mutableStateOf(false) }
    var showTransparency by remember { mutableStateOf(false) }
    var showAllHistory by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }

    val now = System.currentTimeMillis()
    val dayMs = 86_400_000L
    val inPeriod = deposits.filter { it.timestamp >= now - period.days * dayMs }
    val previous = deposits.filter { it.timestamp in (now - 2 * period.days * dayMs) until (now - period.days * dayMs) }
    val kg = inPeriod.sumOf { it.kgEstimate }
    val prevKg = previous.sumOf { it.kgEstimate }
    val points = inPeriod.sumOf { it.points }
    val buckets = bucketize(inPeriod, period)
    val byCat = EWasteCategory.entries.associateWith { cat -> inPeriod.filter { it.category == cat.name }.sumOf { it.quantity } }
    val maxCat = (byCat.values.maxOrNull() ?: 0).coerceAtLeast(1)
    val tier = RewardsCatalog.tierFor(lifetime)
    val next = RewardsCatalog.nextTier(lifetime)

    LazyColumn(Modifier.fillMaxSize().background(SW.Bg), contentPadding = PaddingValues(bottom = 130.dp)) {
        // ------------------------------------------------------------ header
        item {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Impact", color = SW.Text, style = MaterialTheme.typography.headlineMedium)
                    Text("Recycling patterns & goals", color = SW.Muted, style = MaterialTheme.typography.labelMedium)
                }
                Avatar(user?.name.orEmpty(), size = 46.dp, onClick = { editName = true })
            }
            SegmentedControl(Period.entries, period, { it.label }, { period = it }, Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(14.dp))
        }

        // ---------------------------------------------------- headline chart
        item {
            Column(
                Modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(SW.Card)
                    .border(1.dp, SW.Line, RoundedCornerShape(24.dp)).padding(18.dp),
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(String.format(Locale.US, "%.1f kg", kg), color = SW.Text, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    val delta = kg - prevKg
                    TintTag(
                        String.format(Locale.US, "%+.1f kg vs last %s", delta, period.label.lowercase()),
                        if (delta >= 0) SW.Lime else SW.Danger,
                    )
                }
                Text("E-waste diverted · $points pts earned", color = SW.Muted, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(14.dp))
                val data = if (buckets.any { it > 0f }) buckets else List(buckets.size) { 0.15f + (it % 3) * 0.05f }
                TrendChart(data, Modifier.fillMaxWidth().height(120.dp), color = if (buckets.any { it > 0f }) SW.Lime else SW.Line)
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    axisLabels(period).forEach { Text(it, color = SW.Muted, style = MaterialTheme.typography.labelSmall) }
                }
            }
        }

        // ------------------------------------------------ category breakdown
        item {
            Spacer(Modifier.height(16.dp))
            Column(
                Modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(SW.Card)
                    .border(1.dp, SW.Line, RoundedCornerShape(24.dp)).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Category Breakdown", color = SW.Text, style = MaterialTheme.typography.titleMedium)
                EWasteCategory.entries.forEach { cat ->
                    val count = byCat[cat] ?: 0
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(cat.icon, cat.tint, size = 30.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(cat.shortTitle, color = SW.Subtle, style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(96.dp), maxLines = 1)
                        ProgressBar(count.toFloat() / maxCat, cat.tint, Modifier.weight(1f), height = 8.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("$count", color = SW.Text, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(28.dp))
                    }
                }
            }
        }

        // ------------------------------------------------------------- goals
        item {
            Spacer(Modifier.height(22.dp))
            SectionHeader("Goals")
            Spacer(Modifier.height(10.dp))
            val tierProgress = next?.let {
                ((lifetime - tier.minLifetimePoints).toFloat() / (it.minLifetimePoints - tier.minLifetimePoints)).coerceIn(0f, 1f)
            } ?: 1f
            GoalRow(Icons.Rounded.EmojiEvents, "${tier.name} → ${next?.name ?: "Max"}", "$lifetime / ${next?.minLifetimePoints ?: lifetime} pts", tierProgress, SW.Lime)
            val monthItems = deposits.filter { it.timestamp >= startOfMonth() }.sumOf { it.quantity }
            GoalRow(Icons.Rounded.Devices, "Recycle 10 items this month", "$monthItems / 10 items", monthItems / 10f, SW.Mint)
            val verified = deposits.count { it.verification == "ONLINE_VERIFIED" || it.verification == "CHECKSUM_VALID" || it.verification == "PLAUSIBLE" }
            GoalRow(Icons.Rounded.VerifiedUser, "Verify 5 deposits", "$verified / 5 verified", verified / 5f, SW.Amber)
        }

        // ----------------------------------------------------------- history
        item {
            Spacer(Modifier.height(22.dp))
            SectionHeader(
                "Deposit history",
                action = if (deposits.size > 4) (if (showAllHistory) "Less" else "View All") else null,
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

        // ---------------------------------------------------------- settings
        item {
            Spacer(Modifier.height(22.dp))
            SectionHeader("Account & settings")
            Spacer(Modifier.height(10.dp))
            SettingRow(Icons.Rounded.Edit, user?.name ?: "Your name", user?.email.orEmpty()) { editName = true }
            SettingRow(
                Icons.Rounded.VpnKey, "AI API key",
                when {
                    apiKey.isNotBlank() -> "Custom key set (…${apiKey.takeLast(4)})"
                    BuildConfig.OPENROUTER_API_KEY.isNotBlank() -> "Using build key from local.properties"
                    else -> "Not set. Using on-device backup AI only"
                },
            ) { editKey = true }
            SettingRow(
                Icons.Rounded.Security, "Data & transparency", "What we collect and how points are calculated",
                trailing = if (showTransparency) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
            ) { showTransparency = !showTransparency }
            if (showTransparency) TransparencyCard()
            SettingRow(Icons.Rounded.Info, "About SmartWaste", "v${BuildConfig.VERSION_NAME} · Every recycled item becomes a data point for a cleaner Singapore.") {}
            SettingRow(Icons.AutoMirrored.Rounded.ExitToApp, "Sign out", "You can sign back in any time", danger = true) { confirmLogout = true }
        }
    }

    if (editName) {
        TextDialog("Your name", user?.name.orEmpty(), onDismiss = { editName = false }) { v ->
            scope.launch { c.auth.rename(v) }; editName = false
        }
    }
    if (editKey) {
        TextDialog(
            "OpenRouter API key", apiKey, secret = true,
            hint = "Overrides the key from local.properties. Leave empty to use the build key.",
            onDismiss = { editKey = false },
        ) { v -> scope.launch { c.prefs.setApiKey(v) }; editKey = false }
    }
    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            containerColor = SW.Card,
            shape = RoundedCornerShape(26.dp),
            title = { Text("Sign out?", color = SW.Text) },
            text = { Text("Your points and history stay saved on this device for when you sign back in.", color = SW.Subtle) },
            confirmButton = {
                TextButton(onClick = { confirmLogout = false; scope.launch { c.auth.logout() } }) { Text("Sign out", color = SW.Danger, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("Cancel", color = SW.Muted) } },
        )
    }
}

private fun startOfMonth(): Long = Calendar.getInstance().apply {
    set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis

/** Splits the period into chart buckets (kg per bucket). */
private fun bucketize(deposits: List<DepositEntity>, period: Period): List<Float> {
    val buckets = when (period) { Period.MONTH -> 10; Period.QUARTER -> 12; Period.YEAR -> 12 }
    val span = period.days * 86_400_000L
    val start = System.currentTimeMillis() - span
    val out = FloatArray(buckets)
    deposits.forEach { d ->
        val idx = (((d.timestamp - start).toDouble() / span) * buckets).toInt().coerceIn(0, buckets - 1)
        out[idx] += d.kgEstimate.toFloat()
    }
    return out.toList()
}

private fun axisLabels(period: Period): List<String> {
    val fmt = SimpleDateFormat(if (period == Period.YEAR) "MMM" else "d MMM", Locale.US)
    val now = System.currentTimeMillis()
    val span = period.days * 86_400_000L
    return (0..3).map { i -> fmt.format(Date(now - span + span * i / 4)) } + "Now"
}

@Composable
private fun GoalRow(icon: ImageVector, title: String, detail: String, progress: Float, tint: androidx.compose.ui.graphics.Color) {
    Row(
        Modifier.padding(horizontal = 20.dp, vertical = 5.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(SW.Card)
            .border(1.dp, SW.Line, RoundedCornerShape(20.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon, tint, size = 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row {
                Text(title, color = SW.Text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f), maxLines = 1)
                Text(detail, color = SW.Muted, style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.height(8.dp))
            ProgressBar(progress.coerceIn(0f, 1f), tint)
        }
    }
}

@Composable
private fun DepositRow(d: DepositEntity) {
    val cat = EWasteCategory.fromKey(d.category)
    val verified = d.verification == "ONLINE_VERIFIED" || d.verification == "CHECKSUM_VALID" || d.verification == "PLAUSIBLE"
    Row(
        Modifier.padding(horizontal = 20.dp, vertical = 5.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(SW.Card)
            .border(1.dp, SW.Line, RoundedCornerShape(20.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(cat?.icon ?: Icons.Rounded.Devices, cat?.tint ?: SW.Lime, size = 42.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("${d.itemName}${if (d.quantity > 1) " × ${d.quantity}" else ""}", style = MaterialTheme.typography.titleSmall, color = SW.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${d.placeName} · ${SimpleDateFormat("d MMM, h:mm a", Locale.getDefault()).format(Date(d.timestamp))}",
                style = MaterialTheme.typography.labelSmall, color = SW.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (verified) TintTag("Verified", SW.Lime, icon = Icons.Rounded.VerifiedUser) else TintTag("Unverified", SW.Muted)
                if (d.binProof == "QR") TintTag("Smart bin", SW.Mint)
            }
        }
        Text("+${d.points}", style = MaterialTheme.typography.titleMedium, color = SW.Lime)
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailing: ImageVector? = null,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        Modifier.padding(horizontal = 20.dp, vertical = 5.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(SW.Card)
            .border(1.dp, SW.Line, RoundedCornerShape(20.dp)).clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon, if (danger) SW.Danger else SW.Lime, size = 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = if (danger) SW.Danger else SW.Text)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = SW.Muted)
        }
        trailing?.let { Icon(it, null, tint = SW.Muted) }
    }
}

@Composable
private fun TransparencyCard() {
    Column(
        Modifier.padding(horizontal = 20.dp, vertical = 5.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(SW.OliveDark)
            .border(1.dp, SW.Lime.copy(alpha = 0.2f), RoundedCornerShape(20.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Data we keep (on this device)", style = MaterialTheme.typography.titleSmall, color = SW.Lime)
        Text("• Your name, email and a salted password hash (never the password itself)\n• Bin ID, timestamp, item category, quantity, points\n• Your proof photo and a one-way hash of scanned codes (to stop re-use)", color = SW.Subtle, style = MaterialTheme.typography.bodySmall)
        Text("Sent to third parties", style = MaterialTheme.typography.titleSmall, color = SW.Lime)
        Text("• Photos go to the AI provider (OpenRouter) only for identification and proof checks\n• Barcodes go to UPCitemdb / Open Products Facts for product lookup\n• Location is used for nearby places and routing (OpenStreetMap services)", color = SW.Subtle, style = MaterialTheme.typography.bodySmall)
        Text("How points work", style = MaterialTheme.typography.titleSmall, color = SW.Lime)
        Text(
            "Base points per category × quantity\n× 1.2 if you scan a SmartWaste bin QR\n× 0.6 if AI can't confirm the bin photo\n+ verification bonus: online-verified barcode +100%, IMEI/barcode checksum +80%, plausible serial +50%, none 0%\nCodes already used earn no bonus · max 10 deposits per day",
            color = SW.Subtle, style = MaterialTheme.typography.bodySmall,
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
        containerColor = SW.Card,
        shape = RoundedCornerShape(26.dp),
        title = { Text(title, color = SW.Text) },
        text = {
            Column {
                OutlinedTextField(
                    value = value, onValueChange = { value = it }, singleLine = true,
                    visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
                    shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SW.Lime, unfocusedBorderColor = SW.Line,
                        focusedContainerColor = SW.Field, unfocusedContainerColor = SW.Field,
                        focusedTextColor = SW.Text, unfocusedTextColor = SW.Text, cursorColor = SW.Lime,
                    ),
                )
                hint?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, style = MaterialTheme.typography.labelSmall, color = SW.Muted)
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(value) }) { Text("Save", color = SW.Lime) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = SW.Muted) } },
    )
}
