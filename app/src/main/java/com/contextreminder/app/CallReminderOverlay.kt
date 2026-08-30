package com.contextreminder.app

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

object CallReminderOverlay {
    private const val DISPLAY_MS = 45_000L
    private const val TOP_OFFSET_DP = 140

    private val handler = Handler(Looper.getMainLooper())
    private var currentView: View? = null
    private var currentWindowManager: WindowManager? = null
    private var dismissRunnable: Runnable? = null

    fun show(context: Context, reminderText: String) {
        val appContext = context.applicationContext
        if (reminderText.isBlank() || !Settings.canDrawOverlays(appContext)) return

        handler.post {
            runCatching {
                removeCurrent()

                val windowManager = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                val container = FrameLayout(appContext).apply {
                    setPadding(dp(appContext, 16), 0, dp(appContext, 16), 0)
                }

                val banner = LinearLayout(appContext).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(
                        dp(appContext, 16),
                        dp(appContext, 12),
                        dp(appContext, 8),
                        dp(appContext, 12)
                    )
                    elevation = dp(appContext, 10).toFloat()
                    background = GradientDrawable().apply {
                        setColor(Color.rgb(35, 35, 38))
                        cornerRadius = dp(appContext, 16).toFloat()
                    }
                }

                val textColumn = LinearLayout(appContext).apply {
                    orientation = LinearLayout.VERTICAL
                }
                textColumn.addView(
                    TextView(appContext).apply {
                        text = "REMINDER"
                        setTextColor(Color.rgb(205, 205, 210))
                        textSize = 11f
                    },
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                )
                textColumn.addView(
                    TextView(appContext).apply {
                        text = reminderText
                        setTextColor(Color.WHITE)
                        textSize = 17f
                        maxLines = 4
                    },
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                )

                banner.addView(
                    textColumn,
                    LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                )
                banner.addView(
                    TextView(appContext).apply {
                        text = "×"
                        contentDescription = "Dismiss reminder"
                        setTextColor(Color.WHITE)
                        textSize = 28f
                        gravity = Gravity.CENTER
                        setPadding(dp(appContext, 14), dp(appContext, 6), dp(appContext, 8), dp(appContext, 6))
                        setOnClickListener { removeCurrent() }
                    },
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                )

                container.addView(
                    banner,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                    )
                )

                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                    y = dp(appContext, TOP_OFFSET_DP)
                }

                windowManager.addView(container, params)
                currentView = container
                currentWindowManager = windowManager

                val dismiss = Runnable { removeCurrent() }
                dismissRunnable = dismiss
                handler.postDelayed(dismiss, DISPLAY_MS)
            }
        }
    }

    fun removeCurrent() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            handler.post { removeCurrent() }
            return
        }

        dismissRunnable?.let(handler::removeCallbacks)
        dismissRunnable = null
        val view = currentView
        val windowManager = currentWindowManager
        currentView = null
        currentWindowManager = null
        if (view != null && windowManager != null) {
            runCatching { windowManager.removeView(view) }
        }
    }

    private fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()
}
