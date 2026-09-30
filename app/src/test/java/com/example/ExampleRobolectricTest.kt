package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.formatByteSize
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("NetPulse", appName)
  }

  @Test
  fun `byte formatting formats appropriately`() {
    assertEquals("500 B", formatByteSize(500))
    assertEquals("1.5 KB", formatByteSize(1536))
    assertEquals("1.00 MB", formatByteSize(1024 * 1024))
    assertEquals("2.50 GB", formatByteSize((2.5 * 1024 * 1024 * 1024).toLong()))
  }
}

