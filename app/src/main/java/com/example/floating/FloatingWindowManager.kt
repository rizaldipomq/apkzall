package com.example.floating

import android.content.Context
import android.provider.Settings
import com.example.data.preferences.BrowserPreferences

class FloatingWindowManager(
    private val context: Context,
    private val preferences: BrowserPreferences,
    private val onStopServiceRequested: () -> Unit
) {

    enum class OverlayMode {
        NONE,
        WINDOW,
        BUBBLE
    }

    var currentMode: OverlayMode = OverlayMode.NONE
        private set

    private var dismissTargetView: DismissTargetView? = null
    private var bubbleView: FloatingBubbleView? = null
    private var browserView: FloatingBrowserView? = null

    init {
        dismissTargetView = DismissTargetView(context)
    }

    fun hasOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun showBrowserWindow(urlToLoad: String? = null) {
        if (!hasOverlayPermission()) {
            onStopServiceRequested()
            return
        }

        // Hide bubble if active
        bubbleView?.hide()

        if (browserView == null) {
            browserView = FloatingBrowserView(
                context = context,
                preferences = preferences,
                onMinimizeToBubble = {
                    showBubble()
                },
                onCloseWindow = {
                    closeAll()
                    onStopServiceRequested()
                },
                onPageLoadingChanged = { isLoading ->
                    bubbleView?.setLoading(isLoading)
                }
            )
        }

        browserView?.show()
        currentMode = OverlayMode.WINDOW

        if (!urlToLoad.isNullOrBlank()) {
            browserView?.loadUrl(urlToLoad)
        }
    }

    fun showBubble() {
        if (!hasOverlayPermission()) {
            onStopServiceRequested()
            return
        }

        // Hide browser window
        browserView?.hide()

        if (bubbleView == null) {
            bubbleView = FloatingBubbleView(
                context = context,
                preferences = preferences,
                onBubbleClick = {
                    showBrowserWindow()
                },
                onDismiss = {
                    closeAll()
                    onStopServiceRequested()
                },
                onDragMove = { rawX, rawY ->
                    val metrics = context.resources.displayMetrics
                    val isHovering = dismissTargetView?.checkHover(rawX, rawY, metrics.widthPixels, metrics.heightPixels) ?: false
                    if (isHovering) {
                        dismissTargetView?.show()
                    } else {
                        dismissTargetView?.show()
                    }
                    isHovering
                }
            )
        }

        bubbleView?.show()
        currentMode = OverlayMode.BUBBLE
        dismissTargetView?.hide()
    }

    fun loadUrl(url: String) {
        if (currentMode != OverlayMode.WINDOW) {
            showBrowserWindow(url)
        } else {
            browserView?.loadUrl(url)
        }
    }

    fun onConfigurationChanged() {
        // Handles screen rotation / screen size changes
        if (currentMode == OverlayMode.WINDOW) {
            browserView?.let {
                // Keep window inside new screen bounds
                val metrics = context.resources.displayMetrics
                if (it.layoutParams.x > metrics.widthPixels - 100) {
                    it.layoutParams.x = metrics.widthPixels - it.layoutParams.width - 20
                }
                if (it.layoutParams.y > metrics.heightPixels - 100) {
                    it.layoutParams.y = metrics.heightPixels - it.layoutParams.height - 20
                }
            }
        }
    }

    fun closeAll() {
        dismissTargetView?.hide()
        bubbleView?.hide()
        browserView?.destroy()
        browserView = null
        bubbleView = null
        currentMode = OverlayMode.NONE
    }
}
