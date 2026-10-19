package sg.smartwaste.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max

object ImageUtils {

    fun newPhotoFile(context: Context, prefix: String): File {
        val dir = File(context.filesDir, "photos").apply { mkdirs() }
        return File(dir, "${prefix}_${System.currentTimeMillis()}.jpg")
    }

    /** Copies a picked gallery image into app storage so it can be re-read later. */
    fun copyUriToFile(context: Context, uri: Uri, prefix: String): File? = runCatching {
        val target = newPhotoFile(context, prefix)
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        target
    }.getOrNull()

    /** Decodes a JPEG downscaled to [maxDim] on the long edge and rotated upright using EXIF. */
    fun loadUprightBitmap(file: File, maxDim: Int): Bitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxDim) sample *= 2
        val decoded = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: return null

        val scale = maxDim.toFloat() / max(decoded.width, decoded.height)
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                decoded,
                (decoded.width * scale).toInt().coerceAtLeast(1),
                (decoded.height * scale).toInt().coerceAtLeast(1),
                true
            )
        } else decoded

        val rotation = runCatching { ExifInterface(file.path).rotationDegrees }.getOrDefault(0)
        if (rotation == 0) scaled
        else Bitmap.createBitmap(
            scaled, 0, 0, scaled.width, scaled.height,
            Matrix().apply { postRotate(rotation.toFloat()) }, true
        )
    }.getOrNull()

    /** Encodes the photo as a base64 JPEG suitable for a vision-model data URL. */
    fun toBase64Jpeg(file: File, maxDim: Int = 1024, quality: Int = 82): String? {
        val bitmap = loadUprightBitmap(file, maxDim) ?: return null
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }
}
