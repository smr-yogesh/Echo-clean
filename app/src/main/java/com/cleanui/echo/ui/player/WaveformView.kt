package com.cleanui.echo.ui.player

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.ColorUtils
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Decorative waveform progress bar (like the equaliser strip in the design sketch).
 *
 * It is purely visual: the real seeking is still done by the transparent Slider layered on top,
 * and the bar heights are generated deterministically from [setSeed] (no audio analysis), so the
 * same track always shows the same shape. Played bars are coloured green -> yellow -> red by
 * height, the remaining bars are muted.
 */
class WaveformView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private val barWidth = 3f * density
    private val gap = 2f * density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    private var seed = 0
    private var heights = FloatArray(0)

    /** Colour of the not-yet-played bars. */
    var idleColor = 0xFF3A3F46.toInt()
        set(value) {
            field = value
            invalidate()
        }

    /** Played fraction, 0..1. */
    var progress = 0f
        set(value) {
            val clamped = min(1f, max(0f, value))
            if (clamped != field) {
                field = clamped
                invalidate()
            }
        }

    fun setSeed(value: Int) {
        if (seed == value && heights.isNotEmpty()) return
        seed = value
        heights = FloatArray(0)
        invalidate()
    }

    private fun barCount() = max(1, ((width + gap) / (barWidth + gap)).toInt())

    private fun noise(i: Int): Float {
        var x = i * 374761393 + seed * 668265263
        x = (x xor (x ushr 13)) * 1274126177
        x = x xor (x ushr 16)
        return (x and 0xFFFF) / 65535f
    }

    private fun buildHeights(count: Int) {
        heights = FloatArray(count) { i ->
            val envelope = 0.55f + 0.45f * abs(sin(i * 0.11f + (seed and 0xFF) * 0.07f))
            val h = (0.25f + 0.75f * noise(i)) * envelope
            min(1f, max(0.12f, h))
        }
    }

    private fun colorFor(h: Float): Int = when {
        h < 0.5f -> ColorUtils.blendARGB(0xFF3DDC84.toInt(), 0xFFFFC107.toInt(), h / 0.5f)
        else -> ColorUtils.blendARGB(0xFFFFC107.toInt(), 0xFFFF5252.toInt(), (h - 0.5f) / 0.5f)
    }

    override fun onDraw(canvas: Canvas) {
        val count = barCount()
        if (heights.size != count) buildHeights(count)
        val centerY = height / 2f
        val maxHalf = height / 2f
        val playedBars = (count * progress)
        val radius = barWidth / 2f
        for (i in 0 until count) {
            val left = i * (barWidth + gap)
            val half = maxHalf * heights[i]
            rect.set(left, centerY - half, left + barWidth, centerY + half)
            paint.color = if (i < playedBars) colorFor(heights[i]) else idleColor
            canvas.drawRoundRect(rect, radius, radius, paint)
        }
    }

    init {
        // keep the bar usable on a plain dark surface even if a tint is never applied
        setBackgroundColor(Color.TRANSPARENT)
    }
}
