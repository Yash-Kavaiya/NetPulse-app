package com.example.data.export

import android.content.Context
import android.net.Uri
import com.example.data.model.AppNetworkUsage
import com.example.data.model.UsageBucket
import com.example.data.plan.PlanRepository
import com.example.data.prefs.AppSettings
import com.example.data.prefs.SettingsRepository
import com.example.data.room.DataPlanEntity
import com.example.data.room.SpeedTestResultEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

object Csv {
    fun escape(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }

    fun row(vararg values: Any?): String = values.joinToString(",") { escape(it?.toString() ?: "") }

    private fun date(time: Long): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(time))
    private fun dateTime(time: Long): String = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(time))

    fun dailyUsage(buckets: List<UsageBucket>): String = buildString {
        appendLine(row("date", "mobile_bytes", "wifi_bytes", "total_bytes"))
        buckets.forEach { appendLine(row(date(it.startTime), it.mobileBytes, it.wifiBytes, it.totalBytes)) }
    }

    fun apps(apps: List<AppNetworkUsage>): String = buildString {
        appendLine(row("app", "package", "uid", "mobile_rx", "mobile_tx", "wifi_rx", "wifi_tx", "total_bytes"))
        apps.forEach {
            appendLine(
                row(
                    it.appName, it.packageName, it.uid, it.rxBytesMobile, it.txBytesMobile,
                    it.rxBytesWifi, it.txBytesWifi, it.grandTotalBytes
                )
            )
        }
    }

    fun speedTests(results: List<SpeedTestResultEntity>): String = buildString {
        appendLine(row("time", "network", "download_mbps", "upload_mbps", "ping_ms", "jitter_ms"))
        results.forEach {
            appendLine(
                row(
                    dateTime(it.timestamp), it.networkType, "%.2f".format(Locale.US, it.downloadMbps),
                    "%.2f".format(Locale.US, it.uploadMbps), it.pingMs, it.jitterMs
                )
            )
        }
    }
}

@Serializable
data class BackupPlan(
    val monthlyLimitBytes: Long,
    val warningPercent: Int,
    val isEnabled: Boolean,
    val planType: String,
    val cycleStartDay: Int
)

@Serializable
data class BackupFile(
    val version: Int = 1,
    val createdAt: Long,
    val plan: BackupPlan,
    val settings: AppSettings
)

@Singleton
class ExportManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val planRepository: PlanRepository,
    private val settingsRepository: SettingsRepository
) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun writeText(uri: Uri, text: String) = withContext(Dispatchers.IO) {
        context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { it.write(text) }
            ?: error("Unable to open file for writing")
    }

    suspend fun exportBackup(uri: Uri) {
        val plan = planRepository.current()
        val backup = BackupFile(
            createdAt = System.currentTimeMillis(),
            plan = BackupPlan(plan.monthlyLimitBytes, plan.warningPercent, plan.isEnabled, plan.planType, plan.cycleStartDay),
            settings = settingsRepository.current()
        )
        writeText(uri, json.encodeToString(BackupFile.serializer(), backup))
    }

    suspend fun importBackup(uri: Uri) {
        val text = withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: error("Unable to open backup file")
        }
        val backup = json.decodeFromString(BackupFile.serializer(), text)
        require(backup.version == 1) { "Unsupported backup version ${backup.version}" }
        val p = backup.plan
        planRepository.save(
            DataPlanEntity(
                monthlyLimitBytes = p.monthlyLimitBytes.coerceAtLeast(0),
                warningPercent = p.warningPercent.coerceIn(1, 100),
                isEnabled = p.isEnabled,
                planType = if (p.planType == DataPlanEntity.PLAN_TYPE_TOTAL) DataPlanEntity.PLAN_TYPE_TOTAL else DataPlanEntity.PLAN_TYPE_MOBILE,
                cycleStartDay = p.cycleStartDay.coerceIn(1, 31)
            )
        )
        settingsRepository.restore(backup.settings)
    }
}
