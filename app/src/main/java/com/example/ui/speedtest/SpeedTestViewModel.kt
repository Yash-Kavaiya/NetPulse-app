package com.example.ui.speedtest

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.room.SpeedTestDao
import com.example.data.room.SpeedTestResultEntity
import com.example.data.speedtest.SpeedTestEngine
import com.example.data.speedtest.SpeedTestPhase
import com.example.data.speedtest.SpeedTestProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SpeedTestUiState(
    val progress: SpeedTestProgress = SpeedTestProgress(SpeedTestPhase.IDLE),
    val networkType: String = "",
    val error: String? = null
) {
    val isRunning: Boolean
        get() = progress.phase != SpeedTestPhase.IDLE && progress.phase != SpeedTestPhase.DONE
}

@HiltViewModel
class SpeedTestViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val engine: SpeedTestEngine,
    private val dao: SpeedTestDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(SpeedTestUiState())
    val uiState: StateFlow<SpeedTestUiState> = _uiState.asStateFlow()

    val history: StateFlow<List<SpeedTestResultEntity>> = dao.observeRecent(30)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var job: Job? = null

    fun start() {
        if (_uiState.value.isRunning) return
        val network = currentNetworkType()
        if (network == null) {
            _uiState.update { it.copy(error = "You're offline. Connect to a network and try again.") }
            return
        }
        _uiState.update { SpeedTestUiState(progress = SpeedTestProgress(SpeedTestPhase.PING), networkType = network) }
        job = viewModelScope.launch {
            try {
                engine.run().collect { p -> _uiState.update { it.copy(progress = p) } }
                val done = _uiState.value.progress
                dao.insert(
                    SpeedTestResultEntity(
                        timestamp = System.currentTimeMillis(),
                        downloadMbps = done.downloadMbps ?: 0.0,
                        uploadMbps = done.uploadMbps ?: 0.0,
                        pingMs = done.pingMs ?: 0L,
                        jitterMs = done.jitterMs ?: 0L,
                        networkType = network
                    )
                )
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        progress = SpeedTestProgress(SpeedTestPhase.IDLE),
                        error = "Speed test failed: ${e.localizedMessage ?: "network error"}"
                    )
                }
            }
        }
    }

    fun cancel() {
        job?.cancel()
        _uiState.update { it.copy(progress = SpeedTestProgress(SpeedTestPhase.IDLE)) }
    }

    fun clearHistory() {
        viewModelScope.launch { dao.clear() }
    }

    fun dismissError() = _uiState.update { it.copy(error = null) }

    private fun currentNetworkType(): String? {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return null
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return null
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
            else -> "Other"
        }
    }
}
