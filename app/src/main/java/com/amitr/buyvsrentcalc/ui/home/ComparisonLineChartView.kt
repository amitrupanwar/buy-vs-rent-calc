package com.amitr.buyvsrentcalc.ui.home

import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import com.amitr.buyvsrentcalc.util.CurrencyFormatter
import com.google.android.material.R
import com.google.android.material.color.MaterialColors
import kotlin.math.max
import kotlin.math.min

class ComparisonLineChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class ChartMode {
        NET_WORTH,
        CUMULATIVE_OUTFLOW
    }

    var mode: ChartMode = ChartMode.NET_WORTH
        set(value) {
            field = value
            invalidate()
        }

    var currencySymbol: String = "₹"
        set(value) {
            field = value
            invalidate()
        }

    private var buySeries: List<Double> = emptyList()
    private var rentSeries: List<Double> = emptyList()
    private var seriesLabels: List<String> = emptyList()

    private var selectedIndex: Int = -1

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(1f)
        pathEffect = DashPathEffect(floatArrayOf(dpToPx(4f), dpToPx(4f)), 0f)
    }

    private val buyLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(3f)
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val rentLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(3f)
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val buyFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val rentFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = spToPx(10f)
    }

    private val tooltipBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val tooltipTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = spToPx(11f)
        isFakeBoldText = true
    }

    fun setSeriesData(buyData: List<Double>, rentData: List<Double>, labels: List<String>) {
        this.buySeries = buyData
        this.rentSeries = rentData
        this.seriesLabels = labels
        this.selectedIndex = if (buyData.isNotEmpty()) buyData.lastIndex else -1
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (buySeries.isEmpty() || width <= 0) return super.onTouchEvent(event)

        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val paddingLeft = dpToPx(12f)
                val paddingRight = dpToPx(12f)
                val plotWidth = width.toFloat() - paddingLeft - paddingRight

                val touchX = event.x.coerceIn(paddingLeft, width.toFloat() - paddingRight)
                val ratio = (touchX - paddingLeft) / plotWidth
                val index = (ratio * (buySeries.size - 1)).toInt().coerceIn(0, buySeries.lastIndex)

                if (index != selectedIndex) {
                    selectedIndex = index
                    invalidate()
                }
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (buySeries.isEmpty() || rentSeries.isEmpty()) return

        val primaryColor = MaterialColors.getColor(this, R.attr.colorPrimary, 0xFF0284C7.toInt())
        val secondaryColor = MaterialColors.getColor(this, R.attr.colorSecondary, 0xFFE11D48.toInt())
        val outlineColor = MaterialColors.getColor(this, R.attr.colorOutlineVariant, 0xFFCCCCCC.toInt())
        val textColor = MaterialColors.getColor(this, R.attr.colorOnSurfaceVariant, 0xFF666666.toInt())
        val surfaceColor = MaterialColors.getColor(this, R.attr.colorSurfaceContainerHigh, 0xFF333333.toInt())

        gridPaint.color = outlineColor
        buyLinePaint.color = primaryColor
        rentLinePaint.color = secondaryColor
        textPaint.color = textColor

        val paddingLeft = dpToPx(12f)
        val paddingRight = dpToPx(12f)
        val paddingTop = dpToPx(24f)
        val paddingBottom = dpToPx(28f)

        val plotWidth = width.toFloat() - paddingLeft - paddingRight
        val plotHeight = height.toFloat() - paddingTop - paddingBottom

        if (plotWidth <= 0 || plotHeight <= 0) return

        val allValues = buySeries + rentSeries
        var minY = allValues.minOrNull() ?: 0.0
        var maxY = allValues.maxOrNull() ?: 1.0

        if (minY > 0) minY = 0.0
        if (maxY <= minY) maxY = minY + 1.0

        val rangeY = maxY - minY

        // 1. Draw horizontal grid lines (3 lines)
        for (i in 0..2) {
            val ratio = i / 2f
            val y = paddingTop + ratio * plotHeight
            canvas.drawLine(paddingLeft, y, paddingLeft + plotWidth, y, gridPaint)
        }

        // Helper to convert data point to Canvas (X, Y)
        val getX: (Int) -> Float = { idx ->
            val ratio = if (buySeries.size > 1) idx.toFloat() / (buySeries.size - 1) else 0f
            paddingLeft + ratio * plotWidth
        }

        val getY: (Double) -> Float = { valY ->
            val ratio = ((valY - minY) / rangeY).toFloat().coerceIn(0f, 1f)
            paddingTop + (1f - ratio) * plotHeight
        }

        // 2. Build Buy Path
        val buyPath = Path()
        val buyFillPath = Path()

        val buyStartX = getX(0)
        val buyStartY = getY(buySeries[0])

        buyPath.moveTo(buyStartX, buyStartY)
        buyFillPath.moveTo(buyStartX, paddingTop + plotHeight)
        buyFillPath.lineTo(buyStartX, buyStartY)

        for (i in 1..buySeries.lastIndex) {
            val cx = getX(i)
            val cy = getY(buySeries[i])
            buyPath.lineTo(cx, cy)
            buyFillPath.lineTo(cx, cy)
        }

        val buyEndX = getX(buySeries.lastIndex)
        buyFillPath.lineTo(buyEndX, paddingTop + plotHeight)
        buyFillPath.close()

        buyFillPaint.shader = LinearGradient(
            0f, paddingTop, 0f, paddingTop + plotHeight,
            primaryColor and 0x33FFFFFF, primaryColor and 0x05FFFFFF,
            Shader.TileMode.CLAMP
        )

        // 3. Build Rent Path
        val rentPath = Path()
        val rentFillPath = Path()

        val rentStartX = getX(0)
        val rentStartY = getY(rentSeries[0])

        rentPath.moveTo(rentStartX, rentStartY)
        rentFillPath.moveTo(rentStartX, paddingTop + plotHeight)
        rentFillPath.lineTo(rentStartX, rentStartY)

        for (i in 1..rentSeries.lastIndex) {
            val cx = getX(i)
            val cy = getY(rentSeries[i])
            rentPath.lineTo(cx, cy)
            rentFillPath.lineTo(cx, cy)
        }

        val rentEndX = getX(rentSeries.lastIndex)
        rentFillPath.lineTo(rentEndX, paddingTop + plotHeight)
        rentFillPath.close()

        rentFillPaint.shader = LinearGradient(
            0f, paddingTop, 0f, paddingTop + plotHeight,
            secondaryColor and 0x33FFFFFF, secondaryColor and 0x05FFFFFF,
            Shader.TileMode.CLAMP
        )

        // Draw fills and lines
        canvas.drawPath(rentFillPath, rentFillPaint)
        canvas.drawPath(buyFillPath, buyFillPaint)

        canvas.drawPath(rentPath, rentLinePaint)
        canvas.drawPath(buyPath, buyLinePaint)

        // 4. Draw Selected Point Overlay & Tooltip
        if (selectedIndex in buySeries.indices) {
            val cx = getX(selectedIndex)
            val buyY = getY(buySeries[selectedIndex])
            val rentY = getY(rentSeries[selectedIndex])

            canvas.drawLine(cx, paddingTop, cx, paddingTop + plotHeight, gridPaint)

            val pointRadius = dpToPx(5f)
            val pointBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFFFFFFF.toInt()
                style = Paint.Style.FILL
            }

            canvas.drawCircle(cx, buyY, pointRadius + dpToPx(2f), pointBorder)
            canvas.drawCircle(cx, buyY, pointRadius, buyLinePaint)

            canvas.drawCircle(cx, rentY, pointRadius + dpToPx(2f), pointBorder)
            canvas.drawCircle(cx, rentY, pointRadius, rentLinePaint)

            // Tooltip Card Box
            val labelText = if (selectedIndex in seriesLabels.indices) seriesLabels[selectedIndex] else "Period ${selectedIndex + 1}"
            val buyText = "Buy: " + CurrencyFormatter.format(buySeries[selectedIndex], currencySymbol)
            val rentText = "Rent: " + CurrencyFormatter.format(rentSeries[selectedIndex], currencySymbol)

            val tooltipWidth = dpToPx(130f)
            val tooltipHeight = dpToPx(52f)

            var boxLeft = cx - tooltipWidth / 2f
            if (boxLeft < paddingLeft) boxLeft = paddingLeft
            if (boxLeft + tooltipWidth > width - paddingRight) boxLeft = width - paddingRight - tooltipWidth

            val boxTop = paddingTop - dpToPx(12f)
            val rect = RectF(boxLeft, boxTop, boxLeft + tooltipWidth, boxTop + tooltipHeight)

            tooltipBoxPaint.color = surfaceColor
            canvas.drawRoundRect(rect, dpToPx(8f), dpToPx(8f), tooltipBoxPaint)

            tooltipTextPaint.color = 0xFFFFFFFF.toInt()
            tooltipTextPaint.textSize = spToPx(10f)
            canvas.drawText(labelText, boxLeft + dpToPx(8f), boxTop + dpToPx(14f), tooltipTextPaint)

            tooltipTextPaint.color = primaryColor
            canvas.drawText(buyText, boxLeft + dpToPx(8f), boxTop + dpToPx(28f), tooltipTextPaint)

            tooltipTextPaint.color = secondaryColor
            canvas.drawText(rentText, boxLeft + dpToPx(8f), boxTop + dpToPx(42f), tooltipTextPaint)
        }
    }

    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)
    }

    private fun spToPx(sp: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, resources.displayMetrics)
    }
}
