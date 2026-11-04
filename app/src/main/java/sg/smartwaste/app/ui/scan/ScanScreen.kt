package sg.smartwaste.app.ui.scan

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.CameraController
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import sg.smartwaste.app.ui.camera.CameraPreview
import sg.smartwaste.app.ui.camera.CameraStatus
import sg.smartwaste.app.ui.camera.CodeAnalyzer
import sg.smartwaste.app.ui.camera.NoCameraMessage
import sg.smartwaste.app.ui.camera.ScannedCode
import sg.smartwaste.app.ui.camera.SwitchCameraButton
import sg.smartwaste.app.ui.camera.capture
import sg.smartwaste.app.ui.camera.rememberCameraHandle
import sg.smartwaste.app.ui.components.CAMERA_PERMISSION
import sg.smartwaste.app.ui.components.PillChip
import sg.smartwaste.app.ui.components.PrimaryButton
import sg.smartwaste.app.ui.components.appViewModel
import sg.smartwaste.app.ui.components.bottomContentPadding
import sg.smartwaste.app.ui.components.rememberPermissionState
import sg.smartwaste.app.ui.theme.SW
import sg.smartwaste.app.util.ImageUtils
import java.io.File

private enum class ScanMode(val label: String) { ITEM("Identify item"), BIN_QR("Scan bin QR") }

@Composable
fun ScanScreen(
    onResult: () -> Unit,
    onBinQr: (String) -> Unit,
) {
    val vm = appViewModel { ScanViewModel(it) }
    val s by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var mode by rememberSaveable { mutableStateOf(ScanMode.ITEM) }
    val camera = rememberPermissionState(CAMERA_PERMISSION)

    LaunchedEffect(Unit) { if (!camera.granted) camera.request() }

    var handledVersion by rememberSaveable { mutableStateOf(s.resultVersion) }
    LaunchedEffect(s.resultVersion) {
        if (s.resultVersion != handledVersion) {
            handledVersion = s.resultVersion
            onResult()
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.analyzePicked(context.applicationContext, uri)
    }

    val header: @Composable () -> Unit = {
        ScanHeader(mode, onMode = { mode = it })
    }
    val cameraCard: @Composable () -> Unit = {
        // Always fits the space it is given: on tablets a full-width 4:5 box is taller than
        // the screen and pushes the shutter under the bottom bar.
        Box(
            Modifier
                .widthIn(max = 560.dp)
                .aspectRatio(0.8f)
                .clip(RoundedCornerShape(34.dp))
                .background(SW.Ink),
            contentAlignment = Alignment.Center,
        ) {
            if (!camera.granted) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Icon(Icons.Outlined.CameraAlt, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(42.dp))
                    Spacer(Modifier.height(10.dp))
                    Text("Camera access is needed to scan", color = Color.White, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(14.dp))
                    PrimaryButton("Allow camera", { camera.request() }, height = 48.dp, container = SW.Surface, contentColor = SW.Ink)
                }
            } else if (s.analyzing && s.photoPath != null) {
                AsyncImage(
                    model = File(s.photoPath!!), contentDescription = null,
                    contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
                )
                ScanLineOverlay()
            } else if (mode == ScanMode.ITEM) {
                ItemCamera(
                    onCaptured = { vm.analyze(it) },
                    onGallery = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                )
            } else {
                QrCamera(onBinQr)
            }
            CornerBrackets(Modifier.fillMaxSize().padding(18.dp), color = Color.White)
        }
    }
    val status: @Composable () -> Unit = {
        AnimatedVisibility(s.analyzing, enter = fadeIn(), exit = fadeOut()) {
            Row(
                Modifier.clip(RoundedCornerShape(50)).background(SW.Surface).padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(Modifier.size(18.dp), color = SW.Ink, strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text(s.stage, color = SW.Text, style = MaterialTheme.typography.labelMedium)
            }
        }
        s.error?.let {
            Text(
                it, color = SW.Danger, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp).clickable { vm.clearError() },
            )
        }
    }
    // Space the floating bottom bar takes (92 dp above the nav-bar inset, plus a small gap).
    val barReserve = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 100.dp

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(SW.Bg)
            .statusBarsPadding(),
    ) {
        if (maxWidth >= 600.dp && maxWidth > maxHeight) {
            // Landscape tablet: camera on the left, title and status on the right.
            Row(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
                Box(
                    Modifier.weight(1f).fillMaxHeight().padding(top = 16.dp, bottom = barReserve),
                    contentAlignment = Alignment.Center,
                ) { cameraCard() }
                Spacer(Modifier.width(24.dp))
                Column(
                    Modifier
                        .width(340.dp)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(top = 24.dp, bottom = bottomContentPadding()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    header()
                    status()
                }
            }
        } else {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                header()
                Spacer(Modifier.height(20.dp))
                Box(
                    Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center,
                ) { cameraCard() }
                Column(
                    Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(top = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) { status() }
                Spacer(Modifier.height(barReserve))
            }
        }
    }
}

