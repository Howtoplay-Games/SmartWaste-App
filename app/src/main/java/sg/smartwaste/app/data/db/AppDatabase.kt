package sg.smartwaste.app.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "deposits")
data class DepositEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val itemName: String,
    val category: String,
    val quantity: Int,
    val placeId: String,
    val placeName: String,
    val lat: Double,
    val lng: Double,
    /** GPS, QR (SmartWaste bin) */
    val binProof: String,
    val photoPath: String?,
    val photoConfirmed: Boolean,
    /** VerificationLevel name */
    val verification: String,
    val codeType: String?,
    /** SHA-256 of the normalised code, used to block re-using the same item. */
    val codeHash: String?,
    val codeMasked: String?,
    val productTitle: String?,
    val points: Int,
    val kgEstimate: Double,
    val co2Estimate: Double,
)

@Entity(tableName = "redemptions")
data class RedemptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val rewardId: String,
    val rewardTitle: String,
    val cost: Int,
    val voucherCode: String,
)

@Entity(tableName = "scans")
data class ScanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val itemName: String,
    val category: String?,
    val confidence: Float,
    val engine: String,
    val model: String,
    val photoPath: String?,
)

@Dao
interface DepositDao {
    @Insert
    suspend fun insert(entity: DepositEntity): Long

    @Query("SELECT * FROM deposits ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<DepositEntity>>

    @Query("SELECT COALESCE(SUM(points), 0) FROM deposits")
    fun observeEarned(): Flow<Int>

    @Query("SELECT COALESCE(SUM(points), 0) FROM deposits")
    suspend fun earned(): Int

    @Query("SELECT COUNT(*) FROM deposits WHERE codeHash = :hash")
    suspend fun countByCodeHash(hash: String): Int

    @Query("SELECT COUNT(*) FROM deposits WHERE timestamp >= :since")
    suspend fun countSince(since: Long): Int
}

@Dao
interface RedemptionDao {
    @Insert
    suspend fun insert(entity: RedemptionEntity): Long

    @Query("SELECT * FROM redemptions ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<RedemptionEntity>>

    @Query("SELECT COALESCE(SUM(cost), 0) FROM redemptions")
    fun observeSpent(): Flow<Int>

    @Query("SELECT COALESCE(SUM(cost), 0) FROM redemptions")
    suspend fun spent(): Int
}

@Dao
interface ScanDao {
    @Insert
    suspend fun insert(entity: ScanEntity): Long

    @Query("SELECT * FROM scans ORDER BY timestamp DESC LIMIT 50")
    fun observeRecent(): Flow<List<ScanEntity>>
}

@Database(
    entities = [DepositEntity::class, RedemptionEntity::class, ScanEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun depositDao(): DepositDao
    abstract fun redemptionDao(): RedemptionDao
    abstract fun scanDao(): ScanDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "smartwaste.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
