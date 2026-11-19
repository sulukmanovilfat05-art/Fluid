package com.example.fluidnotif

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import kotlin.math.min

const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

fun Context.dpF(v: Float): Float = v * resources.displayMetrics.density
fun Context.dpI(v: Float): Int = (v * resources.displayMetrics.density).toInt()
fun View.dpF(v: Float): Float = context.dpF(v)
fun View.dpI(v: Float): Int = context.dpI(v)
fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

/** Everything the overlay needs to show one notification (real or fake). */
data class NotifData(
    val appName: String,
    val icon: () -> Drawable?,
    val title: String,
    val text: String,
    val canReply: Boolean,
    val onOpen: () -> Unit,
    val onRead: () -> Unit,
    val onReply: (String) -> Unit
)

/** Simple round letter icon used for fake notifications. */
class LetterIcon(private val c: Int, private val letter: String) : Drawable() {
    private val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = c }
    private val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    override fun draw(canvas: Canvas) {
        val b = bounds
        val r = min(b.width(), b.height()) / 2f
        canvas.drawCircle(b.exactCenterX(), b.exactCenterY(), r, bg)
        tp.textSize = r * 1.05f
        canvas.drawText(letter, b.exactCenterX(), b.exactCenterY() - (tp.ascent() + tp.descent()) / 2f, tp)
    }

    override fun setAlpha(alpha: Int) { bg.alpha = alpha }
    override fun setColorFilter(colorFilter: ColorFilter?) { bg.colorFilter = colorFilter }
    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
