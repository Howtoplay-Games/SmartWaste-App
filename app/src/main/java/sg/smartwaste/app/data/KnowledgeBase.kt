package sg.smartwaste.app.data

import sg.smartwaste.app.data.model.EWasteCategory

/**
 * Offline e-waste knowledge for Singapore. Used to enrich AI answers, as the content
 * for the on-device fallback, and to answer common chat questions without a network.
 */
object KnowledgeBase {

    data class Guidance(
        val preparation: List<String>,
        val safety: List<String>,
        val whereToGo: String,
    )

    fun guidance(category: EWasteCategory): Guidance = when (category) {
        EWasteCategory.DEVICE_WITH_BATTERY -> Guidance(
            preparation = listOf(
                "Back up your data, sign out of accounts (e.g. Find My / Google) and factory reset.",
                "Remove SIM and memory cards.",
                "If the battery is removable, take it out and put it in the battery chute separately.",
                "Drop the device in the e-waste bin one item at a time.",
            ),
            safety = listOf(
                "Do not deposit swollen, punctured or hot batteries. Bring them to a staffed collection point.",
                "Never put devices with batteries in blue recycling bins or general waste. They can start fires.",
            ),
            whereToGo = "E-waste bins at malls, electronics retailers and community clubs. Working devices can also be sold or traded in.",
        )

        EWasteCategory.APPLIANCE_NO_BATTERY -> Guidance(
            preparation = listOf(
                "Unplug the appliance and let it cool down completely.",
                "Remove any ink/toner cartridges from printers. Some retailers take these back separately.",
                "Wipe stored data on routers or set-top boxes (factory reset).",
                "Check the item fits through the bin opening. Never leave items outside the bin.",
            ),
            safety = listOf(
                "Do not break screens or monitors. They can contain hazardous materials.",
                "Bulky items that don't fit must go to retailer take-back or a collection service.",
            ),
            whereToGo = "Lower-left compartment of a SmartWaste bin or any ICT e-waste bin.",
        )

        EWasteCategory.SMALL_BATTERY -> Guidance(
            preparation = listOf(
                "Tape the terminals of lithium, 9V and button batteries with clear tape.",
                "Seal leaking batteries in a clear, leak-proof bag.",
                "Insert batteries one at a time into the battery chute so the sensor can count them.",
            ),
            safety = listOf(
                "Loose batteries touching each other can short-circuit and cause fires.",
                "Never put batteries in general waste or blue recycling bins.",
            ),
            whereToGo = "Battery chutes in e-waste bins, or battery & bulb bins at supermarkets.",
        )

        EWasteCategory.COMPONENT -> Guidance(
            preparation = listOf(
                "Wipe or physically destroy storage drives (HDD/SSD) to protect your data.",
                "Hold circuit boards by the edges and avoid sharp pins.",
                "Bundle small parts (RAM, cables) in a bag so they don't scatter.",
            ),
            safety = listOf(
                "Some boards have coin-cell batteries. Remove them and recycle them as batteries.",
                "Capacitors in power supplies can stay charged. Don't open PSUs.",
            ),
            whereToGo = "Lower-right compartment of a SmartWaste bin or an ICT e-waste bin. Working parts can be sold.",
        )

        EWasteCategory.LAMP -> Guidance(
            preparation = listOf(
                "Let the bulb cool down before handling.",
                "Keep fluorescent tubes and CFLs intact, ideally in their original packaging.",
            ),
            safety = listOf(
                "Fluorescent lamps contain mercury. If one breaks, air the room and don't vacuum the pieces.",
            ),
            whereToGo = "Battery & bulb bins at supermarkets, or lamp slots in e-waste bins.",
        )

        EWasteCategory.LARGE_APPLIANCE -> Guidance(
            preparation = listOf(
                "Large appliances don't fit in public bins.",
                "When buying a new one, retailers must provide free 1-for-1 take-back of the old one.",
                "Otherwise arrange a collection with a licensed e-waste collector or your town council.",
                "Defrost and dry fridges, and drain washing machines, before collection.",
            ),
            safety = listOf(
                "Fridges and air-cons contain refrigerants. Don't puncture the coolant lines.",
                "Electric mobility devices (PMDs/PABs) must go to designated collection points, not bins.",
            ),
            whereToGo = "Retailer take-back, licensed e-waste recyclers, or buyers if it still works.",
        )
    }

    val safetyTips = listOf(
        "Tape lithium battery terminals before recycling to prevent fires.",
        "Factory reset and remove SIM cards before letting go of a phone.",
        "Never put e-waste in blue recycling bins, because it contaminates the stream.",
        "Insert one item at a time so the smart bin can count it accurately.",
        "Swollen battery? Don't bin it. Bring it to a staffed collection point.",
        "Working devices are worth more sold or donated than recycled.",
    )

    /** Small offline FAQ used when the AI assistant cannot reach the network. */
    fun offlineAnswer(question: String): String {
        val q = question.lowercase()
        return when {
            listOf("point", "reward", "earn", "redeem").any { it in q } ->
                "You earn points by recycling at a bin. Use the deposit flow: (1) confirm your location or scan the SmartWaste bin QR, " +
                    "(2) take a photo of your item at the bin, (3) scan the barcode, serial number or IMEI. Verified items earn full points " +
                    "plus a verification bonus. Unverified items earn fewer points. Redeem points in the Rewards tab."
            "battery" in q || "batteries" in q ->
                "Tape the terminals of lithium/9V batteries, seal leaking ones in a bag, and put them in a battery chute or the battery & bulb " +
                    "bins at supermarkets. Never put batteries in general waste or blue recycling bins."
            listOf("phone", "laptop", "tablet", "data", "wipe", "reset").any { it in q } ->
                "Back up, sign out of your accounts and factory reset the device, then remove SIM/SD cards. If it still works, check the " +
                    "'Sell' tab on the map for buyers. Otherwise drop it into an e-waste bin."
            listOf("fridge", "washing", "aircon", "air-con", "tv", "television", "large").any { it in q } ->
                "Large appliances don't fit in bins. Retailers must offer free 1-for-1 take-back when you buy a new one, or you can book a " +
                    "licensed e-waste collector. Working units can be sold to buyers listed in the app."
            listOf("bulb", "lamp", "fluorescent").any { it in q } ->
                "Bulbs and lamps go into battery & bulb bins (often at supermarkets). Keep fluorescent tubes intact because they contain mercury."
            listOf("where", "near", "bin", "location").any { it in q } ->
                "Open the Map tab to see e-waste bins and buyers near you, with live routing. Bins that report being full are flagged so " +
                    "you don't make a wasted trip."
            listOf("sell", "cash", "buy", "trade").any { it in q } ->
                "Open the Map tab and switch to 'Buyers' to find shops and recyclers in Singapore that pay for used electronics, " +
                    "or check the online buyers section for doorstep pickup."
            else ->
                "I'm offline right now, but here's the short version: prepare your item safely (reset devices, tape batteries), " +
                    "find the nearest bin on the Map tab, and verify your deposit to earn points."
        }
    }
}
