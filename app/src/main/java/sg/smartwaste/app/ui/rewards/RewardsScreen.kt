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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Lock
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
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import sg.smartwaste.app.data.db.RedemptionEntity
import sg.smartwaste.app.data.rewards.Reward
import sg.smartwaste.app.data.rewards.RewardCategory
import sg.smartwaste.app.data.rewards.RewardsCatalog
import sg.smartwaste.app.ui.components.bottomContentPadding
import sg.smartwaste.app.ui.components.ArrowCircle
import sg.smartwaste.app.ui.components.CoverArt
import sg.smartwaste.app.ui.components.DayCard
import sg.smartwaste.app.ui.components.PillChip
import sg.smartwaste.app.ui.components.PrimaryButton
import sg.smartwaste.app.ui.components.ProgressBar
import sg.smartwaste.app.ui.components.RatingChip
import sg.smartwaste.app.ui.components.SectionHeader
import sg.smartwaste.app.ui.components.StatsStrip
import sg.smartwaste.app.ui.components.Tones
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

    LazyColumn(Modifier.fillMaxSize().background(SW.Bg), contentPadding = PaddingValues(bottom = bottomContentPadding())) {
        // ------------------------------------------------------------ header
        item {
            Column(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 14.dp)) {
                Text("Rewards", style = MaterialTheme.typography.headlineSmall, color = SW.Text)
                Text("Turn recycling into vouchers and treats", style = MaterialTheme.typography.bodySmall, color = SW.Muted)
            }
            Spacer(Modifier.height(18.dp))
        }

        // ------------------------------------------------------ balance card
        item {
            Box(Modifier.padding(horizontal = 20.dp).fillMaxWidth().height(230.dp).clip(RoundedCornerShape(30.dp))) {
                CoverArt(
                    Tones.Alpine, Modifier.matchParentSize(), seed = 5, icon = Icons.Outlined.EmojiEvents,
                    iconSize = 56.dp, iconAlignment = BiasAlignment(0.8f, -0.65f), scrim = true,
                )
                Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(20.dp)) {
                    Text("Points to redeem", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
                    Text(
                        buildAnnotatedString {
                            append(fmt.format(balance))
                            withStyle(SpanStyle(color = Color.White.copy(alpha = 0.7f), fontSize = 20.sp, fontWeight = FontWeight.Medium)) { append(" pts") }
                        },
                        color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RatingChip(tier.name, icon = Icons.Outlined.EmojiEvents, onImage = true)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            next?.let { "${fmt.format(it.minLifetimePoints - lifetime)} pts to ${it.name}" } ?: "Top tier reached",
                            color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    val progress = next?.let {
                        ((lifetime - tier.minLifetimePoints).toFloat() / (it.minLifetimePoints - tier.minLifetimePoints)).coerceIn(0f, 1f)
                    } ?: 1f
                    ProgressBar(progress, Color.White, track = Color.White.copy(alpha = 0.25f))
                }
            }
            Spacer(Modifier.height(12.dp))
            StatsStrip(
                listOf(
                    Triple("Lifetime earned", fmt.format(lifetime), "pts"),
                    Triple("Vouchers", "${redemptions.size}", "${fmt.format(spent)} pts used"),
                    Triple("Next reward", if (balance >= cheapest) "Ready" else fmt.format(cheapest - balance), if (balance >= cheapest) "redeem now" else "pts to go"),
                ),
                Modifier.padding(horizontal = 20.dp),
                color = SW.Surface,
            )
            Spacer(Modifier.height(24.dp))
        }

        // ------------------------------------------------------ category chips
        item {
            Text("Browse rewards", style = MaterialTheme.typography.titleLarge, color = SW.Text, modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(12.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(RewardCategory.entries) { cat ->
                    PillChip(cat.label, selected = filter == cat, onClick = { filter = cat })
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        // ---------------------------------------------------------- catalogue
        val list = RewardsCatalog.rewards.filter { filter == RewardCategory.ALL || it.category == filter }
        items(list, key = { it.id }) { reward -> RewardRow(reward, balance) { confirm = reward } }

        // ----------------------------------------------------------- vouchers
        item {
            Spacer(Modifier.height(24.dp))
            SectionHeader(
                "My vouchers",
                action = if (redemptions.size > 3) (if (showAllVouchers) "Show less" else "See all") else null,
                onAction = { showAllVouchers = !showAllVouchers },
            )
            Spacer(Modifier.height(12.dp))
            if (redemptions.isEmpty()) {
                Text("No vouchers yet. Redeem a reward above.", color = SW.Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 20.dp))
            }
        }
        items(if (showAllVouchers) redemptions else redemptions.take(3), key = { "red-${it.id}" }) { r ->
            VoucherRow(r) { copy(context, r.voucherCode) }
        }
        item {
            Spacer(Modifier.height(18.dp))
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
            containerColor = SW.Surface,
            shape = RoundedCornerShape(28.dp),
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
                    }) { Text("Redeem", color = SW.Ink, fontWeight = FontWeight.SemiBold) }
                } else {
                    TextButton(onClick = { confirm = null; onScan() }) { Text("Scan e-waste", color = SW.Ink, fontWeight = FontWeight.SemiBold) }
                }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancel", color = SW.Muted) } },
        )
    }

    redeemed?.let { r ->
        AlertDialog(
            onDismissRequest = { redeemed = null },
            containerColor = SW.Surface,
            shape = RoundedCornerShape(28.dp),
            icon = { Icon(Icons.Outlined.CardGiftcard, null, tint = SW.Ink) },
            title = { Text("Reward redeemed!", color = SW.Text) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(r.rewardTitle, color = SW.Subtle)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        r.voucherCode, fontFamily = FontFamily.Monospace, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = SW.Text,
                        modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(SW.SurfaceAlt)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Show this code at the partner counter.", color = SW.Muted, style = MaterialTheme.typography.labelSmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { copy(context, r.voucherCode); redeemed = null }) { Text("Copy code", color = SW.Ink, fontWeight = FontWeight.SemiBold) }
            },
        )
    }
}

