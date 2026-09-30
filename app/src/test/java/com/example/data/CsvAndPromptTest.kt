package com.example.data

import com.example.data.ai.InsightInput
import com.example.data.ai.InsightPrompt
import com.example.data.export.Csv
import com.example.data.model.AppNetworkUsage
import com.example.data.model.DeviceNetworkSummary
import com.example.data.model.UsageBucket
import com.example.ui.history.HistoryMerge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvAndPromptTest {
    @Test
    fun `csv escapes commas and quotes`() {
        assertEquals("plain", Csv.escape("plain"))
        assertEquals("\"a,b\"", Csv.escape("a,b"))
        assertEquals("\"say \"\"hi\"\"\"", Csv.escape("say \"hi\""))
    }

    @Test
    fun `app csv has header and one row per app`() {
        val csv = Csv.apps(
            listOf(AppNetworkUsage(uid = 10001, packageName = "com.x", appName = "X, Inc", rxBytesMobile = 1, txBytesWifi = 2))
        )
        val lines = csv.trim().lines()
        assertEquals(2, lines.size)
        assertEquals("\"X, Inc\",com.x,10001,1,0,0,2,3", lines[1])
    }

    @Test
    fun `history merge falls back to snapshots only for empty live days`() {
        val live = listOf(UsageBucket(0, 1, 0, 0), UsageBucket(1, 2, 5, 5))
        val snaps = mapOf(0L to UsageBucket(0, 1, 7, 0), 1L to UsageBucket(1, 2, 99, 99))
        val merged = HistoryMerge.merge(live, snaps)
        assertEquals(7L, merged[0].totalBytes)
        assertEquals(10L, merged[1].totalBytes)
    }

    @Test
    fun `insight prompt contains totals and app names but no package names`() {
        val prompt = InsightPrompt.build(
            InsightInput(
                rangeLabel = "Today",
                summary = DeviceNetworkSummary(mobileRxBytes = 2048),
                topApps = listOf(AppNetworkUsage(uid = 1, packageName = "com.secret.pkg", appName = "Maps", rxBytesMobile = 1024)),
                cycle = null
            )
        )
        assertTrue(prompt.contains("Maps"))
        assertTrue(prompt.contains("Mobile: 2.0 KB"))
        assertFalse(prompt.contains("com.secret.pkg"))
    }
}
