package com.example.data.room

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "data_plans")
data class DataPlanEntity(
    @PrimaryKey val id: Int = 1,
    val monthlyLimitBytes: Long = 10L * 1024L * 1024L * 1024L, // Default 10 GB
    val warningPercent: Int = 80,
    val isEnabled: Boolean = true,
    val planType: String = PLAN_TYPE_MOBILE,
    /** Day of month (1-31) on which the carrier billing cycle resets. */
    @ColumnInfo(defaultValue = "1") val cycleStartDay: Int = 1,
    /** Cycle start timestamp for which the warning alert was already sent (0 = never). */
    @ColumnInfo(defaultValue = "0") val warningAlertedCycle: Long = 0L,
    /** Cycle start timestamp for which the limit-reached alert was already sent (0 = never). */
    @ColumnInfo(defaultValue = "0") val limitAlertedCycle: Long = 0L
) {
    companion object {
        const val PLAN_TYPE_MOBILE = "MOBILE"
        const val PLAN_TYPE_TOTAL = "TOTAL"
    }
}

/** Device-wide usage for one local calendar day, persisted by the daily snapshot worker. */
@Entity(tableName = "daily_usage")
data class DailyUsageEntity(
    @PrimaryKey val dayStart: Long,
    val mobileRxBytes: Long,
    val mobileTxBytes: Long,
    val wifiRxBytes: Long,
    val wifiTxBytes: Long
) {
    val mobileBytes: Long get() = mobileRxBytes + mobileTxBytes
    val wifiBytes: Long get() = wifiRxBytes + wifiTxBytes
}

@Entity(tableName = "speed_tests")
data class SpeedTestResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val timestamp: Long,
    val downloadMbps: Double,
    val uploadMbps: Double,
    val pingMs: Long,
    val jitterMs: Long,
    val networkType: String
)

@Entity(tableName = "insights")
data class InsightEntity(
    @PrimaryKey val id: Int = 1,
    val createdAt: Long,
    val rangeLabel: String,
    val text: String
)

@Dao
interface DataPlanDao {
    @Query("SELECT * FROM data_plans WHERE id = 1 LIMIT 1")
    fun getDataPlan(): Flow<DataPlanEntity?>

    @Query("SELECT * FROM data_plans WHERE id = 1 LIMIT 1")
    suspend fun getDataPlanOnce(): DataPlanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePlan(plan: DataPlanEntity)
}

@Dao
interface DailyUsageDao {
    @Upsert
    suspend fun upsert(rows: List<DailyUsageEntity>)

    @Query("SELECT * FROM daily_usage WHERE dayStart >= :from ORDER BY dayStart ASC")
    fun observeSince(from: Long): Flow<List<DailyUsageEntity>>

    @Query("SELECT * FROM daily_usage ORDER BY dayStart ASC")
    suspend fun getAll(): List<DailyUsageEntity>

    @Query("DELETE FROM daily_usage WHERE dayStart < :before")
    suspend fun deleteOlderThan(before: Long)
}

@Dao
interface SpeedTestDao {
    @Insert
    suspend fun insert(result: SpeedTestResultEntity): Long

    @Query("SELECT * FROM speed_tests ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int = 50): Flow<List<SpeedTestResultEntity>>

    @Query("SELECT * FROM speed_tests ORDER BY timestamp DESC")
    suspend fun getAll(): List<SpeedTestResultEntity>

    @Query("DELETE FROM speed_tests")
    suspend fun clear()
}

@Dao
interface InsightDao {
    @Query("SELECT * FROM insights WHERE id = 1")
    fun observe(): Flow<InsightEntity?>

    @Upsert
    suspend fun save(insight: InsightEntity)
}

@Database(
    entities = [
        DataPlanEntity::class,
        DailyUsageEntity::class,
        SpeedTestResultEntity::class,
        InsightEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dataPlanDao(): DataPlanDao
    abstract fun dailyUsageDao(): DailyUsageDao
    abstract fun speedTestDao(): SpeedTestDao
    abstract fun insightDao(): InsightDao

    companion object {
        const val NAME = "netpulse_database"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE data_plans ADD COLUMN cycleStartDay INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE data_plans ADD COLUMN warningAlertedCycle INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE data_plans ADD COLUMN limitAlertedCycle INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS daily_usage (dayStart INTEGER NOT NULL, " +
                        "mobileRxBytes INTEGER NOT NULL, mobileTxBytes INTEGER NOT NULL, " +
                        "wifiRxBytes INTEGER NOT NULL, wifiTxBytes INTEGER NOT NULL, PRIMARY KEY(dayStart))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS speed_tests (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "timestamp INTEGER NOT NULL, downloadMbps REAL NOT NULL, uploadMbps REAL NOT NULL, " +
                        "pingMs INTEGER NOT NULL, jitterMs INTEGER NOT NULL, networkType TEXT NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS insights (id INTEGER NOT NULL, createdAt INTEGER NOT NULL, " +
                        "rangeLabel TEXT NOT NULL, text TEXT NOT NULL, PRIMARY KEY(id))"
                )
            }
        }
    }
}
