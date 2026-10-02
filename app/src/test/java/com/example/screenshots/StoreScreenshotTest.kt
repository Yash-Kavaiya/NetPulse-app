package com.example.screenshots

import android.app.Application
import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AppDetailBreakdown
import com.example.data.model.AppNetworkUsage
import com.example.data.model.DeviceNetworkSummary
import com.example.data.model.LiveTrafficSpeed
import com.example.data.model.UsageBucket
import com.example.data.plan.PlanRepository
import com.example.data.room.DailyUsageDao
import com.example.data.room.DailyUsageEntity
import com.example.data.room.DataPlanDao
import com.example.data.room.DataPlanEntity
import com.example.data.room.SpeedTestResultEntity
import com.example.data.speedtest.SpeedTestPhase
import com.example.data.speedtest.SpeedTestProgress
import com.example.data.stats.TimeRanges
import com.example.data.stats.UsageStatsSource
import com.example.ui.apps.AppsScreen
import com.example.ui.apps.AppsViewModel
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.dashboard.DashboardViewModel
import com.example.ui.history.HistoryScreen
import com.example.ui.history.HistoryViewModel
import com.example.ui.navigation.NetPulseChrome
import com.example.ui.navigation.topLevelTitle
import com.example.ui.speedtest.SpeedTestContent
import com.example.ui.speedtest.SpeedTestUiState
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.Calendar

private const val MB = 1024L * 1024L
private const val GB = 1024L * MB

/** Simple app icon: a colored rounded square with the app's initial. */
private class LetterIcon(private val letter: String, private val color: Int) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun getIntrinsicWidth() = 144
    override fun getIntrinsicHeight() = 144
    override fun draw(canvas: Canvas) {
        val b = bounds
        paint.color = color
        val r = b.width() * 0.24f
        canvas.drawRoundRect(RectF(b), r, r, paint)
        paint.color = 0xFFFFFFFF.toInt()
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.textSize = b.height() * 0.5f
        val y = b.exactCenterY() - (paint.descent() + paint.ascent()) / 2
        canvas.drawText(letter, b.exactCenterX(), y, paint)
    }
    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(colorFilter: ColorFilter?) {}
    @Deprecated("Deprecated in Java")
    override fun getOpacity() = PixelFormat.TRANSLUCENT
}

/** Sample usage data with made-up app names, so no third-party brands appear in screenshots. */
private class DemoStats : UsageStatsSource {
    private val mobilePattern = longArrayOf(900, 1100, 1300, 1000, 1200, 1400, 800)
    private val wifiPattern = longArrayOf(2200, 1800, 2600, 2000, 3100, 3600, 2900)

    private data class DemoApp(val name: String, val pkg: String, val color: Int, val mobileShare: Double, val wifiShare: Double)

    private val demoApps = listOf(
        DemoApp("StreamBox Video", "demo.streambox", 0xFFE5484D.toInt(), 0.34, 0.41),
        DemoApp("PhotoLoop", "demo.photoloop", 0xFFD6409F.toInt(), 0.22, 0.14),
        DemoApp("Web Browser", "demo.browser", 0xFF3E63DD.toInt(), 0.15, 0.12),
        DemoApp("TuneWave Music", "demo.tunewave", 0xFF30A46C.toInt(), 0.11, 0.08),
        DemoApp("ChatLine", "demo.chatline", 0xFF12A594.toInt(), 0.08, 0.05),
        DemoApp("RouteMaps", "demo.routemaps", 0xFFF76B15.toInt(), 0.05, 0.03),
        DemoApp("CloudVault Backup", "demo.cloudvault", 0xFF8E4EC6.toInt(), 0.01, 0.14),
        DemoApp("MailBox", "demo.mailbox", 0xFF0090FF.toInt(), 0.04, 0.03)
    )

    private fun day(dayStart: Long): UsageBucket {
        val i = ((dayStart / TimeRanges.DAY_MS) % 7).toInt()
        return UsageBucket(dayStart, dayStart + TimeRanges.DAY_MS, mobilePattern[i] * MB, wifiPattern[i] * MB)
    }

    private fun total(start: Long, end: Long): UsageBucket {
        val days = TimeRanges.dayStarts(start, end).map(::day)
        return UsageBucket(start, end, days.sumOf { it.mobileBytes }, days.sumOf { it.wifiBytes })
    }

    override fun hasPermission() = true

    override suspend fun deviceSummary(start: Long, end: Long): DeviceNetworkSummary {
        val t = total(start, end)
        return DeviceNetworkSummary(
            mobileRxBytes = t.mobileBytes * 86 / 100,
            mobileTxBytes = t.mobileBytes * 14 / 100,
            wifiRxBytes = t.wifiBytes * 90 / 100,
            wifiTxBytes = t.wifiBytes * 10 / 100,
            startTime = start,
            endTime = end
        )
    }

    override suspend fun apps(start: Long, end: Long): List<AppNetworkUsage> {
        val t = total(start, end)
        return demoApps.mapIndexed { i, app ->
            val mobile = (t.mobileBytes * app.mobileShare).toLong()
            val wifi = (t.wifiBytes * app.wifiShare).toLong()
            AppNetworkUsage(
                uid = 10100 + i,
                packageName = app.pkg,
                appName = app.name,
                icon = LetterIcon(app.name.take(1), app.color),
                rxBytesMobile = mobile * 88 / 100,
                txBytesMobile = mobile * 12 / 100,
                rxBytesWifi = wifi * 92 / 100,
                txBytesWifi = wifi * 8 / 100
            )
        }.sortedByDescending { it.grandTotalBytes }
    }

