package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppThemeMode
import com.example.data.model.InjectionEntity
import com.example.data.model.InjectionSite
import com.example.data.model.UserSettings
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EstradiolPink
import com.example.ui.theme.StatusElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    injections: List<InjectionEntity>,
    settings: UserSettings = UserSettings(),
    onOpenLogDialog: () -> Unit,
    onEditInjection: (InjectionEntity) -> Unit,
    onDeleteInjection: (InjectionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var injectionToDelete by remember { mutableStateOf<InjectionEntity?>(null) }

    val isOled = settings.themeMode == AppThemeMode.OLED_BLACK
    val cardBg = if (isOled) DarkSurface else MaterialTheme.colorScheme.surface
    val cardVariantBg = if (isOled) DarkSurfaceVariant else MaterialTheme.colorScheme.surfaceVariant
    val cardBorder = if (isOled) DarkBorder else MaterialTheme.colorScheme.outlineVariant
    val e2Accent = if (isOled) EstradiolPink else MaterialTheme.colorScheme.primary
    val textPrimaryColor = if (isOled) TextPrimary else MaterialTheme.colorScheme.onSurface
    val textSecondaryColor = if (isOled) TextSecondary else MaterialTheme.colorScheme.onSurfaceVariant
    val textMutedColor = if (isOled) TextMuted else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 12.dp)
            .testTag("history_screen"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "INJECTION LOGS & SITE ROTATION",
                        fontSize = 11.sp,
                        color = e2Accent,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "${injections.size} total recorded events",
                        fontSize = 10.sp,
                        color = textMutedColor
                    )
                }

                Button(
                    onClick = onOpenLogDialog,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = e2Accent,
                        contentColor = if (isOled) Color.Black else MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add",
                        tint = if (isOled) Color.Black else MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "ADD DOSE",
                        fontSize = 10.sp,
                        color = if (isOled) Color.Black else MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        // Site Rotation Summary Card
        item {
            Surface(
                color = cardBg,
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "SITE ROTATION TRACKER",
                        fontSize = 10.sp,
                        color = textMutedColor,
                        letterSpacing = 0.5.sp
                    )

                    val siteCounts = remember(injections) {
                        InjectionSite.entries.associateWith { site ->
                            injections.count { it.site == site }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(InjectionSite.LEFT_THIGH, InjectionSite.RIGHT_THIGH, InjectionSite.ABDOMEN).forEach { site ->
                            val count = siteCounts[site] ?: 0
                            Surface(
                                color = cardVariantBg,
                                shape = RoundedCornerShape(3.dp),
                                border = BorderStroke(1.dp, cardBorder),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(site.shortCode, fontSize = 10.sp, color = textSecondaryColor)
                                    Text("$count doses", fontSize = 11.sp, color = if (count > 0) e2Accent else textMutedColor)
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(InjectionSite.LEFT_GLUTE, InjectionSite.RIGHT_GLUTE, InjectionSite.LEFT_DELTOID, InjectionSite.RIGHT_DELTOID).forEach { site ->
                            val count = siteCounts[site] ?: 0
                            Surface(
                                color = cardVariantBg,
                                shape = RoundedCornerShape(3.dp),
                                border = BorderStroke(1.dp, cardBorder),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(site.shortCode, fontSize = 10.sp, color = textSecondaryColor)
                                    Text("$count doses", fontSize = 11.sp, color = if (count > 0) e2Accent else textMutedColor)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Table Header
        item {
            Surface(
                color = cardVariantBg,
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("DATE & TIME", fontSize = 10.sp, color = textMutedColor, modifier = Modifier.weight(1.4f))
                    Text("ESTER", fontSize = 10.sp, color = textMutedColor, modifier = Modifier.weight(0.7f))
                    Text("DOSE", fontSize = 10.sp, color = textMutedColor, modifier = Modifier.weight(0.8f))
                    Text("SITE", fontSize = 10.sp, color = textMutedColor, modifier = Modifier.weight(1.1f))
                    Text("ACTIONS", fontSize = 10.sp, color = textMutedColor, modifier = Modifier.weight(0.8f))
                }
            }
        }

        // Table Rows
        items(injections, key = { it.id }) { inj ->
            Surface(
                color = cardBg,
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEditInjection(inj) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1.4f)) {
                        Text(
                            text = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(inj.timestamp)),
                            fontSize = 11.sp,
                            color = textPrimaryColor
                        )
                        Text(
                            text = SimpleDateFormat("HH:mm", Locale.US).format(Date(inj.timestamp)),
                            fontSize = 10.sp,
                            color = textSecondaryColor
                        )
                    }

                    Text(
                        text = inj.ester.code,
                        fontSize = 11.sp,
                        color = e2Accent,
                        modifier = Modifier.weight(0.7f)
                    )

                    Text(
                        text = "${inj.doseMg} mg",
                        fontSize = 11.sp,
                        color = textPrimaryColor,
                        modifier = Modifier.weight(0.8f)
                    )

                    Text(
                        text = inj.site.shortCode,
                        fontSize = 10.sp,
                        color = textSecondaryColor,
                        modifier = Modifier.weight(1.1f)
                    )

                    Row(
                        modifier = Modifier.weight(0.8f),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = { onEditInjection(inj) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = textMutedColor,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        IconButton(
                            onClick = { injectionToDelete = inj },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = StatusElevated,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }

    // Delete confirmation dialog
    if (injectionToDelete != null) {
        val inj = injectionToDelete!!
        AlertDialog(
            onDismissRequest = { injectionToDelete = null },
            title = { Text("Delete Injection Log?", fontSize = 14.sp, color = textPrimaryColor) },
            text = {
                Text(
                    text = "Are you sure you want to delete the ${inj.doseMg}mg ${inj.ester.code} injection on ${SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(inj.timestamp))}?",
                    fontSize = 12.sp,
                    color = textSecondaryColor
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteInjection(inj)
                        injectionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusElevated)
                ) {
                    Text("DELETE", fontSize = 11.sp, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { injectionToDelete = null },
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
