package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.room.DataPlanEntity
import com.example.ui.theme.BorderGray
import com.example.ui.theme.GoogleGreen
import com.example.ui.theme.GoogleUIBlue
import com.example.ui.theme.LocalGoogleColors

@Composable
fun DataPlanDialog(
    currentPlan: DataPlanEntity?,
    onSave: (DataPlanEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val googleColors = LocalGoogleColors.current

    val initialGb = remember(currentPlan) {
        val gb = (currentPlan?.monthlyLimitBytes ?: (10L * 1024L * 1024L * 1024L)) / (1024.0 * 1024.0 * 1024.0)
        String.format(java.util.Locale.US, "%.1f", gb)
    }

    var gbInput by remember { mutableStateOf(initialGb) }
    var isEnabled by remember { mutableStateOf(currentPlan?.isEnabled ?: true) }
    var warningPercent by remember { mutableIntStateOf(currentPlan?.warningPercent ?: 80) }
    var planType by remember { mutableStateOf(currentPlan?.planType ?: DataPlanEntity.PLAN_TYPE_MOBILE) }
    var cycleDay by remember { mutableIntStateOf(currentPlan?.cycleStartDay ?: 1) }
    val gbValue = gbInput.replace(",", ".").toDoubleOrNull()
    val inputValid = gbValue != null && gbValue > 0.0 && gbValue < 100_000.0

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("data_plan_dialog"),
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        icon = {
            Icon(
                imageVector = Icons.Default.DataUsage,
                contentDescription = null,
                tint = GoogleUIBlue,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Data Budget & Alerts",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = googleColors.darkGray
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                // Enable switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Enable Data Budget",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = googleColors.darkGray
                    )
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { isEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = googleColors.white,
                            checkedTrackColor = GoogleUIBlue
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Network Target Selector: Cellular vs Total
                Text(
                    text = "Tracked Traffic",
                    style = MaterialTheme.typography.labelMedium,
                    color = googleColors.mediumGray
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(DataPlanEntity.PLAN_TYPE_MOBILE to "Cellular Only", DataPlanEntity.PLAN_TYPE_TOTAL to "Cellular + Wi-Fi").forEach { (type, label) ->
                        val selected = planType == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) googleColors.infoBg else googleColors.lightGray)
                                .clickable { planType = type }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) GoogleUIBlue else googleColors.darkGray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Monthly Quota in GB input
                Text(
                    text = "Monthly Data Limit (GB)",
                    style = MaterialTheme.typography.labelMedium,
                    color = googleColors.mediumGray
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = gbInput,
                    onValueChange = { gbInput = it },
                    singleLine = true,
                    isError = !inputValid,
                    supportingText = if (!inputValid) {
                        { Text("Enter a limit greater than 0") }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoogleUIBlue,
                        unfocusedBorderColor = BorderGray
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                // Quick GB presets
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("2.0", "5.0", "10.0", "20.0", "50.0").forEach { preset ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(googleColors.lightGray)
                                .clickable { gbInput = preset }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${preset.substringBefore(".")}GB",
                                style = MaterialTheme.typography.labelSmall,
                                color = googleColors.darkGray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Warning threshold percent
                Text(
                    text = "Warning Alert Threshold: $warningPercent%",
                    style = MaterialTheme.typography.labelMedium,
                    color = googleColors.mediumGray
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(70, 80, 90).forEach { pct ->
                        val isSelected = warningPercent == pct
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) googleColors.infoBg else googleColors.lightGray)
                                .clickable { warningPercent = pct }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$pct%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) GoogleUIBlue else googleColors.darkGray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Billing cycle reset day
                Text(
                    text = "Billing cycle resets on day",
                    style = MaterialTheme.typography.labelMedium,
                    color = googleColors.mediumGray
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = { cycleDay = if (cycleDay <= 1) 31 else cycleDay - 1 },
                        modifier = Modifier.testTag("cycle_day_minus")
                    ) { Text("-") }
                    Text(
                        text = "$cycleDay",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = googleColors.darkGray,
                        modifier = Modifier.padding(horizontal = 20.dp).testTag("cycle_day_value")
                    )
                    OutlinedButton(
                        onClick = { cycleDay = if (cycleDay >= 31) 1 else cycleDay + 1 },
                        modifier = Modifier.testTag("cycle_day_plus")
                    ) { Text("+") }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val bytes = ((gbValue ?: 10.0) * 1024.0 * 1024.0 * 1024.0).toLong()
                    val base = currentPlan ?: DataPlanEntity()
                    val resetAlerts = base.monthlyLimitBytes != bytes || base.warningPercent != warningPercent ||
                        base.planType != planType || base.cycleStartDay != cycleDay
                    onSave(
                        base.copy(
                            monthlyLimitBytes = bytes,
                            warningPercent = warningPercent,
                            isEnabled = isEnabled,
                            planType = planType,
                            cycleStartDay = cycleDay,
                            // Changing the plan re-arms alerts for the current cycle.
                            warningAlertedCycle = if (resetAlerts) 0L else base.warningAlertedCycle,
                            limitAlertedCycle = if (resetAlerts) 0L else base.limitAlertedCycle
                        )
                    )
                },
                enabled = inputValid,
                modifier = Modifier.testTag("save_plan_button"),
                colors = ButtonDefaults.buttonColors(containerColor = GoogleUIBlue),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("Save Budget")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("Cancel", color = googleColors.mediumGray)
            }
        }
    )
}
