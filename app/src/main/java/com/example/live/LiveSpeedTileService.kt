package com.example.live

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.example.R
import com.example.data.prefs.SettingsRepository
import com.example.notifications.Notifications
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Quick Settings tile that toggles the live-speed notification. */
@RequiresApi(Build.VERSION_CODES.N)
@AndroidEntryPoint
class LiveSpeedTileService : TileService() {

    @Inject lateinit var settingsRepository: SettingsRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onStartListening() {
        super.onStartListening()
        render(LiveSpeedService.running.value)
    }

    override fun onClick() {
        super.onClick()
        val enable = !LiveSpeedService.running.value
        if (enable && !Notifications.canPost(this)) {
            // Notifications are required for the live indicator; open the app to request them.
            val intent = Notifications.openAppIntent(this)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startActivityAndCollapse(intent)
            }
            return
        }
        val active = if (enable) LiveSpeedService.start(this) else {
            LiveSpeedService.stop(this)
            false
        }
        scope.launch { settingsRepository.setLiveSpeedNotification(active) }
        render(active)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun render(active: Boolean) {
        val tile = qsTile ?: return
        tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.tile_label)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = getString(if (active) R.string.tile_on else R.string.tile_off)
        }
        tile.updateTile()
    }
}
