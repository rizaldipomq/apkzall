package com.example.floating

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.R

class DismissTargetView(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var containerView: FrameLayout? = null
    private var targetCircle: FrameLayout? = null
    private var isAttached = false
    private var isTargetHovered = false

    val layoutParams: WindowManager.LayoutParams by lazy {
        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN

        WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            dpToPx(120),
            windowType,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = 0
        }
    }

    init {
        createViews()
    }

    private fun createViews() {
        val density = context.resources.displayMetrics.density
        val targetSize = (70 * density).toInt()

        containerView = FrameLayout(context).apply {
            // Container
        }

        targetCircle = FrameLayout(context).apply {
            background = ContextCompat.getDrawable(context, R.drawable.bg_dismiss_target)
            val icon = ImageView(context).apply {
                setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                setColorFilter(Color.WHITE)
            }
            val iconParams = FrameLayout.LayoutParams(
                (32 * density).toInt(),
                (32 * density).toInt(),
                Gravity.CENTER
            )
            addView(icon, iconParams)
        }

        val circleParams = FrameLayout.LayoutParams(
            targetSize,
            targetSize,
            Gravity.CENTER
        )
        containerView?.addView(targetCircle, circleParams)

        val hint = TextView(context).apply {
            text = "Tutup di sini"
            setTextColor(Color.WHITE)
            textSize = 12f
            gravity = Gravity.CENTER
            alpha = 0.85f
        }
        val hintParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        ).apply {
            bottomMargin = (12 * density).toInt()
        }
        containerView?.addView(hint, hintParams)
    }

    fun show() {
        if (!isAttached && containerView != null) {
            try {
                windowManager.addView(containerView, layoutParams)
                isAttached = true
                containerView?.alpha = 0f
                containerView?.animate()?.alpha(1f)?.setDuration(150)?.start()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun hide() {
        if (isAttached && containerView != null) {
            try {
                windowManager.removeView(containerView)
                isAttached = false
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun checkHover(rawX: Float, rawY: Float, screenWidth: Int, screenHeight: Int): Boolean {
        val targetX = screenWidth / 2f
        val targetY = screenHeight - dpToPx(60).toFloat()
        val distance = Math.hypot((rawX - targetX).toDouble(), (rawY - targetY).toDouble())
        val isHovering = distance < dpToPx(75)

        if (isHovering != isTargetHovered) {
            isTargetHovered = isHovering
            targetCircle?.animate()
                ?.scaleX(if (isHovering) 1.25f else 1.0f)
                ?.scaleY(if (isHovering) 1.25f else 1.0f)
                ?.setDuration(120)
                ?.start()
        }
        return isHovering
    }

    private fun dpToPx(dp: Int): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density).toInt()
    }
}
