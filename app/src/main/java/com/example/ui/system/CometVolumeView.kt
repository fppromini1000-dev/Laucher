package com.example.ui.system

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * CometOS style Volume Slider that emerges from the right screen bezel.
 * - Collapsed state: Slim pill (14dp wide) with white level indicator.
 * - Expanded state: Wide pill (46dp wide) with circular knob and speaker/mute icon.
 * - Zero volume: Displays red muted speaker icon with red accent.
 */
@SuppressLint("ViewConstructor")
class CometVolumeView(
    context: Context,
    private val onVolumeChangedByUser: (Float) -> Unit,
    private val onDismissRequested: () -> Unit
) : View(context) {

    private val density = context.resources.displayMetrics.density

    // Dimensions
    private val collapsedWidth = 14f * density
    private val expandedWidth = 46f * density
    private val totalHeight = 186f * density
    private val knobRadius = 13f * density
    private val iconAreaHeight = 36f * density

    // Paints
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#141416") // Deep CometOS black
        style = Paint.Style.FILL
        setShadowLayer(8f * density, -2f * density, 0f, Color.parseColor("#40000000"))
    }

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#26FFFFFF") // 15% subtle white edge
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
    }

    private val trackBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2A2B32") // Track background
        style = Paint.Style.FILL
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val knobPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        setShadowLayer(4f * density, 0f, 2f * density, Color.parseColor("#40000000"))
    }

    private val knobInnerBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E0E0E0")
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
    }

    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val muteRedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#EF4444") // CometOS bright red
        style = Paint.Style.FILL
    }

    private val muteRedStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#EF4444")
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
        strokeCap = Paint.Cap.ROUND
    }

    private val percentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E0E0E0")
        textSize = 9f * density
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }

    // State
    var currentVolumeFraction: Float = 0.5f
        private set

    private var animatedWidth: Float = collapsedWidth
    private var isExpanded: Boolean = false
    private var isDragging: Boolean = false

    private val handler = Handler(Looper.getMainLooper())
    private val autoDismissRunnable = Runnable { dismiss() }
    private val autoCollapseRunnable = Runnable { collapse() }

    private var widthAnimator: ValueAnimator? = null
    private var slideAnimator: ValueAnimator? = null

    private var lastVibratedStep = -1

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null) // Required for shadowLayer
    }

    fun updateVolume(volumeFraction: Float, userInitiated: Boolean = false) {
        val clamped = volumeFraction.coerceIn(0f, 1f)
        if (abs(this.currentVolumeFraction - clamped) > 0.001f) {
            this.currentVolumeFraction = clamped
            invalidate()
        }

        resetDismissTimer()
        if (!userInitiated && !isExpanded) {
            // Briefly expand slightly or show on volume rocker click
            reveal()
        }
    }

    fun reveal(expandFully: Boolean = true) {
        handler.removeCallbacks(autoDismissRunnable)
        handler.removeCallbacks(autoCollapseRunnable)

        slideAnimator?.cancel()
        translationX = 0f
        alpha = 1f
        visibility = VISIBLE

        if (expandFully) {
            expand()
        }

        resetDismissTimer()
    }

    fun expand() {
        if (isExpanded) return
        isExpanded = true
        handler.removeCallbacks(autoCollapseRunnable)

        widthAnimator?.cancel()
        widthAnimator = ValueAnimator.ofFloat(animatedWidth, expandedWidth).apply {
            duration = 240
            interpolator = OvershootInterpolator(0.8f)
            addUpdateListener {
                animatedWidth = it.animatedValue as Float
                requestLayout()
                invalidate()
            }
            start()
        }
    }

    fun collapse() {
        if (!isExpanded) return
        isExpanded = false

        widthAnimator?.cancel()
        widthAnimator = ValueAnimator.ofFloat(animatedWidth, collapsedWidth).apply {
            duration = 200
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                animatedWidth = it.animatedValue as Float
                requestLayout()
                invalidate()
            }
            start()
        }
    }

    fun dismiss() {
        handler.removeCallbacks(autoDismissRunnable)
        handler.removeCallbacks(autoCollapseRunnable)

        slideAnimator?.cancel()
        slideAnimator = ValueAnimator.ofFloat(0f, width.toFloat() + 20f * density).apply {
            duration = 220
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                translationX = it.animatedValue as Float
                alpha = 1f - (it.animatedFraction * 0.8f)
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    visibility = GONE
                    collapse()
                    onDismissRequested()
                }
            })
            start()
        }
    }

    private fun resetDismissTimer() {
        handler.removeCallbacks(autoDismissRunnable)
        handler.removeCallbacks(autoCollapseRunnable)

        if (!isDragging) {
            handler.postDelayed(autoCollapseRunnable, 1400)
            handler.postDelayed(autoDismissRunnable, 2600)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                isDragging = true
                expand()
                handleTouchPosition(event.y)
                performHapticTick()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                handleTouchPosition(event.y)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                resetDismissTimer()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun handleTouchPosition(touchY: Float) {
        val topPadding = 12f * density
        val bottomPadding = if (isExpanded) iconAreaHeight + 8f * density else 12f * density
        val trackTop = topPadding
        val trackBottom = totalHeight - bottomPadding
        val trackHeight = max(1f, trackBottom - trackTop)

        // y = trackBottom corresponds to 0, y = trackTop corresponds to 1
        val fraction = ((trackBottom - touchY) / trackHeight).coerceIn(0f, 1f)
        currentVolumeFraction = fraction
        onVolumeChangedByUser(fraction)

        // Haptic feedback when crossing discrete 10% steps
        val currentStep = (fraction * 15).toInt()
        if (currentStep != lastVibratedStep) {
            lastVibratedStep = currentStep
            performHapticTick()
        }

        invalidate()
    }

    private fun performHapticTick() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(10)
            }
        } catch (_: Exception) {}
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(animatedWidth.toInt(), totalHeight.toInt())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        val cornerRadius = min(w / 2f, 24f * density)

        // 1. Draw Container Background (Rounded on left, flush on right edge)
        val bgPath = Path().apply {
            moveTo(w, 0f)
            lineTo(cornerRadius, 0f)
            quadTo(0f, 0f, 0f, cornerRadius)
            lineTo(0f, h - cornerRadius)
            quadTo(0f, h, cornerRadius, h)
            lineTo(w, h)
            close()
        }
        canvas.drawPath(bgPath, bgPaint)
        canvas.drawPath(bgPath, strokePaint)

        // 2. Volume Track Dimensions
        val isFull = animatedWidth > (collapsedWidth + 8f * density)
        val padHorizontal = if (isFull) 10f * density else 3f * density
        val topPadding = 12f * density
        val bottomPadding = if (isFull) iconAreaHeight + 8f * density else 12f * density

        val trackLeft = padHorizontal
        val trackRight = w - padHorizontal
        val trackTop = topPadding
        val trackBottom = h - bottomPadding
        val trackWidth = max(2f, trackRight - trackLeft)
        val trackHeight = max(2f, trackBottom - trackTop)
        val trackCorner = trackWidth / 2f

        // Draw track background pill
        val trackRect = RectF(trackLeft, trackTop, trackRight, trackBottom)
        canvas.drawRoundRect(trackRect, trackCorner, trackCorner, trackBgPaint)

        // 3. Draw Volume Fill Level
        if (currentVolumeFraction > 0.005f) {
            val fillTop = trackBottom - (trackHeight * currentVolumeFraction)
            val fillRect = RectF(trackLeft, fillTop, trackRight, trackBottom)

            canvas.save()
            // Clip to track boundary
            val clipPath = Path().apply {
                addRoundRect(trackRect, trackCorner, trackCorner, Path.Direction.CW)
            }
            canvas.clipPath(clipPath)
            canvas.drawRect(fillRect, fillPaint)
            canvas.restore()
        }

        // 4. Draw Knob and Percentage (when in expanded state)
        if (isFull) {
            val knobCenterY = trackBottom - (trackHeight * currentVolumeFraction)
            val knobCenterX = trackLeft + (trackWidth / 2f)

            // Draw circular knob
            canvas.drawCircle(knobCenterX, knobCenterY, knobRadius, knobPaint)
            canvas.drawCircle(knobCenterX, knobCenterY, knobRadius, knobInnerBorder)

            // Draw percentage text
            val percentText = if (currentVolumeFraction <= 0.005f) "MUTE" else "${(currentVolumeFraction * 100).toInt()}%"
            canvas.drawText(percentText, w / 2f, 18f * density, percentPaint)

            // 5. Draw Speaker Icon in the bottom section
            val iconCenterY = h - (iconAreaHeight / 2f)
            val iconCenterX = w / 2f

            drawSpeakerIcon(canvas, iconCenterX, iconCenterY, currentVolumeFraction <= 0.005f)
        }
    }

    private fun drawSpeakerIcon(canvas: Canvas, cx: Float, cy: Float, isMuted: Boolean) {
        val s = density * 1.1f

        if (isMuted) {
            // Red accent dot/background
            canvas.drawCircle(cx, cy, 14f * density, Paint().apply {
                color = Color.parseColor("#33EF4444")
                style = Paint.Style.FILL
            })

            // Red speaker cone
            val conePath = Path().apply {
                moveTo(cx - 5f * s, cy - 3f * s)
                lineTo(cx - 2f * s, cy - 3f * s)
                lineTo(cx + 2f * s, cy - 6f * s)
                lineTo(cx + 2f * s, cy + 6f * s)
                lineTo(cx - 2f * s, cy + 3f * s)
                lineTo(cx - 5f * s, cy + 3f * s)
                close()
            }
            canvas.drawPath(conePath, muteRedPaint)

            // Red diagonal slash
            canvas.drawLine(cx - 6f * s, cy - 6f * s, cx + 6f * s, cy + 6f * s, muteRedStroke)
        } else {
            // White speaker cone
            val conePath = Path().apply {
                moveTo(cx - 6f * s, cy - 3.5f * s)
                lineTo(cx - 2.5f * s, cy - 3.5f * s)
                lineTo(cx + 2f * s, cy - 7f * s)
                lineTo(cx + 2f * s, cy + 7f * s)
                lineTo(cx - 2.5f * s, cy + 3.5f * s)
                lineTo(cx - 6f * s, cy + 3.5f * s)
                close()
            }
            canvas.drawPath(conePath, iconPaint)

            // Waves based on volume
            val waveStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.STROKE
                strokeWidth = 1.8f * density
                strokeCap = Paint.Cap.ROUND
            }

            // Small wave (always if vol > 0.05)
            if (currentVolumeFraction > 0.05f) {
                val waveRect1 = RectF(cx + 1f * s, cy - 4f * s, cx + 5f * s, cy + 4f * s)
                canvas.drawArc(waveRect1, -60f, 120f, false, waveStroke)
            }

            // Large wave (if vol > 0.45)
            if (currentVolumeFraction > 0.45f) {
                val waveRect2 = RectF(cx + 3f * s, cy - 7f * s, cx + 8.5f * s, cy + 7f * s)
                canvas.drawArc(waveRect2, -60f, 120f, false, waveStroke)
            }
        }
    }
}
