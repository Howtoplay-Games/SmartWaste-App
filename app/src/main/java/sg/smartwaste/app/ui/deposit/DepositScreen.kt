package sg.smartwaste.app.ui.deposit

import androidx.camera.view.CameraController
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import sg.smartwaste.app.data.model.EWasteCategory
import sg.smartwaste.app.data.rewards.BinProof
import sg.smartwaste.app.data.rewards.PointsEngine
import sg.smartwaste.app.data.verify.VerificationLevel
import sg.smartwaste.app.data.verify.VerificationOutcome
import sg.smartwaste.app.ui.camera.CameraPreview
import sg.smartwaste.app.ui.camera.CodeAnalyzer
import sg.smartwaste.app.ui.camera.ScannedCode
import sg.smartwaste.app.ui.camera.capture
import sg.smartwaste.app.ui.camera.rememberCameraController
import sg.smartwaste.app.ui.components.CAMERA_PERMISSION
import sg.smartwaste.app.ui.components.CategoryTile
import sg.smartwaste.app.ui.components.CircleIconButton
import sg.smartwaste.app.ui.components.DragHandle
import sg.smartwaste.app.ui.components.FillLevelBar
import sg.smartwaste.app.ui.components.IconBadge
import sg.smartwaste.app.ui.components.LOCATION_PERMISSIONS
import sg.smartwaste.app.ui.components.PrimaryButton
import sg.smartwaste.app.ui.components.ScreenHeader
import sg.smartwaste.app.ui.components.SecondaryButton
import sg.smartwaste.app.ui.components.TintTag
import sg.smartwaste.app.ui.components.appViewModel
import sg.smartwaste.app.ui.components.icon
import sg.smartwaste.app.ui.components.rememberPermissionState
import sg.smartwaste.app.ui.components.tint
import sg.smartwaste.app.ui.scan.CornerBrackets
import sg.smartwaste.app.ui.theme.SW
import sg.smartwaste.app.util.Geo
import sg.smartwaste.app.util.ImageUtils
import java.io.File
import java.util.Locale

@Composable
fun DepositScreen(
    placeId: String?,
    qr: String?,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onRewards: () -> Unit,
) {
    val vm = appViewModel { DepositViewModel(it) }
    val s by vm.state.collectAsStateWithLifecycle()
    val location = rememberPermissionState(*LOCATION_PERMISSIONS)
    val camera = rememberPermissionState(CAMERA_PERMISSION)

    LaunchedEffect(Unit) {
        vm.init(placeId, qr)
        if (!location.granted) location.request()
    }

    val estimate = if (s.step == DepositStep.DONE) s.result?.total
    else vm.preview(VerificationLevel.ONLINE_VERIFIED) ?: s.category?.let { c -> c.basePoints * s.quantity * 2 }

    Column(Modifier.fillMaxSize().background(SW.Bg)) {
        // ---------------------------------------------------------- header
        ScreenHeader(
            title = "Verify deposit",
            subtitle = s.target?.place?.name ?: "Nearest bin is found automatically",
            onBack = onBack,
            trailing = {
                Text(
                    "+${estimate ?: 0}",
                    color = SW.Text, style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(SW.Surface).padding(horizontal = 12.dp, vertical = 12.dp),
                )
            },
        )

        // ----------------------------------------------------------- sheet
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(SW.Surface)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            DragHandle(Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp))
            Spacer(Modifier.height(16.dp))
            StepChips(s.step)
            Spacer(Modifier.height(20.dp))

            Text(
                if (s.step == DepositStep.DONE) "Points earned" else "You can earn up to",
                color = SW.Muted, style = MaterialTheme.typography.bodySmall,
            )
            Text(
                buildAnnotatedString {
                    append(if (estimate != null) "+$estimate" else "+0")
                    withStyle(SpanStyle(color = SW.Muted, fontSize = 18.sp, fontWeight = FontWeight.Medium)) { append(" pts") }
                },
                color = SW.Text, fontSize = 34.sp, fontWeight = FontWeight.SemiBold,
            )
            Text(
                s.category?.let { String.format(Locale.US, "≈ %.2f kg diverted · %.1f kg CO₂e avoided", PointsEngine.kgFor(it, s.quantity), PointsEngine.co2For(it, s.quantity)) }
                    ?: "Choose a category to see your points",
                color = SW.Muted, style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(20.dp))

            if (s.proof == BinProof.QR && s.sessionStart != null && s.step != DepositStep.DONE) {
                SmartBinSession(s.sessionStart!!, s.quantity)
                Spacer(Modifier.height(14.dp))
            }
            if (s.step != DepositStep.DONE) {
                CategoryGrid(s, vm)
                Spacer(Modifier.height(14.dp))
                FieldRow(
                    "Bin",
                    s.target?.place?.name ?: "Nearest bin (found automatically)",
                    s.target?.let { Geo.formatDistance(it.distanceMeters) } ?: "",
                    Icons.Outlined.Place,
                    "Proof",
                    when (s.proof) {
                        BinProof.QR -> "Smart-bin QR ✓"
                        BinProof.GPS -> "GPS confirmed ✓"
                        null -> "Not yet"
                    },
                    Icons.Outlined.VerifiedUser,
                )
                Spacer(Modifier.height(20.dp))
            }
            s.error?.let {
                ErrorBanner(it, vm::clearError)
                Spacer(Modifier.height(14.dp))
            }
            AnimatedContent(s.step, label = "step") { step ->
                when (step) {
                    DepositStep.LOCATION -> LocationStep(s, vm, cameraGranted = camera.granted, requestCamera = camera::request)
                    DepositStep.PHOTO -> PhotoStep(s, vm, cameraGranted = camera.granted, requestCamera = camera::request)
                    DepositStep.VERIFY -> VerifyStep(s, vm, cameraGranted = camera.granted, requestCamera = camera::request)
                    DepositStep.DONE -> DoneStep(s, onHome, onRewards)
                }
            }
            // Clears the AI button that floats over the bottom-right corner.
            Spacer(Modifier.height(96.dp))
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

/** Tab-style chips for the four steps. Finished steps get a check mark. */
@Composable
private fun StepChips(step: DepositStep) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DepositStep.entries.forEach { st ->
            val current = st == step
            val done = st.ordinal < step.ordinal
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (current) SW.Ink else SW.SurfaceAlt)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (done) {
                    Icon(Icons.Rounded.Check, null, tint = SW.Text, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    st.label, maxLines = 1, style = MaterialTheme.typography.labelMedium,
                    color = when {
                        current -> SW.OnInk
                        done -> SW.Text
                        else -> SW.Muted
                    },
                )
            }
        }
    }
}