@Composable
private fun ScanHeader(mode: ScanMode, onMode: (ScanMode) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(14.dp))
        Text(
            if (mode == ScanMode.ITEM) "Scan e-waste" else "Scan bin QR code",
            color = SW.Text, style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            if (mode == ScanMode.ITEM) "Point your camera at the item. AI will identify it and guide you."
            else "Point at the \"SCAN ME TO GET POINTS!\" QR on a SmartWaste bin.",
            color = SW.Muted, style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 40.dp),
        )
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ScanMode.entries.forEach { m ->
                PillChip(m.label, selected = mode == m, onClick = { onMode(m) },
                    leading = if (m == ScanMode.ITEM) Icons.Outlined.CameraAlt else Icons.Outlined.QrCodeScanner)
            }
        }
    }
}

@Composable
private fun ItemCamera(onCaptured: (File) -> Unit, onGallery: () -> Unit) {
    val context = LocalContext.current
    val handle = rememberCameraHandle(CameraController.IMAGE_CAPTURE)
    // Torch state belongs to one camera, so it resets when the user switches.
    var torch by remember(handle.lens) { mutableStateOf(false) }
    var capturing by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        CameraPreview(handle, Modifier.fillMaxSize())
        if (handle.status == CameraStatus.NO_CAMERA) {
            NoCameraMessage("You can still pick a photo of the item from your gallery.", Modifier.fillMaxSize()) {
                Spacer(Modifier.height(14.dp))
                PrimaryButton(
                    "Choose from gallery", onGallery, height = 48.dp,
                    container = SW.Surface, contentColor = SW.Ink, leading = Icons.Outlined.PhotoLibrary,
                )
            }
        } else {
            if (handle.canSwitch) {
                SwitchCameraButton(handle, Modifier.align(Alignment.TopEnd).padding(top = 30.dp, end = 30.dp), size = 44.dp)
            }
            Row(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 22.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(26.dp),
            ) {
                GlassButton(Icons.Outlined.PhotoLibrary, "Gallery", onGallery)
                val ready = handle.status == CameraStatus.READY
                Box(
                    Modifier
                        .size(78.dp)
                        .border(4.dp, Color.White, CircleShape)
                        .padding(7.dp)
                        .clip(CircleShape)
                        .background(if (capturing || !ready) SW.Track else Color.White)
                        .clickable(enabled = !capturing && ready) {
                            capturing = true
                            handle.controller.capture(
                                context, ImageUtils.newPhotoFile(context, "scan"),
                                saved = { capturing = false; onCaptured(it) },
                                failed = { capturing = false },
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.CameraAlt, "Capture", tint = SW.Ink, modifier = Modifier.size(28.dp))
                }
                if (handle.hasFlash) {
                    GlassButton(if (torch) Icons.Outlined.FlashOn else Icons.Outlined.FlashOff, "Flash") {
                        torch = !torch
                        handle.controller.enableTorch(torch)
                    }
                } else {
                    // Most tablets have no flash; keep the shutter centred.
                    Spacer(Modifier.size(52.dp))
                }
            }
        }
    }
}

