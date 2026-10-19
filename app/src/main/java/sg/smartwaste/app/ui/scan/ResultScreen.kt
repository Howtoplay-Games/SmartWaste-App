package sg.smartwaste.app.ui.scan

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
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
import sg.smartwaste.app.ui.components.IconBadge
import sg.smartwaste.app.ui.components.PillChip
import sg.smartwaste.app.ui.components.PrimaryButton
import sg.smartwaste.app.ui.components.SecondaryButton
import sg.smartwaste.app.ui.components.Tag
import sg.smartwaste.app.ui.components.appContainer
import sg.smartwaste.app.ui.components.icon
import sg.smartwaste.app.ui.components.tint
import sg.smartwaste.app.ui.theme.SW
import java.io.File
import kotlin.math.roundToInt

private enum class ResultTab(val label: String) { PREPARE("Prepare"), SAFETY("Safety"), VALUE("Value") }

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

    Box(Modifier.fillMaxSize().background(SW.Sheet)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            // ---------------------------------------------------------- hero photo
            Box(Modifier.fillMaxWidth().height(340.dp)) {
                if (d.photoPath != null) {
                    AsyncImage(File(d.photoPath), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Box(Modifier.fillMaxSize().background(SW.heroBrush))
                }
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.35f), Color.Transparent, Color.Transparent))))
                Row(
                    Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    CircleIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack, container = SW.Bg.copy(alpha = 0.6f))
                    CircleIconButton(Icons.Rounded.CameraAlt, "Scan again", onScanAgain, container = SW.Bg.copy(alpha = 0.6f))
                }
            }

            // ------------------------------------------------------ content sheet
            Column(
                Modifier
                    .offset(y = (-28).dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(SW.Sheet)
                    .padding(horizontal = 20.dp),
            ) {
                Box(Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp).size(width = 40.dp, height = 4.dp).clip(RoundedCornerShape(2.dp)).background(SW.Line))
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(d.itemName, style = MaterialTheme.typography.headlineSmall, color = SW.Text)
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val cat = d.category
                            if (cat != null) {
                                Icon(cat.icon, null, tint = cat.tint, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(cat.title, style = MaterialTheme.typography.labelMedium, color = SW.Subtle)
                            } else {
                                Text(if (d.isEWaste) "Category unknown" else "Not e-waste", style = MaterialTheme.typography.labelMedium, color = SW.Subtle)
                            }
                            d.brand?.let {
                                Text("  ·  $it", style = MaterialTheme.typography.labelMedium, color = SW.Subtle)
                            }
                        }
                    }
                    Row(
                        Modifier.clip(RoundedCornerShape(50)).background(SW.Card2).border(1.dp, SW.Line, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Star, null, tint = SW.Lime, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("${(d.confidence * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium, color = SW.Text)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val hazardColor = when (d.hazardLevel) {
                        HazardLevel.HIGH -> SW.Danger
                        HazardLevel.MEDIUM -> SW.Warn
                        HazardLevel.LOW -> SW.Lime
                    }
                    Tag("${d.hazardLevel.name.lowercase().replaceFirstChar { it.uppercase() }} hazard", hazardColor.copy(alpha = 0.14f), hazardColor, icon = Icons.Rounded.Warning)
                    if (d.containsBattery) Tag("Contains battery", SW.Amber.copy(alpha = 0.14f), SW.Amber, icon = Icons.Rounded.BatteryChargingFull)
                    if (d.neaRegulated) Tag("NEA regulated", SW.Mint.copy(alpha = 0.14f), SW.Mint, icon = Icons.Rounded.VerifiedUser)
                    Tag(if (d.engine == DetectionEngine.CLOUD) "Cloud AI" else d.engine.label, SW.Card2, SW.Subtle)
                }

                if (d.description.isNotBlank()) {
                    Spacer(Modifier.height(14.dp))
                    Text(d.description, style = MaterialTheme.typography.bodyMedium, color = SW.Subtle)
                }

                // Category correction (important for the on-device backup AI).
                if (d.engine == DetectionEngine.ON_DEVICE || d.category == null || d.confidence < 0.6f) {
                    Spacer(Modifier.height(16.dp))
                    Text("Not right? Choose the category", style = MaterialTheme.typography.titleSmall, color = SW.Text)
                    Spacer(Modifier.height(8.dp))
                    val session2 = session
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        EWasteCategory.entries.forEach { c ->
                            PillChip(c.shortTitle, selected = d.category == c, onClick = { session2.overrideCategory(c) }, leading = c.icon)
                        }
                    }
                }

                if (d.isEWaste && d.category != null) {
                    Spacer(Modifier.height(18.dp))
                    CompartmentCard(d.category)
                    Spacer(Modifier.height(18.dp))
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(SW.Card).border(1.dp, SW.Line, RoundedCornerShape(50)).padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        ResultTab.entries.forEach { t ->
                            val sel = t == tab
                            Box(
                                Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (sel) SW.Lime else Color.Transparent)
                                    .clickableNoRipple { tab = t }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(t.label, color = if (sel) SW.OnLime else SW.Muted, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    AnimatedContent(tab, label = "tab") { t ->
                        when (t) {
                            ResultTab.PREPARE -> StepsList(d.preparationSteps.ifEmpty { KnowledgeBase.guidance(d.category).preparation })
                            ResultTab.SAFETY -> WarningsList(d.safetyWarnings.ifEmpty { KnowledgeBase.guidance(d.category).safety })
                            ResultTab.VALUE -> ValueSection(d)
                        }
                    }
                } else if (!d.isEWaste) {
                    Spacer(Modifier.height(18.dp))
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(SW.Card).border(1.dp, SW.Line, RoundedCornerShape(24.dp)).padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBadge(Icons.Rounded.Info, SW.Mint)
                            Spacer(Modifier.width(12.dp))
                            Text("This doesn't look like e-waste", style = MaterialTheme.typography.titleMedium, color = SW.Text)
                        }
                        Spacer(Modifier.height(8.dp))
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
                    modifier = Modifier.padding(top = 16.dp),
                )
                Spacer(Modifier.height(170.dp))
            }
        }

        // ------------------------------------------------------- sticky actions
        if (d.isEWaste && d.category != null) {
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, SW.Sheet, SW.Sheet)))
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
            ) {
                Row {
                    PrimaryButton(
                        if (d.category.binFriendly) "Find nearest bin" else "Find take-back",
                        { onFindBin(d.category) }, Modifier.weight(1f), leading = Icons.Rounded.Place,
                    )
                    Spacer(Modifier.width(10.dp))
                    PrimaryButton(
                        "Sell", { onSell(d.category) }, container = SW.Card2, contentColor = SW.Amber, leading = Icons.Rounded.AttachMoney,
                    )
                }
                if (d.category.binFriendly) {
                    Spacer(Modifier.height(8.dp))
                    SecondaryButton("I'm at a bin: verify & earn points", onDeposit, Modifier.fillMaxWidth(), leading = Icons.Rounded.VerifiedUser, height = 50.dp)
                }
            }
        }
    }
}

