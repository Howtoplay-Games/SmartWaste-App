package sg.smartwaste.app.ui.camera

/**
 * Which physical camera to use. Phones always have a back camera, but many tablets, Chromebooks
 * and kiosks only have a front camera or an external (USB) one.
 */
enum class CameraLens { BACK, FRONT, OTHER }

/** Pure camera-choice rules (no Android types, so they can be tested on the JVM). */
object CameraLenses {

    /** Cameras to offer, in order of preference. [CameraLens.OTHER] is only used when there is no back or front camera. */
    fun available(hasBack: Boolean, hasFront: Boolean, hasAny: Boolean): List<CameraLens> = buildList {
        if (hasBack) add(CameraLens.BACK)
        if (hasFront) add(CameraLens.FRONT)
        if (isEmpty() && hasAny) add(CameraLens.OTHER)
    }

    /** Order to try when starting: keep the camera the user last picked, then fall back to the rest. */
    fun startOrder(available: List<CameraLens>, previous: CameraLens?): List<CameraLens> =
        if (previous != null && previous in available) listOf(previous) + (available - previous) else available

    /** Camera the switch button moves to, or null when there is nothing to switch to. */
    fun next(current: CameraLens?, available: List<CameraLens>): CameraLens? {
        if (available.size < 2) return null
        val i = available.indexOf(current)
        return available[(i + 1) % available.size]
    }
}
