package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.formatByteSize
import com.example.data.plan.CycleUsage
import com.example.data.plan.PlanAlert

object Notifications {
    const val CHANNEL_ALERTS = "data_alerts"
    const val CHANNEL_LIVE = "live_speed"

    const val ID_PLAN_ALERT = 1001
    const val ID_LIVE_SPEED = 1002

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(
                    CHANNEL_ALERTS,
                    context.getString(R.string.channel_alerts_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply { description = context.getString(R.string.channel_alerts_desc) },
                NotificationChannel(
                    CHANNEL_LIVE,
                    context.getString(R.string.channel_live_name),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = context.getString(R.string.channel_live_desc)
                    setShowBadge(false)
                }
            )
        )
    }

    fun canPost(context: Context): Boolean =
        (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun openAppIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    fun showPlanAlert(context: Context, alert: PlanAlert, usage: CycleUsage) {
        if (alert == PlanAlert.NONE || !canPost(context)) return
        val percent = (usage.fraction * 100).toInt()
        val title = when (alert) {
            PlanAlert.LIMIT -> context.getString(R.string.alert_limit_title)
            else -> context.getString(R.string.alert_warning_title, percent)
        }
        val text = context.getString(
            R.string.alert_body,
            formatByteSize(usage.usedBytes),
            formatByteSize(usage.plan.monthlyLimitBytes)
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_stat_netpulse)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(openAppIntent(context))
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(ID_PLAN_ALERT, notification)
        } catch (_: SecurityException) {
        }
    }
}
