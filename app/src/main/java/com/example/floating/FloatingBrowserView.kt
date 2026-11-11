package com.example.floating

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.model.BrowserTab
import com.example.data.preferences.BrowserPreferences
import kotlin.math.max
import kotlin.math.min

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
class FloatingBrowserView(
    private val context: Context,
    private val preferences: BrowserPreferences,
    private val onMinimizeToBubble: () -> Unit,
    private val onCloseWindow: () -> Unit,
    private val onPageLoadingChanged: (Boolean) -> Unit
) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    private val density = context.resources.displayMetrics.density

    var isAttached = false
        private set

    lateinit var rootView: FrameLayout
        private set
    lateinit var layoutParams: WindowManager.LayoutParams
        private set

    // Tabs
    private val tabs = mutableListOf<BrowserTab>()
    private var activeTabIndex = 0

    // Views
    private lateinit var headerBar: LinearLayout
    private lateinit var titleTextView: TextView
    private lateinit var tabScrollView: HorizontalScrollView
    private lateinit var tabContainer: LinearLayout
    private lateinit var newTabButton: ImageButton

    private lateinit var navBar: LinearLayout
    private lateinit var btnBack: ImageButton
    private lateinit var btnForward: ImageButton
    private lateinit var btnRefresh: ImageButton
    private lateinit var btnHome: ImageButton
    private lateinit var urlEditText: EditText
    private lateinit var btnClearUrl: ImageButton
    private lateinit var btnDesktop: ImageButton

    private lateinit var btnStealth: ImageButton
    private lateinit var btnOpacity: ImageButton
    private lateinit var btnMaximize: ImageButton
    private lateinit var btnMinimize: ImageButton
    private lateinit var btnClose: ImageButton

    private lateinit var webContainer: FrameLayout
    private lateinit var pageProgressBar: ProgressBar
    private lateinit var errorLayout: LinearLayout
    private lateinit var errorTextView: TextView
    private lateinit var retryButton: TextView
    private lateinit var resizeHandle: ImageView

    // State for drag & resize
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var initialWidth = 0
    private var initialHeight = 0
    private var isFullscreen = false
    private var savedWidth = 0
    private var savedHeight = 0
    private var savedX = 0
    private var savedY = 0

    private var lastHeaderTapTime = 0L

    init {
        createViews()
        initLayoutParams()
        createNewTab(preferences.homeUrl)
    }

    private fun initLayoutParams() {
        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        var flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

        if (preferences.stealthSecure) {
            flags = flags or WindowManager.LayoutParams.FLAG_SECURE
        }

        if (preferences.stealthNonFocus) {
            flags = flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }

        val screenWidth = context.resources.displayMetrics.widthPixels
        val defaultWidth = min((preferences.windowWidthDp * density).toInt(), screenWidth - dpToPx(32))
        val defaultHeight = (preferences.windowHeightDp * density).toInt()

        layoutParams = WindowManager.LayoutParams(
            defaultWidth,
            defaultHeight,
            windowType,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = preferences.initialPosX
            y = preferences.initialPosY
        }
    }

    private fun createViews() {
        rootView = FrameLayout(context).apply {
            background = ContextCompat.getDrawable(context, R.drawable.bg_floating_window)
            elevation = 20 * density
            alpha = preferences.opacity
            clipToOutline = true
        }

        val contentLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        rootView.addView(contentLayout)

        // 1. Header Bar
        buildHeaderBar(contentLayout)

        // 2. Tab Bar
        buildTabBar(contentLayout)

        // 3. Navigation & Address Bar
        buildNavBar(contentLayout)

        // 4. Web Container
        buildWebContainer(contentLayout)

        // 5. Corner Resize Handle (overlayed at bottom-right)
        buildResizeHandle()
    }

    private fun buildHeaderBar(parent: LinearLayout) {
        headerBar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.parseColor("#101726"))
            setPadding(dpToPx(10), dpToPx(6), dpToPx(8), dpToPx(6))
        }

        val icon = ImageView(context).apply {
            setImageResource(R.drawable.ic_zall_logo)
            setColorFilter(Color.parseColor("#00D2FF"))
        }
        headerBar.addView(icon, LinearLayout.LayoutParams(dpToPx(20), dpToPx(20)))

        titleTextView = TextView(context).apply {
            text = "Zall Floating Browser"
            setTextColor(Color.parseColor("#F1F5F9"))
            textSize = 12f
            setSingleLine(true)
            ellipsize = android.text.TextUtils.TruncateAt.END
            val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            params.marginStart = dpToPx(8)
            params.marginEnd = dpToPx(4)
            layoutParams = params
        }
        headerBar.addView(titleTextView)

        // Header Action Buttons
        btnStealth = createHeaderButton(
            if (preferences.stealthSecure) android.R.drawable.ic_menu_view else android.R.drawable.ic_menu_help,
            "Stealth Shield"
        ) {
            toggleStealthMode()
        }
        headerBar.addView(btnStealth)

        btnOpacity = createHeaderButton(android.R.drawable.ic_menu_sort_by_size, "Transparansi") {
            cycleOpacity()
        }
        headerBar.addView(btnOpacity)

        btnMinimize = createHeaderButton(android.R.drawable.arrow_down_float, "Minimize") {
            onMinimizeToBubble()
        }
        headerBar.addView(btnMinimize)

        btnMaximize = createHeaderButton(android.R.drawable.arrow_up_float, "Maximize") {
            toggleFullscreen()
        }
        headerBar.addView(btnMaximize)

        btnClose = createHeaderButton(android.R.drawable.ic_menu_close_clear_cancel, "Tutup") {
            onCloseWindow()
        }
        headerBar.addView(btnClose)

        parent.addView(
            headerBar,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        setupHeaderDragListener()
    }

    private fun createHeaderButton(iconRes: Int, tooltip: String, onClick: () -> Unit): ImageButton {
        return ImageButton(context).apply {
            setImageResource(iconRes)
            setBackgroundColor(Color.TRANSPARENT)
            setColorFilter(Color.parseColor("#00D2FF"))
            contentDescription = tooltip
            setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4))
            setOnClickListener { onClick() }
        }.also {
            val params = LinearLayout.LayoutParams(dpToPx(26), dpToPx(26))
            params.marginStart = dpToPx(2)
            it.layoutParams = params
        }
    }

    private fun buildTabBar(parent: LinearLayout) {
        val tabWrapper = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.parseColor("#0B101D"))
            setPadding(dpToPx(6), dpToPx(2), dpToPx(6), dpToPx(2))
        }

        tabScrollView = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        tabContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        tabScrollView.addView(tabContainer)

        val scrollParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        tabWrapper.addView(tabScrollView, scrollParams)

        newTabButton = ImageButton(context).apply {
            setImageResource(android.R.drawable.ic_input_add)
            setBackgroundColor(Color.TRANSPARENT)
            setColorFilter(Color.parseColor("#00D2FF"))
            contentDescription = "Tab Baru"
            setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4))
            setOnClickListener {
                createNewTab(preferences.homeUrl)
            }
        }
        val btnParams = LinearLayout.LayoutParams(dpToPx(28), dpToPx(28)).apply {
            marginStart = dpToPx(4)
        }
        tabWrapper.addView(newTabButton, btnParams)

        parent.addView(
            tabWrapper,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
    }

    private fun buildNavBar(parent: LinearLayout) {
        navBar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.parseColor("#101726"))
            setPadding(dpToPx(6), dpToPx(4), dpToPx(6), dpToPx(4))
        }

        btnBack = createNavButton(android.R.drawable.ic_media_previous, "Kembali") {
            getActiveTab()?.webView?.let { if (it.canGoBack()) it.goBack() }
        }
        navBar.addView(btnBack)

        btnForward = createNavButton(android.R.drawable.ic_media_next, "Maju") {
            getActiveTab()?.webView?.let { if (it.canGoForward()) it.goForward() }
        }
        navBar.addView(btnForward)

        btnRefresh = createNavButton(android.R.drawable.ic_menu_rotate, "Refresh") {
            getActiveTab()?.let { tab ->
                if (tab.isLoading) tab.webView?.stopLoading() else tab.webView?.reload()
            }
        }
        navBar.addView(btnRefresh)

        btnHome = createNavButton(android.R.drawable.ic_menu_myplaces, "Home") {
            loadUrl(preferences.homeUrl)
        }
        navBar.addView(btnHome)

        // URL Field Container
        val urlContainer = FrameLayout(context).apply {
            background = ContextCompat.getDrawable(context, R.drawable.bg_url_bar)
            setPadding(dpToPx(10), dpToPx(2), dpToPx(6), dpToPx(2))
        }

        urlEditText = EditText(context).apply {
            hint = "Ketik URL atau cari..."
            setHintTextColor(Color.parseColor("#64748B"))
            setTextColor(Color.parseColor("#F1F5F9"))
            textSize = 12f
            setBackgroundColor(Color.TRANSPARENT)
            setSingleLine(true)
            imeOptions = EditorInfo.IME_ACTION_GO
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_URI or android.text.InputType.TYPE_CLASS_TEXT

            setOnFocusChangeListener { _, hasFocus ->
                handleFocusChange(hasFocus)
                btnClearUrl.visibility = if (hasFocus && text.isNotEmpty()) View.VISIBLE else View.GONE
            }

            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE) {
                    val input = text.toString()
                    loadUrl(preferences.getSearchUrl(input))
                    clearFocus()
                    hideKeyboard()
                    handleFocusChange(false)
                    true
                } else false
            }
        }
        val editParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER_VERTICAL
        ).apply {
            marginEnd = dpToPx(24)
        }
        urlContainer.addView(urlEditText, editParams)

        btnClearUrl = ImageButton(context).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setBackgroundColor(Color.TRANSPARENT)
            setColorFilter(Color.parseColor("#94A3B8"))
            visibility = View.GONE
            setOnClickListener {
                urlEditText.text.clear()
            }
        }
        val clearParams = FrameLayout.LayoutParams(
            dpToPx(22),
            dpToPx(22),
            Gravity.END or Gravity.CENTER_VERTICAL
        )
        urlContainer.addView(btnClearUrl, clearParams)

        val urlParams = LinearLayout.LayoutParams(0, dpToPx(34), 1f).apply {
            marginStart = dpToPx(4)
            marginEnd = dpToPx(4)
        }
        navBar.addView(urlContainer, urlParams)

        btnDesktop = createNavButton(android.R.drawable.ic_menu_agenda, "Desktop Mode") {
            preferences.desktopMode = !preferences.desktopMode
            updateDesktopMode()
        }
        navBar.addView(btnDesktop)

        parent.addView(
            navBar,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
    }

    private fun createNavButton(iconRes: Int, tooltip: String, onClick: () -> Unit): ImageButton {
        return ImageButton(context).apply {
            setImageResource(iconRes)
            setBackgroundColor(Color.TRANSPARENT)
            setColorFilter(Color.parseColor("#38BDF8"))
            contentDescription = tooltip
            setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4))
            setOnClickListener { onClick() }
        }.also {
            val params = LinearLayout.LayoutParams(dpToPx(30), dpToPx(30))
            it.layoutParams = params
        }
    }

    private fun buildWebContainer(parent: LinearLayout) {
        val containerFrame = FrameLayout(context).apply {
            setBackgroundColor(Color.parseColor("#080B11"))
        }

        webContainer = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        containerFrame.addView(webContainer)

        // Glowing cyan progress bar
        pageProgressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = false
            max = 100
            progress = 0
            visibility = View.GONE
            progressDrawable?.setTint(Color.parseColor("#00D2FF"))
        }
        val progParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            dpToPx(3),
            Gravity.TOP
        )
        containerFrame.addView(pageProgressBar, progParams)

        // Error view
        buildErrorView(containerFrame)

        val containerParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        )
        parent.addView(containerFrame, containerParams)
    }

    private fun buildErrorView(parent: FrameLayout) {
        errorLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#0F1522"))
            visibility = View.GONE
            setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20))
        }

        val errIcon = ImageView(context).apply {
            setImageResource(android.R.drawable.ic_dialog_alert)
            setColorFilter(Color.parseColor("#EF4444"))
        }
        errorLayout.addView(errIcon, LinearLayout.LayoutParams(dpToPx(40), dpToPx(40)))

        errorTextView = TextView(context).apply {
            text = "Gagal memuat halaman.\nPeriksa koneksi internet Anda."
            setTextColor(Color.parseColor("#F1F5F9"))
            textSize = 13f
            gravity = Gravity.CENTER
            val p = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(12)
                bottomMargin = dpToPx(14)
            }
            layoutParams = p
        }
        errorLayout.addView(errorTextView)

        retryButton = TextView(context).apply {
            text = "Coba Lagi"
            setTextColor(Color.parseColor("#080B11"))
            textSize = 13f
            gravity = Gravity.CENTER
            background = ContextCompat.getDrawable(context, R.drawable.bg_url_bar)
            background?.setTint(Color.parseColor("#00D2FF"))
            setPadding(dpToPx(18), dpToPx(8), dpToPx(18), dpToPx(8))
            setOnClickListener {
                errorLayout.visibility = View.GONE
                getActiveTab()?.webView?.reload()
            }
        }
        errorLayout.addView(retryButton)

        parent.addView(
            errorLayout,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
    }

    private fun buildResizeHandle() {
        resizeHandle = ImageView(context).apply {
            setImageResource(android.R.drawable.ic_menu_crop)
            setColorFilter(Color.parseColor("#00D2FF"))
            alpha = 0.7f
            contentDescription = "Resize Window"
            setPadding(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2))
        }
        val handleParams = FrameLayout.LayoutParams(
            dpToPx(24),
            dpToPx(24),
            Gravity.BOTTOM or Gravity.END
        )
        rootView.addView(resizeHandle, handleParams)

        setupResizeListener()
    }

    private fun setupHeaderDragListener() {
        headerBar.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastHeaderTapTime < 300) {
                        // Double tap: Quick Boss-Key / Panic
                        onMinimizeToBubble()
                        return@setOnTouchListener true
                    }
                    lastHeaderTapTime = currentTime

                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    if (!isFullscreen) {
                        val dx = event.rawX - initialTouchX
                        val dy = event.rawY - initialTouchY
                        layoutParams.x = (initialX + dx).toInt()
                        layoutParams.y = (initialY + dy).toInt()
                        updateWindowLayout()
                    }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    if (!isFullscreen) {
                        preferences.initialPosX = layoutParams.x
                        preferences.initialPosY = layoutParams.y
                    }
                    true
                }

                else -> false
            }
        }
    }

    private fun setupResizeListener() {
        resizeHandle.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    initialWidth = layoutParams.width
                    initialHeight = layoutParams.height
                    resizeHandle.alpha = 1.0f
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()

                    val minW = dpToPx(240)
                    val minH = dpToPx(280)
                    val maxW = context.resources.displayMetrics.widthPixels
                    val maxH = context.resources.displayMetrics.heightPixels

                    layoutParams.width = (initialWidth + dx).coerceIn(minW, maxW)
                    layoutParams.height = (initialHeight + dy).coerceIn(minH, maxH)
                    updateWindowLayout()
                    true
                }

                MotionEvent.ACTION_UP -> {
                    resizeHandle.alpha = 0.7f
                    preferences.windowWidthDp = (layoutParams.width / density).toInt()
                    preferences.windowHeightDp = (layoutParams.height / density).toInt()
                    true
                }

                else -> false
            }
        }
    }

    private fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        val displayMetrics = context.resources.displayMetrics

        if (isFullscreen) {
            savedWidth = layoutParams.width
            savedHeight = layoutParams.height
            savedX = layoutParams.x
            savedY = layoutParams.y

            layoutParams.width = displayMetrics.widthPixels
            layoutParams.height = displayMetrics.heightPixels - dpToPx(24)
            layoutParams.x = 0
            layoutParams.y = dpToPx(24)
            btnMaximize.setImageResource(android.R.drawable.arrow_down_float)
            resizeHandle.visibility = View.GONE
        } else {
            layoutParams.width = savedWidth
            layoutParams.height = savedHeight
            layoutParams.x = savedX
            layoutParams.y = savedY
            btnMaximize.setImageResource(android.R.drawable.arrow_up_float)
            resizeHandle.visibility = View.VISIBLE
        }
        updateWindowLayout()
    }

    private fun cycleOpacity() {
        val current = rootView.alpha
        val next = when {
            current >= 0.9f -> 0.75f
            current >= 0.7f -> 0.50f
            current >= 0.45f -> 0.25f
            else -> 0.95f
        }
        rootView.alpha = next
        preferences.opacity = next
    }

    private fun toggleStealthMode() {
        preferences.stealthSecure = !preferences.stealthSecure
        if (preferences.stealthSecure) {
            layoutParams.flags = layoutParams.flags or WindowManager.LayoutParams.FLAG_SECURE
            btnStealth.setColorFilter(Color.parseColor("#00D2FF"))
        } else {
            layoutParams.flags = layoutParams.flags and WindowManager.LayoutParams.FLAG_SECURE.inv()
            btnStealth.setColorFilter(Color.parseColor("#94A3B8"))
        }
        updateWindowLayout()
    }

    private fun handleFocusChange(hasFocus: Boolean) {
        if (hasFocus) {
            layoutParams.flags = layoutParams.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        } else {
            if (preferences.stealthNonFocus) {
                layoutParams.flags = layoutParams.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            }
        }
        updateWindowLayout()
    }

    private fun hideKeyboard() {
        imm?.hideSoftInputFromWindow(urlEditText.windowToken, 0)
    }

    fun loadUrl(url: String) {
        val tab = getActiveTab() ?: return
        tab.url = url
        urlEditText.setText(url)
        tab.webView?.loadUrl(url)
    }

    fun createNewTab(initialUrl: String) {
        val webView = createWebView()
        val tab = BrowserTab(
            title = "Tab Baru",
            url = initialUrl,
            webView = webView
        )
        tabs.add(tab)
        refreshTabViews()
        selectTab(tabs.size - 1)
        webView.loadUrl(initialUrl)
    }

    private fun closeTab(index: Int) {
        if (tabs.size <= 1) {
            // Keep at least one tab, reload home
            loadUrl(preferences.homeUrl)
            return
        }

        val tabToRemove = tabs[index]
        webContainer.removeView(tabToRemove.webView)
        tabToRemove.webView?.destroy()
        tabs.removeAt(index)

        if (activeTabIndex >= tabs.size) {
            activeTabIndex = tabs.size - 1
        }
        refreshTabViews()
        selectTab(activeTabIndex)
    }

    private fun selectTab(index: Int) {
        if (index !in tabs.indices) return
        activeTabIndex = index

        webContainer.removeAllViews()
        val currentTab = tabs[index]
        currentTab.webView?.let { wv ->
            if (wv.parent != null) {
                (wv.parent as? ViewGroup)?.removeView(wv)
            }
            webContainer.addView(
                wv,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )
        }

        urlEditText.setText(currentTab.url)
        titleTextView.text = currentTab.title
        updateNavButtons()
        refreshTabViews()
    }

    private fun refreshTabViews() {
        tabContainer.removeAllViews()
        tabs.forEachIndexed { i, tab ->
            val isActive = i == activeTabIndex
            val tabView = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = ContextCompat.getDrawable(
                    context,
                    if (isActive) R.drawable.bg_tab_active else R.drawable.bg_tab_inactive
                )
                setPadding(dpToPx(8), dpToPx(4), dpToPx(6), dpToPx(4))
                setOnClickListener { selectTab(i) }
            }

            val titleView = TextView(context).apply {
                text = tab.title
                setTextColor(if (isActive) Color.parseColor("#00D2FF") else Color.parseColor("#94A3B8"))
                textSize = 11f
                maxLines = 1
                maxWidth = dpToPx(110)
                ellipsize = android.text.TextUtils.TruncateAt.END
            }
            tabView.addView(titleView)

            if (tabs.size > 1) {
                val closeBtn = ImageView(context).apply {
                    setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                    setColorFilter(Color.parseColor("#94A3B8"))
                    setPadding(dpToPx(3), dpToPx(3), dpToPx(3), dpToPx(3))
                    setOnClickListener { closeTab(i) }
                }
                val p = LinearLayout.LayoutParams(dpToPx(18), dpToPx(18)).apply {
                    marginStart = dpToPx(4)
                }
                tabView.addView(closeBtn, p)
            }

            val tabParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dpToPx(4)
            }
            tabContainer.addView(tabView, tabParams)
        }
    }

    private fun createWebView(): WebView {
        return WebView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#080B11"))

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                useWideViewPort = true
                loadWithOverviewMode = true
                builtInZoomControls = true
                displayZoomControls = false
                setSupportZoom(true)
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                mediaPlaybackRequiresUserGesture = false
                cacheMode = WebSettings.LOAD_DEFAULT

                if (preferences.desktopMode) {
                    userAgentString = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    getActiveTab()?.let { tab ->
                        tab.progress = newProgress
                        pageProgressBar.progress = newProgress
                        if (newProgress < 100) {
                            pageProgressBar.visibility = View.VISIBLE
                            tab.isLoading = true
                            btnRefresh.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                            onPageLoadingChanged(true)
                        } else {
                            pageProgressBar.visibility = View.GONE
                            tab.isLoading = false
                            btnRefresh.setImageResource(android.R.drawable.ic_menu_rotate)
                            onPageLoadingChanged(false)
                        }
                    }
                }

                override fun onReceivedTitle(view: WebView?, title: String?) {
                    title?.let {
                        getActiveTab()?.title = it
                        titleTextView.text = it
                        refreshTabViews()
                    }
                }
            }

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val uri = request?.url?.toString() ?: return false
                    view?.loadUrl(uri)
                    return true
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    url?.let {
                        getActiveTab()?.url = it
                        if (!urlEditText.hasFocus()) {
                            urlEditText.setText(it)
                        }
                    }
                    errorLayout.visibility = View.GONE
                    updateNavButtons()
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    updateNavButtons()
                }

                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    if (request?.isForMainFrame == true) {
                        errorLayout.visibility = View.VISIBLE
                        errorTextView.text = "Gagal memuat:\n${request.url}\n(${error?.description})"
                    }
                }
            }
        }
    }

    private fun updateDesktopMode() {
        val tab = getActiveTab() ?: return
        val wv = tab.webView ?: return
        if (preferences.desktopMode) {
            wv.settings.userAgentString = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            btnDesktop.setColorFilter(Color.parseColor("#00D2FF"))
        } else {
            wv.settings.userAgentString = null // Reset to default mobile
            btnDesktop.setColorFilter(Color.parseColor("#38BDF8"))
        }
        wv.reload()
    }

    private fun updateNavButtons() {
        val tab = getActiveTab() ?: return
        val wv = tab.webView ?: return
        tab.canGoBack = wv.canGoBack()
        tab.canGoForward = wv.canGoForward()

        btnBack.alpha = if (tab.canGoBack) 1.0f else 0.4f
        btnForward.alpha = if (tab.canGoForward) 1.0f else 0.4f
    }

    private fun getActiveTab(): BrowserTab? {
        return if (activeTabIndex in tabs.indices) tabs[activeTabIndex] else null
    }

    fun show() {
        if (!isAttached) {
            try {
                windowManager.addView(rootView, layoutParams)
                isAttached = true
                rootView.alpha = 0f
                rootView.animate().alpha(preferences.opacity).setDuration(160).start()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun hide() {
        if (isAttached) {
            try {
                hideKeyboard()
                windowManager.removeView(rootView)
                isAttached = false
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun destroy() {
        hide()
        tabs.forEach { tab ->
            tab.webView?.destroy()
        }
        tabs.clear()
    }

    private fun updateWindowLayout() {
        if (isAttached) {
            try {
                windowManager.updateViewLayout(rootView, layoutParams)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * density).toInt()
    }
}
