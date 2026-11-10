package com.example.util

import com.example.data.model.AppThemeMode
import com.example.data.model.E2Unit
import com.example.data.model.EstradiolEster
import com.example.data.model.InjectionEntity
import com.example.data.model.InjectionSite
import com.example.data.model.RegimenProfile
import com.example.data.model.TUnit
import com.example.data.model.UserSettings
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class BackupPayload(
    val injections: List<InjectionEntity>,
    val settings: UserSettings,
    val regimen: RegimenProfile
)

sealed class BackupInspectResult {
    data class Plain(val payload: BackupPayload) : BackupInspectResult()
    object Encrypted : BackupInspectResult()
    data class Error(val message: String) : BackupInspectResult()
}

object SafeBase64 {
    fun encodeToString(bytes: ByteArray): String {
        return try {
            java.util.Base64.getEncoder().encodeToString(bytes)
        } catch (_: Throwable) {
            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        }
    }

    fun decode(str: String): ByteArray {
        return try {
            java.util.Base64.getDecoder().decode(str.trim())
        } catch (_: Throwable) {
            android.util.Base64.decode(str.trim(), android.util.Base64.DEFAULT)
        }
    }
}

object BackupCryptoManager {
    private const val MAGIC_HEADER = "ESTROCURVE_BACKUP"
    private const val CURRENT_VERSION = 2
    private const val GCM_TAG_LENGTH = 128
    private const val GCM_IV_LENGTH = 12
    private const val SALT_LENGTH = 16
    private const val PBKDF2_ITERATIONS = 65536
    private const val KEY_LENGTH_BITS = 256

    private fun deriveKey(password: CharArray, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val secretKey = factory.generateSecret(spec)
        return SecretKeySpec(secretKey.encoded, "AES")
    }

    fun encrypt(plaintext: ByteArray, password: String): JSONObject {
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH).also { random.nextBytes(it) }
        val iv = ByteArray(GCM_IV_LENGTH).also { random.nextBytes(it) }

        val key = deriveKey(password.toCharArray(), salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        val ciphertext = cipher.doFinal(plaintext)

        val json = JSONObject()
        json.put("magic", MAGIC_HEADER)
        json.put("encrypted", true)
        json.put("version", CURRENT_VERSION)
        json.put("salt", SafeBase64.encodeToString(salt))
        json.put("iv", SafeBase64.encodeToString(iv))
        json.put("ciphertext", SafeBase64.encodeToString(ciphertext))
        return json
    }

    fun decrypt(envelope: JSONObject, password: String): ByteArray {
        val salt = SafeBase64.decode(envelope.getString("salt"))
        val iv = SafeBase64.decode(envelope.getString("iv"))
        val ciphertext = SafeBase64.decode(envelope.getString("ciphertext"))

        val key = deriveKey(password.toCharArray(), salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        return cipher.doFinal(ciphertext)
    }

    fun serializePayload(payload: BackupPayload): String {
        val root = JSONObject()
        root.put("version", CURRENT_VERSION)
        root.put("appName", "EstroCurve")
        root.put("exportedAt", System.currentTimeMillis())

        // User Settings
        val s = payload.settings
        val sObj = JSONObject()
        sObj.put("e2Unit", s.e2Unit.name)
        sObj.put("tUnit", s.tUnit.name)
        sObj.put("targetE2Min", s.targetE2Min)
        sObj.put("targetE2Max", s.targetE2Max)
        sObj.put("reminderEnabled", s.reminderEnabled)
        sObj.put("advanceReminderHours", s.advanceReminderHours)
        sObj.put("calibrationFactor", s.calibrationFactor)
        sObj.put("enabledSites", JSONArray(s.enabledSites.map { it.name }))
        sObj.put("themeMode", s.themeMode.name)
        root.put("settings", sObj)

        // Regimen Profile
        val r = payload.regimen
        val rObj = JSONObject()
        rObj.put("esterCode", r.ester.code)
        rObj.put("doseMg", r.doseMg)
        rObj.put("intervalDays", r.intervalDays)
        rObj.put("preferredHour", r.preferredHour)
        rObj.put("preferredMinute", r.preferredMinute)
        root.put("regimen", rObj)

        // Injections
        val arr = JSONArray()
        for (inj in payload.injections) {
            val obj = JSONObject()
            obj.put("id", inj.id)
            obj.put("timestamp", inj.timestamp)
            obj.put("esterCode", inj.esterCode)
            obj.put("doseMg", inj.doseMg)
            obj.put("siteId", inj.siteId)
            obj.put("notes", inj.notes)
            arr.put(obj)
        }
        root.put("injections", arr)
        return root.toString(2)
    }

    fun parsePayload(jsonString: String): BackupPayload {
        val root = JSONObject(jsonString)

        val injList = mutableListOf<InjectionEntity>()
        if (root.has("injections")) {
            val arr = root.getJSONArray("injections")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                injList.add(
                    InjectionEntity(
                        id = 0L,
                        timestamp = obj.getLong("timestamp"),
                        esterCode = obj.getString("esterCode"),
                        doseMg = obj.getDouble("doseMg"),
                        siteId = obj.getString("siteId"),
                        notes = obj.optString("notes", "")
                    )
                )
            }
        }

        val settings = if (root.has("settings")) {
            val sObj = root.getJSONObject("settings")
            val e2 = try { E2Unit.valueOf(sObj.optString("e2Unit", "PG_ML")) } catch (_: Exception) { E2Unit.PG_ML }
            val t = try { TUnit.valueOf(sObj.optString("tUnit", "NG_DL")) } catch (_: Exception) { TUnit.NG_DL }
            val sites = if (sObj.has("enabledSites")) {
                val sArr = sObj.getJSONArray("enabledSites")
                (0 until sArr.length()).mapNotNull { idx ->
                    try { InjectionSite.valueOf(sArr.getString(idx)) } catch (_: Exception) { null }
                }.toSet().ifEmpty { InjectionSite.entries.toSet() }
            } else InjectionSite.entries.toSet()
            val theme = try { AppThemeMode.valueOf(sObj.optString("themeMode", "OLED_BLACK")) } catch (_: Exception) { AppThemeMode.OLED_BLACK }

            UserSettings(
                e2Unit = e2,
                tUnit = t,
                targetE2Min = sObj.optDouble("targetE2Min", 200.0),
                targetE2Max = sObj.optDouble("targetE2Max", 350.0),
                reminderEnabled = sObj.optBoolean("reminderEnabled", true),
                advanceReminderHours = sObj.optInt("advanceReminderHours", 0),
                calibrationFactor = sObj.optDouble("calibrationFactor", 1.0),
                enabledSites = sites,
                themeMode = theme
            )
        } else {
            UserSettings()
        }

        val regimen = if (root.has("regimen")) {
            val rObj = root.getJSONObject("regimen")
            val ester = EstradiolEster.fromCode(rObj.optString("esterCode", "EV"))
            RegimenProfile(
                ester = ester,
                doseMg = rObj.optDouble("doseMg", 4.0),
                intervalDays = rObj.optDouble("intervalDays", 5.0),
                preferredHour = rObj.optInt("preferredHour", 9),
                preferredMinute = rObj.optInt("preferredMinute", 0)
            )
        } else {
            RegimenProfile()
        }

        return BackupPayload(injList, settings, regimen)
    }

