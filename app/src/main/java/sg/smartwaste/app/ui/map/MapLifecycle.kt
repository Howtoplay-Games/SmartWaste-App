package sg.smartwaste.app.ui.map

/**
 * Drives a MapView's lifecycle calls in a valid order: start → resume → pause → stop, any
 * number of times, then destroy exactly once. Calls that would be out of order or repeated
 * are ignored, and nothing runs after destroy.
 *
 * MapLibre crashes natively if onDestroy runs twice or a destroyed map is started again, and
 * Compose can deliver lifecycle events and disposal in several orders, so the guard lives here.
 * Not thread-safe: use it from the main thread only.
 */
class MapLifecycle(
    private val onStart: () -> Unit,
    private val onResume: () -> Unit,
    private val onPause: () -> Unit,
    private val onStop: () -> Unit,
    private val onDestroy: () -> Unit,
) {
    private var started = false
    private var resumed = false

    var destroyed = false
        private set

    fun start() {
        if (destroyed || started) return
        onStart()
        started = true
    }

    fun resume() {
        if (destroyed || resumed) return
        start()
        onResume()
        resumed = true
    }

    fun pause() {
        if (!resumed) return
        onPause()
        resumed = false
    }

    fun stop() {
        pause()
        if (!started) return
        onStop()
        started = false
    }

    fun destroy() {
        if (destroyed) return
        stop()
        onDestroy()
        destroyed = true
    }
}
