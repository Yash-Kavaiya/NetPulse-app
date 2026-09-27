package com.example.data.room

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

@Entity(tableName = "data_plans")
data class DataPlanEntity(
    @PrimaryKey val id: Int = 1,
    val monthlyLimitBytes: Long = 10L * 1024L * 1024L * 1024L, // Default 10 GB
    val warningPercent: Int = 80,
    val isEnabled: Boolean = true,
    val planType: String = "MOBILE" // "MOBILE" or "TOTAL"
)

@Dao
interface DataPlanDao {
    @Query("SELECT * FROM data_plans WHERE id = 1 LIMIT 1")
    fun getDataPlan(): Flow<DataPlanEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePlan(plan: DataPlanEntity)
}

@Database(entities = [DataPlanEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dataPlanDao(): DataPlanDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "netpulse_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class DataPlanRepository(private val dataPlanDao: DataPlanDao) {
    val dataPlan: Flow<DataPlanEntity?> = dataPlanDao.getDataPlan()

    suspend fun updatePlan(plan: DataPlanEntity) {
        dataPlanDao.savePlan(plan)
    }
}
