package com.example

import com.example.data.model.AppThemeMode
import com.example.data.model.E2Unit
import com.example.data.model.EstradiolEster
import com.example.data.model.InjectionEntity
import com.example.data.model.InjectionSite
import com.example.data.model.RegimenProfile
import com.example.data.model.TUnit
import com.example.data.model.UserSettings
import com.example.util.BackupCryptoManager
import com.example.util.BackupInspectResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BackupCryptoManagerTest {

    private val sampleInjections = listOf(
        InjectionEntity(
            id = 1L,
            timestamp = 1700000000000L,
            esterCode = "EV",
            doseMg = 5.0,
            siteId = "LEFT_THIGH",
            notes = "Smooth injection"
        ),
        InjectionEntity(
            id = 2L,
            timestamp = 1700500000000L,
            esterCode = "EV",
            doseMg = 5.0,
            siteId = "RIGHT_THIGH",
            notes = "No swelling"
        )
    )

    private val sampleSettings = UserSettings(
        e2Unit = E2Unit.PMOL_L,
        tUnit = TUnit.NMOL_L,
        targetE2Min = 250.0,
        targetE2Max = 400.0,
        reminderEnabled = true,
        advanceReminderHours = 6,
        calibrationFactor = 1.15,
        enabledSites = setOf(InjectionSite.LEFT_THIGH, InjectionSite.RIGHT_THIGH),
        themeMode = AppThemeMode.DYNAMIC_SYSTEM
    )

    private val sampleRegimen = RegimenProfile(
        ester = EstradiolEster.EC,
        doseMg = 6.0,
        intervalDays = 7.0,
        preferredHour = 10,
        preferredMinute = 30
    )

    @Test
    fun `unencrypted backup roundtrip preserves injections, regimen, and settings`() {
        val bytes = BackupCryptoManager.createBackupBytes(
            injections = sampleInjections,
            settings = sampleSettings,
            regimen = sampleRegimen,
            password = null
        )

        val inspect = BackupCryptoManager.inspectBackup(bytes)
        assertTrue("Inspect result should be Plain", inspect is BackupInspectResult.Plain)

        val decryptResult = BackupCryptoManager.decryptAndParse(bytes, password = null)
        assertTrue("Decrypt result should be success", decryptResult.isSuccess)

        val payload = decryptResult.getOrThrow()
        assertEquals(2, payload.injections.size)
        assertEquals("EV", payload.injections[0].esterCode)
        assertEquals(5.0, payload.injections[0].doseMg, 0.001)
        assertEquals("Smooth injection", payload.injections[0].notes)

        assertEquals(E2Unit.PMOL_L, payload.settings.e2Unit)
        assertEquals(TUnit.NMOL_L, payload.settings.tUnit)
        assertEquals(1.15, payload.settings.calibrationFactor, 0.001)
        assertEquals(AppThemeMode.DYNAMIC_SYSTEM, payload.settings.themeMode)

        assertEquals(EstradiolEster.EC, payload.regimen.ester)
        assertEquals(6.0, payload.regimen.doseMg, 0.001)
        assertEquals(7.0, payload.regimen.intervalDays, 0.001)
    }

    @Test
    fun `encrypted backup roundtrip with password succeeds`() {
        val testPassword = "MyStrongSecurePassword!2026"
        val bytes = BackupCryptoManager.createBackupBytes(
            injections = sampleInjections,
            settings = sampleSettings,
            regimen = sampleRegimen,
            password = testPassword
        )

        val inspect = BackupCryptoManager.inspectBackup(bytes)
        assertTrue("Inspect result should be Encrypted", inspect is BackupInspectResult.Encrypted)

        val decryptResult = BackupCryptoManager.decryptAndParse(bytes, password = testPassword)
        assertTrue("Decrypt result should be success with correct password", decryptResult.isSuccess)

        val payload = decryptResult.getOrThrow()
        assertEquals(2, payload.injections.size)
        assertEquals(EstradiolEster.EC, payload.regimen.ester)
        assertEquals(1.15, payload.settings.calibrationFactor, 0.001)
    }

    @Test
    fun `encrypted backup with incorrect password fails securely`() {
        val testPassword = "OriginalPassword123"
        val bytes = BackupCryptoManager.createBackupBytes(
            injections = sampleInjections,
            settings = sampleSettings,
            regimen = sampleRegimen,
            password = testPassword
        )

        val decryptResult = BackupCryptoManager.decryptAndParse(bytes, password = "WrongPassword!")
        assertTrue("Decrypt result should fail with incorrect password", decryptResult.isFailure)
        assertNotNull(decryptResult.exceptionOrNull())
    }

    @Test
    fun `legacy v1 json string import is backwards compatible`() {
        val legacyJson = """
            {
              "version": 1,
              "appName": "EstroCurve",
              "exportedAt": 1690000000000,
              "injections": [
                {
                  "id": 10,
                  "timestamp": 1690000000000,
                  "esterCode": "EEn",
                  "doseMg": 7.0,
                  "siteId": "LEFT_GLUTE",
                  "notes": "Legacy test"
                }
              ]
            }
        """.trimIndent()

        val bytes = legacyJson.toByteArray(Charsets.UTF_8)
        val inspect = BackupCryptoManager.inspectBackup(bytes)
        assertTrue("Legacy JSON should be recognized as Plain", inspect is BackupInspectResult.Plain)

        val result = BackupCryptoManager.decryptAndParse(bytes, password = null)
        assertTrue("Legacy JSON should be successfully parsed", result.isSuccess)

        val payload = result.getOrThrow()
        assertEquals(1, payload.injections.size)
        assertEquals("EEn", payload.injections[0].esterCode)
        assertEquals(7.0, payload.injections[0].doseMg, 0.001)
    }
}
