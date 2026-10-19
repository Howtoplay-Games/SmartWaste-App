package sg.smartwaste.app.data.places

import sg.smartwaste.app.data.model.BuyerInfo
import sg.smartwaste.app.data.model.EWasteCategory
import sg.smartwaste.app.data.model.EWasteCategory.APPLIANCE_NO_BATTERY
import sg.smartwaste.app.data.model.EWasteCategory.COMPONENT
import sg.smartwaste.app.data.model.EWasteCategory.DEVICE_WITH_BATTERY
import sg.smartwaste.app.data.model.EWasteCategory.LAMP
import sg.smartwaste.app.data.model.EWasteCategory.LARGE_APPLIANCE
import sg.smartwaste.app.data.model.EWasteCategory.SMALL_BATTERY
import sg.smartwaste.app.data.model.OnlineBuyer
import sg.smartwaste.app.data.model.Place
import sg.smartwaste.app.data.model.PlaceSource
import sg.smartwaste.app.data.model.PlaceType
import sg.smartwaste.app.util.GeoPoint
import java.util.Calendar

/**
 * Bundled directory used offline and as a baseline before live sources load.
 *
 * Coordinates are approximate and get refined at runtime by geocoding each postal
 * code with OneMap (Singapore's national map API). Live sources (OpenStreetMap,
 * and NEA open data if configured) are merged on top. Researched October 2026.
 * Users should still confirm on recycle.gov.sg or with the business before going.
 */
object CuratedPlaces {

    private val retailBin = setOf(DEVICE_WITH_BATTERY, APPLIANCE_NO_BATTERY, SMALL_BATTERY, LAMP, COMPONENT)
    private val batteryBulb = setOf(SMALL_BATTERY, LAMP)
    private val nonRegulated = setOf(APPLIANCE_NO_BATTERY, COMPONENT)

    private fun bin(
        id: String, name: String, lat: Double, lng: Double, address: String, postal: String,
        operator: String, accepts: Set<EWasteCategory>, hours: String? = null,
    ) = Place(
        id = id, name = name, type = PlaceType.BIN, point = GeoPoint(lat, lng), address = address,
        postalCode = postal, operator = operator, accepts = accepts, source = PlaceSource.CURATED,
        openingHours = hours,
    )

    private fun buyer(
        id: String, name: String, lat: Double, lng: Double, address: String, postal: String,
        operator: String, accepts: Set<EWasteCategory>, info: BuyerInfo,
        hours: String? = null, phone: String? = null, website: String? = null,
    ) = Place(
        id = id, name = name, type = PlaceType.BUYER, point = GeoPoint(lat, lng), address = address,
        postalCode = postal, operator = operator, accepts = accepts, source = PlaceSource.CURATED,
        openingHours = hours, phone = phone, website = website, buyer = info,
    )

