package sg.smartwaste.app.ui.scan

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import sg.smartwaste.app.data.KnowledgeBase
import sg.smartwaste.app.data.model.DetectionEngine
import sg.smartwaste.app.data.model.DetectionResult
import sg.smartwaste.app.data.model.EWasteCategory
import sg.smartwaste.app.data.model.HazardLevel
import sg.smartwaste.app.data.model.ResalePotential
import sg.smartwaste.app.ui.components.CircleIconButton
import sg.smartwaste.app.ui.components.CoverArt
import sg.smartwaste.app.ui.components.DayCard
import sg.smartwaste.app.ui.components.DetailBlock
import sg.smartwaste.app.ui.components.DragHandle
import sg.smartwaste.app.ui.components.PillChip
import sg.smartwaste.app.ui.components.PrimaryButton
import sg.smartwaste.app.ui.components.RatingChip
import sg.smartwaste.app.ui.components.SecondaryButton
import sg.smartwaste.app.ui.components.TabChips
import sg.smartwaste.app.ui.components.TintTag
import sg.smartwaste.app.ui.components.Tones
import sg.smartwaste.app.ui.components.UnderlinedLink
import sg.smartwaste.app.ui.components.appContainer
import sg.smartwaste.app.ui.components.icon
import sg.smartwaste.app.ui.components.tint
import sg.smartwaste.app.ui.components.tones
import sg.smartwaste.app.ui.theme.SW
import java.io.File
import kotlin.math.roundToInt

private enum class ResultTab(val label: String) { PREPARE("How to prepare"), SAFETY("Safety"), VALUE("Resale value") }