    fun createBackupBytes(
        injections: List<InjectionEntity>,
        settings: UserSettings,
        regimen: RegimenProfile,
        password: String?
    ): ByteArray {
        val payloadString = serializePayload(BackupPayload(injections, settings, regimen))
        val payloadBytes = payloadString.toByteArray(StandardCharsets.UTF_8)

        val envelope = if (!password.isNullOrBlank()) {
            encrypt(payloadBytes, password)
        } else {
            val root = JSONObject()
            root.put("magic", MAGIC_HEADER)
            root.put("encrypted", false)
            root.put("version", CURRENT_VERSION)
            root.put("payload", payloadString)
            root
        }
        return envelope.toString(2).toByteArray(StandardCharsets.UTF_8)
    }

    fun inspectBackup(bytes: ByteArray): BackupInspectResult {
        return try {
            val str = String(bytes, StandardCharsets.UTF_8).trim()
            val root = JSONObject(str)
            if (root.optString("magic") == MAGIC_HEADER) {
                val isEncrypted = root.optBoolean("encrypted", false)
                if (isEncrypted) {
                    BackupInspectResult.Encrypted
                } else {
                    val payloadStr = root.optString("payload", "")
                    if (payloadStr.isNotEmpty()) {
                        BackupInspectResult.Plain(parsePayload(payloadStr))
                    } else {
                        BackupInspectResult.Plain(parsePayload(str))
                    }
                }
            } else if (root.has("injections")) {
                BackupInspectResult.Plain(parsePayload(str))
            } else {
                BackupInspectResult.Error("Selected file is not an EstroCurve backup.")
            }
        } catch (e: Exception) {
            BackupInspectResult.Error("Unable to parse backup file: ${e.message}")
        }
    }

    fun decryptAndParse(bytes: ByteArray, password: String?): Result<BackupPayload> {
        return try {
            val str = String(bytes, StandardCharsets.UTF_8).trim()
            val root = JSONObject(str)
            if (root.optString("magic") == MAGIC_HEADER) {
                val isEncrypted = root.optBoolean("encrypted", false)
                if (isEncrypted) {
                    if (password.isNullOrEmpty()) {
                        return Result.failure(IllegalArgumentException("Password required for encrypted backup"))
                    }
                    val decryptedBytes = decrypt(root, password)
                    val decryptedStr = String(decryptedBytes, StandardCharsets.UTF_8)
                    Result.success(parsePayload(decryptedStr))
                } else {
                    val payloadStr = root.optString("payload", "")
                    val targetStr = if (payloadStr.isNotEmpty()) payloadStr else str
                    Result.success(parsePayload(targetStr))
                }
            } else if (root.has("injections")) {
                Result.success(parsePayload(str))
            } else {
                Result.failure(IllegalArgumentException("Not a valid EstroCurve backup file"))
            }
        } catch (e: javax.crypto.AEADBadTagException) {
            Result.failure(SecurityException("Incorrect password or corrupted backup file."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
