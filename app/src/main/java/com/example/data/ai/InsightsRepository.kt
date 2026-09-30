package com.example.data.ai

import android.content.Context
import com.example.BuildConfig
import com.example.data.model.AppNetworkUsage
import com.example.data.model.DeviceNetworkSummary
import com.example.data.model.formatByteSize
import com.example.data.plan.CycleUsage
import com.example.data.room.InsightDao
import com.example.data.room.InsightEntity
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.generationConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** Anonymized usage snapshot sent to Gemini: app labels and byte counts only. */
data class InsightInput(
    val rangeLabel: String,
    val summary: DeviceNetworkSummary,
    val topApps: List<AppNetworkUsage>,
    val cycle: CycleUsage?
)

sealed interface InsightResult {
    data class Success(val text: String) : InsightResult
    data object NotConfigured : InsightResult
    data class Error(val message: String) : InsightResult
}

object InsightPrompt {
    fun build(input: InsightInput): String = buildString {
        appendLine("You are NetPulse, a friendly mobile data-usage assistant on an Android phone.")
        appendLine("Analyze the usage below and reply in Markdown with exactly these sections:")
        appendLine("## Summary (2-3 sentences)")
        appendLine("## Data-hungry apps (bullet list, max 5, with why they may use data)")
        appendLine("## Tips to save data (3-5 concrete Android actions, e.g. restrict background data, Wi-Fi-only updates)")
        appendLine("## Plan advice (1-2 sentences on whether the data plan fits; skip if no plan)")
        appendLine("Be concise, avoid speculation about personal content, and never invent numbers.")
        appendLine()
        appendLine("Period: ${input.rangeLabel}")
        val s = input.summary
        appendLine("Mobile: ${formatByteSize(s.mobileTotalBytes)} (down ${formatByteSize(s.mobileRxBytes)}, up ${formatByteSize(s.mobileTxBytes)})")
        appendLine("Wi-Fi: ${formatByteSize(s.wifiTotalBytes)} (down ${formatByteSize(s.wifiRxBytes)}, up ${formatByteSize(s.wifiTxBytes)})")
        input.cycle?.takeIf { it.plan.isEnabled }?.let { c ->
            val days = ((c.cycleEnd - c.cycleStart) / 86_400_000L).coerceAtLeast(1)
            val elapsed = ((System.currentTimeMillis() - c.cycleStart) / 86_400_000L).coerceIn(1, days)
            appendLine(
                "Plan: ${formatByteSize(c.plan.monthlyLimitBytes)} per cycle (${c.plan.planType.lowercase()} data), " +
                    "used ${formatByteSize(c.usedBytes)} after $elapsed of $days days"
            )
        }
        appendLine("Top apps:")
        input.topApps.forEach { app ->
            appendLine(
                "- ${app.appName}: mobile ${formatByteSize(app.totalBytesMobile)}, " +
                    "Wi-Fi ${formatByteSize(app.totalBytesWifi)}"
            )
        }
    }
}

@Singleton
class InsightsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: InsightDao
) {
    val cached: Flow<InsightEntity?> = dao.observe()

    fun isConfigured(): Boolean = FirebaseApp.getApps(context).isNotEmpty()

    suspend fun generate(input: InsightInput): InsightResult {
        if (!isConfigured()) return InsightResult.NotConfigured
        return try {
            val model = Firebase.ai(backend = GenerativeBackend.googleAI())
                .generativeModel(
                    modelName = BuildConfig.GEMINI_MODEL,
                    generationConfig = generationConfig {
                        temperature = 0.4f
                        maxOutputTokens = 1024
                    }
                )
            val text = model.generateContent(InsightPrompt.build(input)).text?.trim()
            if (text.isNullOrEmpty()) {
                InsightResult.Error("Gemini returned an empty response. Please try again.")
            } else {
                dao.save(InsightEntity(createdAt = System.currentTimeMillis(), rangeLabel = input.rangeLabel, text = text))
                InsightResult.Success(text)
            }
        } catch (e: Exception) {
            InsightResult.Error(e.localizedMessage ?: "Could not reach Gemini. Check your connection.")
        }
    }
}