@Composable
private fun QrCamera(onBinQr: (String) -> Unit) {
    val handle = rememberCameraHandle(CameraController.IMAGE_ANALYSIS)
    val controller = handle.controller
    var found by remember { mutableStateOf<String?>(null) }
    var invalid by remember { mutableStateOf(false) }
    val analyzer = remember {
        CodeAnalyzer(readText = false) { code ->
            if (code is ScannedCode.Bar) {
                val v = code.value
                if (v.startsWith("SMARTWASTE:BIN:", ignoreCase = true) || v.contains("smartwaste", ignoreCase = true)) {
                    found = v
                } else invalid = true
            }
        }
    }
    DisposableEffect(controller) {
        controller.setImageAnalysisAnalyzer(CodeAnalyzer.analysisExecutor, analyzer)
        onDispose {
            controller.clearImageAnalysisAnalyzer()
            analyzer.close()
        }
    }
    LaunchedEffect(found) {
        found?.let {
            analyzer.pause(true)
            kotlinx.coroutines.delay(700)
            onBinQr(it)
        }
    }
    Box(Modifier.fillMaxSize()) {
        CameraPreview(handle, Modifier.fillMaxSize())
        if (handle.status == CameraStatus.NO_CAMERA) {
            NoCameraMessage("Bin QR scanning needs a camera. You can confirm the bin by GPS when you deposit.", Modifier.fillMaxSize())
        } else {
            if (handle.canSwitch) {
                SwitchCameraButton(handle, Modifier.align(Alignment.TopEnd).padding(top = 30.dp, end = 30.dp), size = 44.dp)
            }
            Column(Modifier.align(Alignment.BottomCenter).padding(bottom = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (found != null) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = Color.White, modifier = Modifier.size(34.dp))
                    Spacer(Modifier.height(6.dp))
                    Text("QR code identified", color = Color.White, style = MaterialTheme.typography.titleSmall)
                } else {
                    Text(
                        if (invalid) "That's not a SmartWaste bin QR" else "Looking for a SmartWaste QR…",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(SW.Smoke).padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun GlassButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(52.dp).clip(CircleShape).background(SW.Frost).border(1.dp, SW.FrostLine, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, label, tint = Color.White, modifier = Modifier.size(22.dp)) }
}

@Composable
fun CornerBrackets(modifier: Modifier = Modifier, color: Color = Color.White.copy(alpha = 0.9f)) {
    Canvas(modifier) {
        val len = 34.dp.toPx()
        val stroke = 4.dp.toPx()
        val w = size.width
        val h = size.height
        fun line(a: Offset, b: Offset) = drawLine(color, a, b, strokeWidth = stroke, cap = StrokeCap.Round)
        line(Offset(0f, 0f), Offset(len, 0f)); line(Offset(0f, 0f), Offset(0f, len))
        line(Offset(w, 0f), Offset(w - len, 0f)); line(Offset(w, 0f), Offset(w, len))
        line(Offset(0f, h), Offset(len, h)); line(Offset(0f, h), Offset(0f, h - len))
        line(Offset(w, h), Offset(w - len, h)); line(Offset(w, h), Offset(w, h - len))
    }
}

@Composable
private fun ScanLineOverlay() {
    val transition = rememberInfiniteTransition(label = "scan")
    val y by transition.animateFloat(0.05f, 0.95f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "y")
    Canvas(Modifier.fillMaxSize()) {
        drawRect(Color(0x551F2023))
        val yy = size.height * y
        drawRect(
            Brush.verticalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.3f), Color.Transparent), startY = yy - 60f, endY = yy + 60f),
            topLeft = Offset(0f, yy - 60f), size = androidx.compose.ui.geometry.Size(size.width, 120f),
        )
        drawLine(Color.White, Offset(0f, yy), Offset(size.width, yy), strokeWidth = 3f)
    }
}
