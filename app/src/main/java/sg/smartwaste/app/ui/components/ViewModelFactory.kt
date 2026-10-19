package sg.smartwaste.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import sg.smartwaste.app.AppContainer
import sg.smartwaste.app.SmartWasteApplication

@Composable
fun appContainer(): AppContainer =
    (LocalContext.current.applicationContext as SmartWasteApplication).container

/** Creates a ViewModel scoped to the current navigation entry, with access to the [AppContainer]. */
@Composable
inline fun <reified VM : ViewModel> appViewModel(crossinline factory: (AppContainer) -> VM): VM {
    val container = appContainer()
    return viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = factory(container) as T
        }
    )
}
