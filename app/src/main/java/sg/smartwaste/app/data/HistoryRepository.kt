package sg.smartwaste.app.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import sg.smartwaste.app.data.auth.AuthRepository
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

/** Per-account recycling history, points and vouchers. */
@OptIn(ExperimentalCoroutinesApi::class)
class HistoryRepository(private val db: AppDatabase, private val auth: AuthRepository) {

    private val user: Flow<String> = auth.currentEmail.map { it.orEmpty() }

    val deposits: Flow<List<DepositEntity>> = user.flatMapLatest { db.depositDao().observeAll(it) }
    val redemptions: Flow<List<RedemptionEntity>> = user.flatMapLatest { db.redemptionDao().observeAll(it) }
    val recentScans: Flow<List<ScanEntity>> = user.flatMapLatest { db.scanDao().observeRecent(it) }

    val lifetimePoints: Flow<Int> = user.flatMapLatest { db.depositDao().observeEarned(it) }
    val balance: Flow<Int> = user.flatMapLatest { u ->
        combine(db.depositDao().observeEarned(u), db.redemptionDao().observeSpent(u)) { e, s -> e - s }
    }

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

    suspend fun currentUser(): String = auth.currentEmailNow()

    suspend fun addDeposit(entity: DepositEntity): Long = db.depositDao().insert(entity.copy(userEmail = currentUser()))

    suspend fun depositsToday(): Int {
        val midnight = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return db.depositDao().countSince(currentUser(), midnight)
    }

    suspend fun recordScan(result: DetectionResult) {
        db.scanDao().insert(
            ScanEntity(
                userEmail = currentUser(),
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
        val u = currentUser()
        val balance = db.depositDao().earned(u) - db.redemptionDao().spent(u)
        if (balance < reward.cost) return Result.failure(IllegalStateException("Not enough points"))
        val code = "SW-" + (1..3).joinToString("-") {
            (1..4).map { "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"[Random.nextInt(32)] }.joinToString("")
        }
        val entity = RedemptionEntity(
            userEmail = u,
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
