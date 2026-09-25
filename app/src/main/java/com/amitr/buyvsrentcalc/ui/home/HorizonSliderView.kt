package com.amitr.buyvsrentcalc.ui.home

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.amitr.buyvsrentcalc.R
import com.amitr.buyvsrentcalc.databinding.ViewHorizonSliderBinding
import com.google.android.material.color.MaterialColors
import com.google.android.material.slider.Slider

class HorizonSliderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding: ViewHorizonSliderBinding =
        ViewHorizonSliderBinding.inflate(LayoutInflater.from(context), this)

    val slider: Slider get() = binding.sliderHorizon

    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(2f)
        strokeCap = Paint.Cap.ROUND
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = spToPx(11f)
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    init {
        setWillNotDraw(false)
        slider.addOnChangeListener { _, _, _ -> invalidate() }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        invalidate()
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)

        val sidePadding = slider.trackSidePadding
        val trackWidth = slider.trackWidth

        if (trackWidth <= 0) return

        val sliderLeft = slider.left.toFloat()
        val sliderTop = slider.top.toFloat()
        val trackY = sliderTop + (slider.height / 2f)

        val valFrom = slider.valueFrom
        val valTo = slider.valueTo
        val totalRange = valTo - valFrom

        if (totalRange <= 0) return

        val outlineColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOutline, 0xFF888888.toInt())
        val textColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant, 0xFF444444.toInt())

        tickPaint.color = outlineColor
        textPaint.color = textColor

        val tickValues = listOf(1f, 5f, 10f, 15f, 20f, 25f, 30f)
        val majorLabelValues = setOf(1f, 10f, 20f, 30f)

        val tickTop = trackY + dpToPx(10f)
        val tickBottom = trackY + dpToPx(18f)
        val textY = tickBottom + dpToPx(14f)

        for (v in tickValues) {
            val ratio = (v - valFrom) / totalRange
            val cx = sliderLeft + sidePadding + ratio * trackWidth

            // Draw vertical tick mark
            canvas.drawLine(cx, tickTop, cx, tickBottom, tickPaint)

            // Draw integrated text label for major milestones (1, 10, 20, 30)
            if (v in majorLabelValues) {
                val labelText = "${v.toInt()} Yr"
                canvas.drawText(labelText, cx, textY, textPaint)
            }
        }
    }

    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)
    }

    private fun spToPx(sp: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, resources.displayMetrics)
    }
}
