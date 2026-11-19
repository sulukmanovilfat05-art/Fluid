package com.example.fluidnotif

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.RectF
import android.provider.Settings
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.WindowInsets
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager

/** Puts a [NotifPanel] into a system overlay window anchored at the camera. */
object OverlayController {

    private var wm: WindowManager? = null
    private var panel: NotifPanel? = null

    fun show(base: Context, d: NotifData) {
        if (!Settings.canDrawOverlays(base)) return
        removeNow()

        val st = Style(base.getSharedPreferences("fluid", Context.MODE_PRIVATE))
        val ctx = ContextThemeWrapper(base, android.R.style.Theme_DeviceDefault_Light_NoActionBar)
        val manager = base.getSystemService(WindowManager::class.java)
        wm = manager

        // Where is the camera?
        val metrics = manager.currentWindowMetrics
        val screenW = metrics.bounds.width()
        val insets = metrics.windowInsets
        val statusTop = insets.getInsets(WindowInsets.Type.statusBars()).top
        val cut = insets.displayCutout?.boundingRects?.firstOrNull { it.top <= 0 }
        val cam = if (cut != null) RectF(cut) else RectF(
            screenW / 2f - ctx.dpF(14f), ctx.dpF(8f),
            screenW / 2f + ctx.dpF(14f), ctx.dpF(36f)
        )
        cam.inset(-ctx.dpF(3f), -ctx.dpF(3f))

        val margin = ctx.dpI(st.marginDp)
        val cardTop = (maxOf(cam.bottom.toInt(), statusTop) + ctx.dpI(st.topGapDp)).coerceAtLeast(0)
        val start = RectF(
            cam.left - margin, cam.top - cardTop,
            cam.right - margin, cam.bottom - cardTop
        )

        val p = NotifPanel(ctx, d, st, start, cardTop)

        val lp = WindowManager.LayoutParams(
            screenW - margin * 2,
            WRAP,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 0
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            if (st.blur) {
                // Experimental: blurs the whole window rectangle, not only the rounded card.
                flags = flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
                blurBehindRadius = ctx.dpI(st.blurDp)
            }
        }

        p.onClosed = { if (panel === p) removeNow() }
        p.onNeedFocus = { input ->
            // Make the overlay focusable so the keyboard can appear.
            lp.flags = lp.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
            lp.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
            try { manager.updateViewLayout(p, lp) } catch (_: Exception) {}
            input.requestFocus()
            input.postDelayed({
                ctx.getSystemService(InputMethodManager::class.java)
                    .showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
            }, 150)
        }

        manager.addView(p, lp)
        panel = p
        p.open()
    }

    fun dismiss() {
        panel?.close()
    }

    private fun removeNow() {
        panel?.let {
            it.release()
            try { wm?.removeViewImmediate(it) } catch (_: Exception) {}
        }
        panel = null
    }
}