@Composable
fun ResultScreen(
    onBack: () -> Unit,
    onFindBin: (EWasteCategory?) -> Unit,
    onSell: (EWasteCategory?) -> Unit,
    onDeposit: () -> Unit,
    onScanAgain: () -> Unit,
) {
    val session = appContainer().session
    val detection by session.detection.collectAsStateWithLifecycle()
    val d = detection
    if (d == null) {
        Box(Modifier.fillMaxSize().background(SW.Bg), contentAlignment = Alignment.Center) {
            PrimaryButton("Scan an item", onScanAgain)
        }
        return
    }
    var tab by rememberSaveable { mutableStateOf(ResultTab.PREPARE) }
    var descExpanded by rememberSaveable { mutableStateOf(false) }
    var descOverflows by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(SW.Surface)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            // ---------------------------------------------------------- hero photo
            Box(Modifier.fillMaxWidth().height(380.dp)) {
                CoverArt(
                    d.category?.tones ?: Tones.Harbor, Modifier.matchParentSize(), seed = d.id.hashCode(),
                    icon = d.category?.icon ?: Icons.Rounded.Eco, iconSize = 96.dp,
                )
                if (d.photoPath != null) {
                    AsyncImage(File(d.photoPath), null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
                }
                Box(
                    Modifier.matchParentSize().background(
                        Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.25f), 0.35f to Color.Transparent)
                    )
                )
                Row(
                    Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    CircleIconButton(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Back", onBack, iconSize = 24.dp)
                    CircleIconButton(Icons.Outlined.CameraAlt, "Scan again", onScanAgain)
                }
            }

            // ------------------------------------------------------ content sheet
            Column(
                Modifier
                    .offset(y = (-32).dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(SW.Surface)
                    .padding(horizontal = 20.dp),
            ) {
                DragHandle(Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp))
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Text(d.itemName, style = MaterialTheme.typography.headlineSmall, color = SW.Text, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(12.dp))
                    RatingChip("${(d.confidence * 100).roundToInt()}%", modifier = Modifier.padding(top = 4.dp))
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val cat = d.category
                    Box(
                        Modifier.size(20.dp).clip(CircleShape).background(cat?.tint ?: SW.Muted),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(cat?.icon ?: Icons.Rounded.Info, null, tint = Color.White, modifier = Modifier.size(12.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        buildString {
                            append(cat?.title ?: if (d.isEWaste) "Category unknown" else "Not e-waste")
                            d.brand?.let { append(" · $it") }
                        },
                        style = MaterialTheme.typography.bodyMedium, color = SW.Text,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    UnderlinedLink(d.engine.label, onClick = null)
                }

                if (d.description.isNotBlank()) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        d.description,
                        style = MaterialTheme.typography.bodyMedium, color = SW.Subtle,
                        maxLines = if (descExpanded) Int.MAX_VALUE else 3,
                        overflow = TextOverflow.Ellipsis,
                        onTextLayout = { if (!descExpanded) descOverflows = it.hasVisualOverflow },
                    )
                    if (descOverflows || descExpanded) {
                        Spacer(Modifier.height(6.dp))
                        UnderlinedLink(if (descExpanded) "Read less" else "Read more", { descExpanded = !descExpanded })
                    }
                }

                Spacer(Modifier.height(14.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val hazardColor = when (d.hazardLevel) {
                        HazardLevel.HIGH -> SW.Danger
                        HazardLevel.MEDIUM -> SW.Warn
                        HazardLevel.LOW -> SW.Success
                    }
                    TintTag("${d.hazardLevel.name.lowercase().replaceFirstChar { it.uppercase() }} hazard", hazardColor, icon = Icons.Rounded.Warning)
                    if (d.containsBattery) TintTag("Contains battery", SW.Warn, icon = Icons.Rounded.BatteryChargingFull)
                    if (d.neaRegulated) TintTag("NEA regulated", SW.Info, icon = Icons.Rounded.VerifiedUser)
                }

                // Category correction (important for the on-device backup AI).
                if (d.engine == DetectionEngine.ON_DEVICE || d.category == null || d.confidence < 0.6f) {
                    Spacer(Modifier.height(20.dp))
                    Text("Not right? Choose the category", style = MaterialTheme.typography.titleMedium, color = SW.Text)
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        EWasteCategory.entries.forEach { c ->
                            PillChip(
                                c.shortTitle, selected = d.category == c, onClick = { session.overrideCategory(c) },
                                leading = c.icon, idleColor = SW.SurfaceAlt, small = true,
                            )
                        }
                    }
                }

                if (d.isEWaste && d.category != null) {
                    val category: EWasteCategory = d.category
                    Spacer(Modifier.height(24.dp))
                    TabChips(ResultTab.entries, tab, { it.label }, { tab = it })
                    Spacer(Modifier.height(18.dp))
                    AnimatedContent(tab, label = "tab") { t ->
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            when (t) {
                                ResultTab.PREPARE -> {
                                    Text("Prepare your ${category.shortTitle.lowercase()}", style = MaterialTheme.typography.titleLarge, color = SW.Text)
                                    Spacer(Modifier.height(2.dp))
                                    WhereItGoesCard(category)
                                    StepCards(category, d.preparationSteps.ifEmpty { KnowledgeBase.guidance(category).preparation })
                                }
                                ResultTab.SAFETY -> {
                                    Text("Handle it safely", style = MaterialTheme.typography.titleLarge, color = SW.Text)
                                    Spacer(Modifier.height(2.dp))
                                    WarningCards(d.safetyWarnings.ifEmpty { KnowledgeBase.guidance(category).safety })
                                }
                                ResultTab.VALUE -> {
                                    Text("Is it worth selling?", style = MaterialTheme.typography.titleLarge, color = SW.Text)
                                    Spacer(Modifier.height(2.dp))
                                    ValueCard(d)
                                }
                            }
                        }
                    }
                } else if (!d.isEWaste) {
                    Spacer(Modifier.height(20.dp))
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(SW.SurfaceAlt).padding(18.dp)) {
                        Text("This doesn't look like e-waste", style = MaterialTheme.typography.titleMedium, color = SW.Text)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "If it's paper, plastic, metal or glass, use the blue recycling bins. Otherwise, use general waste. " +
                                "If it is electronic, pick the right category above.",
                            style = MaterialTheme.typography.bodyMedium, color = SW.Subtle,
                        )
                    }
                }
                Text(
                    "Identified by ${d.modelName}",
                    style = MaterialTheme.typography.labelSmall, color = SW.Muted,
                    modifier = Modifier.padding(top = 18.dp),
                )
                Spacer(Modifier.height(170.dp))
            }
        }

        // ------------------------------------------------------- sticky actions
        if (d.isEWaste && d.category != null) {
            val category: EWasteCategory = d.category
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(0f to Color.Transparent, 0.25f to SW.Surface, 1f to SW.Surface))
                    .navigationBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row {
                    PrimaryButton(
                        if (category.binFriendly) "Find nearest bin" else "Find take-back",
                        { onFindBin(category) }, Modifier.weight(1f), leading = Icons.Outlined.Place,
                    )
                    Spacer(Modifier.width(10.dp))
                    SecondaryButton("Sell", { onSell(category) }, leading = Icons.Outlined.Payments)
                }
                if (category.binFriendly) {
                    Spacer(Modifier.height(10.dp))
                    UnderlinedLink("I'm at a bin: verify & earn points", onDeposit, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

/** The expanded "Day 1" card: which compartment, examples, points and where to go. */
@Composable
private fun WhereItGoesCard(category: EWasteCategory) {
    var open by rememberSaveable { mutableStateOf(true) }
    DayCard(
        overline = "Goes into",
        title = category.compartment,
        expanded = open,
        onToggle = { open = !open },
        thumbnail = { BinThumb(category) },
        details = {
            DetailBlock("Examples", category.examples)
            DetailBlock("Points", "${category.basePoints} pts per item. Verifying the item doubles that, or 2.2× at a SmartWaste smart bin.")
            DetailBlock("Where to go", KnowledgeBase.guidance(category).whereToGo)
        },
    )
}

/** Mini 2×2 SmartWaste bin with the target compartment highlighted. */
@Composable
private fun BoxScope.BinThumb(category: EWasteCategory) {
    val cells = listOf(
        EWasteCategory.DEVICE_WITH_BATTERY, EWasteCategory.SMALL_BATTERY,
        EWasteCategory.APPLIANCE_NO_BATTERY, EWasteCategory.COMPONENT,
    )
    Box(Modifier.matchParentSize().background(SW.Ink), contentAlignment = Alignment.Center) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            cells.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    row.forEach { c ->
                        val on = c == category
                        Box(
                            Modifier.size(width = 22.dp, height = 18.dp).clip(RoundedCornerShape(5.dp))
                                .background(if (on) Color.White else Color.White.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(c.icon, null, tint = if (on) SW.Ink else Color.White.copy(alpha = 0.6f), modifier = Modifier.size(11.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepCards(category: EWasteCategory, steps: List<String>) {
    steps.forEachIndexed { i, step ->
        DayCard(
            overline = "Step ${i + 1}",
            title = step,
            thumbnail = {
                CoverArt(category.tones, Modifier.matchParentSize(), seed = i * 7 + 1)
                Text("${i + 1}", color = Color.White, style = MaterialTheme.typography.titleLarge, modifier = Modifier.align(Alignment.Center))
            },
        )
    }
}

@Composable
private fun WarningCards(warnings: List<String>) {
    if (warnings.isEmpty()) {
        Text("No special hazards. Handle with normal care.", style = MaterialTheme.typography.bodyMedium, color = SW.Subtle)
    }
    warnings.forEachIndexed { i, w ->
        DayCard(
            overline = "Warning ${i + 1}",
            title = w,
            thumbnail = {
                CoverArt(Tones.Golden, Modifier.matchParentSize(), seed = i * 5 + 2, icon = Icons.Rounded.Warning, iconSize = 24.dp)
            },
        )
    }
}

@Composable
private fun ValueCard(d: DetectionResult) {
    val level = when (d.resalePotential) {
        ResalePotential.NONE -> 0
        ResalePotential.LOW -> 1
        ResalePotential.MEDIUM -> 2
        ResalePotential.HIGH -> 3
    }
    var open by rememberSaveable { mutableStateOf(true) }
    DayCard(
        overline = "Resale potential",
        title = d.estimatedValue ?: d.resalePotential.name.lowercase().replaceFirstChar { it.uppercase() },
        expanded = open,
        onToggle = { open = !open },
        thumbnail = {
            CoverArt(Tones.Golden, Modifier.matchParentSize(), seed = 9, icon = Icons.Outlined.Payments, iconSize = 24.dp)
        },
        details = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) { i ->
                    Box(Modifier.size(width = 28.dp, height = 6.dp).clip(RoundedCornerShape(3.dp)).background(if (i < level) SW.Ink else SW.Track))
                }
            }
            DetailBlock(
                "Our advice",
                if (level >= 2) "This may still be worth money. Consider selling or trading it in before recycling."
                else "Low resale value. Recycling is the best option, and you'll earn points.",
            )
            if (d.materials.isNotEmpty()) DetailBlock("Recoverable materials", d.materials.joinToString(", "))
        },
    )
}
