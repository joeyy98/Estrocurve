package com.example.ui.components

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.E2Unit
import com.example.data.model.TSuppressionStatus
import com.example.data.model.TUnit
import com.example.pk.DataPoint
import com.example.ui.theme.ChartGridLine
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.EstradiolPink
import com.example.ui.theme.HormoneColorResolver
import com.example.ui.theme.StatusBorderline
import com.example.ui.theme.StatusElevated
import com.example.ui.theme.StatusSuppressed
import com.example.ui.theme.TargetRangeBand
import com.example.ui.theme.TestosteroneCyan
import com.example.ui.theme.TextMuted
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

@Composable
fun TechnicalHormoneChart(
    dataPoints: List<DataPoint>,
    modifier: Modifier = Modifier,
    targetE2Min: Double = 200.0,
    targetE2Max: Double = 350.0,
    e2Unit: E2Unit = E2Unit.PG_ML,
    tUnit: TUnit = TUnit.NG_DL,
    isOled: Boolean = true,
    showNowMarker: Boolean = true,
    showSecondaryComparePoints: List<DataPoint>? = null,
    compareLabel: String = "Compare Regimen"
) {
    var showE2 by remember { mutableStateOf(true) }
    var showT by remember { mutableStateOf(true) }
    var showTargetBand by remember { mutableStateOf(true) }
    var touchXFraction by remember { mutableStateOf<Float?>(null) }

    val context = LocalContext.current
    val isNight = isSystemInDarkTheme()

    val chipContainer = if (isOled) DarkSurface else MaterialTheme.colorScheme.surfaceVariant
    val chipBorder = if (isOled) DarkBorder else MaterialTheme.colorScheme.outlineVariant

    // Distinct hormone line & legend pigments with monochrome & low-contrast protection
    val hormoneColors = remember(context, isOled, isNight) {
        HormoneColorResolver.resolve(context, isOled, isNight)
    }
    val e2CurveColor = hormoneColors.e2Color
    val tCurveColor = hormoneColors.tColor
    val compareCurveColor = hormoneColors.compareColor

    val gridColor = if (isOled) ChartGridLine else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val axisTextColor = if (isOled) Color(0xFF888888) else MaterialTheme.colorScheme.onSurfaceVariant
    val nowColor = if (isOled) Color(0xFFE0E0E0) else MaterialTheme.colorScheme.onSurface
    val scrubberColor = if (isOled) Color.White else MaterialTheme.colorScheme.onSurface
    val textMutedColor = if (isOled) TextMuted else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("technical_hormone_chart")
    ) {
        // High density controls row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterChip(
                    selected = showE2,
                    onClick = { showE2 = !showE2 },
                    label = { Text("E2 (${e2Unit.label})", fontSize = 11.sp, color = if (showE2) e2CurveColor else textMutedColor) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = chipContainer,
                        selectedContainerColor = if (isOled) Color(0xFF220E18) else e2CurveColor.copy(alpha = 0.18f)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = showE2,
                        borderColor = chipBorder,
                        selectedBorderColor = e2CurveColor
                    ),
                    modifier = Modifier.height(28.dp)
                )

                FilterChip(
                    selected = showT,
                    onClick = { showT = !showT },
                    label = { Text("T (${tUnit.label})", fontSize = 11.sp, color = if (showT) tCurveColor else textMutedColor) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = chipContainer,
                        selectedContainerColor = if (isOled) Color(0xFF0A1F26) else tCurveColor.copy(alpha = 0.18f)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = showT,
                        borderColor = chipBorder,
                        selectedBorderColor = tCurveColor
                    ),
                    modifier = Modifier.height(28.dp)
                )

                FilterChip(
                    selected = showTargetBand,
                    onClick = { showTargetBand = !showTargetBand },
                    label = { Text("Target", fontSize = 11.sp, color = if (showTargetBand) e2CurveColor else textMutedColor) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = chipContainer,
                        selectedContainerColor = if (isOled) Color(0xFF1F1218) else e2CurveColor.copy(alpha = 0.18f)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = showTargetBand,
                        borderColor = chipBorder,
                        selectedBorderColor = e2CurveColor
                    ),
                    modifier = Modifier.height(28.dp)
                )
            }
        }

        // Active inspection HUD badge when user touches the chart
        if (dataPoints.isNotEmpty() && touchXFraction != null) {
            val clampedFraction = touchXFraction!!.coerceIn(0f, 1f)
            val index = (clampedFraction * (dataPoints.size - 1)).toInt().coerceIn(0, dataPoints.size - 1)
            val point = dataPoints[index]
            val status = TSuppressionStatus.fromTestosteroneNgDl(point.testosteroneNgDl)

            val statusColor = when (status) {
                TSuppressionStatus.SUPPRESSED -> StatusSuppressed
                TSuppressionStatus.BORDERLINE -> StatusBorderline
                TSuppressionStatus.ELEVATED -> StatusElevated
            }

            Surface(
                color = if (isOled) Color(0xFF141414) else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(4.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, chipBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val timeStr = if (point.timeMillis > 100000000000L) {
                        SimpleDateFormat("MMM d, HH:mm", Locale.US).format(Date(point.timeMillis))
                    } else {
                        String.format(Locale.US, "Day %.1f", point.hoursFromNow / 24.0)
                    }

                    Text(
                        text = timeStr,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "E2: ${e2Unit.format(point.e2PgMl)}",
                            fontSize = 11.sp,
                            color = e2CurveColor
                        )
                        Text(
                            text = "T: ${tUnit.format(point.testosteroneNgDl)}",
                            fontSize = 11.sp,
                            color = tCurveColor
                        )
                        Text(
                            text = status.name,
                            fontSize = 10.sp,
                            color = statusColor
                        )
                    }
                }
            }
        }

        // Precision Canvas Chart
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .padding(top = 4.dp, bottom = 4.dp)
                .pointerInput(dataPoints) {
                    detectTapGestures(
                        onPress = { offset ->
                            val padLeft = 45.dp.toPx()
                            val padRight = 40.dp.toPx()
                            val chartWidth = size.width - padLeft - padRight
                            if (chartWidth > 0) {
                                touchXFraction = ((offset.x - padLeft) / chartWidth).coerceIn(0f, 1f)
                            }
                        }
                    )
                }
                .pointerInput(dataPoints) {
                    detectDragGestures(
                        onDrag = { change, _ ->
                            val padLeft = 45.dp.toPx()
                            val padRight = 40.dp.toPx()
                            val chartWidth = size.width - padLeft - padRight
                            if (chartWidth > 0) {
                                touchXFraction = ((change.position.x - padLeft) / chartWidth).coerceIn(0f, 1f)
                            }
                        },
                        onDragEnd = {
                            // Keep touchHUD active or dismiss
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (dataPoints.isEmpty()) return@Canvas

                val padLeft = 46.dp.toPx()
                val padRight = 42.dp.toPx()
                val padTop = 14.dp.toPx()
                val padBottom = 22.dp.toPx()

                val chartWidth = size.width - padLeft - padRight
                val chartHeight = size.height - padTop - padBottom

                if (chartWidth <= 0 || chartHeight <= 0) return@Canvas

                // Compute Y range for Estradiol
                var maxE2 = dataPoints.maxOfOrNull { it.e2PgMl } ?: 300.0
                if (showSecondaryComparePoints != null) {
                    val compMax = showSecondaryComparePoints.maxOfOrNull { it.e2PgMl } ?: 0.0
                    maxE2 = max(maxE2, compMax)
                }
                maxE2 = max(maxE2, targetE2Max * 1.15)
                maxE2 = (kotlin.math.ceil(maxE2 / 100.0) * 100.0).coerceAtLeast(400.0)

                // Compute Y range for Testosterone (0 to 600 ng/dL)
                val maxT = 600.0

                // Draw background grid lines and tick labels
                val numGridLines = 4
                val textPaint = android.graphics.Paint().apply {
                    color = axisTextColor.toArgb()
                    textSize = 9.sp.toPx()
                    isAntiAlias = true
                }
                val e2AxisPaint = android.graphics.Paint().apply {
                    color = e2CurveColor.toArgb()
                    textSize = 9.sp.toPx()
                    isAntiAlias = true
                }
                val tAxisPaint = android.graphics.Paint().apply {
                    color = tCurveColor.toArgb()
                    textSize = 9.sp.toPx()
                    isAntiAlias = true
                }

                for (i in 0..numGridLines) {
                    val fraction = i.toFloat() / numGridLines.toFloat()
                    val y = padTop + chartHeight * (1f - fraction)
                    val e2Value = maxE2 * fraction
                    val tValue = maxT * fraction

                    // Grid line
                    drawLine(
                        color = gridColor,
                        start = Offset(padLeft, y),
                        end = Offset(padLeft + chartWidth, y),
                        strokeWidth = 1.dp.toPx()
                    )

                    // Left Y-axis label (E2)
                    val formattedE2 = e2Unit.fromPgMl(e2Value).toInt().toString()
                    drawContext.canvas.nativeCanvas.drawText(
                        formattedE2,
                        4.dp.toPx(),
                        y + 3.dp.toPx(),
                        e2AxisPaint
                    )

                    // Right Y-axis label (T)
                    val formattedT = tUnit.fromNgDl(tValue).toInt().toString()
                    drawContext.canvas.nativeCanvas.drawText(
                        formattedT,
                        padLeft + chartWidth + 6.dp.toPx(),
                        y + 3.dp.toPx(),
                        tAxisPaint
                    )
                }

                // Draw target E2 band
                if (showTargetBand) {
                    val yMin = padTop + chartHeight * (1f - (targetE2Min / maxE2).toFloat().coerceIn(0f, 1f))
                    val yMax = padTop + chartHeight * (1f - (targetE2Max / maxE2).toFloat().coerceIn(0f, 1f))

                    drawRect(
                        color = if (isOled) TargetRangeBand else e2CurveColor.copy(alpha = 0.15f),
                        topLeft = Offset(padLeft, yMax),
                        size = androidx.compose.ui.geometry.Size(chartWidth, yMin - yMax)
                    )

                    // Dashed boundary lines
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    drawLine(
                        color = e2CurveColor.copy(alpha = 0.55f),
                        start = Offset(padLeft, yMin),
                        end = Offset(padLeft + chartWidth, yMin),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = dashEffect
                    )
                    drawLine(
                        color = e2CurveColor.copy(alpha = 0.55f),
                        start = Offset(padLeft, yMax),
                        end = Offset(padLeft + chartWidth, yMax),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = dashEffect
                    )
                }

                // Draw "NOW" marker line if requested
                if (showNowMarker) {
                    val nowIndex = dataPoints.indexOfLast { it.isPast }
                    if (nowIndex >= 0 && dataPoints.size > 1) {
                        val nowXFraction = nowIndex.toFloat() / (dataPoints.size - 1).toFloat()
                        val nowX = padLeft + chartWidth * nowXFraction

                        drawLine(
                            color = nowColor,
                            start = Offset(nowX, padTop),
                            end = Offset(nowX, padTop + chartHeight),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                        )

                        drawContext.canvas.nativeCanvas.drawText(
                            "NOW",
                            nowX - 8.dp.toPx(),
                            padTop - 2.dp.toPx(),
                            textPaint
                        )
                    }
                }

                // Secondary comparison curve if provided
                if (showSecondaryComparePoints != null && showSecondaryComparePoints.isNotEmpty()) {
                    val compPath = Path()
                    showSecondaryComparePoints.forEachIndexed { i, pt ->
                        val x = padLeft + chartWidth * (i.toFloat() / (showSecondaryComparePoints.size - 1).toFloat())
                        val y = padTop + chartHeight * (1f - (pt.e2PgMl / maxE2).toFloat().coerceIn(0f, 1f))
                        if (i == 0) compPath.moveTo(x, y) else compPath.lineTo(x, y)
                    }
                    drawPath(
                        path = compPath,
                        color = compareCurveColor,
                        style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f))
                    )
                }

                // Primary E2 Curve
                if (showE2) {
                    val e2Path = Path()
                    dataPoints.forEachIndexed { i, pt ->
                        val x = padLeft + chartWidth * (i.toFloat() / (dataPoints.size - 1).toFloat())
                        val y = padTop + chartHeight * (1f - (pt.e2PgMl / maxE2).toFloat().coerceIn(0f, 1f))
                        if (i == 0) e2Path.moveTo(x, y) else e2Path.lineTo(x, y)
                    }
                    drawPath(
                        path = e2Path,
                        color = e2CurveColor,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                // Secondary T Curve
                if (showT) {
                    val tPath = Path()
                    dataPoints.forEachIndexed { i, pt ->
                        val x = padLeft + chartWidth * (i.toFloat() / (dataPoints.size - 1).toFloat())
                        val y = padTop + chartHeight * (1f - (pt.testosteroneNgDl / maxT).toFloat().coerceIn(0f, 1f))
                        if (i == 0) tPath.moveTo(x, y) else tPath.lineTo(x, y)
                    }
                    drawPath(
                        path = tPath,
                        color = tCurveColor,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }

                // X-axis day labels
                val numXTicks = 5
                for (j in 0..numXTicks) {
                    val frac = j.toFloat() / numXTicks.toFloat()
                    val x = padLeft + chartWidth * frac
                    val index = (frac * (dataPoints.size - 1)).toInt().coerceIn(0, dataPoints.size - 1)
                    val pt = dataPoints[index]

                    val xLabel = if (pt.timeMillis > 100000000000L) {
                        SimpleDateFormat("MM/dd", Locale.US).format(Date(pt.timeMillis))
                    } else {
                        String.format(Locale.US, "d%.0f", pt.hoursFromNow / 24.0)
                    }

                    drawContext.canvas.nativeCanvas.drawText(
                        xLabel,
                        x - 10.dp.toPx(),
                        padTop + chartHeight + 14.dp.toPx(),
                        textPaint
                    )
                }

                // Touch scrubber cursor
                if (touchXFraction != null) {
                    val cursorX = padLeft + chartWidth * touchXFraction!!.coerceIn(0f, 1f)
                    drawLine(
                        color = scrubberColor,
                        start = Offset(cursorX, padTop),
                        end = Offset(cursorX, padTop + chartHeight),
                        strokeWidth = 1.dp.toPx()
                    )

                    val index = (touchXFraction!!.coerceIn(0f, 1f) * (dataPoints.size - 1)).toInt().coerceIn(0, dataPoints.size - 1)
                    val pt = dataPoints[index]

                    if (showE2) {
                        val e2Y = padTop + chartHeight * (1f - (pt.e2PgMl / maxE2).toFloat().coerceIn(0f, 1f))
                        drawCircle(color = e2CurveColor, radius = 4.dp.toPx(), center = Offset(cursorX, e2Y))
                    }
                    if (showT) {
                        val tY = padTop + chartHeight * (1f - (pt.testosteroneNgDl / maxT).toFloat().coerceIn(0f, 1f))
                        drawCircle(color = tCurveColor, radius = 4.dp.toPx(), center = Offset(cursorX, tY))
                    }
                }
            }
        }
    }
}
