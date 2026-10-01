package com.example.ui.onboarding

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.stats.NetworkStatsHelper
import com.example.notifications.Notifications
import com.example.ui.common.OnResume
import com.example.ui.theme.LocalGoogleColors
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val colors = LocalGoogleColors.current
    val pager = rememberPagerState { 3 }
    val scope = rememberCoroutineScope()
    var usageGranted by remember { mutableStateOf(NetworkStatsHelper.hasUsageAccessPermission(context)) }
    var notifGranted by remember { mutableStateOf(Notifications.canPost(context)) }

    OnResume {
        usageGranted = NetworkStatsHelper.hasUsageAccessPermission(context)
        notifGranted = Notifications.canPost(context)
    }

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notifGranted = it
    }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).systemBarsPadding().padding(24.dp)
    ) {
        HorizontalPager(pager, modifier = Modifier.weight(1f), userScrollEnabled = true) { page ->
            when (page) {
                0 -> Page(
                    icon = Icons.Default.DataUsage,
                    title = "Know where your data goes",
                    body = "NetPulse tracks mobile and Wi-Fi usage per app, forecasts your billing cycle, " +
                        "tests your speed and gives you tips to save data."
                )
                1 -> Page(
                    icon = Icons.Default.Security,
                    title = "Allow usage access",
                    body = "Android requires Usage access for apps to read network statistics. " +
                        "Your usage data never leaves the device.",
                    granted = usageGranted,
                    actionLabel = "Open usage access settings",
                    onAction = {
                        val pkgIntent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.parse("package:${context.packageName}"))
                        runCatching { context.startActivity(pkgIntent) }
                            .onFailure { runCatching { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) } }
                    },
                    testTag = "grant_usage_access"
                )
                else -> Page(
                    icon = Icons.Default.NotificationsActive,
                    title = "Get plan alerts",
                    body = "We'll warn you before you run out of data and when you hit your limit. " +
                        "You can change this any time in Settings.",
                    granted = notifGranted,
                    actionLabel = "Allow notifications",
                    onAction = {
                        if (Build.VERSION.SDK_INT >= 33) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    },
                    testTag = "grant_notifications"
                )
            }
        }

        Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.Center) {
            repeat(3) { i ->
                Box(
                    Modifier.padding(4.dp).size(8.dp).clip(CircleShape)
                        .background(if (pager.currentPage == i) colors.uiBlue else colors.borderGray)
                )
            }
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onFinished, modifier = Modifier.testTag("onboarding_skip")) { Text("Skip") }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    if (pager.currentPage < 2) scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                    else onFinished()
                },
                modifier = Modifier.testTag("onboarding_next")
            ) { Text(if (pager.currentPage < 2) "Next" else "Get started") }
        }
    }
}

@Composable
private fun Page(
    icon: ImageVector,
    title: String,
    body: String,
    granted: Boolean? = null,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
    testTag: String? = null
) {
    val colors = LocalGoogleColors.current
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.size(96.dp).clip(CircleShape).background(colors.infoBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = colors.uiBlue, modifier = Modifier.size(48.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = colors.darkGray, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(body, style = MaterialTheme.typography.bodyLarge, color = colors.mediumGray, textAlign = TextAlign.Center)
        if (granted != null && actionLabel != null) {
            Spacer(Modifier.height(24.dp))
            if (granted) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, tint = colors.green)
                    Spacer(Modifier.width(8.dp))
                    Text("Granted", color = colors.green, fontWeight = FontWeight.SemiBold)
                }
            } else {
                OutlinedButton(
                    onClick = onAction,
                    modifier = if (testTag != null) Modifier.testTag(testTag) else Modifier
                ) { Text(actionLabel) }
            }
        }
    }
}
