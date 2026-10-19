package sg.smartwaste.app.ui.navigation

import android.net.Uri

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val MAP = "map"
    const val SCAN = "scan"
    const val RESULT = "result"
    const val DEPOSIT = "deposit"
    const val REWARDS = "rewards"
    const val PROFILE = "profile"

    const val MAP_PATTERN = "$MAP?mode={mode}&category={category}&auto={auto}&place={place}&q={q}"
    const val DEPOSIT_PATTERN = "$DEPOSIT?place={place}&qr={qr}"

    /** mode: "bins" | "buyers" | null (all). auto: pick the nearest match and route to it. */
    fun map(mode: String? = null, category: String? = null, auto: Boolean = false, place: String? = null, query: String? = null): String =
        "$MAP?mode=${mode.orEmpty()}&category=${category.orEmpty()}&auto=$auto&place=${place.orEmpty()}&q=${Uri.encode(query.orEmpty())}"

    fun deposit(place: String? = null, qr: String? = null): String =
        "$DEPOSIT?place=${place.orEmpty()}&qr=${Uri.encode(qr.orEmpty())}"
}
