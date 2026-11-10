package com.example.ui.components

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.AppThemeMode
import com.example.data.model.UserSettings
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EstradiolPink
import com.example.ui.theme.StatusElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.BackupCryptoManager
import com.example.util.BackupInspectResult
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ExportImportDialog(
    exportJsonString: String = "",
    settings: UserSettings = UserSettings(),
    onDismiss: () -> Unit,
    onExportBytes: (password: String?) -> ByteArray = { byteArrayOf() },
    onRestoreBytes: suspend (bytes: ByteArray, password: String?) -> Result<Int> = { _, _ -> Result.success(0) },
    onImportJson: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val isOled = settings.themeMode == AppThemeMode.OLED_BLACK
    val cardBg = if (isOled) DarkSurface else MaterialTheme.colorScheme.surface
    val cardVariantBg = if (isOled) DarkSurfaceVariant else MaterialTheme.colorScheme.surfaceVariant
    val cardBorder = if (isOled) DarkBorder else MaterialTheme.colorScheme.outlineVariant
    val e2Accent = if (isOled) EstradiolPink else MaterialTheme.colorScheme.primary
    val textPrimaryColor = if (isOled) TextPrimary else MaterialTheme.colorScheme.onSurface
    val textSecondaryColor = if (isOled) TextSecondary else MaterialTheme.colorScheme.onSurfaceVariant

    // Export state
    var isProtectWithPassword by remember { mutableStateOf(false) }
    var exportPassword by remember { mutableStateOf("") }
    var showExportPassword by remember { mutableStateOf(false) }

    // Import / Decrypt state
    var pendingRestoreBytes by remember { mutableStateOf<ByteArray?>(null) }
    var showDecryptDialog by remember { mutableStateOf(false) }
    var decryptPassword by remember { mutableStateOf("") }
    var showDecryptPassword by remember { mutableStateOf(false) }
    var decryptError by remember { mutableStateOf<String?>(null) }
    var isRestoring by remember { mutableStateOf(false) }

    // File download launcher (.estrocurve)
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri?.let { destUri ->
            try {
                val pwd = if (isProtectWithPassword && exportPassword.isNotBlank()) exportPassword.trim() else null
                val bytes = onExportBytes(pwd)
                context.contentResolver.openOutputStream(destUri)?.use { stream ->
                    stream.write(bytes)
                }
                val statusMsg = if (pwd != null) {
                    "Encrypted backup file saved successfully (.estrocurve)"
                } else {
                    "Backup file saved successfully (.estrocurve)"
                }
                Toast.makeText(context, statusMsg, Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error saving backup: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // File upload launcher
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { sourceUri ->
            try {
                val bytes = context.contentResolver.openInputStream(sourceUri)?.use { stream ->
                    stream.readBytes()
                }
                if (bytes == null || bytes.isEmpty()) {
                    Toast.makeText(context, "Selected file is empty", Toast.LENGTH_SHORT).show()
                    return@let
                }

                val inspect = BackupCryptoManager.inspectBackup(bytes)
                when (inspect) {
                    is BackupInspectResult.Encrypted -> {
                        pendingRestoreBytes = bytes
                        decryptPassword = ""
                        decryptError = null
                        showDecryptDialog = true
                    }
                    is BackupInspectResult.Plain -> {
                        coroutineScope.launch {
                            isRestoring = true
                            val result = onRestoreBytes(bytes, null)
                            isRestoring = false
                            if (result.isSuccess) {
                                val count = result.getOrNull() ?: 0
                                Toast.makeText(context, "Restored $count injections and app settings!", Toast.LENGTH_LONG).show()
                                onDismiss()
                            } else {
                                Toast.makeText(context, "Restore failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                    is BackupInspectResult.Error -> {
                        Toast.makeText(context, inspect.message, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to read file: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Main Backup / Restore Dialog
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = cardBg,
            border = BorderStroke(1.dp, cardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "BACKUP & RESTORE DATA",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimaryColor,
                    letterSpacing = 0.5.sp
                )

                Text(
                    text = "Export and import your complete database (.estrocurve file) including all recorded injection history, active regimen, and calibration settings.",
                    fontSize = 11.sp,
                    color = textSecondaryColor
                )

                // Section 1: Export Card
                Surface(
                    color = cardVariantBg,
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = "Export File",
                                tint = e2Accent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "DOWNLOAD BACKUP FILE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textPrimaryColor
                            )
                        }

                        // Optional Password Toggle
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isProtectWithPassword = !isProtectWithPassword }
                        ) {
                            Checkbox(
                                checked = isProtectWithPassword,
                                onCheckedChange = { isProtectWithPassword = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = e2Accent,
                                    uncheckedColor = textSecondaryColor
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = if (isProtectWithPassword) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = "Lock",
                                tint = if (isProtectWithPassword) e2Accent else textSecondaryColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Encrypt file with password (AES-256)",
                                fontSize = 11.sp,
                                color = textPrimaryColor
                            )
                        }

                        if (isProtectWithPassword) {
                            OutlinedTextField(
                                value = exportPassword,
                                onValueChange = { exportPassword = it },
                                label = { Text("Set Backup Password", fontSize = 10.sp) },
                                singleLine = true,
                                visualTransformation = if (showExportPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { showExportPassword = !showExportPassword }) {
                                        Icon(
                                            imageVector = if (showExportPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = "Toggle password visibility",
                                            tint = textSecondaryColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("backup_export_password_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = e2Accent,
                                    unfocusedBorderColor = cardBorder,
                                    focusedTextColor = textPrimaryColor,
                                    unfocusedTextColor = textPrimaryColor
                                )
                            )
                        }

                        Button(
                            onClick = {
                                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                                createDocumentLauncher.launch("estrocurve_backup_$timestamp.estrocurve")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = e2Accent),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .testTag("backup_export_download_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = null,
                                tint = if (isOled) Color.Black else MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "SAVE .ESTROCURVE FILE",
                                fontSize = 11.sp,
                                color = if (isOled) Color.Black else MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }

                // Section 2: Import Card
                Surface(
                    color = cardVariantBg,
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FileUpload,
                                contentDescription = "Upload File",
                                tint = e2Accent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "UPLOAD BACKUP FILE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textPrimaryColor
                            )
                        }

                        Text(
                            text = "Select a saved .estrocurve file. If it was encrypted, you will be prompted for its password to decrypt and restore.",
                            fontSize = 10.sp,
                            color = textSecondaryColor
                        )

                        OutlinedButton(
                            onClick = {
                                openDocumentLauncher.launch(arrayOf("*/*"))
                            },
                            border = BorderStroke(1.dp, e2Accent),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .testTag("backup_import_upload_button")
                        ) {
                            if (isRestoring) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = e2Accent, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("RESTORING...", fontSize = 11.sp, color = e2Accent)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.FileUpload,
                                    contentDescription = null,
                                    tint = e2Accent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("SELECT BACKUP FILE", fontSize = 11.sp, color = e2Accent)
                            }
                        }
                    }
                }


                // Close Button
                OutlinedButton(
                    onClick = onDismiss,
                    border = BorderStroke(1.dp, cardBorder),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("CLOSE", fontSize = 11.sp, color = textSecondaryColor)
                }
            }
        }
    }

    // Decrypt Password Modal Dialog
    if (showDecryptDialog && pendingRestoreBytes != null) {
        Dialog(onDismissRequest = { showDecryptDialog = false }) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = cardBg,
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Encrypted",
                            tint = e2Accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ENCRYPTED BACKUP FILE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimaryColor
                        )
                    }

                    Text(
                        text = "This backup is protected with AES-256 encryption. Enter the decryption password set when the file was exported:",
                        fontSize = 11.sp,
                        color = textSecondaryColor
                    )

                    OutlinedTextField(
                        value = decryptPassword,
                        onValueChange = {
                            decryptPassword = it
                            decryptError = null
                        },
                        label = { Text("Password", fontSize = 10.sp) },
                        singleLine = true,
                        visualTransformation = if (showDecryptPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showDecryptPassword = !showDecryptPassword }) {
                                Icon(
                                    imageVector = if (showDecryptPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle password visibility",
                                    tint = textSecondaryColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        },
                        isError = decryptError != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("backup_decrypt_password_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = e2Accent,
                            unfocusedBorderColor = cardBorder,
                            focusedTextColor = textPrimaryColor,
                            unfocusedTextColor = textPrimaryColor,
                            errorBorderColor = StatusElevated
                        )
                    )

                    if (decryptError != null) {
                        Text(
                            text = decryptError ?: "",
                            fontSize = 10.sp,
                            color = StatusElevated
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showDecryptDialog = false },
                            border = BorderStroke(1.dp, cardBorder),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("CANCEL", fontSize = 11.sp, color = textSecondaryColor)
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isRestoring = true
                                    val result = onRestoreBytes(pendingRestoreBytes!!, decryptPassword)
                                    isRestoring = false
                                    if (result.isSuccess) {
                                        val count = result.getOrNull() ?: 0
                                        Toast.makeText(context, "Restored $count injections and app settings!", Toast.LENGTH_LONG).show()
                                        showDecryptDialog = false
                                        onDismiss()
                                    } else {
                                        decryptError = result.exceptionOrNull()?.message ?: "Decryption failed. Incorrect password."
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = e2Accent),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .weight(1.3f)
                                .testTag("backup_decrypt_confirm_button")
                        ) {
                            if (isRestoring) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = if (isOled) Color.Black else MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "DECRYPT & RESTORE",
                                    fontSize = 11.sp,
                                    color = if (isOled) Color.Black else MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
