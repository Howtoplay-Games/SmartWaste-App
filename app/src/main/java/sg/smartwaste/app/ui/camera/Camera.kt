package sg.smartwaste.app.ui.camera

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import sg.smartwaste.app.data.verify.ProductVerifier
import sg.smartwaste.app.util.FrameThrottle
import java.io.File
import java.util.concurrent.Executors

enum class CameraStatus { STARTING, READY, NO_CAMERA }

/**
 * A CameraX controller that picks a camera the device actually has.
 *
 * `LifecycleCameraController` always asks for the back camera. On a tablet with only a front
 * (or external) camera that bind fails, which shows a black preview or crashes. This handle waits
 * for CameraX to start, checks which cameras exist, binds the first one that works and lets the
 * user switch between them.
 */
@Stable
class CameraHandle internal constructor(
    val controller: LifecycleCameraController,
    private val context: Context,
) {
    var status by mutableStateOf(CameraStatus.STARTING)
        private set
    var lens by mutableStateOf<CameraLens?>(null)
        private set
    var lenses by mutableStateOf<List<CameraLens>>(emptyList())
        private set
    var hasFlash by mutableStateOf(false)
        private set
    val canSwitch: Boolean get() = status == CameraStatus.READY && lenses.size > 1

    private var owner: LifecycleOwner? = null
    private var session = 0

    internal fun start(owner: LifecycleOwner) {
        this.owner = owner
        val mine = ++session
        status = CameraStatus.STARTING
        controller.initializationFuture.addListener({
            if (mine != session) return@addListener
            lenses = CameraLenses.available(
                hasBack = has(CameraSelector.DEFAULT_BACK_CAMERA),
                hasFront = has(CameraSelector.DEFAULT_FRONT_CAMERA),
                hasAny = has(ANY_CAMERA),
            )
            val started = CameraLenses.startOrder(lenses, lens).firstOrNull { bind(it) }
            lens = started
            status = if (started != null) CameraStatus.READY else CameraStatus.NO_CAMERA
        }, ContextCompat.getMainExecutor(context))
    }

    internal fun stop() {
        session++
        owner = null
        controller.unbind()
    }

    fun switchCamera() {
        val current = lens ?: return
        val next = CameraLenses.next(current, lenses) ?: return
        if (bind(next)) {
            lens = next
        } else {
            lenses = lenses - next
            if (!bind(current)) {
                lens = null
                status = CameraStatus.NO_CAMERA
            }
        }
    }

    /** Called once frames arrive: the camera is bound for real, so its flash info can be read. */
    internal fun refreshInfo() {
        hasFlash = runCatching { controller.cameraInfo?.hasFlashUnit() == true }.getOrDefault(false)
    }

    /** `hasCamera` throws if CameraX failed to start, which also means there is no usable camera. */
    private fun has(selector: CameraSelector) = runCatching { controller.hasCamera(selector) }.getOrDefault(false)

    private fun bind(target: CameraLens): Boolean {
        val owner = owner ?: return false
        hasFlash = false
        return try {
            controller.unbind()
            controller.cameraSelector = selectorFor(target)
            controller.bindToLifecycle(owner)
            refreshInfo()
            true
        } catch (e: RuntimeException) {
            Log.w(TAG, "Camera $target could not start", e)
            controller.unbind()
            false
        }
    }

    private companion object {
        const val TAG = "CameraHandle"

        /** First camera CameraX reports, whatever way it faces (external USB cameras on Chromebooks/kiosks). */
        val ANY_CAMERA: CameraSelector = CameraSelector.Builder().addCameraFilter { cameras -> cameras.take(1) }.build()

        fun selectorFor(lens: CameraLens): CameraSelector = when (lens) {
            CameraLens.BACK -> CameraSelector.DEFAULT_BACK_CAMERA
            CameraLens.FRONT -> CameraSelector.DEFAULT_FRONT_CAMERA
            CameraLens.OTHER -> ANY_CAMERA
        }
    }
}

@Composable
fun rememberCameraHandle(useCases: Int = CameraController.IMAGE_CAPTURE): CameraHandle {
    val context = LocalContext.current
    return remember(useCases) {
        val controller = LifecycleCameraController(context).apply {
            setEnabledUseCases(useCases)
            imageAnalysisBackpressureStrategy = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
        }
        CameraHandle(controller, context.applicationContext)
    }
}