/** Reward row in the collapsed "Day" card style: cover thumbnail, partner, title and progress. */
@Composable
private fun RewardRow(reward: Reward, balance: Int, onClick: () -> Unit) {
    val affordable = balance >= reward.cost
    val fmt = NumberFormat.getIntegerInstance(Locale.US)
    Row(
        Modifier.padding(horizontal = 20.dp, vertical = 5.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(SW.Surface)
            .clickable(onClick = onClick).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(width = 76.dp, height = 64.dp).clip(RoundedCornerShape(16.dp))) {
            CoverArt(Tones.forKey(reward.id), Modifier.matchParentSize(), seed = reward.id.hashCode())
            Text(
                reward.valueLabel, color = Color.White, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center).padding(horizontal = 4.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(reward.partner, style = MaterialTheme.typography.bodySmall, color = SW.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(reward.title, style = MaterialTheme.typography.titleSmall, color = SW.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProgressBar((balance.toFloat() / reward.cost).coerceIn(0f, 1f), SW.Ink, Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                Text("${fmt.format(reward.cost)} pts", style = MaterialTheme.typography.labelMedium, color = SW.Text)
            }
        }
        Spacer(Modifier.width(10.dp))
        if (affordable) {
            ArrowCircle(size = 40.dp, contentDescription = "Redeem ${reward.title}", onClick = onClick)
        } else {
            ArrowCircle(
                size = 40.dp, container = SW.SurfaceAlt, content = SW.Muted, icon = Icons.Outlined.Lock,
                contentDescription = "${reward.title}: not enough points yet", onClick = onClick,
            )
        }
    }
}

@Composable
private fun VoucherRow(r: RedemptionEntity, onCopy: () -> Unit) {
    DayCard(
        overline = "${r.voucherCode} · ${SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(r.timestamp))}",
        title = r.rewardTitle,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp),
        trailing = {
            ArrowCircle(
                size = 40.dp, container = SW.SurfaceAlt, content = SW.Text, icon = Icons.Outlined.ContentCopy,
                contentDescription = "Copy voucher code", onClick = onCopy,
            )
        },
        thumbnail = {
            CoverArt(Tones.forKey(r.rewardId), Modifier.matchParentSize(), seed = r.id.hashCode(), icon = Icons.Outlined.ConfirmationNumber, iconSize = 24.dp)
        },
    )
}

private fun copy(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("SmartWaste voucher", text))
    Toast.makeText(context, "Copied $text", Toast.LENGTH_SHORT).show()
}
