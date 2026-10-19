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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.QrCodeScanner
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
import sg.smartwaste.app.ui.camera.CodeAnalyzer
import sg.smartwaste.app.ui.camera.ScannedCode
import sg.smartwaste.app.ui.camera.capture
import sg.smartwaste.app.ui.camera.rememberCameraController
import sg.smartwaste.app.ui.components.CAMERA_PERMISSION
import sg.smartwaste.app.ui.components.PillChip
import sg.smartwaste.app.ui.components.PrimaryButton
import sg.smartwaste.app.ui.components.appViewModel
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
        if (uri != null) ImageUtils.copyUriToFile(context, uri, "scan")?.let { vm.analyze(it) }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SW.Navy, SW.Navy2)))
            .statusBarsPadding(),
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(12.dp))
            Text(
                if (mode == ScanMode.ITEM) "Scan e-waste" else "Scan bin QR code",
                color = Color.White, style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (mode == ScanMode.ITEM) "Point your camera at the item. AI will identify it and guide you."
                else "Point at the \"SCAN ME TO GET POINTS!\" QR on a SmartWaste bin.",
                color = Color.White.copy(alpha = 0.65f), style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 40.dp),
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ScanMode.entries.forEach { m ->
                    PillChip(m.label, selected = mode == m, onClick = { mode = m }, dark = true, selectedColor = SW.Electric,
                        leading = if (m == ScanMode.ITEM) Icons.Rounded.CameraAlt else Icons.Rounded.QrCodeScanner)
                }
            }
            Spacer(Modifier.height(20.dp))

            Box(
                Modifier
                    .padding(horizontal = 28.dp)
                    .fillMaxWidth()
                    .aspectRatio(0.82f)
                    .clip(RoundedCornerShape(34.dp))
                    .background(SW.Navy3),
                contentAlignment = Alignment.Center,
            ) {
                if (!camera.granted) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Icon(Icons.Rounded.CameraAlt, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(42.dp))
                        Spacer(Modifier.height(10.dp))
                        Text("Camera access is needed to scan", color = Color.White, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(14.dp))
                        PrimaryButton("Allow camera", { camera.request() }, container = SW.Electric, height = 48.dp)
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
                CornerBrackets(Modifier.fillMaxSize().padding(18.dp))
            }

            Spacer(Modifier.height(18.dp))
            AnimatedVisibility(s.analyzing, enter = fadeIn(), exit = fadeOut()) {
                Row(
                    Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.1f)).padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = SW.Lime, strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(s.stage, color = Color.White, style = MaterialTheme.typography.labelMedium)
                }
            }
            s.error?.let {
                Text(
                    it, color = SW.Coral, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp).clickable { vm.clearError() },
                )
            }
        }
    }
}

@Composable
private fun ItemCamera(onCaptured: (File) -> Unit, onGallery: () -> Unit) {
    val context = LocalContext.current
    val controller = rememberCameraController(CameraController.IMAGE_CAPTURE)
    var torch by remember { mutableStateOf(false) }
    var capturing by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        CameraPreview(controller, Modifier.fillMaxSize())
        Row(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(26.dp),
        ) {
            GlassButton(Icons.Rounded.PhotoLibrary, "Gallery", onGallery)
            Box(
                Modifier
                    .size(78.dp)
                    .border(4.dp, Color.White, CircleShape)
                    .padding(7.dp)
                    .clip(CircleShape)
                    .background(if (capturing) SW.Lime else SW.Electric)
                    .clickable(enabled = !capturing) {
                        capturing = true
                        controller.capture(
                            context, ImageUtils.newPhotoFile(context, "scan"),
                            saved = { capturing = false; onCaptured(it) },
                            failed = { capturing = false },
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.CameraAlt, "Capture", tint = Color.White, modifier = Modifier.size(28.dp))
            }
            GlassButton(if (torch) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff, "Flash") {
                torch = !torch
                controller.enableTorch(torch)
            }
        }
    }
}

@Composable
private fun QrCamera(onBinQr: (String) -> Unit) {
    val controller = rememberCameraController(CameraController.IMAGE_ANALYSIS)
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
        CameraPreview(controller, Modifier.fillMaxSize())
        Column(Modifier.align(Alignment.BottomCenter).padding(bottom = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (found != null) {
                Icon(Icons.Rounded.CheckCircle, null, tint = SW.Lime, modifier = Modifier.size(34.dp))
                Spacer(Modifier.height(6.dp))
                Text("QR code identified", color = Color.White, style = MaterialTheme.typography.titleSmall)
            } else {
                Text(
                    if (invalid) "That's not a SmartWaste bin QR" else "Looking for a SmartWaste QR…",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.4f)).padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun GlassButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(52.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.35f)).clickable(onClick = onClick),
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
        drawRect(Color(0x55071339))
        val yy = size.height * y
        drawRect(
            Brush.verticalGradient(listOf(Color.Transparent, SW.Lime.copy(alpha = 0.35f), Color.Transparent), startY = yy - 60f, endY = yy + 60f),
            topLeft = Offset(0f, yy - 60f), size = androidx.compose.ui.geometry.Size(size.width, 120f),
        )
        drawLine(SW.Lime, Offset(0f, yy), Offset(size.width, yy), strokeWidth = 3f)
    }
}
