package sg.smartwaste.app.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import sg.smartwaste.app.data.db.AppDatabase
import sg.smartwaste.app.data.db.DepositEntity
import sg.smartwaste.app.data.db.RedemptionEntity
import sg.smartwaste.app.data.db.ScanEntity
import sg.smartwaste.app.data.model.DetectionResult
import sg.smartwaste.app.data.model.EWasteCategory
import sg.smartwaste.app.data.rewards.Reward
import java.util.Calendar
import kotlin.random.Random

data class Impact(
    val items: Int = 0,
    val kg: Double = 0.0,
    val co2Kg: Double = 0.0,
    val deposits: Int = 0,
    val verifiedDeposits: Int = 0,
    val byCategory: Map<EWasteCategory, Int> = emptyMap(),
)

class HistoryRepository(private val db: AppDatabase) {

    val deposits: Flow<List<DepositEntity>> = db.depositDao().observeAll()
    val redemptions: Flow<List<RedemptionEntity>> = db.redemptionDao().observeAll()
    val recentScans: Flow<List<ScanEntity>> = db.scanDao().observeRecent()

    val lifetimePoints: Flow<Int> = db.depositDao().observeEarned()
    val balance: Flow<Int> = combine(db.depositDao().observeEarned(), db.redemptionDao().observeSpent()) { e, s -> e - s }

    val impact: Flow<Impact> = deposits.map { list ->
        Impact(
            items = list.sumOf { it.quantity },
            kg = list.sumOf { it.kgEstimate },
            co2Kg = list.sumOf { it.co2Estimate },
            deposits = list.size,
            verifiedDeposits = list.count { it.verification == "ONLINE_VERIFIED" || it.verification == "CHECKSUM_VALID" },
            byCategory = list.groupBy { EWasteCategory.fromKey(it.category) }
                .mapNotNull { (k, v) -> k?.let { it to v.sumOf { d -> d.quantity } } }
                .toMap(),
        )
    }

    suspend fun addDeposit(entity: DepositEntity): Long = db.depositDao().insert(entity)

    suspend fun depositsToday(): Int {
        val midnight = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return db.depositDao().countSince(midnight)
    }

    suspend fun recordScan(result: DetectionResult) {
        db.scanDao().insert(
            ScanEntity(
                timestamp = result.timestamp,
                itemName = result.itemName,
                category = result.category?.name,
                confidence = result.confidence,
                engine = result.engine.name,
                model = result.modelName,
                photoPath = result.photoPath,
            )
        )
    }

    /** Redeems a reward if the balance allows. Returns the created redemption. */
    suspend fun redeem(reward: Reward): Result<RedemptionEntity> {
        val balance = db.depositDao().earned() - db.redemptionDao().spent()
        if (balance < reward.cost) return Result.failure(IllegalStateException("Not enough points"))
        val code = "SW-" + (1..3).joinToString("-") {
            (1..4).map { "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"[Random.nextInt(32)] }.joinToString("")
        }
        val entity = RedemptionEntity(
            timestamp = System.currentTimeMillis(),
            rewardId = reward.id,
            rewardTitle = reward.title,
            cost = reward.cost,
            voucherCode = code,
        )
        val id = db.redemptionDao().insert(entity)
        return Result.success(entity.copy(id = id))
    }
}
