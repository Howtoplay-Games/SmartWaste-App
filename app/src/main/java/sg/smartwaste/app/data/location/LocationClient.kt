package sg.smartwaste.app.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import sg.smartwaste.app.util.GeoPoint

data class LocationFix(
    val point: GeoPoint,
    val accuracyMeters: Float,
    val bearing: Float?,
    val speedMps: Float?,
    val isMock: Boolean,
    val time: Long,
)

class LocationClient(private val context: Context) {

    private val fused = LocationServices.getFusedLocationProviderClient(context)

    private val _last = MutableStateFlow<LocationFix?>(null)
    val last: StateFlow<LocationFix?> = _last.asStateFlow()

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    suspend fun current(): LocationFix? {
        if (!hasPermission()) return null
        return runCatching {
            val token = CancellationTokenSource()
            val loc = withTimeoutOrNull(12_000) {
                fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token.token).await()
            } ?: fused.lastLocation.await()
            if (loc == null) token.cancel()
            loc?.toFix()?.also { _last.value = it }
        }.getOrNull()
    }

    @SuppressLint("MissingPermission")
    fun updates(intervalMs: Long = 2_000): Flow<LocationFix> = callbackFlow {
        if (!hasPermission()) {
            close()
            return@callbackFlow
        }
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(1_000)
            .build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.toFix()?.let {
                    _last.value = it
                    trySend(it)
                }
            }
        }
        fused.requestLocationUpdates(request, callback, Looper.getMainLooper())
        awaitClose { fused.removeLocationUpdates(callback) }
    }

    private fun Location.toFix() = LocationFix(
        point = GeoPoint(latitude, longitude),
        accuracyMeters = accuracy,
        bearing = if (hasBearing()) bearing else null,
        speedMps = if (hasSpeed()) speed else null,
        isMock = if (Build.VERSION.SDK_INT >= 31) isMock else @Suppress("DEPRECATION") isFromMockProvider,
        time = time,
    )
}
