package com.example.fluidnotif

import android.content.Context
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.animation.OvershootInterpolator
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import kotlin.math.abs

/**
 * The whole notification UI: glass card + action pills, with open/close animations.
 * Used both inside the system overlay window and inside the settings preview.
 */
class NotifPanel(
    private val ctx: Context,
    private val d: NotifData,
    private val st: Style,
    startRect: RectF,
    cardTop: Int
) : LinearLayout(ctx) {

    val card = GlassCardView(ctx, d, st)
    private val row = LinearLayout(ctx)
    private val handler = Handler(Looper.getMainLooper())
    private var autoClose: Runnable? = null
    private var closing = false
    private val fg = if (st.dark) 0xFFF4F4F8.toInt() else 0xFF121216.toInt()
    private val hasButtons = st.buttons && (st.btnReply || st.btnRead)

    var onClosed: (() -> Unit)? = null
    var onNeedFocus: ((EditText) -> Unit)? = null

    init {
        orientation = VERTICAL
        card.startRect = startRect
        card.progress = 0f
        addView(card, LayoutParams(MATCH, WRAP).apply { topMargin = cardTop.coerceAtLeast(0) })

        row.orientation = HORIZONTAL
        row.gravity = Gravity.CENTER_HORIZONTAL
        if (hasButtons) {
            addView(row, LayoutParams(MATCH, WRAP).apply { topMargin = dpI(8f) })
            buildButtons()
            row.alpha = 0f
            row.translationY = -dpF(18f)
            row.scaleX = 0.9f
            row.scaleY = 0.9f
        }

        var downY = 0f
        card.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> { downY = e.rawY; true }
                MotionEvent.ACTION_UP -> {
                    val dy = e.rawY - downY
                    if (dy < -dpF(28f)) {
                        close()
                    } else if (abs(dy) < dpF(8f)) {
                        d.onOpen()
                        close()
                    }
                    true
                }
                else -> true
            }
        }
    }

    fun open(haptic: Boolean = st.haptic) {
        closing = false
        if (haptic) card.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        SpringAnimation(card, GlassCardView.PROGRESS, 1f).apply {
            spring.stiffness = st.openStiffness
            spring.dampingRatio = st.openDamping
            start()
        }
        if (hasButtons) {
            row.animate()
                .alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
                .setStartDelay(st.btnDelay).setDuration(320)
                .setInterpolator(OvershootInterpolator(1.4f))
                .start()
        }
        scheduleAutoClose()
    }

    fun close() {
        if (closing) return
        closing = true
        cancelAuto()
        if (hasButtons) {
            row.animate().alpha(0f).translationY(-dpF(10f)).setStartDelay(0).setDuration(160).start()
        }
        SpringAnimation(card, GlassCardView.PROGRESS, 0f).apply {
            spring.stiffness = st.closeStiffness
            spring.dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
            addEndListener { _, _, _, _ -> onClosed?.invoke() }
            start()
        }
    }

    fun release() {
        cancelAuto()
        handler.removeCallbacksAndMessages(null)
        row.animate().cancel()
    }

    private fun scheduleAutoClose() {
        cancelAuto()
        autoClose = Runnable { close() }.also { handler.postDelayed(it, st.stayMs) }
    }

    private fun cancelAuto() {
        autoClose?.let { handler.removeCallbacks(it) }
        autoClose = null
    }

    private fun buildButtons() {
        row.removeAllViews()
        if (st.btnReply) {
            row.addView(pill("Reply", d.icon()) {
                if (d.canReply) showReply() else { d.onOpen(); close() }
            })
        }
        if (st.btnRead) {
            val check = ctx.getDrawable(R.drawable.ic_check)?.mutate()?.also { it.setTint(fg) }
            row.addView(pill("Mark as read", check) {
                d.onRead()
                close()
            }, LayoutParams(WRAP, WRAP).apply { marginStart = if (st.btnReply) dpI(8f) else 0 })
        }
    }

    private fun showReply() {
        cancelAuto()
        row.removeAllViews()
        val input = EditText(ctx).apply {
            hint = "Reply…"
            textSize = 15f
            setSingleLine()
            setTextColor(fg)
            setHintTextColor(0xFF8A8A92.toInt())
            setPadding(dpI(20f), 0, dpI(20f), 0)
            background = glassBg()
        }
        row.addView(input, LayoutParams(0, dpI(48f), 1f))
        row.addView(pill("Send", null) {
            val text = input.text.toString().trim()
            if (text.isNotEmpty()) d.onReply(text)
            close()
        }, LayoutParams(WRAP, WRAP).apply { marginStart = dpI(8f) })
        row.alpha = 1f
        row.translationY = 0f
        row.scaleX = 1f
        row.scaleY = 1f
        onNeedFocus?.invoke(input)
    }

    private fun glassBg() = GradientDrawable().apply {
        cornerRadius = dpF(24f)
        setColor(if (st.dark) 0xE62A2A30.toInt() else 0xEBFFFFFF.toInt())
        setStroke(dpI(1f), 0x55FFFFFF)
    }

    private fun pill(label: String, icon: Drawable?, onClick: () -> Unit): TextView {
        return TextView(ctx).apply {
            text = label
            textSize = 15f * st.textScale
            setTextColor(fg)
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER_VERTICAL
            minHeight = dpI(48f)
            setPadding(dpI(if (icon != null) 14f else 22f), 0, dpI(22f), 0)
            compoundDrawablePadding = dpI(8f)
            icon?.let {
                it.setBounds(0, 0, dpI(26f), dpI(26f))
                setCompoundDrawablesRelative(it, null, null, null)
            }
            background = glassBg()
            setOnClickListener { onClick() }
        }
    }
}
