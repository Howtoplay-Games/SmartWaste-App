package sg.smartwaste.app.util

/**
 * Lets a refresh start at most once every [minIntervalMs], and never while another one is
 * still running. `force = true` skips the interval but still never overlaps a running refresh.
 *
 * Not thread-safe: call it from one thread (ViewModels use the main thread).
 */
class RefreshGate(
    private val minIntervalMs: Long,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private var running = false
    private var lastStartAt: Long? = null

    /** Returns true if the caller should start a refresh now; call [finish] when it ends. */
    fun tryStart(force: Boolean = false): Boolean {
        if (running) return false
        val now = clock()
        val last = lastStartAt
        if (!force && last != null && now - last < minIntervalMs) return false
        running = true
        lastStartAt = now
        return true
    }

    fun finish() {
        running = false
    }
}
