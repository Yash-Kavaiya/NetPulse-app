package com.example

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.data.prefs.SettingsRepository
import com.example.live.LiveSpeedService
import com.example.notifications.Notifications
import com.example.work.WorkScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class NetPulseApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var settingsRepository: SettingsRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        Notifications.createChannels(this)
        WorkScheduler.schedule(this)
        appScope.launch {
            if (settingsRepository.current().liveSpeedNotification && Notifications.canPost(this@NetPulseApp)) {
                LiveSpeedService.start(this@NetPulseApp)
            }
        }
    }
}
