package sg.smartwaste.app.ui.rewards

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Redeem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import sg.smartwaste.app.data.db.RedemptionEntity
import sg.smartwaste.app.data.rewards.Reward
import sg.smartwaste.app.data.rewards.RewardCategory
import sg.smartwaste.app.data.rewards.RewardsCatalog
import sg.smartwaste.app.ui.components.PillChip
import sg.smartwaste.app.ui.components.SectionHeader
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
    var showHistory by remember { mutableStateOf(false) }

    val tier = RewardsCatalog.tierFor(lifetime)
    val next = RewardsCatalog.nextTier(lifetime)
    val fmt = NumberFormat.getIntegerInstance(Locale.US)

    LazyColumn(
        Modifier.fillMaxSize().background(SW.Bg),
        contentPadding = PaddingValues(bottom = 150.dp),
    ) {
        // ---------------------------------------------------------- balance hero
        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
                    .background(SW.Navy)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Rewards", color = Color.White, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(18.dp))
                Text("Balance", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium)
                Text("${fmt.format(balance)} pts", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                    HeroAction(Icons.Rounded.QrCodeScanner, "Earn", onEarn)
                    HeroAction(Icons.Rounded.Redeem, "Redeem", { filter = RewardCategory.ALL })
                    HeroAction(Icons.Rounded.History, "Vouchers", { showHistory = !showHistory })
                }
                Spacer(Modifier.height(20.dp))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(SW.Navy2).padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.EmojiEvents, null, tint = SW.Lime, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("${tier.name} tier", color = Color.White, style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.weight(1f))
                        Text(
                            next?.let { "${fmt.format(it.minLifetimePoints - lifetime)} pts to ${it.name}" } ?: "Top tier reached",
                            color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    val progress = next?.let {
                        ((lifetime - tier.minLifetimePoints).toFloat() / (it.minLifetimePoints - tier.minLifetimePoints)).coerceIn(0f, 1f)
                    } ?: 1f
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = SW.Lime, trackColor = Color.White.copy(alpha = 0.12f), strokeCap = StrokeCap.Round,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(tier.perk, color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // ---------------------------------------------------------- vouchers
        if (showHistory || redemptions.isNotEmpty()) {
            item {
                Spacer(Modifier.height(20.dp))
                SectionHeader("My vouchers", action = if (showHistory) "Hide" else "Show all", onAction = { showHistory = !showHistory })
                Spacer(Modifier.height(10.dp))
            }
            val shown = if (showHistory) redemptions else redemptions.take(2)
            if (shown.isEmpty()) {
                item {
                    Text("No vouchers yet. Redeem a reward below.", color = SW.Muted, modifier = Modifier.padding(horizontal = 20.dp))
                }
            }
            items(shown, key = { "red-${it.id}" }) { r -> VoucherRow(r) { copy(context, r.voucherCode) } }
        }

        // ---------------------------------------------------------- catalogue
        item {
            Spacer(Modifier.height(22.dp))
            SectionHeader("Redeem your points")
            Spacer(Modifier.height(12.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(RewardCategory.entries) { cat ->
                    PillChip(cat.label, selected = filter == cat, onClick = { filter = cat })
                }
            }
            Spacer(Modifier.height(14.dp))
        }
        val list = RewardsCatalog.rewards.filter { filter == RewardCategory.ALL || it.category == filter }
        items(list.chunked(2), key = { row -> row.joinToString { it.id } }) { row ->
            Row(Modifier.padding(horizontal = 20.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { reward ->
                    RewardCard(reward, balance, Modifier.weight(1f)) { confirm = reward }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        item {
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
            containerColor = Color.White,
            shape = RoundedCornerShape(28.dp),
            title = { Text(reward.title) },
            text = {
                Column {
                    Text(reward.description, color = SW.Subtle)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (enough) "Redeem for ${fmt.format(reward.cost)} points? You'll have ${fmt.format(balance - reward.cost)} left."
                        else "You need ${fmt.format(reward.cost - balance)} more points. Recycle and verify items to earn them!",
                        color = SW.Ink,
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
                    }) { Text("Redeem", color = SW.EmeraldDeep, fontWeight = FontWeight.Bold) }
                } else {
                    TextButton(onClick = { confirm = null; onScan() }) { Text("Scan e-waste", color = SW.EmeraldDeep, fontWeight = FontWeight.Bold) }
                }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancel", color = SW.Subtle) } },
        )
    }

    redeemed?.let { r ->
        AlertDialog(
            onDismissRequest = { redeemed = null },
            containerColor = Color.White,
            shape = RoundedCornerShape(28.dp),
            icon = { Icon(Icons.Rounded.CardGiftcard, null, tint = SW.Emerald) },
            title = { Text("Reward redeemed!") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(r.rewardTitle, color = SW.Subtle)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        r.voucherCode, fontFamily = FontFamily.Monospace, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = SW.Ink,
                        modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(SW.Field).padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Show this code at the partner counter.", color = SW.Muted, style = MaterialTheme.typography.labelSmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { copy(context, r.voucherCode); redeemed = null }) { Text("Copy code", color = SW.EmeraldDeep, fontWeight = FontWeight.Bold) }
            },
        )
    }
}

@Composable
private fun HeroAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(58.dp).clip(RoundedCornerShape(18.dp)).background(SW.Navy3).clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, label, tint = Color.White, modifier = Modifier.size(24.dp)) }
        Spacer(Modifier.height(6.dp))
        Text(label, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun RewardCard(reward: Reward, balance: Int, modifier: Modifier, onClick: () -> Unit) {
    val color = Color(reward.color)
    val affordable = balance >= reward.cost
    Column(modifier.clip(RoundedCornerShape(26.dp)).background(Color.White).clickable(onClick = onClick)) {
        Box(
            Modifier.fillMaxWidth().height(96.dp).padding(8.dp).clip(RoundedCornerShape(20.dp)).background(color),
        ) {
            Icon(
                rewardIcon(reward.category), null, tint = Color.White.copy(alpha = 0.3f),
                modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp).size(52.dp),
            )
            Text(
                reward.valueLabel, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
            )
        }
        Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
            Text(reward.title, style = MaterialTheme.typography.titleSmall, color = SW.Ink, maxLines = 1)
            Text(reward.partner, style = MaterialTheme.typography.labelSmall, color = SW.Muted, maxLines = 1)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${NumberFormat.getIntegerInstance(Locale.US).format(reward.cost)} pts", style = MaterialTheme.typography.labelLarge, color = SW.Ink)
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier.size(30.dp).clip(CircleShape).background(if (affordable) SW.Emerald else SW.Field),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Redeem, null, tint = if (affordable) Color.White else SW.Muted, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun VoucherRow(r: RedemptionEntity, onCopy: () -> Unit) {
    Row(
        Modifier.padding(horizontal = 20.dp, vertical = 5.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color.White).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(SW.Mint), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.ConfirmationNumber, null, tint = SW.EmeraldDeep)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(r.rewardTitle, style = MaterialTheme.typography.titleSmall, color = SW.Ink)
            Text(
                "${r.voucherCode} · ${SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(r.timestamp))}",
                style = MaterialTheme.typography.labelSmall, color = SW.Muted,
            )
        }
        Icon(Icons.Rounded.ContentCopy, "Copy", tint = SW.Subtle, modifier = Modifier.size(20.dp).clickable(onClick = onCopy))
    }
}

private fun rewardIcon(cat: RewardCategory): ImageVector = when (cat) {
    RewardCategory.TRANSPORT -> Icons.Rounded.DirectionsBus
    RewardCategory.FOOD -> Icons.Rounded.LocalCafe
    RewardCategory.GREEN -> Icons.Rounded.Eco
    else -> Icons.Rounded.CardGiftcard
}

private fun copy(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("SmartWaste voucher", text))
    Toast.makeText(context, "Copied $text", Toast.LENGTH_SHORT).show()
}
