package com.example.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.AppThemeMode
import com.example.data.model.EstradiolEster
import com.example.data.model.InjectionEntity
import com.example.data.model.InjectionSite
import com.example.data.model.UserSettings
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EstradiolPink
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun QuickLogDialog(
    initialEster: EstradiolEster = EstradiolEster.EV,
    initialDoseMg: Double = 4.0,
    initialSite: InjectionSite = InjectionSite.LEFT_THIGH,
    enabledSites: Set<InjectionSite> = InjectionSite.entries.toSet(),
    editingInjection: InjectionEntity? = null,
    settings: UserSettings = UserSettings(),
    onDismiss: () -> Unit,
    onSave: (timestamp: Long, ester: EstradiolEster, doseMg: Double, site: InjectionSite, notes: String) -> Unit
) {
    val context = LocalContext.current
    val isOled = settings.themeMode == AppThemeMode.OLED_BLACK
    val cardBg = if (isOled) DarkSurface else MaterialTheme.colorScheme.surface
    val cardVariantBg = if (isOled) DarkSurfaceVariant else MaterialTheme.colorScheme.surfaceVariant
    val cardBorder = if (isOled) DarkBorder else MaterialTheme.colorScheme.outlineVariant
    val e2Accent = if (isOled) EstradiolPink else MaterialTheme.colorScheme.primary
    val textPrimaryColor = if (isOled) TextPrimary else MaterialTheme.colorScheme.onSurface
    val textSecondaryColor = if (isOled) TextSecondary else MaterialTheme.colorScheme.onSurfaceVariant
    val textMutedColor = if (isOled) TextMuted else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)

    var showAllSites by remember { mutableStateOf(false) }
    var selectedEster by remember { mutableStateOf(editingInjection?.ester ?: initialEster) }
    var doseMg by remember { mutableDoubleStateOf(editingInjection?.doseMg ?: initialDoseMg) }
    var selectedSite by remember { mutableStateOf(editingInjection?.site ?: initialSite) }
    var notes by remember { mutableStateOf(editingInjection?.notes ?: "") }
    var timestamp by remember { mutableStateOf(editingInjection?.timestamp ?: System.currentTimeMillis()) }

    fun showDateTimePicker() {
        val currentCal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val datePicker = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val timePicker = TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        val newCal = Calendar.getInstance().apply {
                            set(Calendar.YEAR, year)
                            set(Calendar.MONTH, month)
                            set(Calendar.DAY_OF_MONTH, dayOfMonth)
                            set(Calendar.HOUR_OF_DAY, hourOfDay)
                            set(Calendar.MINUTE, minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        timestamp = newCal.timeInMillis
                    },
                    currentCal.get(Calendar.HOUR_OF_DAY),
                    currentCal.get(Calendar.MINUTE),
                    false
                )
                timePicker.show()
            },
            currentCal.get(Calendar.YEAR),
            currentCal.get(Calendar.MONTH),
            currentCal.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = cardBg,
            border = BorderStroke(1.dp, cardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .testTag("quick_log_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (editingInjection != null) "EDIT INJECTION LOG" else "LOG HRT INJECTION",
                    fontSize = 14.sp,
                    color = textPrimaryColor,
                    letterSpacing = 0.5.sp
                )

                // Date & Time Picker Section (Supports past doses)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Injection Date & Time", fontSize = 11.sp, color = textSecondaryColor)
                    Surface(
                        color = cardVariantBg,
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, e2Accent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDateTimePicker() }
                            .testTag("date_time_picker_button")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = "Pick Date",
                                    tint = e2Accent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Column {
                                    Text(
                                        text = SimpleDateFormat("EEE, MMM d, yyyy", Locale.US).format(Date(timestamp)),
                                        fontSize = 13.sp,
                                        color = textPrimaryColor
                                    )
                                    Text(
                                        text = SimpleDateFormat("hh:mm a (24h: HH:mm)", Locale.US).format(Date(timestamp)),
                                        fontSize = 11.sp,
                                        color = textSecondaryColor
                                    )
                                }
                            }

                            Surface(
                                color = if (isOled) Color(0xFF2C101F) else e2Accent.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(3.dp),
                                border = BorderStroke(1.dp, e2Accent)
                            ) {
                                Text(
                                    text = "CHANGE",
                                    fontSize = 10.sp,
                                    color = e2Accent,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    // Quick past-dose selector chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            "NOW" to 0,
                            "-1d" to 1,
                            "-2d" to 2,
                            "-3d" to 3,
                            "-5d" to 5,
                            "-7d" to 7
                        ).forEach { (label, daysAgo) ->
                            Surface(
                                color = cardVariantBg,
                                shape = RoundedCornerShape(3.dp),
                                border = BorderStroke(1.dp, cardBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        val cal = Calendar.getInstance().apply {
                                            timeInMillis = System.currentTimeMillis()
                                            add(Calendar.DAY_OF_YEAR, -daysAgo)
                                        }
                                        timestamp = cal.timeInMillis
                                    }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    color = textSecondaryColor,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Ester selector buttons
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Estradiol Ester", fontSize = 11.sp, color = textSecondaryColor)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        EstradiolEster.entries.forEach { ester ->
                            val isSelected = ester == selectedEster
                            BoxSelectorItem(
                                title = ester.code,
                                isSelected = isSelected,
                                isOled = isOled,
                                activeBg = if (isOled) Color(0xFF2E1020) else e2Accent.copy(alpha = 0.15f),
                                inactiveBg = cardVariantBg,
                                activeBorder = e2Accent,
                                inactiveBorder = cardBorder,
                                activeText = e2Accent,
                                inactiveText = textSecondaryColor,
                                modifier = Modifier.weight(1f),
                                onClick = { selectedEster = ester }
                            )
                        }
                    }
                    Text(
                        text = selectedEster.displayName,
                        fontSize = 11.sp,
                        color = e2Accent
                    )
                }

                // Dose Stepper
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Dose (mg)", fontSize = 11.sp, color = textSecondaryColor)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = { if (doseMg > 0.5) doseMg -= 0.5 },
                            modifier = Modifier
                                .border(1.dp, cardBorder, RoundedCornerShape(4.dp))
                                .testTag("dose_minus_button")
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease Dose", tint = textPrimaryColor)
                        }

                        Text(
                            text = String.format(Locale.US, "%.1f mg", doseMg),
                            fontSize = 20.sp,
                            color = e2Accent
                        )

                        IconButton(
                            onClick = { doseMg += 0.5 },
                            modifier = Modifier
                                .border(1.dp, cardBorder, RoundedCornerShape(4.dp))
                                .testTag("dose_plus_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase Dose", tint = textPrimaryColor)
                        }
                    }
                }

                // Injection Site selector (filtered by user settings)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Injection Site (Rotation)", fontSize = 11.sp, color = textSecondaryColor)
                        if (!showAllSites && enabledSites.size < InjectionSite.entries.size) {
                            Text(
                                text = "+ Show all sites",
                                fontSize = 10.sp,
                                color = e2Accent,
                                modifier = Modifier
                                    .clickable { showAllSites = true }
                                    .padding(vertical = 2.dp)
                            )
                        }
                    }

                    val activeSites = remember(enabledSites, showAllSites, editingInjection) {
                        if (showAllSites) {
                            InjectionSite.entries
                        } else {
                            val list = InjectionSite.entries.filter { it in enabledSites }
                            if (editingInjection != null && editingInjection.site !in list) {
                                list + editingInjection.site
                            } else {
                                list.ifEmpty { InjectionSite.entries }
                            }
                        }
                    }

                    val rows = activeSites.chunked(if (activeSites.size <= 4) 2 else 3)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        rows.forEach { rowSites ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                rowSites.forEach { site ->
                                    BoxSelectorItem(
                                        title = site.shortCode,
                                        isSelected = site == selectedSite,
                                        isOled = isOled,
                                        activeBg = if (isOled) Color(0xFF2E1020) else e2Accent.copy(alpha = 0.15f),
                                        inactiveBg = cardVariantBg,
                                        activeBorder = e2Accent,
                                        inactiveBorder = cardBorder,
                                        activeText = e2Accent,
                                        inactiveText = textSecondaryColor,
                                        modifier = Modifier.weight(1f),
                                        onClick = { selectedSite = site }
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = selectedSite.displayName,
                        fontSize = 11.sp,
                        color = textSecondaryColor
                    )
                }

                // Notes input
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (needle gauge, site reaction, etc.)", fontSize = 11.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = cardVariantBg,
                        unfocusedContainerColor = cardVariantBg,
                        focusedBorderColor = e2Accent,
                        unfocusedBorderColor = cardBorder,
                        focusedTextColor = textPrimaryColor,
                        unfocusedTextColor = textPrimaryColor,
                        focusedLabelColor = e2Accent,
                        unfocusedLabelColor = textSecondaryColor
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.dp, cardBorder),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("CANCEL", color = textSecondaryColor, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            onSave(timestamp, selectedEster, doseMg, selectedSite, notes)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = e2Accent,
                            contentColor = if (isOled) Color.Black else MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_injection_button")
                    ) {
                        Text(
                            text = "SAVE LOG",
                            color = if (isOled) Color.Black else MaterialTheme.colorScheme.onPrimary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxSelectorItem(
    title: String,
    isSelected: Boolean,
    isOled: Boolean,
    activeBg: Color,
    inactiveBg: Color,
    activeBorder: Color,
    inactiveBorder: Color,
    activeText: Color,
    inactiveText: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) activeBg else inactiveBg,
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, if (isSelected) activeBorder else inactiveBorder),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                color = if (isSelected) activeText else inactiveText
            )
        }
    }
}
