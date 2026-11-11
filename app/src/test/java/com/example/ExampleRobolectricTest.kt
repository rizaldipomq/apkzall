package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.preferences.BrowserPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Zall Floating Browser", appName)
    }

    @Test
    fun `test browser preferences search query formatting`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = BrowserPreferences(context)
        prefs.searchEngine = "Google"

        assertEquals("https://www.google.com", prefs.getSearchUrl("https://www.google.com"))
        assertEquals("https://example.com", prefs.getSearchUrl("example.com"))
        assertTrue(prefs.getSearchUrl("mobile legends tier list").contains("google.com/search?q=mobile%20legends%20tier%20list"))
    }
}
