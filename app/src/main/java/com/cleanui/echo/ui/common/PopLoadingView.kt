package com.cleanui.echo.ui.common

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import com.google.android.material.color.MaterialColors

/**
 * Music-style loading indicator: five rounded bars that "pop" up with a small overshoot in a
 * travelling wave. Drawn with a single animator, so it is cheap and never touches playback.
 * The animation stops when the view is detached or hidden, and is shown as a still frame when
 * the system has animations turned off.
 */
class PopLoadingView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private val barWidth = 7f * density
    private val gap = 6f * density
    private val rect = RectF()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = MaterialColors.getColor(
            this@PopLoadingView, com.google.android.material.R.attr.colorOnSurface, 0xFFFFFFFF.toInt()
        )
    }
    private val overshoot = OvershootInterpolator(2.2f)
    private val decelerate = DecelerateInterpolator()
    private var phase = 0.35f

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1100L
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            phase = it.animatedValue as Float
            invalidate()
        }
    }

    private fun animationsEnabled() =
        Build.VERSION.SDK_INT < 26 || ValueAnimator.areAnimatorsEnabled()

    private fun startIfPossible() {
        if (animationsEnabled()) {
            if (!animator.isStarted) animator.start()
        } else {
            phase = 0.35f
            invalidate()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (isShown) startIfPossible()
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        if (isVisible && isAttachedToWindow) startIfPossible() else animator.cancel()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = (BARS * barWidth + (BARS - 1) * gap + paddingLeft + paddingRight).toInt()
        val desiredHeight = (48f * density + paddingTop + paddingBottom).toInt()
        setMeasuredDimension(
            resolveSize(desiredWidth, widthMeasureSpec),
            resolveSize(desiredHeight, heightMeasureSpec)
        )
    }

    override fun onDraw(canvas: Canvas) {
        val availableHeight = (height - paddingTop - paddingBottom).toFloat()
        val totalWidth = BARS * barWidth + (BARS - 1) * gap
        val startX = (width - totalWidth) / 2f
        val centerY = paddingTop + availableHeight / 2f
        val minHeight = availableHeight * 0.22f
        val peak = availableHeight * 0.82f
        for (i in 0 until BARS) {
            val p = (phase + (BARS - 1 - i) * 0.11f) % 1f
            val e = if (p < 0.5f) overshoot.getInterpolation(p * 2f)
            else 1f - decelerate.getInterpolation((p - 0.5f) * 2f)
            val h = (minHeight + (peak - minHeight) * e).coerceIn(minHeight, availableHeight)
            val left = startX + i * (barWidth + gap)
            rect.set(left, centerY - h / 2f, left + barWidth, centerY + h / 2f)
            canvas.drawRoundRect(rect, barWidth / 2f, barWidth / 2f, paint)
        }
    }

    private companion object {
        const val BARS = 5
    }
}
