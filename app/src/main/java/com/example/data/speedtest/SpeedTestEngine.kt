package com.example.data.speedtest

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import javax.inject.Inject
import javax.inject.Named
import kotlin.math.abs

enum class SpeedTestPhase { IDLE, PING, DOWNLOAD, UPLOAD, DONE }

data class SpeedTestProgress(
    val phase: SpeedTestPhase,
    /** 0..1 progress within the current phase. */
    val phaseProgress: Float = 0f,
    val currentMbps: Double = 0.0,
    val pingMs: Long? = null,
    val jitterMs: Long? = null,
    val downloadMbps: Double? = null,
    val uploadMbps: Double? = null
)

data class SpeedTestConfig(
    val pingSamples: Int = 8,
    val downloadDurationMs: Long = 8_000L,
    val uploadDurationMs: Long = 8_000L,
    val downloadChunkBytes: Long = 25L * 1024 * 1024,
    val uploadChunkBytes: Long = 4L * 1024 * 1024
)

object SpeedMath {
    /** Converts bytes transferred in [elapsedMs] to megabits per second. */
    fun mbps(bytes: Long, elapsedMs: Long): Double {
        if (elapsedMs <= 0 || bytes <= 0) return 0.0
        return bytes * 8.0 / (elapsedMs / 1000.0) / 1_000_000.0
    }

    /** Mean absolute difference between consecutive samples. */
    fun jitter(samples: List<Long>): Long {
        if (samples.size < 2) return 0L
        return samples.zipWithNext { a, b -> abs(b - a) }.average().toLong()
    }
}

/**
 * HTTP throughput test against Cloudflare's public speed-test endpoints
 * (`/__down?bytes=N` and `/__up`). Latency is measured as the median HTTP round trip of a
 * zero-byte download on a warm connection.
 */
class SpeedTestEngine @Inject constructor(
    private val client: OkHttpClient,
    @Named("speedTestBaseUrl") private val baseUrl: String
) {
    private val base: HttpUrl get() = baseUrl.toHttpUrl()

    fun run(config: SpeedTestConfig = SpeedTestConfig()): Flow<SpeedTestProgress> = flow {
        emit(SpeedTestProgress(SpeedTestPhase.PING))
        val pings = mutableListOf<Long>()
        repeat(config.pingSamples) { i ->
            currentCoroutineContext().ensureActive()
            val t0 = System.nanoTime()
            execute(downloadRequest(0))
            val ms = (System.nanoTime() - t0) / 1_000_000
            // The first request includes DNS + TLS setup; skip it for latency.
            if (i > 0) pings += ms
            emit(SpeedTestProgress(SpeedTestPhase.PING, (i + 1f) / config.pingSamples))
        }
        val ping = pings.sorted().let { if (it.isEmpty()) 0L else it[it.size / 2] }
        val jitter = SpeedMath.jitter(pings)

        val download = measureDownload(config, ping, jitter)
        val upload = measureUpload(config, ping, jitter, download)

        emit(
            SpeedTestProgress(
                phase = SpeedTestPhase.DONE,
                phaseProgress = 1f,
                pingMs = ping,
                jitterMs = jitter,
                downloadMbps = download,
                uploadMbps = upload
            )
        )
    }.flowOn(Dispatchers.IO)

    private suspend fun FlowCollector<SpeedTestProgress>.measureDownload(
        config: SpeedTestConfig, ping: Long, jitter: Long
    ): Double {
        val start = System.currentTimeMillis()
        var total = 0L
        var lastEmit = 0L
        val buffer = ByteArray(64 * 1024)
        while (System.currentTimeMillis() - start < config.downloadDurationMs) {
            client.newCall(downloadRequest(config.downloadChunkBytes)).execute().use { response ->
                if (!response.isSuccessful) throw SpeedTestException("Download failed: HTTP ${response.code}")
                val stream = response.body?.byteStream() ?: throw SpeedTestException("Empty response body")
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val read = stream.read(buffer)
                    if (read < 0) break
                    total += read
                    val elapsed = System.currentTimeMillis() - start
                    if (elapsed - lastEmit >= 150) {
                        lastEmit = elapsed
                        emit(
                            SpeedTestProgress(
                                phase = SpeedTestPhase.DOWNLOAD,
                                phaseProgress = (elapsed.toFloat() / config.downloadDurationMs).coerceAtMost(1f),
                                currentMbps = SpeedMath.mbps(total, elapsed),
                                pingMs = ping,
                                jitterMs = jitter
                            )
                        )
                    }
                    if (elapsed >= config.downloadDurationMs) break
                }
            }
        }
        return SpeedMath.mbps(total, System.currentTimeMillis() - start)
    }

    private suspend fun FlowCollector<SpeedTestProgress>.measureUpload(
        config: SpeedTestConfig, ping: Long, jitter: Long, download: Double
    ): Double {
        val payload = ByteArray(64 * 1024) { (it % 251).toByte() }
        val start = System.currentTimeMillis()
        var total = 0L
        while (System.currentTimeMillis() - start < config.uploadDurationMs) {
            currentCoroutineContext().ensureActive()
            val body = object : RequestBody() {
                override fun contentType() = "application/octet-stream".toMediaType()
                override fun contentLength() = config.uploadChunkBytes
                override fun writeTo(sink: BufferedSink) {
                    var remaining = config.uploadChunkBytes
                    while (remaining > 0) {
                        val n = minOf(remaining, payload.size.toLong()).toInt()
                        sink.write(payload, 0, n)
                        remaining -= n
                    }
                }
            }
            val request = Request.Builder().url(base.newBuilder().addPathSegment("__up").build()).post(body).build()
            execute(request)
            total += config.uploadChunkBytes
            val elapsed = System.currentTimeMillis() - start
            emit(
                SpeedTestProgress(
                    phase = SpeedTestPhase.UPLOAD,
                    phaseProgress = (elapsed.toFloat() / config.uploadDurationMs).coerceAtMost(1f),
                    currentMbps = SpeedMath.mbps(total, elapsed),
                    pingMs = ping,
                    jitterMs = jitter,
                    downloadMbps = download
                )
            )
        }
        return SpeedMath.mbps(total, System.currentTimeMillis() - start)
    }

    private fun downloadRequest(bytes: Long): Request =
        Request.Builder()
            .url(base.newBuilder().addPathSegment("__down").addQueryParameter("bytes", bytes.toString()).build())
            .header("Cache-Control", "no-cache")
            .build()

    /** Executes a small request and drains its body so the connection can be reused. */
    private fun execute(request: Request) {
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw SpeedTestException("HTTP ${response.code}")
            response.body?.bytes()
        }
    }
}

class SpeedTestException(message: String) : Exception(message)
