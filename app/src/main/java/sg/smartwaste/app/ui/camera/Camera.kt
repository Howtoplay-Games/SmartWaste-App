package sg.smartwaste.app.ui.camera

import android.content.Context
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import sg.smartwaste.app.data.verify.ProductVerifier
import java.io.File
import java.util.concurrent.Executors

@Composable
fun rememberCameraController(useCases: Int = CameraController.IMAGE_CAPTURE): LifecycleCameraController {
    val context = LocalContext.current
    return remember(useCases) {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(useCases)
            imageAnalysisBackpressureStrategy = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
        }
    }
}

/** CameraX preview bound to the current lifecycle. */
@Composable
fun CameraPreview(controller: LifecycleCameraController, modifier: Modifier = Modifier) {
    val owner = LocalLifecycleOwner.current
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                scaleType = PreviewView.ScaleType.FILL_CENTER
                this.controller = controller
            }
        },
    )
    DisposableEffect(owner, controller) {
        controller.bindToLifecycle(owner)
        onDispose { controller.unbind() }
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
    @Volatile private var busy = false
    @Volatile private var paused = false
    private var lastTextCandidate: String? = null

    fun pause(value: Boolean) {
        paused = value
    }

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    override fun analyze(proxy: ImageProxy) {
        val media = proxy.image
        if (media == null || busy || paused) {
            proxy.close(); return
        }
        busy = true
        val input = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
        barcodes.process(input).addOnCompleteListener { task ->
            val code = if (task.isSuccessful) task.result.firstOrNull { !it.rawValue.isNullOrBlank() } else null
            if (code != null) {
                if (!paused) onCode(ScannedCode.Bar(code.rawValue!!.trim(), code.format))
                finish(proxy)
            } else if (readText) {
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
        val analysisExecutor = Executors.newSingleThreadExecutor()
        fun isQr(code: ScannedCode) = code is ScannedCode.Bar && code.format == Barcode.FORMAT_QR_CODE
    }
}