/** Mirrors the smart bin's outer display: session timer, device counter and instructions. */
@Composable
private fun SmartBinSession(start: Long, count: Int) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(start) {
        while (true) { now = System.currentTimeMillis(); delay(1000) }
    }
    val secs = ((now - start) / 1000).coerceAtLeast(0)
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(SW.Ink).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Timer, null, tint = SW.OnInk, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(String.format(Locale.US, "%02d:%02d", secs / 60, secs % 60), color = SW.OnInk, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(14.dp))
        Text("NUM OF DEVICES: $count", color = SW.OnInk.copy(alpha = 0.7f), style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.weight(1f))
        Text("One item at a time", color = SW.OnInk.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
    }
}

/** 3 × 2 category grid plus quantity stepper. */
@Composable
private fun CategoryGrid(s: DepositUiState, vm: DepositViewModel) {
    Text("What are you recycling?", color = SW.Text, style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(12.dp))
    val cats = EWasteCategory.entries.filter { it.binFriendly }
    cats.chunked(3).forEach { row ->
        Row(Modifier.padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { c ->
                CategoryTile(
                    c.icon, c.shortTitle, selected = s.category == c, onClick = { vm.setCategory(c) },
                    modifier = Modifier.weight(1f), tint = c.tint, idleColor = SW.SurfaceAlt,
                )
            }
            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
    if (s.category != null) {
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(SW.SurfaceAlt).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("${s.category.compartment} · ${s.category.basePoints} pts each", style = MaterialTheme.typography.bodySmall, color = SW.Muted)
                Text(s.itemName.ifBlank { s.category.title }, style = MaterialTheme.typography.titleSmall, color = SW.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            CircleIconButton(Icons.Rounded.Remove, "Less", { vm.changeQuantity(-1) }, size = 34.dp, iconSize = 16.dp)
            Text("${s.quantity}", style = MaterialTheme.typography.titleMedium, color = SW.Text, modifier = Modifier.padding(horizontal = 12.dp))
            CircleIconButton(Icons.Rounded.Add, "More", { vm.changeQuantity(1) }, container = SW.Ink, content = SW.OnInk, size = 34.dp, iconSize = 16.dp)
        }
    }
}

/** Two side-by-side label/value fields. */
@Composable
private fun FieldRow(
    label1: String, value1: String, trailing1: String, icon1: ImageVector,
    label2: String, value2: String, icon2: ImageVector,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1.4f)) {
            Text(label1, color = SW.Muted, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(6.dp))
            Row(
                Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(18.dp)).background(SW.SurfaceAlt).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(value1, color = SW.Text, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (trailing1.isNotBlank()) Text(trailing1, color = SW.Muted, style = MaterialTheme.typography.labelSmall)
                }
                Icon(icon1, null, tint = SW.Text, modifier = Modifier.size(18.dp))
            }
        }
        Column(Modifier.weight(1f)) {
            Text(label2, color = SW.Muted, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(6.dp))
            Row(
                Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(18.dp)).background(SW.SurfaceAlt).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(value2, color = SW.Text, style = MaterialTheme.typography.labelMedium, maxLines = 1, modifier = Modifier.weight(1f), overflow = TextOverflow.Ellipsis)
                Icon(icon2, null, tint = SW.Text, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun ErrorBanner(text: String, onDismiss: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(SW.DangerSoft).clickable(onClick = onDismiss).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Error, null, tint = SW.Danger, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = SW.Text, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StepTitle(overline: String, title: String, body: String) {
    Text(overline, style = MaterialTheme.typography.bodySmall, color = SW.Muted)
    Text(title, style = MaterialTheme.typography.titleLarge, color = SW.Text)
    Spacer(Modifier.height(6.dp))
    Text(body, style = MaterialTheme.typography.bodyMedium, color = SW.Subtle)
    Spacer(Modifier.height(16.dp))
}

// ================================================================ step 1

@Composable
private fun LocationStep(s: DepositUiState, vm: DepositViewModel, cameraGranted: Boolean, requestCamera: () -> Unit) {
    var scanningQr by rememberSaveable { mutableStateOf(false) }
    Column {
        StepTitle(
            "Step 1 of 3",
            "Prove you're at the bin",
            "Confirm your GPS location (within ${PointsEngine.MAX_BIN_DISTANCE_M.toInt()} m of a bin), or scan the QR code on a SmartWaste smart bin for a 20% bonus.",
        )
        s.target?.let { t ->
            if (t.place.isSmartBin) {
                FillLevelBar(t.place.fillLevel)
                Spacer(Modifier.height(14.dp))
            }
        }
        if (scanningQr) {
            if (!cameraGranted) {
                PrimaryButton("Allow camera", requestCamera, Modifier.fillMaxWidth())
            } else {
                CodeCamera(readText = false, onCode = { code ->
                    if (code is ScannedCode.Bar) { scanningQr = false; vm.onQr(code.value) }
                })
            }
            Spacer(Modifier.height(10.dp))
            SecondaryButton("Use GPS instead", { scanningQr = false }, Modifier.fillMaxWidth())
        } else {
            PrimaryButton("Check my location", vm::checkLocation, Modifier.fillMaxWidth(), loading = s.checkingLocation, leading = Icons.Outlined.MyLocation)
            Spacer(Modifier.height(10.dp))
            SecondaryButton("Scan SmartWaste bin QR", { scanningQr = true }, Modifier.fillMaxWidth(), leading = Icons.Outlined.QrCodeScanner)
            if (s.proof == BinProof.QR && s.category != null) {
                Spacer(Modifier.height(10.dp))
                PrimaryButton("Continue", vm::continueAfterQrCategory, Modifier.fillMaxWidth())
            }
        }
    }
}

// ================================================================ step 2

@Composable
private fun PhotoStep(s: DepositUiState, vm: DepositViewModel, cameraGranted: Boolean, requestCamera: () -> Unit) {
    val context = LocalContext.current
    Column {
        StepTitle("Step 2 of 3", "Photo proof", "Take a photo of your item going into the bin opening. AI checks the photo shows an e-waste bin.")
        if (s.photoPath == null) {
            if (!cameraGranted) {
                PrimaryButton("Allow camera", requestCamera, Modifier.fillMaxWidth())
            } else {
                val controller = rememberCameraController(CameraController.IMAGE_CAPTURE)
                var busy by remember { mutableStateOf(false) }
                Box(Modifier.fillMaxWidth().aspectRatio(0.85f).clip(RoundedCornerShape(28.dp)).background(SW.Ink)) {
                    CameraPreview(controller, Modifier.fillMaxSize())
                    CornerBrackets(Modifier.fillMaxSize().padding(16.dp))
                    Box(
                        Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp).size(72.dp)
                            .border(4.dp, Color.White, CircleShape).padding(6.dp).clip(CircleShape)
                            .background(if (busy) SW.Track else Color.White)
                            .clickable(enabled = !busy) {
                                busy = true
                                controller.capture(
                                    context, ImageUtils.newPhotoFile(context, "deposit"),
                                    saved = { busy = false; vm.onPhoto(it) },
                                    failed = { busy = false },
                                )
                            },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Outlined.CameraAlt, "Take photo", tint = SW.Ink) }
                }
            }
        } else {
            Box(Modifier.fillMaxWidth().aspectRatio(0.85f).clip(RoundedCornerShape(28.dp)).background(SW.SurfaceAlt)) {
                AsyncImage(File(s.photoPath), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
            Spacer(Modifier.height(14.dp))
            val check = s.photoCheck
            when {
                s.checkingPhoto -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = SW.Ink)
                    Spacer(Modifier.width(10.dp))
                    Text("AI is checking your photo…", color = SW.Subtle)
                }
                check != null -> ResultRow(
                    ok = check.confirmed,
                    title = if (check.confirmed) "Bin confirmed by AI" else if (check.evaluated) "AI couldn't confirm the bin" else "AI unavailable",
                    subtitle = check.reason + if (!check.confirmed) " · You'll earn 40% fewer points." else "",
                )
            }
            Spacer(Modifier.height(14.dp))
            Row {
                SecondaryButton("Retake", vm::retakePhoto, Modifier.weight(1f), leading = Icons.Outlined.Refresh)
                Spacer(Modifier.width(10.dp))
                PrimaryButton("Continue", vm::continueToVerify, Modifier.weight(1f), enabled = !s.checkingPhoto && check != null)
            }
        }
    }
}

// ================================================================ step 3

@Composable
private fun VerifyStep(s: DepositUiState, vm: DepositViewModel, cameraGranted: Boolean, requestCamera: () -> Unit) {
    var manual by rememberSaveable { mutableStateOf("") }
    val full = vm.preview(VerificationLevel.ONLINE_VERIFIED)
    val none = vm.preview(VerificationLevel.UNVERIFIED)
    Column {
        StepTitle(
            "Step 3 of 3",
            "Verify your item",
            "Scan the product barcode, serial number label or IMEI (dial *#06# on phones). We check it online so you earn the full bonus.",
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TintTag("Verified: +${full ?: 0} pts", SW.Success, icon = Icons.Outlined.VerifiedUser)
            TintTag("Skip: +${none ?: 0} pts", SW.Subtle)
        }
        Spacer(Modifier.height(14.dp))

        val outcome = s.verification
        if (outcome == null) {
            if (!cameraGranted) {
                PrimaryButton("Allow camera", requestCamera, Modifier.fillMaxWidth())
            } else if (!s.verifying) {
                CodeCamera(readText = true, onCode = { vm.onCode(it.value) })
            }
            if (s.verifying) {
                Row(Modifier.fillMaxWidth().padding(vertical = 20.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = SW.Ink)
                    Spacer(Modifier.width(10.dp))
                    Text("Verifying online…", color = SW.Subtle)
                }
            }
            Spacer(Modifier.height(14.dp))
            Text("Note", color = SW.Muted, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = manual,
                onValueChange = { manual = it.take(32) },
                placeholder = { Text("Or type the barcode / serial / IMEI", color = SW.Muted) },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { vm.onCode(manual) }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SW.Ink, unfocusedBorderColor = SW.SurfaceAlt,
                    focusedContainerColor = SW.SurfaceAlt, unfocusedContainerColor = SW.SurfaceAlt,
                    focusedTextColor = SW.Text, unfocusedTextColor = SW.Text, cursorColor = SW.Ink,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Row {
                SecondaryButton("Skip (fewer points)", { vm.claim(skip = true) }, Modifier.weight(1f), enabled = !s.saving)
                Spacer(Modifier.width(10.dp))
                PrimaryButton("Verify", { vm.onCode(manual) }, Modifier.weight(1f), enabled = manual.length >= 6 && !s.verifying)
            }
        } else {
            VerificationCard(outcome)
            Spacer(Modifier.height(14.dp))
            val points = vm.preview(outcome.level) ?: 0
            PrimaryButton("Claim points • +$points", { vm.claim(skip = false) }, Modifier.fillMaxWidth(), loading = s.saving, leading = Icons.Outlined.EmojiEvents)
            Spacer(Modifier.height(10.dp))
            SecondaryButton("Scan a different code", vm::clearVerification, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun VerificationCard(o: VerificationOutcome) {
    val ok = o.level == VerificationLevel.ONLINE_VERIFIED || o.level == VerificationLevel.CHECKSUM_VALID || o.level == VerificationLevel.PLAUSIBLE
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(SW.SurfaceAlt).padding(16.dp)) {
        ResultRow(ok = ok, title = o.level.label, subtitle = o.details)
        Spacer(Modifier.height(12.dp))
        InfoLine("Type", o.codeType.label)
        InfoLine("Code", o.code)
        o.productTitle?.let { InfoLine("Product", it) }
        o.brand?.let { InfoLine("Brand", it) }
        InfoLine("Checked with", o.source)
        if (o.categoryMatch == false) {
            Spacer(Modifier.height(8.dp))
            TintTag("Product doesn't match the scanned item category", SW.Warn, icon = Icons.Rounded.Warning)
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = SW.Muted, modifier = Modifier.width(100.dp))
        Text(value, style = MaterialTheme.typography.labelMedium, color = SW.Text, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ResultRow(ok: Boolean, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconBadge(if (ok) Icons.Rounded.CheckCircle else Icons.Rounded.Warning, if (ok) SW.Success else SW.Warn, size = 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = SW.Text)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = SW.Subtle)
        }
    }
}

/** Live barcode / QR scanner with optional OCR for serial numbers and IMEIs. */
@Composable
private fun CodeCamera(readText: Boolean, onCode: (ScannedCode) -> Unit) {
    val controller = rememberCameraController(CameraController.IMAGE_ANALYSIS)
    var torch by remember { mutableStateOf(false) }
    val analyzer = remember(readText) { CodeAnalyzer(readText) { code -> onCode(code) } }
    DisposableEffect(controller, analyzer) {
        controller.setImageAnalysisAnalyzer(CodeAnalyzer.analysisExecutor, analyzer)
        onDispose {
            controller.clearImageAnalysisAnalyzer()
            analyzer.close()
        }
    }
    Box(Modifier.fillMaxWidth().aspectRatio(1.2f).clip(RoundedCornerShape(28.dp)).background(SW.Ink)) {
        CameraPreview(controller, Modifier.fillMaxSize())
        CornerBrackets(Modifier.fillMaxSize().padding(22.dp))
        Text(
            if (readText) "Barcode, serial (S/N) or IMEI" else "SmartWaste bin QR",
            color = Color.White, style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 14.dp).clip(RoundedCornerShape(50))
                .background(SW.Smoke).padding(horizontal = 12.dp, vertical = 6.dp),
        )
        Box(
            Modifier.align(Alignment.BottomEnd).padding(14.dp).size(44.dp).clip(CircleShape)
                .background(if (torch) Color.White else SW.Frost)
                .border(1.dp, SW.FrostLine, CircleShape)
                .clickable { torch = !torch; controller.enableTorch(torch) },
            contentAlignment = Alignment.Center,
        ) {
            Text("⚡", fontSize = 18.sp, color = if (torch) SW.Ink else Color.White)
        }
    }
}

// ================================================================ done

@Composable
private fun DoneStep(s: DepositUiState, onHome: () -> Unit, onRewards: () -> Unit) {
    val result = s.result ?: return
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(68.dp).clip(CircleShape).background(SW.Ink), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.EmojiEvents, null, tint = SW.OnInk, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text("Thank you for recycling responsibly!", color = SW.Text, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(SW.SurfaceAlt).padding(16.dp)) {
            Text("How your points were calculated", style = MaterialTheme.typography.titleSmall, color = SW.Text)
            Spacer(Modifier.height(8.dp))
            result.lines.forEach { line ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(line.label, style = MaterialTheme.typography.bodySmall, color = SW.Subtle, modifier = Modifier.weight(1f))
                    Text(
                        (if (line.points >= 0) "+" else "") + line.points,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (line.points >= 0) SW.Success else SW.Danger,
                    )
                }
            }
            Box(Modifier.fillMaxWidth().padding(vertical = 8.dp).height(1.dp).background(SW.Line))
            Row {
                Text("Total", style = MaterialTheme.typography.titleSmall, color = SW.Text, modifier = Modifier.weight(1f))
                Text("+${result.total} pts", style = MaterialTheme.typography.titleSmall, color = SW.Text)
            }
        }
        Spacer(Modifier.height(16.dp))
        Row {
            SecondaryButton("Home", onHome, Modifier.weight(1f), leading = Icons.Outlined.Home)
            Spacer(Modifier.width(10.dp))
            PrimaryButton("Rewards", onRewards, Modifier.weight(1f), leading = Icons.Outlined.CardGiftcard)
        }
    }
}
