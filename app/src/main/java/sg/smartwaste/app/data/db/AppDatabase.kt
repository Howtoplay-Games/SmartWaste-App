package sg.smartwaste.app.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "users")
data class UserEntity(
    /** Lower-cased email address. */
    @PrimaryKey val email: String,
    val name: String,
    /** "password" or "google" */
    val provider: String,
    /** Base64 PBKDF2 salt and hash. Empty for Google accounts. */
    val salt: String,
    val passwordHash: String,
    val createdAt: Long,
)

@Entity(tableName = "deposits")
data class DepositEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userEmail: String,
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
    /** SHA-256 of the normalised code, used to block re-using the same item (across all accounts). */
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
    val userEmail: String,
    val timestamp: Long,
    val rewardId: String,
    val rewardTitle: String,
    val cost: Int,
    val voucherCode: String,
)

@Entity(tableName = "scans")
data class ScanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userEmail: String,
    val timestamp: Long,
    val itemName: String,
    val category: String?,
    val confidence: Float,
    val engine: String,
    val model: String,
    val photoPath: String?,
)

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(user: UserEntity)

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun find(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    fun observe(email: String): Flow<UserEntity?>

    @Query("UPDATE users SET name = :name WHERE email = :email")
    suspend fun rename(email: String, name: String)

    @Query("UPDATE users SET salt = :salt, passwordHash = :hash, provider = 'password' WHERE email = :email")
    suspend fun updatePassword(email: String, salt: String, hash: String)
}

@Dao
interface DepositDao {
    @Insert
    suspend fun insert(entity: DepositEntity): Long

    @Query("SELECT * FROM deposits WHERE userEmail = :user ORDER BY timestamp DESC")
    fun observeAll(user: String): Flow<List<DepositEntity>>

    @Query("SELECT COALESCE(SUM(points), 0) FROM deposits WHERE userEmail = :user")
    fun observeEarned(user: String): Flow<Int>

    @Query("SELECT COALESCE(SUM(points), 0) FROM deposits WHERE userEmail = :user")
    suspend fun earned(user: String): Int

    /** Global on purpose: an item's code can only ever be claimed once on this device. */
    @Query("SELECT COUNT(*) FROM deposits WHERE codeHash = :hash")
    suspend fun countByCodeHash(hash: String): Int

    @Query("SELECT COUNT(*) FROM deposits WHERE userEmail = :user AND timestamp >= :since")
    suspend fun countSince(user: String, since: Long): Int
}

@Dao
interface RedemptionDao {
    @Insert
    suspend fun insert(entity: RedemptionEntity): Long

    @Query("SELECT * FROM redemptions WHERE userEmail = :user ORDER BY timestamp DESC")
    fun observeAll(user: String): Flow<List<RedemptionEntity>>

    @Query("SELECT COALESCE(SUM(cost), 0) FROM redemptions WHERE userEmail = :user")
    fun observeSpent(user: String): Flow<Int>

    @Query("SELECT COALESCE(SUM(cost), 0) FROM redemptions WHERE userEmail = :user")
    suspend fun spent(user: String): Int
}

@Dao
interface ScanDao {
    @Insert
    suspend fun insert(entity: ScanEntity): Long

    @Query("SELECT * FROM scans WHERE userEmail = :user ORDER BY timestamp DESC LIMIT 50")
    fun observeRecent(user: String): Flow<List<ScanEntity>>
}

@Database(
    entities = [UserEntity::class, DepositEntity::class, RedemptionEntity::class, ScanEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun depositDao(): DepositDao
    abstract fun redemptionDao(): RedemptionDao
    abstract fun scanDao(): ScanDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "smartwaste.db")
                // v1 → v2 adds accounts; pre-release data is reset on upgrade.
                .fallbackToDestructiveMigration()
                .build()
    }
}
