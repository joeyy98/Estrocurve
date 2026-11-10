package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppThemeMode
import com.example.data.model.E2Unit
import com.example.data.model.EstradiolEster
import com.example.data.model.InjectionSite
import com.example.data.model.RegimenProfile
import com.example.data.model.TUnit
import com.example.data.model.TargetRangePreset
import com.example.data.model.UserSettings
import com.example.reminder.NotificationHelper
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EstradiolPink
import com.example.ui.theme.StatusElevated
import com.example.ui.theme.StatusSuppressed
import com.example.ui.theme.TestosteroneCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun SettingsScreen(
    settings: UserSettings,
    regimen: RegimenProfile,
    onSettingsChanged: (UserSettings) -> Unit,
    onRegimenChanged: (RegimenProfile) -> Unit,
    onOpenExportImportDialog: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onClearAllLogs: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var customMinE2 by remember(settings.targetE2Min) { mutableDoubleStateOf(settings.targetE2Min) }
    var calibrationFactor by remember(settings.calibrationFactor) { mutableDoubleStateOf(settings.calibrationFactor) }
    var showClearConfirmation by remember { mutableStateOf(false) }

    val isOled = settings.themeMode == AppThemeMode.OLED_BLACK
    val screenBg = if (isOled) Color.Black else MaterialTheme.colorScheme.background
    val cardBg = if (isOled) DarkSurface else MaterialTheme.colorScheme.surface
    val cardVariantBg = if (isOled) DarkSurfaceVariant else MaterialTheme.colorScheme.surfaceVariant
    val cardBorder = if (isOled) DarkBorder else MaterialTheme.colorScheme.outlineVariant
    val primaryAccent = if (isOled) EstradiolPink else MaterialTheme.colorScheme.primary
    val secondaryAccent = if (isOled) TestosteroneCyan else MaterialTheme.colorScheme.secondary
    val textPrimaryColor = if (isOled) TextPrimary else MaterialTheme.colorScheme.onSurface
    val textSecondaryColor = if (isOled) TextSecondary else MaterialTheme.colorScheme.onSurfaceVariant
    val textMutedColor = if (isOled) TextMuted else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(screenBg)
            .padding(horizontal = 12.dp)
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "SETTINGS & CLINICAL PREFERENCES",
                fontSize = 11.sp,
                color = primaryAccent,
                letterSpacing = 0.5.sp
            )
            Text(
                text = "Theme, Units, Calibration, Target Ranges, Reminders & Privacy",
                fontSize = 10.sp,
                color = textMutedColor
            )
        }

        // App Theme & Appearance Card
        item {
            Surface(
                color = cardBg,
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth().testTag("app_theme_card")
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Palette, contentDescription = "Theme", tint = primaryAccent, modifier = Modifier.size(16.dp))
                            Text("APP THEME & APPEARANCE", fontSize = 10.sp, color = textMutedColor)
                        }
                        Text(
                            text = settings.themeMode.title,
                            fontSize = 10.sp,
                            color = primaryAccent
                        )
                    }

                    Text(
                        text = "Choose between the default high-contrast OLED black theme or dynamic color matching your device wallpaper and system light/dark mode.",
                        fontSize = 10.sp,
                        color = textSecondaryColor
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        AppThemeMode.entries.forEach { mode ->
                            val isSelected = settings.themeMode == mode
                            Surface(
                                color = if (isSelected) {
                                    if (isOled) Color(0xFF2C101F) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                } else cardVariantBg,
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(1.dp, if (isSelected) primaryAccent else cardBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSettingsChanged(settings.copy(themeMode = mode))
                                    }
                                    .testTag("theme_mode_${mode.name.lowercase()}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { onSettingsChanged(settings.copy(themeMode = mode)) },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = primaryAccent,
                                            unselectedColor = textMutedColor
                                        ),
                                        modifier = Modifier.size(20.dp)
                                    )

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = mode.title,
                                            fontSize = 12.sp,
                                            color = if (isSelected) textPrimaryColor else textSecondaryColor
                                        )
                                        Text(
                                            text = mode.description,
                                            fontSize = 9.5.sp,
                                            color = textMutedColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Units of Measurement Card
        item {
            Surface(
                color = cardBg,
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("UNITS OF MEASUREMENT", fontSize = 10.sp, color = textMutedColor)

                    // E2 Unit Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Estradiol Unit", fontSize = 12.sp, color = textPrimaryColor)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            E2Unit.entries.forEach { u ->
                                val selected = settings.e2Unit == u
                                Surface(
                                    color = if (selected) (if (isOled) Color(0xFF2C101F) else primaryAccent.copy(alpha = 0.15f)) else cardVariantBg,
                                    shape = RoundedCornerShape(3.dp),
                                    border = BorderStroke(1.dp, if (selected) primaryAccent else cardBorder),
                                    modifier = Modifier.clickable {
                                        onSettingsChanged(settings.copy(e2Unit = u))
                                    }
                                ) {
                                    Text(
                                        text = u.label,
                                        fontSize = 11.sp,
                                        color = if (selected) primaryAccent else textSecondaryColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // T Unit Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Testosterone Unit", fontSize = 12.sp, color = textPrimaryColor)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TUnit.entries.forEach { u ->
                                val selected = settings.tUnit == u
                                Surface(
                                    color = if (selected) (if (isOled) Color(0xFF0D252E) else secondaryAccent.copy(alpha = 0.15f)) else cardVariantBg,
                                    shape = RoundedCornerShape(3.dp),
                                    border = BorderStroke(1.dp, if (selected) secondaryAccent else cardBorder),
                                    modifier = Modifier.clickable {
                                        onSettingsChanged(settings.copy(tUnit = u))
                                    }
                                ) {
                                    Text(
                                        text = u.label,
                                        fontSize = 11.sp,
                                        color = if (selected) secondaryAccent else textSecondaryColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Injection Sites & Rotation Preferences Card
        item {
            Surface(
                color = cardBg,
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth().testTag("injection_sites_rotation_card")
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("INJECTION SITES & ROTATION", fontSize = 10.sp, color = textMutedColor)
                        Text(
                            text = "${settings.enabledSites.size} of ${InjectionSite.entries.size} active",
                            fontSize = 10.sp,
                            color = primaryAccent
                        )
                    }

                    Text(
                        text = "Choose which sites to cycle through in the tracker and display in the log dialog. Keeps tissue healthy by avoiding scar tissue build-up.",
                        fontSize = 10.sp,
                        color = textSecondaryColor
                    )

                    // Presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            "ALL" to InjectionSite.entries.toSet(),
                            "THIGHS" to setOf(InjectionSite.LEFT_THIGH, InjectionSite.RIGHT_THIGH),
                            "GLUTES" to setOf(InjectionSite.LEFT_GLUTE, InjectionSite.RIGHT_GLUTE),
                            "SUBQ" to InjectionSite.entries.filter { it.isSubQFriendly }.toSet()
                        ).forEach { (presetLabel, presetSites) ->
                            val isCurrentPreset = settings.enabledSites == presetSites
                            Surface(
                                color = if (isCurrentPreset) (if (isOled) Color(0xFF2C101F) else primaryAccent.copy(alpha = 0.15f)) else cardVariantBg,
                                shape = RoundedCornerShape(3.dp),
                                border = BorderStroke(1.dp, if (isCurrentPreset) primaryAccent else cardBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        onSettingsChanged(settings.copy(enabledSites = presetSites))
                                    }
                            ) {
                                Text(
                                    text = presetLabel,
                                    fontSize = 9.sp,
                                    color = if (isCurrentPreset) primaryAccent else textSecondaryColor,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Individual Site Checkboxes/Toggles
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        InjectionSite.entries.forEach { site ->
                            val isEnabled = site in settings.enabledSites
                            Surface(
                                color = if (isEnabled) (if (isOled) Color(0xFF1E101A) else primaryAccent.copy(alpha = 0.12f)) else cardVariantBg,
                                shape = RoundedCornerShape(3.dp),
                                border = BorderStroke(1.dp, if (isEnabled) (if (isOled) Color(0xFF4A1833) else primaryAccent.copy(alpha = 0.4f)) else cardBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isEnabled) {
                                            if (settings.enabledSites.size > 1) {
                                                onSettingsChanged(settings.copy(enabledSites = settings.enabledSites - site))
                                            } else {
                                                Toast.makeText(context, "At least one injection site must remain active", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            onSettingsChanged(settings.copy(enabledSites = settings.enabledSites + site))
                                        }
                                    }
                                    .testTag("site_toggle_${site.idName.lowercase()}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Checkbox(
                                            checked = isEnabled,
                                            onCheckedChange = { checked ->
                                                if (!checked) {
                                                    if (settings.enabledSites.size > 1) {
                                                        onSettingsChanged(settings.copy(enabledSites = settings.enabledSites - site))
                                                    } else {
                                                        Toast.makeText(context, "At least one injection site must remain active", Toast.LENGTH_SHORT).show()
                                                    }
                                                } else {
                                                    onSettingsChanged(settings.copy(enabledSites = settings.enabledSites + site))
                                                }
                                            },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = primaryAccent,
                                                uncheckedColor = textMutedColor,
                                                checkmarkColor = if (isOled) Color.Black else MaterialTheme.colorScheme.onPrimary
                                            ),
                                            modifier = Modifier.size(20.dp)
                                        )

                                        Column {
                                            Text(
                                                text = site.displayName,
                                                fontSize = 11.sp,
                                                color = if (isEnabled) textPrimaryColor else textMutedColor
                                            )
                                            Text(
                                                text = site.routeLabel,
                                                fontSize = 9.sp,
                                                color = if (isEnabled) textSecondaryColor else textMutedColor
                                            )
                                        }
                                    }

                                    Surface(
                                        color = if (isEnabled) (if (isOled) Color(0xFF2C101F) else primaryAccent.copy(alpha = 0.15f)) else Color.Transparent,
                                        shape = RoundedCornerShape(2.dp),
                                        border = BorderStroke(1.dp, if (isEnabled) primaryAccent else cardBorder)
                                    ) {
                                        Text(
                                            text = site.shortCode,
                                            fontSize = 9.sp,
                                            color = if (isEnabled) primaryAccent else textMutedColor,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Pharmacokinetic Model Calibration Factor
        item {
            Surface(
                color = cardBg,
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, cardBorder),
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
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = "Calibration", tint = primaryAccent, modifier = Modifier.size(16.dp))
                            Text("MODEL CALIBRATION FACTOR", fontSize = 10.sp, color = textMutedColor)
                        }

                        Text(
                            text = String.format(Locale.US, "%.2fx", calibrationFactor),
                            fontSize = 12.sp,
                            color = primaryAccent
                        )
                    }

                    Text(
                        text = if (kotlin.math.abs(calibrationFactor - 1.0) < 0.01) {
                            "1.00x: Standard population benchmark model"
                        } else {
                            String.format(Locale.US, "Scaled by %.0f%% to match personal blood test results", (calibrationFactor - 1.0) * 100)
                        },
                        fontSize = 10.sp,
                        color = textSecondaryColor
                    )

                    Slider(
                        value = calibrationFactor.toFloat(),
                        onValueChange = { calibrationFactor = it.toDouble() },
                        onValueChangeFinished = {
                            onSettingsChanged(settings.copy(calibrationFactor = calibrationFactor))
                            Toast.makeText(context, String.format(Locale.US, "Model calibrated to %.2fx", calibrationFactor), Toast.LENGTH_SHORT).show()
                        },
                        valueRange = 0.60f..1.40f,
                        steps = 15,
                        colors = SliderDefaults.colors(
                            thumbColor = primaryAccent,
                            activeTrackColor = primaryAccent,
                            inactiveTrackColor = cardBorder
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("0.60x (Faster clearance)", fontSize = 9.sp, color = textMutedColor)
                        Text("1.00x (Standard)", fontSize = 9.sp, color = textMutedColor)
                        Text("1.40x (Higher retention)", fontSize = 9.sp, color = textMutedColor)
                    }
                }
            }
        }

        // Clinical Target Range Presets Card
        item {
            Surface(
                color = cardBg,
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ESTRADIOL TARGET RANGE", fontSize = 10.sp, color = textMutedColor)

                    TargetRangePreset.PRESETS.forEach { preset ->
                        val isCurrent = settings.targetE2Min == preset.minPgMl && settings.targetE2Max == preset.maxPgMl
                        Surface(
                            color = if (isCurrent) (if (isOled) Color(0xFF20121C) else primaryAccent.copy(alpha = 0.15f)) else cardVariantBg,
                            shape = RoundedCornerShape(3.dp),
                            border = BorderStroke(1.dp, if (isCurrent) primaryAccent else cardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSettingsChanged(settings.copy(targetE2Min = preset.minPgMl, targetE2Max = preset.maxPgMl))
                                }
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(preset.name, fontSize = 11.sp, color = if (isCurrent) primaryAccent else textPrimaryColor)
                                    Text(
                                        "${settings.e2Unit.formatInteger(preset.minPgMl)} - ${settings.e2Unit.formatInteger(preset.maxPgMl)}",
                                        fontSize = 11.sp,
                                        color = if (isCurrent) primaryAccent else textSecondaryColor
                                    )
                                }
                                Text(preset.description, fontSize = 9.sp, color = textMutedColor)
                            }
                        }
                    }

                    // Custom range slider
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Custom Minimum: ${settings.e2Unit.formatInteger(customMinE2)}",
                            fontSize = 10.sp,
                            color = textSecondaryColor
                        )
                        Slider(
                            value = customMinE2.toFloat(),
                            onValueChange = { customMinE2 = it.toDouble() },
                            onValueChangeFinished = {
                                onSettingsChanged(settings.copy(targetE2Min = customMinE2))
                            },
                            valueRange = 50f..400f,
                            steps = 34,
                            colors = SliderDefaults.colors(
                                thumbColor = primaryAccent,
                                activeTrackColor = primaryAccent,
                                inactiveTrackColor = cardBorder
                            )
                        )
                    }
                }
            }
        }

        // Push Notifications Card
        item {
            Surface(
                color = cardBg,
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = "Reminders", tint = primaryAccent, modifier = Modifier.size(16.dp))
                            Text("NEXT INJECTION NOTIFICATIONS", fontSize = 10.sp, color = textMutedColor)
                        }

                        Switch(
                            checked = settings.reminderEnabled,
                            onCheckedChange = {
                                onSettingsChanged(settings.copy(reminderEnabled = it))
                                if (it) onRequestNotificationPermission()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = primaryAccent,
                                checkedTrackColor = if (isOled) Color(0xFF381423) else primaryAccent.copy(alpha = 0.3f),
                                uncheckedThumbColor = textMutedColor,
                                uncheckedTrackColor = cardVariantBg
                            )
                        )
                    }

                    if (settings.reminderEnabled) {
                        Text(
                            text = "Alerts are delivered via native Android notifications on the scheduled injection date to maintain monotherapy trough levels.",
                            fontSize = 10.sp,
                            color = textSecondaryColor
                        )

                        // Custom advance notification selector
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Advance Notice", fontSize = 11.sp, color = textPrimaryColor)
                                Text(
                                    text = if (settings.advanceReminderHours == 0) "Exact time (0h before)"
                                           else if (settings.advanceReminderHours == 24) "1 day (24h) before"
                                           else if (settings.advanceReminderHours % 24 == 0) "${settings.advanceReminderHours / 24} days (${settings.advanceReminderHours}h) before"
                                           else "${settings.advanceReminderHours} hours before",
                                    fontSize = 11.sp,
                                    color = primaryAccent
                                )
                            }

                            // Preset chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(0 to "0h", 2 to "2h", 6 to "6h", 12 to "12h", 24 to "24h", 48 to "48h").forEach { (hrs, label) ->
                                    val sel = settings.advanceReminderHours == hrs
                                    Surface(
                                        color = if (sel) (if (isOled) Color(0xFF2C101F) else primaryAccent.copy(alpha = 0.15f)) else cardVariantBg,
                                        shape = RoundedCornerShape(3.dp),
                                        border = BorderStroke(1.dp, if (sel) primaryAccent else cardBorder),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                onSettingsChanged(settings.copy(advanceReminderHours = hrs))
                                            }
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 10.sp,
                                            color = if (sel) primaryAccent else textSecondaryColor,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            // Custom hours stepper
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Custom Hours Before Dose:", fontSize = 10.sp, color = textSecondaryColor)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        onClick = {
                                            if (settings.advanceReminderHours > 0) {
                                                onSettingsChanged(settings.copy(advanceReminderHours = settings.advanceReminderHours - 1))
                                            }
                                        },
                                        color = Color.Transparent,
                                        shape = RoundedCornerShape(4.dp),
                                        border = BorderStroke(1.dp, cardBorder),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.Remove,
                                                contentDescription = "Decrease 1 hour",
                                                tint = textPrimaryColor,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }

                                    Surface(
                                        color = cardVariantBg,
                                        shape = RoundedCornerShape(4.dp),
                                        border = BorderStroke(1.dp, cardBorder),
                                        modifier = Modifier
                                            .widthIn(min = 54.dp)
                                            .height(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "${settings.advanceReminderHours}h",
                                                fontSize = 12.sp,
                                                color = primaryAccent,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(horizontal = 8.dp)
                                            )
                                        }
                                    }

                                    Surface(
                                        onClick = {
                                            if (settings.advanceReminderHours < 168) {
                                                onSettingsChanged(settings.copy(advanceReminderHours = settings.advanceReminderHours + 1))
                                            }
                                        },
                                        color = Color.Transparent,
                                        shape = RoundedCornerShape(4.dp),
                                        border = BorderStroke(1.dp, cardBorder),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.Add,
                                                contentDescription = "Increase 1 hour",
                                                tint = textPrimaryColor,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Send test notification button
                        OutlinedButton(
                            onClick = {
                                NotificationHelper.showReminderNotification(
                                    context = context,
                                    esterName = regimen.ester.displayName,
                                    doseMg = regimen.doseMg
                                )
                                Toast.makeText(context, "Test notification dispatched", Toast.LENGTH_SHORT).show()
                            },
                            border = BorderStroke(1.dp, cardBorder),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.fillMaxWidth().height(32.dp)
                        ) {
                            Text("SEND TEST NOTIFICATION NOW", fontSize = 10.sp, color = textPrimaryColor)
                        }
                    }
                }
            }
        }

        // Data Privacy & JSON Backup Card
        item {
            Surface(
                color = cardBg,
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("PRIVATE ON-DEVICE STORAGE & BACKUP", fontSize = 10.sp, color = textMutedColor)
                    Text(
                        text = "All injection logs are stored 100% locally on your phone in an encrypted SQLite database. You can export a password-protected .estrocurve backup file anytime.",
                        fontSize = 10.sp,
                        color = textSecondaryColor
                    )

                    Button(
                        onClick = onOpenExportImportDialog,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = cardVariantBg,
                            contentColor = textPrimaryColor
                        ),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, cardBorder),
                        modifier = Modifier.fillMaxWidth().height(34.dp)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Backup", tint = textPrimaryColor, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("BACKUP & RESTORE DATA (.ESTROCURVE)", fontSize = 11.sp, color = textPrimaryColor)
                    }

                    OutlinedButton(
                        onClick = { showClearConfirmation = true },
                        border = BorderStroke(1.dp, Color(0xFF551111)),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.fillMaxWidth().height(34.dp)
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Wipe", tint = StatusElevated, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("WIPE ALL LOGS (RESET BLANK SLATE)", fontSize = 11.sp, color = StatusElevated)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text("Reset to Blank Slate?", fontSize = 14.sp, color = textPrimaryColor) },
            text = {
                Text(
                    text = "This will permanently delete all recorded injection logs from your phone's database. This cannot be undone unless you have a JSON backup.",
                    fontSize = 12.sp,
                    color = textSecondaryColor
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllLogs()
                        showClearConfirmation = false
                        Toast.makeText(context, "All logs cleared", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusElevated)
                ) {
                    Text("CLEAR ALL", fontSize = 11.sp, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showClearConfirmation = false },
                    border = BorderStroke(1.dp, cardBorder)
                ) {
                    Text("CANCEL", fontSize = 11.sp, color = textSecondaryColor)
                }
            },
            containerColor = cardBg,
            shape = RoundedCornerShape(4.dp)
        )
    }
}
