package com.example.ui.settings

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.formatByteSize
import com.example.data.prefs.ThemeMode
import com.example.notifications.Notifications
import com.example.ui.common.SectionCard
import com.example.ui.components.DataPlanDialog
import com.example.ui.theme.LocalGoogleColors

@Composable
fun SettingsScreen(
    snackbarHostState: SnackbarHostState,
    onOpenAbout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val plan by viewModel.plan.collectAsStateWithLifecycle()
    var showPlanDialog by rememberSaveable { mutableStateOf(false) }
    var pendingExport by remember { mutableStateOf<ExportKind?>(null) }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val createDocument = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        val kind = pendingExport
        if (uri != null && kind != null) viewModel.export(kind, uri)
        pendingExport = null
    }
    val openDocument = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.restore(uri)
    }

    fun startExport(kind: ExportKind) {
        pendingExport = kind
        createDocument.launch(kind.fileName)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("settings_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "plan") {
            SectionCard(title = "Data plan") {
                NavRow(
                    title = if (plan.isEnabled) "${formatByteSize(plan.monthlyLimitBytes)} per cycle" else "No plan set",
                    subtitle = "Resets on day ${plan.cycleStartDay} · warns at ${plan.warningPercent}%",
                    onClick = { showPlanDialog = true },
                    modifier = Modifier.testTag("edit_plan_row")
                )
                ToggleRow(
                    title = "Usage alerts",
                    subtitle = "Notify at the warning threshold and when the limit is reached",
                    checked = settings.alertsEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled && Build.VERSION.SDK_INT >= 33 && !Notifications.canPost(context)) {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        viewModel.setAlerts(enabled)
                    }
                )
            }
        }

        item(key = "appearance") {
            SectionCard(title = "Appearance") {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    ThemeMode.entries.forEachIndexed { i, mode ->
                        SegmentedButton(
                            selected = settings.themeMode == mode,
                            onClick = { viewModel.setTheme(mode) },
                            shape = SegmentedButtonDefaults.itemShape(i, ThemeMode.entries.size)
                        ) { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ToggleRow(
                        title = "Dynamic color",
                        subtitle = "Match colors to your wallpaper",
                        checked = settings.dynamicColor,
                        onCheckedChange = viewModel::setDynamicColor
                    )
                }
            }
        }

        item(key = "data") {
            SectionCard(title = "Export & backup") {
                NavRow("Export daily usage (CSV)", "Last 90 days, mobile and Wi-Fi", { startExport(ExportKind.DAILY_CSV) })
                NavRow("Export app usage (CSV)", "Current billing cycle", { startExport(ExportKind.APPS_CSV) })
                NavRow("Export speed tests (CSV)", "All saved results", { startExport(ExportKind.SPEED_CSV) })
                NavRow("Back up settings", "Data plan and preferences as JSON", { startExport(ExportKind.BACKUP) })
                NavRow("Restore from backup", "Pick a NetPulse backup file", { openDocument.launch(arrayOf("application/json", "*/*")) })
            }
        }

        item(key = "system") {
            SectionCard(title = "System") {
                NavRow("Usage access", "Manage the permission NetPulse uses to read network stats", {
                    runCatching {
                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                })
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    NavRow("Notification settings", "Choose which alerts you receive", {
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    })
                }
                NavRow("About NetPulse", "Version, privacy and how it works", onOpenAbout)
            }
        }
    }

    if (showPlanDialog) {
        DataPlanDialog(
            currentPlan = plan,
            onSave = {
                viewModel.savePlan(it)
                showPlanDialog = false
            },
            onDismiss = { showPlanDialog = false }
        )
    }
}

@Composable
private fun NavRow(title: String, subtitle: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalGoogleColors.current
    Row(
        modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = colors.darkGray)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.mediumGray)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.mediumGray)
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val colors = LocalGoogleColors.current
    Row(
        Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = colors.darkGray)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.mediumGray)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