    override suspend fun appDetail(app: AppNetworkUsage, start: Long, end: Long) =
        AppDetailBreakdown(app.uid, app.appName, app.packageName)

    override suspend fun deviceBuckets(boundaries: List<Long>, end: Long): List<UsageBucket> =
        boundaries.map { day(TimeRanges.startOfDay(it)) }

    override suspend fun appDailyBuckets(uid: Int, dayStarts: List<Long>, end: Long) = emptyList<UsageBucket>()

    override fun liveSpeed(intervalMs: Long): Flow<LiveTrafficSpeed> =
        flowOf(LiveTrafficSpeed(rxSpeedBps = 2_400_000, txSpeedBps = 310_000))
}

private class DemoPlanDao(plan: DataPlanEntity) : DataPlanDao {
    private val state = MutableStateFlow<DataPlanEntity?>(plan)
    override fun getDataPlan(): Flow<DataPlanEntity?> = state
    override suspend fun getDataPlanOnce(): DataPlanEntity? = state.value
    override suspend fun savePlan(plan: DataPlanEntity) { state.value = plan }
}

private class EmptyDailyUsageDao : DailyUsageDao {
    override suspend fun upsert(rows: List<DailyUsageEntity>) {}
    override fun observeSince(from: Long): Flow<List<DailyUsageEntity>> = flowOf(emptyList())
    override suspend fun getAll(): List<DailyUsageEntity> = emptyList()
    override suspend fun deleteOlderThan(before: Long) {}
}

/**
 * Renders the app's real screens with sample data to PNG files for the Play Store listing.
 * Output: app/build/outputs/roborazzi/*.png (1080 x 2160).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class, qualifiers = "w360dp-h720dp-xxhdpi")
class StoreScreenshotTest {

    @get:Rule val compose = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val stats = DemoStats()

    /** A plan whose billing cycle started about 18 days ago. */
    private val planRepository: PlanRepository by lazy {
        val cycleDay = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -17)
        }.get(Calendar.DAY_OF_MONTH).coerceAtMost(28)
        PlanRepository(DemoPlanDao(DataPlanEntity(monthlyLimitBytes = 40 * GB, cycleStartDay = cycleDay)), stats)
    }

    private fun capture(name: String, dark: Boolean = false, tab: Int, content: @Composable () -> Unit) {
        compose.setContent {
            MyApplicationTheme(darkTheme = dark, dynamicColor = false) {
                NetPulseChrome(
                    title = topLevelTitle(tab),
                    selectedTab = tab,
                    onTabSelected = {},
                    onBack = {},
                    onOpenSettings = {}
                ) { padding ->
                    Box(Modifier.padding(padding)) { content() }
                }
            }
        }
        // Let data load and entry animations finish before capturing.
        compose.mainClock.advanceTimeBy(3_000)
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    // View models are created once per test, outside composition, so they survive recomposition.
    private fun captureDashboard(name: String, dark: Boolean) {
        val viewModel = DashboardViewModel(context, stats, planRepository)
        capture(name, dark = dark, tab = 0) {
            DashboardScreen(onOpenApps = {}, onOpenSpeedTest = {}, onOpenHistory = {}, viewModel = viewModel)
        }
    }

    @Test
    fun home() = captureDashboard("01_home", dark = false)

    @Test
    fun apps() {
        val viewModel = AppsViewModel(stats, planRepository)
        capture("02_apps", tab = 1) { AppsScreen(viewModel = viewModel) }
    }

    @Test
    fun history() {
        val viewModel = HistoryViewModel(stats, EmptyDailyUsageDao())
        capture("03_history", tab = 2) { HistoryScreen(viewModel = viewModel) }
    }

    @Test
    fun speed() = capture("04_speed", tab = 3) {
        val now = System.currentTimeMillis()
        SpeedTestContent(
            state = SpeedTestUiState(
                progress = SpeedTestProgress(
                    phase = SpeedTestPhase.DONE,
                    phaseProgress = 1f,
                    pingMs = 18,
                    jitterMs = 3,
                    downloadMbps = 142.6,
                    uploadMbps = 38.4
                ),
                networkType = "Wi-Fi"
            ),
            history = listOf(
                SpeedTestResultEntity(1, now - 3_600_000L, 138.2, 36.9, 19, 4, "Wi-Fi"),
                SpeedTestResultEntity(2, now - 26 * 3_600_000L, 54.7, 12.3, 41, 9, "Mobile"),
                SpeedTestResultEntity(3, now - 50 * 3_600_000L, 121.0, 33.5, 22, 5, "Wi-Fi")
            ),
            onStart = {},
            onCancel = {},
            onClearHistory = {},
            onDismissError = {}
        )
    }

    @Test
    fun homeDark() = captureDashboard("05_home_dark", dark = true)

    companion object {
        @JvmStatic
        @BeforeClass
        fun enableRecording() {
            System.setProperty("roborazzi.test.record", "true")
        }
    }
}