/** CameraX preview bound to the current lifecycle, using whichever camera [handle] picked. */
@Composable
fun CameraPreview(handle: CameraHandle, modifier: Modifier = Modifier) {
    val owner = LocalLifecycleOwner.current
    val streaming = remember(handle) {
        Observer<PreviewView.StreamState> { if (it == PreviewView.StreamState.STREAMING) handle.refreshInfo() }
    }
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                scaleType = PreviewView.ScaleType.FILL_CENTER
                controller = handle.controller
                previewStreamState.observeForever(streaming)
            }
        },
        onRelease = { it.previewStreamState.removeObserver(streaming) },
    )
    DisposableEffect(owner, handle) {
        handle.start(owner)
        onDispose { handle.stop() }
    }
}

fun LifecycleCameraController.capture(
    context: Context,
    file: File,
    saved: (File) -> Unit,
    failed: (String) -> Unit,
) {
    val options = ImageCapture.OutputFileOptions.Builder(file).build()
    takePicture(options, ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageSavedCallback {
        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
            saved(file)
        }

        override fun onError(exception: ImageCaptureException) {
            failed(exception.message ?: "Capture failed")
        }
    })
}

sealed interface ScannedCode {
    val value: String

    data class Bar(override val value: String, val format: Int) : ScannedCode
    data class Text(override val value: String) : ScannedCode
}

/** Pulls a serial number / IMEI out of OCR text from a device label. */
object SerialExtractor {
    private val labelled = Regex(
        "(?:S/?N|SERIAL(?:\\s*(?:NO\\.?|NUMBER))?|IMEI\\s*\\d?|MEID)\\s*[:#.]?\\s*([A-Z0-9][A-Z0-9-]{5,23})",
        RegexOption.IGNORE_CASE,
    )
    private val fifteenDigits = Regex("(?<!\\d)(\\d{15})(?!\\d)")

    fun extract(text: String): String? {
        labelled.find(text)?.let { return it.groupValues[1].uppercase() }
        fifteenDigits.findAll(text).map { it.value }.firstOrNull { ProductVerifier.luhnValid(it) }?.let { return it }
        return null
    }
}

/**
 * Live analyzer that finds barcodes/QR codes (ML Kit) and, optionally, serial numbers/IMEIs
 * via on-device OCR. A text candidate must be seen in two consecutive frames to be reported.
 */
class CodeAnalyzer(
    private val readText: Boolean,
    private val onCode: (ScannedCode) -> Unit,
) : ImageAnalysis.Analyzer {

    private val barcodes = BarcodeScanning.getClient()
    private val textRecognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    /** Caps the whole ML pipeline; without it the analyzer starts a new pass as soon as one ends. */
    private val frameThrottle = FrameThrottle(FRAME_INTERVAL_MS)
    /** OCR is far more expensive than barcode scanning, so it runs on a few frames per second only. */
    private val ocrThrottle = FrameThrottle(OCR_INTERVAL_MS)
    @Volatile private var busy = false
    @Volatile private var paused = false
    private var lastTextCandidate: String? = null

    fun pause(value: Boolean) {
        paused = value
    }

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    override fun analyze(proxy: ImageProxy) {
        val media = proxy.image
        if (media == null || busy || paused || !frameThrottle.tryAcquire(SystemClock.elapsedRealtime())) {
            proxy.close(); return
        }
        busy = true
        val input = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
        barcodes.process(input).addOnCompleteListener { task ->
            val code = if (task.isSuccessful) task.result.firstOrNull { !it.rawValue.isNullOrBlank() } else null
            if (code != null) {
                if (!paused) onCode(ScannedCode.Bar(code.rawValue!!.trim(), code.format))
                finish(proxy)
            } else if (readText && ocrThrottle.tryAcquire(SystemClock.elapsedRealtime())) {
                textRecognizer.process(input).addOnCompleteListener { t ->
                    if (t.isSuccessful) {
                        val candidate = SerialExtractor.extract(t.result.text)
                        if (candidate != null && candidate == lastTextCandidate && !paused) onCode(ScannedCode.Text(candidate))
                        lastTextCandidate = candidate
                    }
                    finish(proxy)
                }
            } else {
                finish(proxy)
            }
        }
    }

    private fun finish(proxy: ImageProxy) {
        busy = false
        proxy.close()
    }

    fun close() {
        barcodes.close()
        if (readText) textRecognizer.close()
    }

    companion object {
        /** Every 3rd frame of a 30 fps camera (~10 per second): plenty for barcodes and QR codes. */
        const val FRAME_INTERVAL_MS = 90L

        /** About 3 OCR passes per second is enough to read a held-still label. */
        const val OCR_INTERVAL_MS = 350L

        val analysisExecutor = Executors.newSingleThreadExecutor()
        fun isQr(code: ScannedCode) = code is ScannedCode.Bar && code.format == Barcode.FORMAT_QR_CODE
    }
}
