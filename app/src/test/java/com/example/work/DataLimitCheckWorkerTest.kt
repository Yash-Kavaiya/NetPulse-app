package com.example.work

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.example.data.model.AppDetailBreakdown
import com.example.data.model.AppNetworkUsage
import com.example.data.model.DeviceNetworkSummary
import com.example.data.model.LiveTrafficSpeed
import com.example.data.model.UsageBucket
import com.example.data.plan.PlanRepository
import com.example.data.prefs.SettingsRepository
import com.example.data.room.DataPlanDao
import com.example.data.room.DataPlanEntity
import com.example.data.stats.UsageStatsSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private class FakeStats(var mobileBytes: Long, var permitted: Boolean = true) : UsageStatsSource {
    override fun hasPermission() = permitted
    override suspend fun deviceSummary(start: Long, end: Long) = DeviceNetworkSummary(mobileRxBytes = mobileBytes)
    override suspend fun apps(start: Long, end: Long) = emptyList<AppNetworkUsage>()
    override suspend fun appDetail(app: AppNetworkUsage, start: Long, end: Long) =
        AppDetailBreakdown(app.uid, app.appName, app.packageName)
    override suspend fun deviceBuckets(boundaries: List<Long>, end: Long) = emptyList<UsageBucket>()
    override suspend fun appDailyBuckets(uid: Int, dayStarts: List<Long>, end: Long) = emptyList<UsageBucket>()
    override fun liveSpeed(intervalMs: Long): Flow<LiveTrafficSpeed> = emptyFlow()
}

private class FakePlanDao(initial: DataPlanEntity) : DataPlanDao {
    val state = MutableStateFlow<DataPlanEntity?>(initial)
    override fun getDataPlan(): Flow<DataPlanEntity?> = state
    override suspend fun getDataPlanOnce(): DataPlanEntity? = state.value
    override suspend fun savePlan(plan: DataPlanEntity) { state.value = plan }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class DataLimitCheckWorkerTest {

    @get:Rule val tmp = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var settings: SettingsRepository
    private val gb = 1024L * 1024 * 1024

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        settings = SettingsRepository(PreferenceDataStoreFactory.create { tmp.newFile("settings.preferences_pb") })
    }

    private fun worker(stats: UsageStatsSource, dao: DataPlanDao): DataLimitCheckWorker =
        TestListenableWorkerBuilder<DataLimitCheckWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters): ListenableWorker =
                    DataLimitCheckWorker(appContext, workerParameters, stats, PlanRepository(dao, stats), settings)
            })
            .build()

    @Test
    fun `marks the warning once per cycle`() = runBlocking {
        val dao = FakePlanDao(DataPlanEntity(monthlyLimitBytes = 10 * gb, warningPercent = 80))
        val stats = FakeStats(mobileBytes = 9 * gb)

        assertEquals(ListenableWorker.Result.success(), worker(stats, dao).doWork())
        val afterFirst = dao.state.value!!
        assertNotEquals(0L, afterFirst.warningAlertedCycle)
        assertEquals(0L, afterFirst.limitAlertedCycle)

        // Second run in the same cycle leaves the plan untouched.
        worker(stats, dao).doWork()
        assertEquals(afterFirst, dao.state.value)

        // Crossing the limit fires the limit alert.
        stats.mobileBytes = 11 * gb
        worker(stats, dao).doWork()
        assertEquals(afterFirst.warningAlertedCycle, dao.state.value!!.limitAlertedCycle)
    }

    @Test
    fun `does nothing when alerts are disabled`() = runBlocking {
        settings.setAlertsEnabled(false)
        val plan = DataPlanEntity(monthlyLimitBytes = gb)
        val dao = FakePlanDao(plan)
        worker(FakeStats(mobileBytes = 5 * gb), dao).doWork()
        assertEquals(plan, dao.state.value)
    }

    @Test
    fun `does nothing without usage access`() = runBlocking {
        val plan = DataPlanEntity(monthlyLimitBytes = gb)
        val dao = FakePlanDao(plan)
        worker(FakeStats(mobileBytes = 5 * gb, permitted = false), dao).doWork()
        assertEquals(plan, dao.state.value)
    }
}
