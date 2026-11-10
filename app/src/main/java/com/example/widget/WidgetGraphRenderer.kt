package com.example.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.data.model.E2Unit
import com.example.data.model.TUnit
import com.example.pk.DataPoint
import kotlin.math.ceil
import kotlin.math.max

object WidgetGraphRenderer {

    /**
     * Renders a high-clarity bitmap representation of the 10-day pharmacokinetic dynamics chart,
     * adapting canvas colors, axis lines, and curve pigments to OLED Black or Dynamic System themes.
     */
    fun render(
        context: Context,
        dataPoints: List<DataPoint>,
        targetE2Min: Double,
        targetE2Max: Double,
        e2Unit: E2Unit,
        tUnit: TUnit,
        isOled: Boolean = true,
        isNight: Boolean = true,
        width: Int = 760,
        height: Int = 360
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Determine Theme Palette for Canvas
        val canvasBg: Int
        val gridColor: Int
        val nowLineColor: Int
        val timeLabelColor: Int
        val targetBandColor: Int
        val targetDashedColor: Int
        val emptyTextColor: Int

        val hormoneColors = com.example.ui.theme.HormoneColorResolver.resolve(context, isOled, isNight)
        val e2CurveColor = hormoneColors.e2Argb
        val tCurveColor = hormoneColors.tArgb

        if (isOled) {
            canvasBg = Color.parseColor("#0C0C0C")
            gridColor = Color.parseColor("#222222")
            nowLineColor = Color.parseColor("#FFFFFF")
            timeLabelColor = Color.parseColor("#757575")
            targetBandColor = Color.parseColor("#22FF4081")
            targetDashedColor = Color.parseColor("#55FF4081")
            emptyTextColor = Color.parseColor("#757575")
        } else if (isNight) {
            canvasBg = Color.parseColor("#1E1F24")
            gridColor = Color.parseColor("#2E3036")
            nowLineColor = Color.parseColor("#E6E1E5")
            timeLabelColor = Color.parseColor("#929094")
            targetBandColor = Color.argb(35, Color.red(e2CurveColor), Color.green(e2CurveColor), Color.blue(e2CurveColor))
            targetDashedColor = Color.argb(85, Color.red(e2CurveColor), Color.green(e2CurveColor), Color.blue(e2CurveColor))
            emptyTextColor = Color.parseColor("#929094")
        } else {
            // Dynamic Light Mode
            canvasBg = Color.parseColor("#F4F5F8")
            gridColor = Color.parseColor("#DFE1E6")
            nowLineColor = Color.parseColor("#1C1B1F")
            timeLabelColor = Color.parseColor("#79747E")
            targetBandColor = Color.argb(25, Color.red(e2CurveColor), Color.green(e2CurveColor), Color.blue(e2CurveColor))
            targetDashedColor = Color.argb(90, Color.red(e2CurveColor), Color.green(e2CurveColor), Color.blue(e2CurveColor))
            emptyTextColor = Color.parseColor("#79747E")
        }

        canvas.drawColor(canvasBg)

        if (dataPoints.isEmpty()) {
            val emptyPaint = Paint().apply {
                color = emptyTextColor
                textSize = 24f
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("No injection logs to chart yet", width / 2f, height / 2f - 10f, emptyPaint)
            val subPaint = Paint().apply {
                color = timeLabelColor
                textSize = 18f
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("Open app to log your initial injection", width / 2f, height / 2f + 25f, subPaint)
            return bitmap
        }

        val padLeft = 60f
        val padRight = 55f
        val padTop = 20f
        val padBottom = 38f

        val chartWidth = width - padLeft - padRight
        val chartHeight = height - padTop - padBottom
        if (chartWidth <= 0 || chartHeight <= 0) return bitmap

        // Determine E2 Y range
        val maxPointE2 = dataPoints.maxOfOrNull { it.e2PgMl } ?: 300.0
        var maxE2 = max(maxPointE2, targetE2Max * 1.15)
        maxE2 = (ceil(maxE2 / 100.0) * 100.0).coerceAtLeast(350.0)

        // Determine T Y range (monotherapy standard 0 - 600 ng/dL)
        val maxT = 600.0

        // Time range mapping
        val minTime = dataPoints.minOfOrNull { it.timeMillis } ?: 0L
        val maxTime = dataPoints.maxOfOrNull { it.timeMillis } ?: 1L
        val timeSpan = max(1L, maxTime - minTime)

        fun getX(timeMillis: Long): Float {
            val fraction = (timeMillis - minTime).toFloat() / timeSpan.toFloat()
            return padLeft + chartWidth * fraction.coerceIn(0f, 1f)
        }

        fun getYForE2(e2: Double): Float {
            val fraction = (e2 / maxE2).toFloat().coerceIn(0f, 1f)
            return padTop + chartHeight * (1f - fraction)
        }

        fun getYForT(t: Double): Float {
            val fraction = (t / maxT).toFloat().coerceIn(0f, 1f)
            return padTop + chartHeight * (1f - fraction)
        }

        // Draw horizontal grid lines & labels
        val gridPaint = Paint().apply {
            color = gridColor
            strokeWidth = 1.5f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        val e2AxisPaint = Paint().apply {
            color = e2CurveColor
            textSize = 18f
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
        }
        val tAxisPaint = Paint().apply {
            color = tCurveColor
            textSize = 18f
            isAntiAlias = true
            textAlign = Paint.Align.LEFT
        }

        val numGridLines = 4
        for (i in 0..numGridLines) {
            val frac = i.toFloat() / numGridLines.toFloat()
            val y = padTop + chartHeight * (1f - frac)
            val e2Val = maxE2 * frac
            val tVal = maxT * frac

            canvas.drawLine(padLeft, y, padLeft + chartWidth, y, gridPaint)

            // Left E2 label
            val formattedE2 = e2Unit.fromPgMl(e2Val).toInt().toString()
            canvas.drawText(formattedE2, padLeft - 8f, y + 6f, e2AxisPaint)

            // Right T label
            val formattedT = tUnit.fromNgDl(tVal).toInt().toString()
            canvas.drawText(formattedT, padLeft + chartWidth + 8f, y + 6f, tAxisPaint)
        }

        // Target E2 range band
        val yTargetMin = getYForE2(targetE2Min)
        val yTargetMax = getYForE2(targetE2Max)
        val targetBandPaint = Paint().apply {
            color = targetBandColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRect(padLeft, yTargetMax, padLeft + chartWidth, yTargetMin, targetBandPaint)

        val targetDashedPaint = Paint().apply {
            color = targetDashedColor
            strokeWidth = 1.5f
            style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(6f, 6f), 0f)
            isAntiAlias = true
        }
        canvas.drawLine(padLeft, yTargetMin, padLeft + chartWidth, yTargetMin, targetDashedPaint)
        canvas.drawLine(padLeft, yTargetMax, padLeft + chartWidth, yTargetMax, targetDashedPaint)

        // Build Paths for E2 and T
        val e2Path = Path()
        val e2FillPath = Path()
        val tPath = Path()

        var first = true
        var firstX = padLeft
        var lastX = padLeft + chartWidth

        for (pt in dataPoints) {
            val x = getX(pt.timeMillis)
            val yE2 = getYForE2(pt.e2PgMl)
            val yT = getYForT(pt.testosteroneNgDl)

            if (first) {
                firstX = x
                e2Path.moveTo(x, yE2)
                e2FillPath.moveTo(x, padTop + chartHeight)
                e2FillPath.lineTo(x, yE2)
                tPath.moveTo(x, yT)
                first = false
            } else {
                e2Path.lineTo(x, yE2)
                e2FillPath.lineTo(x, yE2)
                tPath.lineTo(x, yT)
                lastX = x
            }
        }
        e2FillPath.lineTo(lastX, padTop + chartHeight)
        e2FillPath.close()

        // E2 Area Gradient Fill
        val fillShader = LinearGradient(
            0f, padTop, 0f, padTop + chartHeight,
            Color.argb(55, Color.red(e2CurveColor), Color.green(e2CurveColor), Color.blue(e2CurveColor)),
            Color.argb(0, Color.red(e2CurveColor), Color.green(e2CurveColor), Color.blue(e2CurveColor)),
            Shader.TileMode.CLAMP
        )
        val fillPaint = Paint().apply {
            shader = fillShader
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawPath(e2FillPath, fillPaint)

        // E2 Line Paint
        val e2LinePaint = Paint().apply {
            color = e2CurveColor
            strokeWidth = 3.8f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
        }
        canvas.drawPath(e2Path, e2LinePaint)

        // T Line Paint
        val tLinePaint = Paint().apply {
            color = tCurveColor
            strokeWidth = 3.2f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
        }
        canvas.drawPath(tPath, tLinePaint)

        // Draw "NOW" vertical timeline marker
        val nowMillis = System.currentTimeMillis()
        if (nowMillis in minTime..maxTime) {
            val nowX = getX(nowMillis)
            val nowLinePaint = Paint().apply {
                color = nowLineColor
                strokeWidth = 2.0f
                style = Paint.Style.STROKE
                pathEffect = DashPathEffect(floatArrayOf(6f, 6f), 0f)
                isAntiAlias = true
            }
            canvas.drawLine(nowX, padTop, nowX, padTop + chartHeight, nowLinePaint)

            // Current point indicator dots
            val currentPt = dataPoints.minByOrNull { kotlin.math.abs(it.timeMillis - nowMillis) }
            if (currentPt != null) {
                val curE2Y = getYForE2(currentPt.e2PgMl)
                val curTY = getYForT(currentPt.testosteroneNgDl)

                val dotPaint = Paint().apply {
                    isAntiAlias = true
                    style = Paint.Style.FILL
                }
                dotPaint.color = e2CurveColor
                canvas.drawCircle(nowX, curE2Y, 5f, dotPaint)

                dotPaint.color = tCurveColor
                canvas.drawCircle(nowX, curTY, 4.5f, dotPaint)
            }
        }

        // Draw Time Axis Markers at bottom (-7d, -5d, -3d, -1d, +1d, +3d)
        val timeAxisPaint = Paint().apply {
            color = timeLabelColor
            textSize = 17f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        val dayStep = 2 // every 2 days
        val pastDays = 7
        val futureDays = 3
        for (dayOffset in -pastDays..futureDays step dayStep) {
            val targetTime = nowMillis + (dayOffset * 24L * 3600L * 1000L)
            if (targetTime in minTime..maxTime) {
                val tx = getX(targetTime)
                val label = if (dayOffset == 0) "NOW" else "${dayOffset}d"
                canvas.drawText(label, tx, height - 10f, timeAxisPaint)
            }
        }

        return bitmap
    }
}
