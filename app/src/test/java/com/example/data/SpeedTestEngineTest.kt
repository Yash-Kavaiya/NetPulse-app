package com.example.data

import com.example.data.speedtest.SpeedMath
import com.example.data.speedtest.SpeedTestConfig
import com.example.data.speedtest.SpeedTestEngine
import com.example.data.speedtest.SpeedTestException
import com.example.data.speedtest.SpeedTestPhase
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SpeedTestEngineTest {
    private val server = MockWebServer()

    @Before fun setUp() = server.start()
    @After fun tearDown() = server.shutdown()

    @Test
    fun `mbps math`() {
        assertEquals(8.0, SpeedMath.mbps(1_000_000, 1000), 1e-9)
        assertEquals(0.0, SpeedMath.mbps(100, 0), 1e-9)
        assertEquals(10L, SpeedMath.jitter(listOf(10, 20, 10)))
    }

    @Test
    fun `engine reports ping download and upload`() = runBlocking {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val url = request.requestUrl
                return when (url?.encodedPath) {
                    "/__down" -> {
                        val bytes = url.queryParameter("bytes")?.toInt() ?: 0
                        MockResponse().setBody(Buffer().write(ByteArray(bytes)))
                    }
                    "/__up" -> MockResponse().setBody("ok")
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
        val engine = SpeedTestEngine(OkHttpClient(), server.url("/").toString())
        val config = SpeedTestConfig(
            pingSamples = 3,
            downloadDurationMs = 300,
            uploadDurationMs = 300,
            downloadChunkBytes = 256L * 1024,
            uploadChunkBytes = 128L * 1024
        )
        val events = engine.run(config).toList()
        val done = events.last()
        assertEquals(SpeedTestPhase.DONE, done.phase)
        assertTrue((done.downloadMbps ?: 0.0) > 0.0)
        assertTrue((done.uploadMbps ?: 0.0) > 0.0)
        assertTrue(events.any { it.phase == SpeedTestPhase.DOWNLOAD })
    }

    @Test(expected = SpeedTestException::class)
    fun `http errors surface as SpeedTestException`() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) = MockResponse().setResponseCode(503)
        }
        runBlocking {
            SpeedTestEngine(OkHttpClient(), server.url("/").toString()).run(SpeedTestConfig(pingSamples = 1)).toList()
        }
    }
}