    /**
     * SmartWaste IoT pilot bins. Their QR code payload is `SMARTWASTE:BIN:<id>`.
     * Fill levels are demo telemetry until real Arduino sensor data is connected.
     */
    fun smartBins(now: Long = System.currentTimeMillis()): List<Place> {
        val hour = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.HOUR_OF_DAY)
        fun demoFill(id: String) = ((id.hashCode() and 0x7fffffff) % 60 + hour * 3) % 96
        val smart = setOf(DEVICE_WITH_BATTERY, APPLIANCE_NO_BATTERY, SMALL_BATTERY, COMPONENT)
        return listOf(
            Triple("SW-001", "SmartWaste Bin · Our Tampines Hub", Triple(1.3533, 103.9399, "1 Tampines Walk|528523")),
            Triple("SW-002", "SmartWaste Bin · Toa Payoh HDB Hub", Triple(1.3326, 103.8474, "480 Lorong 6 Toa Payoh|310480")),
            Triple("SW-003", "SmartWaste Bin · JEM Jurong East", Triple(1.3333, 103.7436, "50 Jurong Gateway Road|608549")),
            Triple("SW-004", "SmartWaste Bin · Waterway Point", Triple(1.4065, 103.9022, "83 Punggol Central|828761")),
            Triple("SW-005", "SmartWaste Bin · Woodlands Civic Centre", Triple(1.4350, 103.7869, "900 South Woodlands Drive|730900")),
        ).map { (id, name, loc) ->
            val (lat, lng, addr) = loc
            val (street, postal) = addr.split("|")
            Place(
                id = id, name = name, type = PlaceType.BIN, point = GeoPoint(lat, lng),
                address = "$street, Singapore $postal", postalCode = postal,
                operator = "SmartWaste IoT bin (pilot)", accepts = smart, source = PlaceSource.SMARTWASTE,
                fillLevel = demoFill(id), openingHours = "24 hours",
            )
        }
    }

    val bins: List<Place> = listOf(
        bin("C-B01", "E-waste bin · Ngee Ann City (Best Denki)", 1.3025, 103.8345, "391 Orchard Road, Singapore 238872", "238872", "Retailer e-waste bin", retailBin, "10am – 9:30pm"),
        bin("C-B02", "E-waste bin · Millenia Walk (Harvey Norman)", 1.2930, 103.8605, "9 Raffles Boulevard, Singapore 039596", "039596", "Retailer e-waste bin", retailBin, "11am – 9:30pm"),
        bin("C-B03", "E-waste bin · Funan (Challenger)", 1.2912, 103.8500, "107 North Bridge Road, Singapore 179105", "179105", "Retailer e-waste bin", retailBin, "10am – 10pm"),
        bin("C-B04", "E-waste bin · Singtel Shop 313@Somerset", 1.3009, 103.8386, "313 Orchard Road, Singapore 238895", "238895", "Telco e-waste bin", retailBin, "11am – 9pm"),
        bin("C-B05", "E-waste bin · VivoCity (Best Denki)", 1.2643, 103.8223, "1 HarbourFront Walk, Singapore 098585", "098585", "Retailer e-waste bin", retailBin, "10am – 10pm"),
        bin("C-B06", "E-waste bin · Courts Megastore Tampines", 1.3757, 103.9327, "50 Tampines North Drive 2, Singapore 528766", "528766", "Retailer e-waste bin", retailBin, "10am – 10pm"),
        bin("C-B07", "E-waste bin · Parkway Parade", 1.3013, 103.9054, "80 Marine Parade Road, Singapore 449269", "449269", "Retailer e-waste bin", retailBin, "10am – 10pm"),
        bin("C-B08", "E-waste bin · Jurong Point", 1.3398, 103.7065, "1 Jurong West Central 2, Singapore 648886", "648886", "Mall e-waste bin", retailBin, "10am – 10pm"),
        bin("C-B09", "E-waste bin · Causeway Point", 1.4360, 103.7861, "1 Woodlands Square, Singapore 738099", "738099", "Mall e-waste bin", retailBin, "10am – 10pm"),
        bin("C-B10", "E-waste bin · Northpoint City", 1.4295, 103.8357, "930 Yishun Avenue 2, Singapore 769098", "769098", "Mall e-waste bin", retailBin, "10am – 10pm"),
        bin("C-B11", "E-waste bin · AMK Hub", 1.3693, 103.8485, "53 Ang Mo Kio Avenue 3, Singapore 569933", "569933", "Mall e-waste bin", retailBin, "10am – 10pm"),
        bin("C-B12", "E-waste bin · Bedok Mall", 1.3249, 103.9295, "311 New Upper Changi Road, Singapore 467360", "467360", "Mall e-waste bin", retailBin, "10am – 10pm"),
        bin("C-B13", "E-waste bin · NEX Serangoon", 1.3506, 103.8721, "23 Serangoon Central, Singapore 556083", "556083", "Mall e-waste bin", retailBin, "10am – 10pm"),
        bin("C-B14", "E-waste bin · Hougang Mall", 1.3725, 103.8938, "90 Hougang Avenue 10, Singapore 538766", "538766", "Mall e-waste bin", retailBin, "10am – 10pm"),
        bin("C-B15", "E-waste bin · Compass One", 1.3920, 103.8951, "1 Sengkang Square, Singapore 545078", "545078", "Mall e-waste bin", retailBin, "10am – 10pm"),
        bin("C-B16", "E-waste bin · Junction 8 Bishan", 1.3502, 103.8488, "9 Bishan Place, Singapore 579837", "579837", "Mall e-waste bin", retailBin, "10am – 10pm"),
        bin("C-B17", "E-waste bin · Clementi Mall", 1.3149, 103.7644, "3155 Commonwealth Avenue West, Singapore 129588", "129588", "Mall e-waste bin", retailBin, "10am – 10pm"),
        bin("C-B18", "E-waste bin · Westgate", 1.3343, 103.7426, "3 Gateway Drive, Singapore 608532", "608532", "Mall e-waste bin", retailBin, "10am – 10pm"),
        bin("C-B19", "E-waste bin · Bukit Panjang Plaza", 1.3799, 103.7643, "1 Jelebu Road, Singapore 677743", "677743", "Mall e-waste bin", retailBin, "10am – 10pm"),
        bin("C-B20", "E-waste bin · Tampines Mall", 1.3526, 103.9447, "4 Tampines Central 5, Singapore 529510", "529510", "Mall e-waste bin", retailBin, "10am – 10pm"),
        bin("C-B21", "E-waste bin · Plaza Singapura", 1.3006, 103.8450, "68 Orchard Road, Singapore 238839", "238839", "Mall e-waste bin", retailBin, "10am – 10pm"),
        bin("C-B22", "E-waste bin · Suntec City", 1.2950, 103.8584, "3 Temasek Boulevard, Singapore 038983", "038983", "Mall e-waste bin", retailBin, "10am – 10pm"),
        bin("C-B23", "E-waste bin · Lot One Choa Chu Kang", 1.3851, 103.7447, "21 Choa Chu Kang Avenue 4, Singapore 689812", "689812", "Mall e-waste bin", retailBin, "10am – 10pm"),
        bin("C-B24", "E-waste bin · Sim Lim Square", 1.3030, 103.8527, "1 Rochor Canal Road, Singapore 188504", "188504", "Mall e-waste bin", retailBin, "10:30am – 9pm"),
        bin("C-B25", "Battery & bulb bin · Kallang Wave Mall", 1.3027, 103.8753, "1 Stadium Place, Singapore 397628", "397628", "Supermarket battery & bulb bin", batteryBulb, "8am – 10pm"),
        bin("C-B26", "Battery & bulb bin · Bukit Timah Plaza", 1.3394, 103.7781, "1 Jalan Anak Bukit, Singapore 588996", "588996", "Supermarket battery & bulb bin", batteryBulb, "8am – 10pm"),
        bin("C-B27", "Battery & bulb bin · Great World", 1.2935, 103.8320, "1 Kim Seng Promenade, Singapore 237994", "237994", "Supermarket battery & bulb bin", batteryBulb, "8am – 10pm"),
        bin("C-B28", "SGRecycle bin · Punggol Oasis RC", 1.3995, 103.9075, "Blk 175A Punggol Field, Singapore 821175", "821175", "SGRecycle / Virogreen (non-regulated e-waste)", nonRegulated),
        bin("C-B29", "SGRecycle bin · Punggol Gardens RC", 1.3985, 103.9050, "Blk 163A Punggol Central, Singapore 821163", "821163", "SGRecycle / Virogreen (non-regulated e-waste)", nonRegulated),
    )

    private val devices = setOf(DEVICE_WITH_BATTERY, COMPONENT, APPLIANCE_NO_BATTERY)

    val buyers: List<Place> = listOf(
        buyer(
            "C-S01", "myhalo · Bugis Junction", 1.2993, 103.8556,
            "200 Victoria Street, #03-25/26 Bugis Junction, Singapore 188021", "188021", "myhalo (PC Dreams group)", devices,
            BuyerInfo("Cash, PayNow or trade-in credit", "Phones, laptops, tablets, gaming laptops", pickup = true, notes = "Instant quotes online. Devices with no resale value are recycled responsibly."),
            hours = "11am – 8pm", website = "https://myhalo.com.sg",
        ),
        buyer(
            "C-S02", "myhalo · Sim Lim Square", 1.3030, 103.8527,
            "1 Rochor Canal Road, #03-40 Sim Lim Square, Singapore 188504", "188504", "myhalo (PC Dreams group)", devices,
            BuyerInfo("Cash, PayNow or trade-in credit", "Laptops, MacBooks, iPads, phones, PCs", pickup = true),
            website = "https://pcdreams.com.sg",
        ),
        buyer(
            "C-S03", "Mister Mobile · Jurong East", 1.3336, 103.7414,
            "132 Jurong Gateway Road, #01-275, Singapore 600132", "600132", "Mister Mobile", setOf(DEVICE_WITH_BATTERY),
            BuyerInfo("Cash / trade-in", "Phones, tablets, smartwatches"),
            hours = "11am – 9pm", phone = "+6592230410", website = "https://www.mistermobile.com.sg",
        ),
        buyer(
            "C-S04", "Mister Mobile · City Plaza Geylang", 1.3151, 103.8930,
            "810 Geylang Road, #01-67A City Plaza, Singapore 409286", "409286", "Mister Mobile", setOf(DEVICE_WITH_BATTERY),
            BuyerInfo("Cash / trade-in", "Phones, tablets, smartwatches"),
            phone = "+6592230410", website = "https://www.mistermobile.com.sg",
        ),
        buyer(
            "C-S05", "Mister Mobile · Hougang", 1.3782, 103.8994,
            "21 Hougang Street 51, #01-27, Singapore 538719", "538719", "Mister Mobile", setOf(DEVICE_WITH_BATTERY),
            BuyerInfo("Cash / trade-in", "Phones, tablets, smartwatches"),
            phone = "+6592230410", website = "https://www.mistermobile.com.sg",
        ),
        buyer(
            "C-S06", "Cash Converters · Chinatown", 1.2826, 103.8446,
            "80 Eu Tong Sen Street (Old Majestic Theatre), Singapore 059810", "059810", "Cash Converters",
            setOf(DEVICE_WITH_BATTERY, APPLIANCE_NO_BATTERY, LARGE_APPLIANCE),
            BuyerInfo("Cash on the spot", "TVs, hi-fi, electrical appliances, computers, phones", notes = "Several heartland outlets: Tampines, Toa Payoh, Jurong East, Bedok North, Ang Mo Kio, Kallang Bahru."),
            website = "https://cashconverters.sg",
        ),
        buyer(
            "C-S07", "Apple Orchard Road (Apple Trade In)", 1.3015, 103.8366,
            "270 Orchard Road, Singapore 238857", "238857", "Apple", setOf(DEVICE_WITH_BATTERY),
            BuyerInfo("Trade-in credit", "Eligible iPhone, iPad, Mac, Apple Watch", notes = "Devices that aren't eligible for credit can be recycled for free."),
            hours = "10am – 10pm", website = "https://www.apple.com/sg/trade-in/",
        ),
        buyer(
            "C-S08", "SK tes Singapore", 1.3165, 103.6900,
            "9 Benoi Sector, Singapore 629844", "629844", "SK tes (formerly TES-AMM)",
            setOf(DEVICE_WITH_BATTERY, COMPONENT, APPLIANCE_NO_BATTERY, SMALL_BATTERY),
            BuyerInfo("Quote (bulk / corporate)", "IT assets, e-scrap, lithium-ion batteries", pickup = true, notes = "Best for bulk or company IT disposal (ITAD) with certified data destruction."),
            website = "https://www.sktes.com",
        ),
        buyer(
            "C-S09", "Virogreen Singapore", 1.3296, 103.6491,
            "31 Tuas Avenue 11, Singapore 639105", "639105", "Virogreen",
            setOf(DEVICE_WITH_BATTERY, COMPONENT, APPLIANCE_NO_BATTERY, LARGE_APPLIANCE, SMALL_BATTERY),
            BuyerInfo("Quote (bulk)", "E-waste, IT assets, batteries, solar panels", pickup = true),
            website = "https://virogreen.com",
        ),
        buyer(
            "C-S10", "Metalo International", 1.2945, 103.6298,
            "110 Tuas South Avenue 3, #03-11 The Index, Singapore 637369", "637369", "Metalo",
            setOf(COMPONENT, APPLIANCE_NO_BATTERY, LARGE_APPLIANCE, SMALL_BATTERY),
            BuyerInfo("Quote (scrap value)", "E-scrap, PMD batteries, metals"),
            phone = "+6562517855",
        ),
        buyer(
            "C-S11", "Seah Yong Heng", 1.2945, 103.6300,
            "110 Tuas South Avenue 3, #03-04 The Index, Singapore 637369", "637369", "Seah Yong Heng",
            setOf(COMPONENT, APPLIANCE_NO_BATTERY, LARGE_APPLIANCE),
            BuyerInfo("Quote (scrap value)", "Scrap metal & e-waste"),
            hours = "Mon–Fri 9–11am, 2–5pm", phone = "+6569090632",
        ),
        buyer(
            "C-S12", "Esun International", 1.3196, 103.6989,
            "17 Tractor Road, Singapore 627975", "627975", "Esun International",
            setOf(COMPONENT, APPLIANCE_NO_BATTERY, LARGE_APPLIANCE, SMALL_BATTERY),
            BuyerInfo("Quote (scrap value)", "E-waste, PMDs, batteries"),
            phone = "+6568660220",
        ),
    )

    val onlineBuyers: List<OnlineBuyer> = listOf(
        OnlineBuyer(
            "SellUp", "Compare dealer quotes online, then get doorstep pickup with on-the-spot payment.",
            "Phones, tablets, laptops, consoles", "Cash / PayNow", "https://sellup.com.sg",
        ),
        OnlineBuyer(
            "myhalo online", "AI-backed instant quotes and free pickup. Unsellable devices are recycled.",
            "Phones, laptops, tablets", "PayNow / bank transfer", "https://myhalo.com.sg",
        ),
        OnlineBuyer(
            "Carousell", "Sell directly to other people. Best for working devices in good condition.",
            "Almost any working electronics", "Buyer pays you", "https://www.carousell.sg",
        ),
    )
}
