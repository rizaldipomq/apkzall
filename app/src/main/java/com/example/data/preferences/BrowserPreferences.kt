package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences

class BrowserPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var opacity: Float
        get() = prefs.getFloat(KEY_OPACITY, 0.95f)
        set(value) = prefs.edit().putFloat(KEY_OPACITY, value.coerceIn(0.2f, 1.0f)).apply()

    var windowWidthDp: Int
        get() = prefs.getInt(KEY_WINDOW_WIDTH, 340)
        set(value) = prefs.edit().putInt(KEY_WINDOW_WIDTH, value.coerceIn(240, 900)).apply()

    var windowHeightDp: Int
        get() = prefs.getInt(KEY_WINDOW_HEIGHT, 480)
        set(value) = prefs.edit().putInt(KEY_WINDOW_HEIGHT, value.coerceIn(280, 1200)).apply()

    var initialPosX: Int
        get() = prefs.getInt(KEY_POS_X, 60)
        set(value) = prefs.edit().putInt(KEY_POS_X, value).apply()

    var initialPosY: Int
        get() = prefs.getInt(KEY_POSY, 120)
        set(value) = prefs.edit().putInt(KEY_POSY, value).apply()

    var homeUrl: String
        get() = prefs.getString(KEY_HOME_URL, "https://www.google.com") ?: "https://www.google.com"
        set(value) = prefs.edit().putString(KEY_HOME_URL, value).apply()

    var searchEngine: String
        get() = prefs.getString(KEY_SEARCH_ENGINE, "Google") ?: "Google"
        set(value) = prefs.edit().putString(KEY_SEARCH_ENGINE, value).apply()

    var desktopMode: Boolean
        get() = prefs.getBoolean(KEY_DESKTOP_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_DESKTOP_MODE, value).apply()

    // Stealth Mode: Activates FLAG_SECURE to prevent screen capture/recording by test/exam apps
    var stealthSecure: Boolean
        get() = prefs.getBoolean(KEY_STEALTH_SECURE, true)
        set(value) = prefs.edit().putBoolean(KEY_STEALTH_SECURE, value).apply()

    // Stealth Mode: Avoids focus-stealing so background games/apps don't pause or trigger focus change listeners
    var stealthNonFocus: Boolean
        get() = prefs.getBoolean(KEY_STEALTH_NON_FOCUS, true)
        set(value) = prefs.edit().putBoolean(KEY_STEALTH_NON_FOCUS, value).apply()

    var bubbleSnap: Boolean
        get() = prefs.getBoolean(KEY_BUBBLE_SNAP, true)
        set(value) = prefs.edit().putBoolean(KEY_BUBBLE_SNAP, value).apply()

    var bubbleSizeDp: Int
        get() = prefs.getInt(KEY_BUBBLE_SIZE, 58)
        set(value) = prefs.edit().putInt(KEY_BUBBLE_SIZE, value.coerceIn(40, 80)).apply()

    var startAsBubble: Boolean
        get() = prefs.getBoolean(KEY_START_AS_BUBBLE, false)
        set(value) = prefs.edit().putBoolean(KEY_START_AS_BUBBLE, value).apply()

    fun getSearchUrl(query: String): String {
        val trimmed = query.trim()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed
        }
        if (trimmed.contains(".") && !trimmed.contains(" ")) {
            return "https://$trimmed"
        }
        val encoded = android.net.Uri.encode(trimmed)
        return when (searchEngine) {
            "DuckDuckGo" -> "https://duckduckgo.com/?q=$encoded"
            "Bing" -> "https://www.bing.com/search?q=$encoded"
            else -> "https://www.google.com/search?q=$encoded"
        }
    }

    companion object {
        private const val PREFS_NAME = "zall_browser_prefs"
        private const val KEY_OPACITY = "pref_opacity"
        private const val KEY_WINDOW_WIDTH = "pref_width"
        private const val KEY_WINDOW_HEIGHT = "pref_height"
        private const val KEY_POS_X = "pref_pos_x"
        private const val KEY_POSY = "pref_pos_y"
        private const val KEY_HOME_URL = "pref_home_url"
        private const val KEY_SEARCH_ENGINE = "pref_search_engine"
        private const val KEY_DESKTOP_MODE = "pref_desktop_mode"
        private const val KEY_STEALTH_SECURE = "pref_stealth_secure"
        private const val KEY_STEALTH_NON_FOCUS = "pref_stealth_non_focus"
        private const val KEY_BUBBLE_SNAP = "pref_bubble_snap"
        private const val KEY_BUBBLE_SIZE = "pref_bubble_size"
        private const val KEY_START_AS_BUBBLE = "pref_start_as_bubble"
    }
}
