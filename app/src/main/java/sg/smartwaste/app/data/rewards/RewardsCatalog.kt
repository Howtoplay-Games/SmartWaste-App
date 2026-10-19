package sg.smartwaste.app.data.rewards

enum class RewardCategory(val label: String) { ALL("All"), VOUCHERS("Vouchers"), TRANSPORT("Transport"), FOOD("Food"), GREEN("Green") }

data class Reward(
    val id: String,
    val title: String,
    val partner: String,
    val cost: Int,
    val valueLabel: String,
    val category: RewardCategory,
    val description: String,
    /** ARGB colour for the reward card. */
    val color: Long,
)

data class Tier(val name: String, val minLifetimePoints: Int, val perk: String)

/**
 * Demo rewards catalogue. Partners are placeholders until real sponsorships are signed.
 * Voucher codes are generated locally for demonstration.
 */
object RewardsCatalog {

    val rewards = listOf(
        Reward("r-transit3", "S$3 transit top-up", "Transit card partner", 600, "S$3", RewardCategory.TRANSPORT,
            "Top up your transit card for MRT and bus rides.", 0xFF2F6BFF),
        Reward("r-grocery5", "S$5 grocery e-voucher", "Supermarket partner", 1000, "S$5", RewardCategory.VOUCHERS,
            "Use at participating supermarkets islandwide.", 0xFF12B76A),
        Reward("r-ride5", "S$5 ride voucher", "Ride-hailing partner", 1000, "S$5", RewardCategory.TRANSPORT,
            "Discount on your next ride or food delivery.", 0xFF111316),
        Reward("r-coffee", "Free kopi / coffee", "Café partner", 450, "1 drink", RewardCategory.FOOD,
            "Any regular hot or iced coffee at participating outlets.", 0xFF8B5E3C),
        Reward("r-bubble", "Bubble tea treat", "Bubble tea partner", 550, "1 drink", RewardCategory.FOOD,
            "One regular drink with one topping.", 0xFFF59E0B),
        Reward("r-tech10", "S$10 electronics voucher", "Electronics retailer partner", 1800, "S$10", RewardCategory.VOUCHERS,
            "Put it towards repairs or accessories, so your devices last longer.", 0xFF7C3AED),
        Reward("r-movie", "Movie ticket", "Cinema partner", 1500, "1 ticket", RewardCategory.VOUCHERS,
            "Standard 2D movie ticket, any day.", 0xFFEF4444),
        Reward("r-mangrove", "Plant a mangrove", "Coastal restoration partner", 300, "1 tree", RewardCategory.GREEN,
            "We'll fund planting one mangrove sapling along Singapore's coast.", 0xFF067647),
        Reward("r-tote", "Recycled-PET tote bag", "SmartWaste", 700, "1 bag", RewardCategory.GREEN,
            "Collect at any SmartWaste pop-up booth.", 0xFF0E7490),
    )

    val tiers = listOf(
        Tier("Seedling", 0, "Start earning points"),
        Tier("Sprout", 500, "+5% bonus campaigns"),
        Tier("Sapling", 2_000, "Early access to rewards"),
        Tier("Evergreen", 5_000, "Exclusive green rewards"),
    )

    fun tierFor(lifetime: Int): Tier = tiers.last { lifetime >= it.minLifetimePoints }
    fun nextTier(lifetime: Int): Tier? = tiers.firstOrNull { it.minLifetimePoints > lifetime }
}
