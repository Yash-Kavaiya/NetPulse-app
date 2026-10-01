package com.example.ui.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.ui.common.SectionCard
import com.example.ui.theme.LocalGoogleColors

private val sections = listOf(
    "How it works" to
        "NetPulse reads Android's NetworkStatsManager — the same counters behind Settings › Network usage. " +
            "It aggregates mobile and Wi-Fi buckets per app and splits them into foreground and background " +
            "traffic. Live speed comes from TrafficStats sampled once per second.",
    "Why usage access?" to
        "Android only exposes per-app network statistics to apps the user grants Usage access. NetPulse uses " +
            "it solely to read network counters; it does not read which screens you open or your content.",
    "Privacy" to
        "All usage data stays on your device. The only network activity is the speed test, which exchanges " +
            "test data with Cloudflare's speed test servers when you start it. No account, advertising ID " +
            "or analytics are used.",
    "Accuracy" to
        "Carriers may count data differently (rounding, zero-rated apps, tethering rules). Treat NetPulse as an " +
            "estimate and check your carrier's app for billing figures.",
    "Background work" to
        "A periodic job saves daily totals and checks your plan once an hour, so alerts arrive even when the " +
            "app is closed. The live speed notification runs only while you keep it switched on."
)

@Composable
fun AboutScreen() {
    val colors = LocalGoogleColors.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionCard {
                Text("NetPulse", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = colors.darkGray)
                Text(
                    "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.mediumGray
                )
            }
        }
        sections.forEach { (title, body) ->
            item {
                SectionCard(title = title) {
                    Text(body, style = MaterialTheme.typography.bodyMedium, color = colors.darkGray, modifier = Modifier.padding(bottom = 4.dp))
                }
            }
        }
    }
}
