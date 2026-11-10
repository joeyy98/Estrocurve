package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.example.data.model.InjectionEntity
import com.example.data.model.RegimenProfile
import com.example.data.model.TSuppressionStatus
import com.example.data.model.TUnit
import com.example.data.model.UserSettings
import com.example.ui.DashboardState
import com.example.ui.components.MetricCell
import com.example.ui.components.TechnicalHormoneChart
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EstradiolPink
import com.example.ui.theme.StatusBorderline
import com.example.ui.theme.StatusElevated
import com.example.ui.theme.StatusSuppressed
import com.example.ui.theme.TestosteroneCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    dashboardState: DashboardState,
    regimen: RegimenProfile,
    settings: UserSettings,
    onOpenLogDialog: () -> Unit,
    onEditInjection: (InjectionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val isOled = settings.themeMode == AppThemeMode.OLED_BLACK
    val cardBg = if (isOled) DarkSurface else MaterialTheme.colorScheme.surface
    val cardVariantBg = if (isOled) DarkSurfaceVariant else MaterialTheme.colorScheme.surfaceVariant
    val cardBorder = if (isOled) DarkBorder else MaterialTheme.colorScheme.outlineVariant
    val e2Accent = if (isOled) EstradiolPink else MaterialTheme.colorScheme.primary
    val tAccent = if (isOled) TestosteroneCyan else MaterialTheme.colorScheme.secondary
    val textPrimaryColor = if (isOled) TextPrimary else MaterialTheme.colorScheme.onSurface
    val textSecondaryColor = if (isOled) TextSecondary else MaterialTheme.colorScheme.onSurfaceVariant
    val textMutedColor = if (isOled) TextMuted else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)

    val statusColor = when (dashboardState.suppressionStatus) {
        TSuppressionStatus.SUPPRESSED -> StatusSuppressed
        TSuppressionStatus.BORDERLINE -> StatusBorderline
        TSuppressionStatus.ELEVATED -> StatusElevated
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 12.dp)
            .testTag("dashboard_screen"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // App title & top status bar
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ESTROCURVE",
                        fontSize = 11.sp,
                        color = e2Accent,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Live Pharmacokinetic Estimate",
                        fontSize = 10.sp,
                        color = textMutedColor
                    )
                }

                Surface(
                    color = cardVariantBg,
                    shape = RoundedCornerShape(3.dp),
                    border = BorderStroke(1.dp, if (dashboardState.totalInjectionsCount > 0) statusColor else cardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(if (dashboardState.totalInjectionsCount > 0) statusColor else textMutedColor, CircleShape)
                        )
                        Text(
                            text = if (dashboardState.totalInjectionsCount > 0) dashboardState.suppressionStatus.title else "NO DOSES RECORDED",
                            fontSize = 9.sp,
                            color = if (dashboardState.totalInjectionsCount > 0) statusColor else textMutedColor
                        )
                    }
                }
            }
        }

        // Live E2 and T Dual Big Metric Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Estradiol Card
                Surface(
                    color = cardBg,
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, cardBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "ESTRADIOL (E2)",
                            fontSize = 10.sp,
                            color = textMutedColor,
                            letterSpacing = 0.5.sp
                        )
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (dashboardState.totalInjectionsCount > 0) {
                                    String.format(Locale.US, "%.1f", settings.e2Unit.fromPgMl(dashboardState.currentE2PgMl))
                                } else "--",
                                fontSize = 24.sp,
                                color = if (dashboardState.totalInjectionsCount > 0) e2Accent else textMutedColor
                            )
                            Text(
                                text = settings.e2Unit.label,
                                fontSize = 11.sp,
                                color = textSecondaryColor,
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        }
                        Text(
                            text = if (dashboardState.totalInjectionsCount > 0) {
                                "Target: ${settings.e2Unit.formatInteger(settings.targetE2Min)} - ${settings.e2Unit.formatInteger(settings.targetE2Max)}"
                            } else "Awaiting first dose log",
                            fontSize = 9.sp,
                            color = if (dashboardState.totalInjectionsCount > 0 && dashboardState.currentE2PgMl >= settings.targetE2Min) StatusSuppressed else textMutedColor
                        )
                    }
                }

                // Testosterone Card
                Surface(
                    color = cardBg,
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, cardBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "TESTOSTERONE (T)",
                            fontSize = 10.sp,
                            color = textMutedColor,
                            letterSpacing = 0.5.sp
                        )
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (dashboardState.totalInjectionsCount > 0) {
                                    String.format(Locale.US, if (settings.tUnit == TUnit.NMOL_L) "%.2f" else "%.1f", settings.tUnit.fromNgDl(dashboardState.currentTestosteroneNgDl))
                                } else "--",
                                fontSize = 24.sp,
                                color = if (dashboardState.totalInjectionsCount > 0) tAccent else textMutedColor
                            )
                            Text(
                                text = settings.tUnit.label,
                                fontSize = 11.sp,
                                color = textSecondaryColor,
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        }
                        Text(
                            text = if (dashboardState.totalInjectionsCount > 0) {
                                "Monotherapy target: < ${settings.tUnit.formatInteger(50.0)}"
                            } else "Baseline unsuppressed",
                            fontSize = 9.sp,
                            color = if (dashboardState.totalInjectionsCount > 0 && dashboardState.currentTestosteroneNgDl < 50.0) StatusSuppressed else textMutedColor
                        )
                    }
                }
            }
        }

        // Secondary Metrics Row: Hours since dose, Next Dose, Suggested Site
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val hoursSinceStr = if (dashboardState.hoursSinceLastDose != null) {
                    String.format(Locale.US, "%.1fh (%.1fd)", dashboardState.hoursSinceLastDose, dashboardState.hoursSinceLastDose / 24.0)
                } else "None logged"

                val nextDoseStr = if (dashboardState.hoursToNextDose != null) {
                    if (dashboardState.hoursToNextDose > 0) {
                        String.format(Locale.US, "in %.1fd", dashboardState.hoursToNextDose / 24.0)
                    } else "DUE NOW"
                } else "Not scheduled"

                MetricCell(
                    label = "Last Dose",
                    value = hoursSinceStr,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )

                MetricCell(
                    label = "Next Dose",
                    value = nextDoseStr,
                    valueColor = if ((dashboardState.hoursToNextDose ?: 10.0) <= 0.0) StatusElevated else textPrimaryColor,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )

                MetricCell(
                    label = "Next Site",
                    value = dashboardState.nextSuggestedSite.shortCode,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
        }

        // Action Buttons Row: Quick Log & Regimen Info
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = onOpenLogDialog,
                    colors = ButtonDefaults.buttonColors(containerColor = e2Accent),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("dashboard_log_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Log Dose", tint = if (isOled) Color.Black else MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("LOG INJECTION", color = if (isOled) Color.Black else MaterialTheme.colorScheme.onPrimary, fontSize = 11.sp)
                }

                Surface(
                    color = cardBg,
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, cardBorder),
                    modifier = Modifier.weight(1.2f).height(38.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "REGIMEN: ${regimen.doseMg}mg ${regimen.ester.code} / ${regimen.intervalDays.toInt()}d",
                            fontSize = 11.sp,
                            color = textSecondaryColor
                        )
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Active Reminder",
                            tint = if (settings.reminderEnabled) StatusSuppressed else textMutedColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        // Continuous Timeline Chart (Past 14d -> Future 7d)
        item {
            Surface(
                color = cardBg,
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "HORMONE DYNAMICS // PAST 14D -> NEXT 7D",
                            fontSize = 11.sp,
                            color = textPrimaryColor,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Touch to inspect",
                            fontSize = 9.sp,
                            color = textMutedColor
                        )
                    }

                    TechnicalHormoneChart(
                        dataPoints = dashboardState.timelinePoints,
                        targetE2Min = settings.targetE2Min,
                        targetE2Max = settings.targetE2Max,
                        e2Unit = settings.e2Unit,
                        tUnit = settings.tUnit,
                        isOled = isOled,
                        showNowMarker = true
                    )
                }
            }
        }

        // Recent Injections Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RECENT DOSES (${dashboardState.totalInjectionsCount})",
                    fontSize = 11.sp,
                    color = textSecondaryColor,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Tap to edit",
                    fontSize = 10.sp,
                    color = textMutedColor
                )
            }
        }

        // Recent 3 injections
        if (dashboardState.latestInjection != null) {
            item {
                InjectionItemRow(
                    injection = dashboardState.latestInjection,
                    isOled = isOled,
                    onClick = { onEditInjection(dashboardState.latestInjection) }
                )
            }
        } else {
            item {
                Surface(
                    color = cardBg,
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No injections recorded yet.", fontSize = 12.sp, color = textMutedColor)
                        Text("Tap 'LOG INJECTION' to record your first dose.", fontSize = 11.sp, color = textSecondaryColor)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
fun InjectionItemRow(
    injection: InjectionEntity,
    isOled: Boolean = true,
    onClick: () -> Unit
) {
    val cardBg = if (isOled) DarkSurface else MaterialTheme.colorScheme.surface
    val cardBorder = if (isOled) DarkBorder else MaterialTheme.colorScheme.outlineVariant
    val e2Accent = if (isOled) EstradiolPink else MaterialTheme.colorScheme.primary
    val textPrimaryColor = if (isOled) TextPrimary else MaterialTheme.colorScheme.onSurface
    val textSecondaryColor = if (isOled) TextSecondary else MaterialTheme.colorScheme.onSurfaceVariant
    val textMutedColor = if (isOled) TextMuted else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    val esterPillBg = if (isOled) Color(0xFF240E1B) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)

    Surface(
        color = cardBg,
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, cardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = esterPillBg,
                    shape = RoundedCornerShape(3.dp),
                    border = BorderStroke(1.dp, e2Accent)
                ) {
                    Text(
                        text = injection.ester.code,
                        fontSize = 11.sp,
                        color = e2Accent,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Column {
                    Text(
                        text = "${injection.doseMg} mg // ${injection.site.displayName}",
                        fontSize = 12.sp,
                        color = textPrimaryColor
                    )
                    Text(
                        text = SimpleDateFormat("EEE, MMM d, yyyy HH:mm", Locale.US).format(Date(injection.timestamp)),
                        fontSize = 10.sp,
                        color = textSecondaryColor
                    )
                    if (injection.notes.isNotEmpty()) {
                        Text(
                            text = injection.notes,
                            fontSize = 10.sp,
                            color = textMutedColor,
                            maxLines = 1
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit Dose",
                tint = textMutedColor,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
