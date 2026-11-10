package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppThemeMode
import com.example.data.model.E2Unit
import com.example.data.model.EstradiolEster
import com.example.data.model.TUnit
import com.example.data.model.UserSettings
import com.example.ui.SimulatorState
import com.example.ui.components.MetricCell
import com.example.ui.components.TechnicalHormoneChart
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EstradiolPink
import com.example.ui.theme.StatusBorderline
import com.example.ui.theme.StatusElevated
import com.example.ui.theme.StatusSuppressed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun SimulatorScreen(
    simulatorState: SimulatorState,
    settings: UserSettings,
    onEsterChanged: (EstradiolEster) -> Unit,
    onDoseChanged: (Double) -> Unit,
    onIntervalChanged: (Double) -> Unit,
    onSteadyStateToggled: (Boolean) -> Unit,
    onComparisonToggled: (Boolean) -> Unit,
    onCompareEsterChanged: (EstradiolEster) -> Unit,
    onCompareDoseChanged: (Double) -> Unit,
    onCompareIntervalChanged: (Double) -> Unit,
    onApplyToRegimen: (EstradiolEster, Double, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val isOled = settings.themeMode == AppThemeMode.OLED_BLACK
    val cardBg = if (isOled) DarkSurface else MaterialTheme.colorScheme.surface
    val cardVariantBg = if (isOled) DarkSurfaceVariant else MaterialTheme.colorScheme.surfaceVariant
    val cardBorder = if (isOled) DarkBorder else MaterialTheme.colorScheme.outlineVariant
    val primaryAccent = if (isOled) EstradiolPink else MaterialTheme.colorScheme.primary
    val compareAccent = if (isOled) Color(0xFFAB47BC) else MaterialTheme.colorScheme.tertiary
    val textPrimaryColor = if (isOled) TextPrimary else MaterialTheme.colorScheme.onSurface
    val textSecondaryColor = if (isOled) TextSecondary else MaterialTheme.colorScheme.onSurfaceVariant
    val textMutedColor = if (isOled) TextMuted else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 12.dp)
            .testTag("simulator_screen"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Simulator Title & mode switch
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "PHARMACOKINETIC SIMULATOR",
                        fontSize = 11.sp,
                        color = primaryAccent,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "2-Compartment Depot Model",
                        fontSize = 10.sp,
                        color = textMutedColor
                    )
                }

                Surface(
                    color = cardBg,
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, cardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (simulatorState.isSteadyState) "STEADY" else "ACCUM",
                            fontSize = 10.sp,
                            color = if (simulatorState.isSteadyState) primaryAccent else textSecondaryColor
                        )
                        Switch(
                            checked = simulatorState.isSteadyState,
                            onCheckedChange = { onSteadyStateToggled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = primaryAccent,
                                checkedTrackColor = if (isOled) Color(0xFF381423) else MaterialTheme.colorScheme.primaryContainer,
                                uncheckedThumbColor = textSecondaryColor,
                                uncheckedTrackColor = cardVariantBg
                            ),
                            modifier = Modifier.height(24.dp).testTag("simulator_steady_state_switch")
                        )
                    }
                }
            }
        }

        // Primary Regimen Controls
        item {
            Surface(
                color = cardBg,
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "PRIMARY REGIMEN",
                        fontSize = 10.sp,
                        color = textMutedColor,
                        letterSpacing = 0.5.sp
                    )

                    // Ester selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        EstradiolEster.entries.forEach { ester ->
                            val isSelected = ester == simulatorState.ester
                            Surface(
                                color = if (isSelected) {
                                    if (isOled) Color(0xFF2C101F) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                } else cardVariantBg,
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(1.dp, if (isSelected) primaryAccent else cardBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onEsterChanged(ester) }
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = ester.code,
                                        fontSize = 11.sp,
                                        color = if (isSelected) primaryAccent else textSecondaryColor
                                    )
                                }
                            }
                        }
                    }

                    // Dose and Interval Steppers
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Dose Stepper Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Dose", fontSize = 11.sp, color = textSecondaryColor)
                                Text(
                                    text = String.format(Locale.US, "%.1f mg", simulatorState.doseMg),
                                    fontSize = 14.sp,
                                    color = primaryAccent
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = { if (simulatorState.doseMg > 0.5) onDoseChanged(simulatorState.doseMg - 0.5) },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .border(1.dp, cardBorder, RoundedCornerShape(4.dp))
                                        .testTag("primary_dose_minus_button")
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease Dose", tint = textPrimaryColor, modifier = Modifier.size(16.dp))
                                }
                                Surface(
                                    color = cardVariantBg,
                                    shape = RoundedCornerShape(4.dp),
                                    border = BorderStroke(1.dp, cardBorder),
                                    modifier = Modifier.widthIn(min = 68.dp)
                                ) {
                                    Text(
                                        text = String.format(Locale.US, "%.1f mg", simulatorState.doseMg),
                                        fontSize = 12.sp,
                                        color = primaryAccent,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onDoseChanged(simulatorState.doseMg + 0.5) },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .border(1.dp, cardBorder, RoundedCornerShape(4.dp))
                                        .testTag("primary_dose_plus_button")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase Dose", tint = textPrimaryColor, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        // Interval Stepper Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Interval", fontSize = 11.sp, color = textSecondaryColor)
                                Text(
                                    text = String.format(Locale.US, "Every %.0f days", simulatorState.intervalDays),
                                    fontSize = 14.sp,
                                    color = primaryAccent
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = { if (simulatorState.intervalDays > 1.0) onIntervalChanged(simulatorState.intervalDays - 1.0) },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .border(1.dp, cardBorder, RoundedCornerShape(4.dp))
                                        .testTag("primary_interval_minus_button")
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease Interval", tint = textPrimaryColor, modifier = Modifier.size(16.dp))
                                }
                                Surface(
                                    color = cardVariantBg,
                                    shape = RoundedCornerShape(4.dp),
                                    border = BorderStroke(1.dp, cardBorder),
                                    modifier = Modifier.widthIn(min = 68.dp)
                                ) {
                                    Text(
                                        text = String.format(Locale.US, "%.0f d", simulatorState.intervalDays),
                                        fontSize = 12.sp,
                                        color = primaryAccent,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onIntervalChanged(simulatorState.intervalDays + 1.0) },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .border(1.dp, cardBorder, RoundedCornerShape(4.dp))
                                        .testTag("primary_interval_plus_button")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase Interval", tint = textPrimaryColor, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    // Apply to my regimen button
                    Button(
                        onClick = {
                            onApplyToRegimen(simulatorState.ester, simulatorState.doseMg, simulatorState.intervalDays)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = cardVariantBg),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, cardBorder),
                        modifier = Modifier.fillMaxWidth().height(32.dp)
                    ) {
                        Text("SET AS ACTIVE REGIMEN", fontSize = 10.sp, color = textPrimaryColor)
                    }
                }
            }
        }

        // Secondary Regimen Comparison Toggle & Controls
        item {
            Surface(
                color = cardBg,
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, if (simulatorState.isComparing) compareAccent else cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.CompareArrows, contentDescription = "Compare", tint = compareAccent, modifier = Modifier.size(16.dp))
                            Text(
                                text = "COMPARE WITH SECOND REGIMEN",
                                fontSize = 10.sp,
                                color = if (simulatorState.isComparing) compareAccent else textMutedColor
                            )
                        }

                        Switch(
                            checked = simulatorState.isComparing,
                            onCheckedChange = { onComparisonToggled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = compareAccent,
                                checkedTrackColor = if (isOled) Color(0xFF2C1030) else MaterialTheme.colorScheme.tertiaryContainer,
                                uncheckedThumbColor = textSecondaryColor,
                                uncheckedTrackColor = cardVariantBg
                            ),
                            modifier = Modifier.height(24.dp)
                        )
                    }

                    if (simulatorState.isComparing) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Compare ester chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                EstradiolEster.entries.forEach { ester ->
                                    val isSelected = ester == simulatorState.compareEster
                                    Surface(
                                        color = if (isSelected) {
                                            if (isOled) Color(0xFF2A102E) else MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
                                        } else cardVariantBg,
                                        shape = RoundedCornerShape(3.dp),
                                        border = BorderStroke(1.dp, if (isSelected) compareAccent else cardBorder),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { onCompareEsterChanged(ester) }
                                    ) {
                                        Text(
                                            text = ester.code,
                                            fontSize = 10.sp,
                                            color = if (isSelected) compareAccent else textSecondaryColor,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            // Steppers for compare
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Compare Dose Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Compare Dose", fontSize = 10.sp, color = textSecondaryColor)
                                        Text(
                                            text = String.format(Locale.US, "%.1f mg", simulatorState.compareDoseMg),
                                            fontSize = 13.sp,
                                            color = compareAccent
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        IconButton(
                                            onClick = { if (simulatorState.compareDoseMg > 0.5) onCompareDoseChanged(simulatorState.compareDoseMg - 0.5) },
                                            modifier = Modifier
                                                .size(34.dp)
                                                .border(1.dp, cardBorder, RoundedCornerShape(4.dp))
                                                .testTag("compare_dose_minus_button")
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "Decrease Compare Dose", tint = textPrimaryColor, modifier = Modifier.size(15.dp))
                                        }
                                        Surface(
                                            color = cardVariantBg,
                                            shape = RoundedCornerShape(4.dp),
                                            border = BorderStroke(1.dp, cardBorder),
                                            modifier = Modifier.widthIn(min = 64.dp)
                                        ) {
                                            Text(
                                                text = String.format(Locale.US, "%.1f mg", simulatorState.compareDoseMg),
                                                fontSize = 12.sp,
                                                color = compareAccent,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { onCompareDoseChanged(simulatorState.compareDoseMg + 0.5) },
                                            modifier = Modifier
                                                .size(34.dp)
                                                .border(1.dp, cardBorder, RoundedCornerShape(4.dp))
                                                .testTag("compare_dose_plus_button")
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Increase Compare Dose", tint = textPrimaryColor, modifier = Modifier.size(15.dp))
                                        }
                                    }
                                }

                                // Compare Interval Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Compare Interval", fontSize = 10.sp, color = textSecondaryColor)
                                        Text(
                                            text = String.format(Locale.US, "Every %.0f days", simulatorState.compareIntervalDays),
                                            fontSize = 13.sp,
                                            color = compareAccent
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        IconButton(
                                            onClick = { if (simulatorState.compareIntervalDays > 1.0) onCompareIntervalChanged(simulatorState.compareIntervalDays - 1.0) },
                                            modifier = Modifier
                                                .size(34.dp)
                                                .border(1.dp, cardBorder, RoundedCornerShape(4.dp))
                                                .testTag("compare_interval_minus_button")
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "Decrease Compare Interval", tint = textPrimaryColor, modifier = Modifier.size(15.dp))
                                        }
                                        Surface(
                                            color = cardVariantBg,
                                            shape = RoundedCornerShape(4.dp),
                                            border = BorderStroke(1.dp, cardBorder),
                                            modifier = Modifier.widthIn(min = 64.dp)
                                        ) {
                                            Text(
                                                text = String.format(Locale.US, "%.0f d", simulatorState.compareIntervalDays),
                                                fontSize = 12.sp,
                                                color = compareAccent,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { onCompareIntervalChanged(simulatorState.compareIntervalDays + 1.0) },
                                            modifier = Modifier
                                                .size(34.dp)
                                                .border(1.dp, cardBorder, RoundedCornerShape(4.dp))
                                                .testTag("compare_interval_plus_button")
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Increase Compare Interval", tint = textPrimaryColor, modifier = Modifier.size(15.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Precision Simulator Chart
        item {
            Surface(
                color = cardBg,
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "SIMULATION CURVE // ${simulatorState.doseMg}mg ${simulatorState.ester.code} every ${simulatorState.intervalDays.toInt()}d",
                        fontSize = 11.sp,
                        color = textPrimaryColor,
                        letterSpacing = 0.5.sp
                    )

                    TechnicalHormoneChart(
                        dataPoints = simulatorState.points,
                        targetE2Min = settings.targetE2Min,
                        targetE2Max = settings.targetE2Max,
                        e2Unit = settings.e2Unit,
                        tUnit = settings.tUnit,
                        isOled = isOled,
                        showNowMarker = false,
                        showSecondaryComparePoints = if (simulatorState.isComparing) simulatorState.comparePoints else null
                    )
                }
            }
        }

        // Comprehensive PK Metrics Table
        item {
            val metrics = simulatorState.metrics
            val tSuppression = if (metrics.troughTestosteroneNgDl < 50.0) {
                StatusSuppressed
            } else if (metrics.troughTestosteroneNgDl <= 100.0) {
                StatusBorderline
            } else {
                StatusElevated
            }

            Surface(
                color = cardBg,
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PHARMACOKINETIC METRICS",
                            fontSize = 11.sp,
                            color = textMutedColor,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = if (metrics.troughTestosteroneNgDl < 50.0) "TROUGH FULLY SUPPRESSED" else "TROUGH REBOUND RISK",
                            fontSize = 10.sp,
                            color = tSuppression
                        )
                    }

                    // Row 1: Peak & Trough E2
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MetricCell(
                            label = "Peak (Cmax)",
                            value = String.format(Locale.US, "%.0f", settings.e2Unit.fromPgMl(metrics.peakPgMl)),
                            unit = settings.e2Unit.label,
                            caption = String.format(Locale.US, "at Day %.1f", metrics.peakDayInCycle),
                            valueColor = primaryAccent,
                            modifier = Modifier.weight(1f)
                        )

                        MetricCell(
                            label = "Trough (Cmin)",
                            value = String.format(Locale.US, "%.0f", settings.e2Unit.fromPgMl(metrics.troughPgMl)),
                            unit = settings.e2Unit.label,
                            caption = String.format(Locale.US, "at Day %.0f", simulatorState.intervalDays),
                            valueColor = if (metrics.troughPgMl >= settings.targetE2Min) StatusSuppressed else StatusBorderline,
                            modifier = Modifier.weight(1f)
                        )

                        MetricCell(
                            label = "Average (Cavg)",
                            value = String.format(Locale.US, "%.0f", settings.e2Unit.fromPgMl(metrics.averagePgMl)),
                            unit = settings.e2Unit.label,
                            caption = "Integrated mean",
                            valueColor = textPrimaryColor,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Row 2: Testosterone suppression & Fluctuation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MetricCell(
                            label = "Trough T",
                            value = String.format(Locale.US, if (settings.tUnit == TUnit.NMOL_L) "%.2f" else "%.1f", settings.tUnit.fromNgDl(metrics.troughTestosteroneNgDl)),
                            unit = settings.tUnit.label,
                            caption = if (metrics.troughTestosteroneNgDl < 50.0) "Target achieved (<50)" else "Sub-target (>50)",
                            valueColor = tSuppression,
                            modifier = Modifier.weight(1f)
                        )

                        MetricCell(
                            label = "Fluctuation",
                            value = String.format(Locale.US, "%.0f%%", metrics.fluctuationRatioPercent),
                            caption = "(Peak - Trough)/Avg",
                            valueColor = if (metrics.fluctuationRatioPercent < 150.0) StatusSuppressed else textSecondaryColor,
                            modifier = Modifier.weight(1f)
                        )

                        MetricCell(
                            label = "T90% Steady State",
                            value = String.format(Locale.US, "%.0f days", metrics.daysTo90PercentSteadyState),
                            caption = "3.32 x half-life",
                            valueColor = textPrimaryColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
