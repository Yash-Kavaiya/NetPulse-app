package com.example.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.example.data.room.AppDatabase
import com.example.data.room.DailyUsageDao
import com.example.data.room.DataPlanDao
import com.example.data.room.InsightDao
import com.example.data.room.SpeedTestDao
import com.example.data.stats.StatsRepository
import com.example.data.stats.UsageStatsSource
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()

    @Provides fun provideDataPlanDao(db: AppDatabase): DataPlanDao = db.dataPlanDao()
    @Provides fun provideDailyUsageDao(db: AppDatabase): DailyUsageDao = db.dailyUsageDao()
    @Provides fun provideSpeedTestDao(db: AppDatabase): SpeedTestDao = db.speedTestDao()
    @Provides fun provideInsightDao(db: AppDatabase): InsightDao = db.insightDao()

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("netpulse_settings") }

    @Provides
    @Singleton
    fun provideOkHttp(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()

    @Provides
    @Named("speedTestBaseUrl")
    fun provideSpeedTestBaseUrl(): String = "https://speed.cloudflare.com/"
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {
    @Binds
    abstract fun bindUsageStatsSource(impl: StatsRepository): UsageStatsSource
}
