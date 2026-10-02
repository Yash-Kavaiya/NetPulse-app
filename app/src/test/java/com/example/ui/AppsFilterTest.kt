package com.example.ui

import com.example.data.model.AppNetworkUsage
import com.example.data.model.NetworkFilterType
import com.example.data.model.SortOption
import com.example.ui.apps.AppsUiState
import org.junit.Assert.assertEquals
import org.junit.Test

class AppsFilterTest {
    private val apps = listOf(
        AppNetworkUsage(uid = 1, packageName = "com.video", appName = "Video", rxBytesMobile = 500, rxBytesWifi = 1000),
        AppNetworkUsage(uid = 2, packageName = "com.chat", appName = "Chat", txBytesMobile = 800),
        AppNetworkUsage(uid = 3, packageName = "com.backup", appName = "Backup", txBytesWifi = 5000)
    )

    @Test
    fun `mobile filter hides wifi-only apps and sorts by total`() {
        val result = AppsUiState.filterAndSort(apps, "", NetworkFilterType.MOBILE, SortOption.TOTAL_DESC)
        assertEquals(listOf("Chat", "Video"), result.map { it.appName })
    }

    @Test
    fun `search matches package names case-insensitively`() {
        val result = AppsUiState.filterAndSort(apps, "  BACKUP ", NetworkFilterType.ALL, SortOption.TOTAL_DESC)
        assertEquals(listOf("Backup"), result.map { it.appName })
    }

    @Test
    fun `upload sort and name sort`() {
        assertEquals(
            listOf("Backup", "Chat", "Video"),
            AppsUiState.filterAndSort(apps, "", NetworkFilterType.ALL, SortOption.TX_DESC).map { it.appName }
        )
        assertEquals(
            listOf("Backup", "Chat", "Video"),
            AppsUiState.filterAndSort(apps, "", NetworkFilterType.ALL, SortOption.NAME_ASC).map { it.appName }
        )
    }
}
