package sg.smartwaste.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Kitchen
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import sg.smartwaste.app.data.model.EWasteCategory
import sg.smartwaste.app.ui.theme.SW

val EWasteCategory.icon: ImageVector
    get() = when (this) {
        EWasteCategory.DEVICE_WITH_BATTERY -> Icons.Rounded.Smartphone
        EWasteCategory.APPLIANCE_NO_BATTERY -> Icons.Rounded.Print
        EWasteCategory.SMALL_BATTERY -> Icons.Rounded.BatteryChargingFull
        EWasteCategory.COMPONENT -> Icons.Rounded.Memory
        EWasteCategory.LAMP -> Icons.Rounded.Lightbulb
        EWasteCategory.LARGE_APPLIANCE -> Icons.Rounded.Kitchen
    }

val EWasteCategory.tint: Color
    get() = when (this) {
        EWasteCategory.DEVICE_WITH_BATTERY -> SW.Electric
        EWasteCategory.APPLIANCE_NO_BATTERY -> Color(0xFF7C3AED)
        EWasteCategory.SMALL_BATTERY -> SW.Amber
        EWasteCategory.COMPONENT -> SW.Emerald
        EWasteCategory.LAMP -> Color(0xFFEAB308)
        EWasteCategory.LARGE_APPLIANCE -> Color(0xFF0E7490)
    }
