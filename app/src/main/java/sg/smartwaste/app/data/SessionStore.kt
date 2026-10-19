package sg.smartwaste.app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import sg.smartwaste.app.data.model.DetectionResult
import sg.smartwaste.app.data.model.EWasteCategory

/** In-memory state shared across screens: the latest AI detection flows into map and deposit. */
class SessionStore {
    private val _detection = MutableStateFlow<DetectionResult?>(null)
    val detection: StateFlow<DetectionResult?> = _detection.asStateFlow()

    fun setDetection(result: DetectionResult?) {
        _detection.value = result
    }

    fun overrideCategory(category: EWasteCategory) {
        _detection.value = _detection.value?.copy(category = category, isEWaste = true)
    }
}
