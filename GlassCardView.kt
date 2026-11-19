package com.example.fluidnotif

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.Typeface
import android.text.TextUtils
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.dynamicanimation.animation.FloatPropertyCompat

/**
 * Glass notification card. [progress] 0 = small capsule at the camera,
 * 1 = fully expanded card. Driven by a spring animation.
 */
class GlassCardView(context: Context, d: NotifData, private val st: Style) : FrameLayout(context) {

    companion object {
        val PROGRESS = object : FloatPropertyCompat<GlassCardView>("progress") {
            override fun getValue(o: GlassCardView) = o.progress
            override fun setValue(o: GlassCardView, value: Float) { o.progress = value }
        }
    }

    /** Capsule rect (where the camera is) in this view's local coordinates. */
    var startRect = RectF()

    var progress = 0f
        set(v) {
            field = v
            applyProgress()
            invalidate()
        }

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = context.dpF(st.edgeWDp)
    }
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = context.dpF(st.edgeWDp * 4f)
        alpha = st.glowAlpha
    }
    private val rect = RectF()
    private val content = LinearLayout(context)

    private fun withA(c: Int, a: Int): Int = (c and 0x00FFFFFF) or (a.coerceIn(0, 255) shl 24)

    init {
        setWillNotDraw(false)

        val cHeader = if (st.dark) 0xFFB0B0BA.toInt() else 0xFF6B6B73.toInt()
        val cTitle = if (st.dark) 0xFFF4F4F8.toInt() else 0xFF121216.toInt()
        val cBody = if (st.dark) 0xFFD0D0D8.toInt() else 0xFF4A4A52.toInt()
        val s = st.textScale

        content.orientation = LinearLayout.HORIZONTAL
        content.gravity = Gravity.CENTER_VERTICAL
        content.setPadding(dpI(16f), dpI(14f), dpI(16f), dpI(14f))

        val icon = ImageView(context).apply {
            setImageDrawable(d.icon())
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        val isz = dpI(st.iconDp)
        content.addView(icon, LinearLayout.LayoutParams(isz, isz))

        val col = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        col.addView(TextView(context).apply {
            text = "${d.appName} · now"
            textSize = 12f * s
            setTextColor(cHeader)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        })
        col.addView(TextView(context).apply {
            text = d.title
            textSize = 15.5f * s
            setTextColor(cTitle)
            setTypeface(typeface, Typeface.BOLD)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        })
        col.addView(TextView(context).apply {
            text = if (st.hide) "Message hidden" else d.text
            textSize = 14f * s
            setTextColor(cBody)
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
        })
        val lp = LinearLayout.LayoutParams(0, WRAP, 1f)
        lp.marginStart = dpI(12f)
        content.addView(col, lp)

        addView(content, LayoutParams(MATCH, WRAP))
        applyProgress()
    }

    private fun applyProgress() {
        // Content fades in and un-blurs during the second half of the expansion.
        val a = ((progress - 0.35f) / 0.5f).coerceIn(0f, 1f)
        content.alpha = a
        val r = (1f - a) * dpF(st.textBlurDp)
        content.setRenderEffect(
            if (r > 0.5f) RenderEffect.createBlurEffect(r, r, Shader.TileMode.CLAMP) else null
        )
    }

    override fun onDraw(canvas: Canvas) {
        val p = progress.coerceAtLeast(0f)
        val w = width.toFloat()
        val h = height.toFloat()
        rect.set(
            lerp(startRect.left, 0f, p),
            lerp(startRect.top, 0f, p),
            lerp(startRect.right, w, p),
            lerp(startRect.bottom, h, p)
        )
        val pad = if (st.edgeOn) stroke.strokeWidth / 2f else 0f
        rect.inset(pad, pad)
        val radius = lerp(startRect.height() / 2f, dpF(st.cornerDp), p.coerceAtMost(1f))

        // Milky translucent glass body
        val top = if (st.dark) 0x2E2E36 else 0xFFFFFF
        val bot = if (st.dark) 0x17171C else 0xECECF2
        fill.shader = LinearGradient(
            rect.left, rect.top, rect.right, rect.bottom,
            withA(top, st.glassAlpha), withA(bot, (st.glassAlpha * 0.82f).toInt()),
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(rect, radius, radius, fill)

        if (st.edgeOn) {
            val edge = LinearGradient(
                rect.left, rect.top, rect.right, rect.bottom,
                st.c1, st.c2, Shader.TileMode.CLAMP
            )
            glow.shader = edge
            stroke.shader = edge
            canvas.drawRoundRect(rect, radius, radius, glow)
            canvas.drawRoundRect(rect, radius, radius, stroke)
        }
    }
}