@Composable
private fun CompartmentCard(category: EWasteCategory) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SW.OliveDark)
            .border(1.dp, SW.Lime.copy(alpha = 0.25f), RoundedCornerShape(24.dp))
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Goes into", color = SW.Muted, style = MaterialTheme.typography.labelMedium)
            Text(category.compartment, color = SW.Text, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(category.examples, color = SW.Subtle, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.width(12.dp))
        BinDiagram(category)
    }
}

/** Mini 2×2 diagram of the SmartWaste bin highlighting the target compartment. */
@Composable
private fun BinDiagram(category: EWasteCategory) {
    val cells = listOf(
        EWasteCategory.DEVICE_WITH_BATTERY, EWasteCategory.SMALL_BATTERY,
        EWasteCategory.APPLIANCE_NO_BATTERY, EWasteCategory.COMPONENT,
    )
    Column(
        Modifier.clip(RoundedCornerShape(12.dp)).background(SW.Bg.copy(alpha = 0.5f)).padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        cells.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { c ->
                    Box(
                        Modifier.size(28.dp).clip(RoundedCornerShape(7.dp))
                            .background(if (c == category) SW.Lime else SW.Card2),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(c.icon, null, tint = if (c == category) SW.OnLime else SW.Muted, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun StepsList(steps: List<String>) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(SW.Card).border(1.dp, SW.Line, RoundedCornerShape(24.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        steps.forEachIndexed { i, step ->
            Row(verticalAlignment = Alignment.Top) {
                Box(Modifier.size(26.dp).clip(CircleShape).background(SW.LimeSoft), contentAlignment = Alignment.Center) {
                    Text("${i + 1}", color = SW.Lime, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(12.dp))
                Text(step, style = MaterialTheme.typography.bodyMedium, color = SW.Text, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun WarningsList(warnings: List<String>) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(SW.Card).border(1.dp, SW.Line, RoundedCornerShape(24.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (warnings.isEmpty()) Text("No special hazards. Handle with normal care.", color = SW.Subtle)
        warnings.forEach { w ->
            Row(verticalAlignment = Alignment.Top) {
                IconBadge(Icons.Rounded.Warning, SW.Danger, size = 30.dp)
                Spacer(Modifier.width(12.dp))
                Text(w, style = MaterialTheme.typography.bodyMedium, color = SW.Text, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ValueSection(d: DetectionResult) {
    val level = when (d.resalePotential) {
        ResalePotential.NONE -> 0
        ResalePotential.LOW -> 1
        ResalePotential.MEDIUM -> 2
        ResalePotential.HIGH -> 3
    }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(SW.Card).border(1.dp, SW.Line, RoundedCornerShape(24.dp)).padding(18.dp)) {
        Text("Resale potential", style = MaterialTheme.typography.labelMedium, color = SW.Muted)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(d.estimatedValue ?: d.resalePotential.name.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.headlineSmall, color = SW.Text)
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) { i ->
                    Box(Modifier.size(width = 22.dp, height = 8.dp).clip(RoundedCornerShape(4.dp)).background(if (i < level) SW.Amber else SW.Card2))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            if (level >= 2) "This may still be worth money. Consider selling or trading it in before recycling."
            else "Low resale value. Recycling is the best option, and you'll earn points.",
            style = MaterialTheme.typography.bodySmall, color = SW.Subtle,
        )
        if (d.materials.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Text("Recoverable materials", style = MaterialTheme.typography.labelMedium, color = SW.Muted)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                d.materials.forEach { Tag(it, SW.LimeSoft, SW.Lime) }
            }
        }
    }
}

@Composable
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.then(Modifier.clickable(
        interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
        indication = null,
        onClick = onClick,
    ))
