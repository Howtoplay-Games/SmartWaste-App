package sg.smartwaste.app.ui.rewards

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.Redeem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import sg.smartwaste.app.data.db.RedemptionEntity
import sg.smartwaste.app.data.rewards.Reward
import sg.smartwaste.app.data.rewards.RewardCategory
import sg.smartwaste.app.data.rewards.RewardsCatalog
import sg.smartwaste.app.ui.components.CategoryTile
import sg.smartwaste.app.ui.components.HeroHeader
import sg.smartwaste.app.ui.components.PrimaryButton
import sg.smartwaste.app.ui.components.ProgressBar
import sg.smartwaste.app.ui.components.SectionHeader
import sg.smartwaste.app.ui.components.StatsStrip
import sg.smartwaste.app.ui.components.appContainer
import sg.smartwaste.app.ui.theme.SW
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RewardsScreen(onEarn: () -> Unit, onScan: () -> Unit) {
    val c = appContainer()
    val balance by c.history.balance.collectAsStateWithLifecycle(initialValue = 0)
    val lifetime by c.history.lifetimePoints.collectAsStateWithLifecycle(initialValue = 0)
    val redemptions by c.history.redemptions.collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var filter by remember { mutableStateOf(RewardCategory.ALL) }
    var confirm by remember { mutableStateOf<Reward?>(null) }
    var redeemed by remember { mutableStateOf<RedemptionEntity?>(null) }
    var showAllVouchers by remember { mutableStateOf(false) }

    val tier = RewardsCatalog.tierFor(lifetime)
    val next = RewardsCatalog.nextTier(lifetime)
    val fmt = NumberFormat.getIntegerInstance(Locale.US)
    val spent = redemptions.sumOf { it.cost }
    val cheapest = RewardsCatalog.rewards.minOf { it.cost }

    LazyColumn(Modifier.fillMaxSize().background(SW.Sheet), contentPadding = PaddingValues(bottom = 130.dp)) {
        // -------------------------------------------------------------- hero
        item {
            HeroHeader {
                Text(
                    "Rewards", color = SW.Text, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(top = 14.dp),
                )
                Spacer(Modifier.height(18.dp))
                Text("Points to redeem", color = SW.Subtle, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = SW.Text)) { append(fmt.format(balance)) }
                        withStyle(SpanStyle(color = SW.Text.copy(alpha = 0.45f), fontSize = 26.sp)) { append(" pts") }
                    },
                    fontSize = 46.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                )
                Text(
                    next?.let { "${tier.name} tier · ${fmt.format(it.minLifetimePoints - lifetime)} pts to ${it.name}" } ?: "${tier.name} tier · top tier reached",
                    color = SW.Muted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(10.dp))
                val progress = next?.let {
                    ((lifetime - tier.minLifetimePoints).toFloat() / (it.minLifetimePoints - tier.minLifetimePoints)).coerceIn(0f, 1f)
                } ?: 1f
                ProgressBar(progress, SW.Lime, Modifier.padding(horizontal = 60.dp))
                Spacer(Modifier.height(18.dp))
                StatsStrip(
                    listOf(
                        Triple("Lifetime earned", fmt.format(lifetime), "pts"),
                        Triple("Vouchers", "${redemptions.size}", "${fmt.format(spent)} pts used"),
                        Triple("Next reward", if (balance >= cheapest) "Ready" else fmt.format(cheapest - balance), if (balance >= cheapest) "redeem now" else "pts to go"),
                    ),
                    Modifier.padding(horizontal = 20.dp),
                )
            }
        }

        // ----------------------------------------------- category grid (3 × 2)
        item {
            Text("Category", color = SW.Text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(10.dp))
            RewardCategory.entries.chunked(3).forEach { row ->
                Row(Modifier.padding(horizontal = 20.dp, vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { cat ->
                        CategoryTile(rewardIcon(cat), cat.label, selected = filter == cat, onClick = { filter = cat }, modifier = Modifier.weight(1f))
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // ---------------------------------------------------------- catalogue
        val list = RewardsCatalog.rewards.filter { filter == RewardCategory.ALL || it.category == filter }
        items(list, key = { it.id }) { reward -> RewardRow(reward, balance) { confirm = reward } }

        // ----------------------------------------------------------- vouchers
        item {
            Spacer(Modifier.height(22.dp))
            SectionHeader(
                "My vouchers",
                action = if (redemptions.size > 3) (if (showAllVouchers) "Less" else "View All") else null,
                onAction = { showAllVouchers = !showAllVouchers },
            )
            Spacer(Modifier.height(10.dp))
            if (redemptions.isEmpty()) {
                Text("No vouchers yet. Redeem a reward above.", color = SW.Muted, modifier = Modifier.padding(horizontal = 20.dp))
            }
        }
        items(if (showAllVouchers) redemptions else redemptions.take(3), key = { "red-${it.id}" }) { r ->
            VoucherRow(r) { copy(context, r.voucherCode) }
        }
        item {
            Spacer(Modifier.height(14.dp))
            PrimaryButton("Earn more points at a bin", onEarn, Modifier.padding(horizontal = 20.dp).fillMaxWidth())
            Text(
                "Rewards shown are a demo catalogue. Partner vouchers will be issued once sponsorships are confirmed.",
                color = SW.Muted, style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
        }
    }

    confirm?.let { reward ->
        val enough = balance >= reward.cost
        AlertDialog(
            onDismissRequest = { confirm = null },
            containerColor = SW.Card,
            shape = RoundedCornerShape(26.dp),
            title = { Text(reward.title, color = SW.Text) },
            text = {
                Column {
                    Text(reward.description, color = SW.Subtle)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (enough) "Redeem for ${fmt.format(reward.cost)} points? You'll have ${fmt.format(balance - reward.cost)} left."
                        else "You need ${fmt.format(reward.cost - balance)} more points. Recycle and verify items to earn them!",
                        color = SW.Text,
                    )
                }
            },
            confirmButton = {
                if (enough) {
                    TextButton(onClick = {
                        confirm = null
                        scope.launch {
                            c.history.redeem(reward)
                                .onSuccess { redeemed = it }
                                .onFailure { Toast.makeText(context, it.message ?: "Couldn't redeem", Toast.LENGTH_SHORT).show() }
                        }
                    }) { Text("Redeem", color = SW.Lime, fontWeight = FontWeight.Bold) }
                } else {
                    TextButton(onClick = { confirm = null; onScan() }) { Text("Scan e-waste", color = SW.Lime, fontWeight = FontWeight.Bold) }
                }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancel", color = SW.Muted) } },
        )
    }

    redeemed?.let { r ->
        AlertDialog(
            onDismissRequest = { redeemed = null },
            containerColor = SW.Card,
            shape = RoundedCornerShape(26.dp),
            icon = { Icon(Icons.Rounded.CardGiftcard, null, tint = SW.Lime) },
            title = { Text("Reward redeemed!", color = SW.Text) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(r.rewardTitle, color = SW.Subtle)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        r.voucherCode, fontFamily = FontFamily.Monospace, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = SW.Lime,
                        modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(SW.Field).border(1.dp, SW.Line, RoundedCornerShape(14.dp))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Show this code at the partner counter.", color = SW.Muted, style = MaterialTheme.typography.labelSmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { copy(context, r.voucherCode); redeemed = null }) { Text("Copy code", color = SW.Lime, fontWeight = FontWeight.Bold) }
            },
        )
    }
}

@Composable
private fun RewardRow(reward: Reward, balance: Int, onClick: () -> Unit) {
    val color = Color(reward.color)
    val affordable = balance >= reward.cost
    Row(
        Modifier.padding(horizontal = 20.dp, vertical = 5.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(SW.Card)
            .border(1.dp, SW.Line, RoundedCornerShape(22.dp)).clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(64.dp).clip(RoundedCornerShape(18.dp)).background(Brush.linearGradient(listOf(color, color.copy(alpha = 0.55f)))),
            contentAlignment = Alignment.Center,
        ) {
            Text(reward.valueLabel, color = SW.OnLime, fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(reward.title, style = MaterialTheme.typography.titleSmall, color = SW.Text, maxLines = 1)
            Text(reward.partner, style = MaterialTheme.typography.labelSmall, color = SW.Muted, maxLines = 1)
            Spacer(Modifier.height(8.dp))
            ProgressBar((balance.toFloat() / reward.cost).coerceIn(0f, 1f), if (affordable) SW.Lime else color.copy(alpha = 0.7f))
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text("${NumberFormat.getIntegerInstance(Locale.US).format(reward.cost)}", style = MaterialTheme.typography.titleMedium, color = SW.Text)
            Text("pts", style = MaterialTheme.typography.labelSmall, color = SW.Muted)
            Spacer(Modifier.height(4.dp))
            Box(
                Modifier.size(30.dp).clip(CircleShape).background(if (affordable) SW.Lime else SW.Card2),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Redeem, null, tint = if (affordable) SW.OnLime else SW.Muted, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun VoucherRow(r: RedemptionEntity, onCopy: () -> Unit) {
    Row(
        Modifier.padding(horizontal = 20.dp, vertical = 5.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(SW.Card)
            .border(1.dp, SW.Line, RoundedCornerShape(20.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(SW.LimeSoft), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.ConfirmationNumber, null, tint = SW.Lime)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(r.rewardTitle, style = MaterialTheme.typography.titleSmall, color = SW.Text)
            Text(
                "${r.voucherCode} · ${SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(r.timestamp))}",
                style = MaterialTheme.typography.labelSmall, color = SW.Muted,
            )
        }
        Icon(Icons.Rounded.ContentCopy, "Copy", tint = SW.Subtle, modifier = Modifier.size(20.dp).clickable(onClick = onCopy))
    }
}

private fun rewardIcon(cat: RewardCategory): ImageVector = when (cat) {
    RewardCategory.ALL -> Icons.Rounded.Apps
    RewardCategory.VOUCHERS -> Icons.Rounded.CardGiftcard
    RewardCategory.TRANSPORT -> Icons.Rounded.DirectionsBus
    RewardCategory.FOOD -> Icons.Rounded.LocalCafe
    RewardCategory.GREEN -> Icons.Rounded.Eco
}

private fun copy(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("SmartWaste voucher", text))
    Toast.makeText(context, "Copied $text", Toast.LENGTH_SHORT).show()
}
