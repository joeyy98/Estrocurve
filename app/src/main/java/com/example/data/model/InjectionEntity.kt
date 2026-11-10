package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "injections")
data class InjectionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val timestamp: Long,          // Epoch millis
    val esterCode: String,        // EV, EC, EEn, EB, EU
    val doseMg: Double,           // In milligrams
    val siteId: String,           // InjectionSite enum name
    val notes: String = ""
) {
    val ester: EstradiolEster
        get() = EstradiolEster.fromCode(esterCode)

    val site: InjectionSite
        get() = InjectionSite.fromId(siteId)
}

data class RegimenProfile(
    val ester: EstradiolEster = EstradiolEster.EV,
    val doseMg: Double = 4.0,
    val intervalDays: Double = 5.0,
    val preferredHour: Int = 9,      // 09:00 AM default
    val preferredMinute: Int = 0,
    val nextSiteIndex: Int = 0
)

data class TargetRangePreset(
    val name: String,
    val description: String,
    val minPgMl: Double,
    val maxPgMl: Double
) {
    companion object {
        val MONOTHERAPY = TargetRangePreset(
            name = "Monotherapy (Suppression)",
            description = "Maintains gonadal T suppression without blockers (200 - 350 pg/mL)",
            minPgMl = 200.0,
            maxPgMl = 350.0
        )
        val WPATH_ENDOCRINE = TargetRangePreset(
            name = "Endocrine Society / WPATH",
            description = "Standard clinical guidelines with antiandrogens (100 - 200 pg/mL)",
            minPgMl = 100.0,
            maxPgMl = 200.0
        )
        val LOW_DOSE = TargetRangePreset(
            name = "Low-Dose / Non-Binary",
            description = "Lower target range (50 - 100 pg/mL)",
            minPgMl = 50.0,
            maxPgMl = 100.0
        )
        val PRESETS = listOf(MONOTHERAPY, WPATH_ENDOCRINE, LOW_DOSE)
    }
}

enum class AppThemeMode(val title: String, val description: String) {
    OLED_BLACK("OLED Black", "Pure black (#000000) background with high-contrast neon accents"),
    DYNAMIC_SYSTEM("Dynamic System", "Matches device wallpaper colors and follows system light/dark mode")
}

data class UserSettings(
    val e2Unit: E2Unit = E2Unit.PG_ML,
    val tUnit: TUnit = TUnit.NG_DL,
    val targetE2Min: Double = 200.0,
    val targetE2Max: Double = 350.0,
    val reminderEnabled: Boolean = true,
    val advanceReminderHours: Int = 12, // e.g. 12h before or day-of
    val calibrationFactor: Double = 1.0,  // Individual metabolic calibration (0.6x to 1.4x)
    val enabledSites: Set<InjectionSite> = InjectionSite.entries.toSet(),
    val themeMode: AppThemeMode = AppThemeMode.OLED_BLACK
)

