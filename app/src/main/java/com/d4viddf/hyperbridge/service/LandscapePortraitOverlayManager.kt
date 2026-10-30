package com.d4viddf.hyperbridge.service

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout

class LandscapePortraitOverlayManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null
    private var isVisible = false
    private var currentWidthSteps = 0

    fun updateWidth(widthSteps: Int) {
        currentWidthSteps = widthSteps
        if (isVisible) {
            updatePillSize()
        }
    }

    fun show() {
        if (isVisible) return

        if (!Settings.canDrawOverlays(context)) {
            Log.w(TAG, "Cannot show overlay: SYSTEM_ALERT_WINDOW permission not granted")
            return
        }

        try {
            val root = FrameLayout(context)
            val pill = View(context).apply {
                val backgroundDrawable = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dpToPx(16f)
                    setColor(Color.BLACK)
                }
                background = backgroundDrawable
            }

            val pillLayoutParams = FrameLayout.LayoutParams(
                calculatePillWidthPx(),
                dpToPx(32f).toInt()
            ).apply {
                gravity = Gravity.CENTER
            }
            root.addView(pill, pillLayoutParams)

            // In landscape mode, phone's physical top (portrait camera cutout) is on Gravity.LEFT (or Gravity.RIGHT depending on rotation 90/270).
            // Defaulting layout parameters to left edge near cutout
            val rotation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.display?.rotation ?: android.view.Surface.ROTATION_0
            } else {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay.rotation
            }

            val sideGravity = if (rotation == android.view.Surface.ROTATION_270) {
                Gravity.RIGHT or Gravity.CENTER_VERTICAL
            } else {
                Gravity.LEFT or Gravity.CENTER_VERTICAL
            }

            val layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = sideGravity
                x = dpToPx(12f).toInt()
                y = 0
            }

            windowManager.addView(root, layoutParams)
            overlayView = root
            isVisible = true
            Log.d(TAG, "Landscape portrait overlay shown successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error displaying landscape portrait overlay", e)
        }
    }

    fun hide() {
        if (!isVisible) return

        try {
            overlayView?.let { view ->
                windowManager.removeViewImmediate(view)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding landscape portrait overlay", e)
        } finally {
            overlayView = null
            isVisible = false
            Log.d(TAG, "Landscape portrait overlay removed")
        }
    }

    private fun updatePillSize() {
        val root = overlayView as? FrameLayout ?: return
        val pill = root.getChildAt(0) ?: return
        val params = pill.layoutParams as? FrameLayout.LayoutParams ?: return
        params.width = calculatePillWidthPx()
        pill.layoutParams = params
    }

    private fun calculatePillWidthPx(): Int {
        // Base width ~120dp + 5dp per widthStep (matching PermanentIslandPreview)
        val widthDp = 120f + (currentWidthSteps * 5f)
        return dpToPx(widthDp).toInt()
    }

    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            context.resources.displayMetrics
        )
    }

    companion object {
        private const val TAG = "LandscapePortraitOverlay"
    }
}
