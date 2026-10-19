package sg.smartwaste.app.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

class PermissionState(
    private val context: Context,
    private val permissions: List<String>,
    private val launch: () -> Unit,
) {
    var granted by mutableStateOf(check())
        internal set

    fun check(): Boolean = permissions.any {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    fun request() = launch()
}

@Composable
fun rememberPermissionState(vararg permissions: String, onResult: (Boolean) -> Unit = {}): PermissionState {
    val context = LocalContext.current
    val list = permissions.toList()
    var state: PermissionState? = null
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        val ok = result.values.any { it }
        state?.granted = ok || state?.check() == true
        onResult(ok)
    }
    val s = remember(list) { PermissionState(context, list) { launcher.launch(list.toTypedArray()) } }
    state = s
    return s
}

val LOCATION_PERMISSIONS = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
const val CAMERA_PERMISSION = Manifest.permission.CAMERA
