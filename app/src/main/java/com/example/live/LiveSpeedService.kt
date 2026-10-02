package com.example.live

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.example.R
import com.example.data.model.LiveTrafficSpeed
import com.example.data.stats.UsageStatsSource
import com.example.notifications.Notifications
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service that shows current download/upload throughput in an ongoing notification.
 */
@AndroidEntryPoint
class LiveSpeedService : LifecycleService() {

    @Inject lateinit var stats: UsageStatsSource

    private var job: Job? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        startInForeground(LiveTrafficSpeed())
        if (job == null) {
            _running.value = true
            job = lifecycleScope.launch {
                stats.liveSpeed().collect { speed -> update(speed) }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        job?.cancel()
        job = null
        _running.value = false
        super.onDestroy()
    }

    private fun startInForeground(speed: LiveTrafficSpeed) {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, Notifications.ID_LIVE_SPEED, build(speed), type)
    }

    private fun update(speed: LiveTrafficSpeed) {
        if (!Notifications.canPost(this)) return
        try {
            NotificationManagerCompat.from(this).notify(Notifications.ID_LIVE_SPEED, build(speed))
        } catch (_: SecurityException) {
        }
    }

    private fun build(speed: LiveTrafficSpeed): Notification {
        val stopIntent = PendingIntent.getService(
            this, 1, Intent(this, LiveSpeedService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, Notifications.CHANNEL_LIVE)
            .setSmallIcon(R.drawable.ic_stat_netpulse)
            .setContentTitle(getString(R.string.live_notif_title, speed.rxFormatted, speed.txFormatted))
            .setContentText(getString(R.string.live_notif_text))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(Notifications.openAppIntent(this))
            .addAction(0, getString(R.string.action_stop), stopIntent)
            .build()
    }

    companion object {
        private const val ACTION_STOP = "com.example.live.STOP"

        private val _running = MutableStateFlow(false)
        val running: StateFlow<Boolean> = _running.asStateFlow()

        /** Returns false when the platform refuses to start the service from the background. */
        fun start(context: Context): Boolean = try {
            ContextCompat.startForegroundService(context, Intent(context, LiveSpeedService::class.java))
            true
        } catch (_: IllegalStateException) {
            false
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LiveSpeedService::class.java))
        }
    }
}
