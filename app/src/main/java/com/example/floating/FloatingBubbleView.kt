package com.example.floating

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.preferences.BrowserPreferences
import kotlin.math.abs

@SuppressLint("ClickableViewAccessibility")
class FloatingBubbleView(
    private val context: Context,
    private val preferences: BrowserPreferences,
    private val onBubbleClick: () -> Unit,
    private val onDismiss: () -> Unit,
    private val onDragMove: (rawX: Float, rawY: Float) -> Boolean
) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val density = context.resources.displayMetrics.density
    val bubbleSize = (preferences.bubbleSizeDp * density).toInt()

    var isAttached = false
        private set

    lateinit var rootView: FrameLayout
        private set
    private lateinit var bubbleImage: ImageView
    private lateinit var loadingIndicator: ProgressBar

    lateinit var layoutParams: WindowManager.LayoutParams
        private set

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false
    private val touchSlop = 12 * density

    init {
        createViews()
        initLayoutParams()
    }

    private fun createViews() {
        rootView = FrameLayout(context).apply {
            background = ContextCompat.getDrawable(context, R.drawable.bg_floating_bubble)
            elevation = 16 * density
        }

        bubbleImage = ImageView(context).apply {
            setImageResource(R.drawable.ic_zall_logo)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setColorFilter(Color.parseColor("#00D2FF"))
            val pad = (12 * density).toInt()
            setPadding(pad, pad, pad, pad)
        }

        val imgParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
            Gravity.CENTER
        )
        rootView.addView(bubbleImage, imgParams)

        loadingIndicator = ProgressBar(context, null, android.R.attr.progressBarStyleSmall).apply {
            visibility = View.GONE
            indeterminateDrawable?.setTint(Color.parseColor("#00D2FF"))
        }
        val loaderParams = FrameLayout.LayoutParams(
            (24 * density).toInt(),
            (24 * density).toInt(),
            Gravity.CENTER
        )
        rootView.addView(loadingIndicator, loaderParams)

        setupTouchListener()
    }

    private fun initLayoutParams() {
        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        var flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

        if (preferences.stealthSecure) {
            flags = flags or WindowManager.LayoutParams.FLAG_SECURE
        }

        val screenWidth = context.resources.displayMetrics.widthPixels
        val defaultX = screenWidth - bubbleSize - (16 * density).toInt()
        val defaultY = (180 * density).toInt()

        layoutParams = WindowManager.LayoutParams(
            bubbleSize,
            bubbleSize,
            windowType,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = defaultX
            y = defaultY
        }
    }

    private fun setupTouchListener() {
        rootView.setOnTouchListener { _, event ->
            val screenWidth = context.resources.displayMetrics.widthPixels
            val screenHeight = context.resources.displayMetrics.heightPixels

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    rootView.animate().scaleX(1.1f).scaleY(1.1f).setDuration(100).start()
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY

                    if (!isDragging && (abs(dx) > touchSlop || abs(dy) > touchSlop)) {
                        isDragging = true
                    }

                    if (isDragging) {
                        layoutParams.x = (initialX + dx).toInt()
                        layoutParams.y = (initialY + dy).toInt()
                        updateWindow()

                        onDragMove(event.rawX, event.rawY)
                    }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    rootView.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()

                    if (isDragging) {
                        val isOverDismiss = onDragMove(event.rawX, event.rawY)
                        if (isOverDismiss) {
                            onDismiss()
                        } else {
                            if (preferences.bubbleSnap) {
                                snapToEdge(screenWidth)
                            }
                        }
                    } else {
                        // Click
                        onBubbleClick()
                    }
                    true
                }

                else -> false
            }
        }
    }

    private fun snapToEdge(screenWidth: Int) {
        val margin = (16 * density).toInt()
        val targetX = if (layoutParams.x + bubbleSize / 2 < screenWidth / 2) {
            margin
        } else {
            screenWidth - bubbleSize - margin
        }

        val startX = layoutParams.x
        val animator = ValueAnimator.ofInt(startX, targetX).apply {
            duration = 200
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                layoutParams.x = animation.animatedValue as Int
                updateWindow()
            }
        }
        animator.start()
    }

    fun show() {
        if (!isAttached) {
            try {
                windowManager.addView(rootView, layoutParams)
                isAttached = true
                rootView.alpha = 0f
                rootView.scaleX = 0.5f
                rootView.scaleY = 0.5f
                rootView.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(180)
                    .start()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun hide() {
        if (isAttached) {
            try {
                windowManager.removeView(rootView)
                isAttached = false
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setLoading(isLoading: Boolean) {
        loadingIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
        bubbleImage.visibility = if (isLoading) View.GONE else View.VISIBLE
    }

    private fun updateWindow() {
        if (isAttached) {
            try {
                windowManager.updateViewLayout(rootView, layoutParams)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
