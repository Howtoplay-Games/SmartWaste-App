package sg.smartwaste.app.ui.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import sg.smartwaste.app.AppContainer
import java.io.File

data class ScanUiState(
    val analyzing: Boolean = false,
    val photoPath: String? = null,
    val stage: String = "",
    val error: String? = null,
    /** Incremented when a detection is ready so the screen can navigate once. */
    val resultVersion: Int = 0,
)

class ScanViewModel(private val c: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(ScanUiState())
    val state: StateFlow<ScanUiState> = _state.asStateFlow()

    private val stages = listOf(
        "Uploading photo securely…",
        "Identifying the item…",
        "Classifying e-waste category…",
        "Checking battery & safety hazards…",
        "Estimating resale value…",
    )

    fun analyze(file: File) {
        if (_state.value.analyzing) return
        _state.update { it.copy(analyzing = true, photoPath = file.path, error = null, stage = stages.first()) }
        val ticker = viewModelScope.launch {
            var i = 0
            while (isActive) {
                delay(1_400)
                i = (i + 1).coerceAtMost(stages.lastIndex)
                _state.update { it.copy(stage = stages[i]) }
            }
        }
        viewModelScope.launch {
            try {
                val result = c.ai.detect(file)
                c.session.setDetection(result)
                runCatching { c.history.recordScan(result) }
                _state.update { it.copy(analyzing = false, resultVersion = it.resultVersion + 1) }
            } catch (e: Exception) {
                _state.update { it.copy(analyzing = false, error = e.message ?: "Couldn't analyse the photo") }
            } finally {
                ticker.cancel()
            }
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }
}
