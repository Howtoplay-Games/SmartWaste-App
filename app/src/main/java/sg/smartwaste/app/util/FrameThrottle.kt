package sg.smartwaste.app.util

/** Allows an expensive per-frame step (such as OCR) at most once every [minIntervalMs]. */
class FrameThrottle(private val minIntervalMs: Long) {
    @Volatile
    private var lastRunAt: Long? = null

    /** Returns true if the step should run for the frame seen at [nowMs]. */
    fun tryAcquire(nowMs: Long): Boolean {
        val last = lastRunAt
        if (last != null && nowMs - last < minIntervalMs) return false
        lastRunAt = nowMs
        return true
    }
}
