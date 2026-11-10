package com.example.widget

import android.content.Context
import android.graphics.Color
import com.example.data.db.AppDatabase
import com.example.data.model.E2Unit
import com.example.data.model.EstradiolEster
import com.example.data.model.InjectionEntity
import com.example.data.model.InjectionSite
import com.example.data.model.RegimenProfile
import com.example.data.model.TSuppressionStatus
import com.example.data.model.TUnit
import com.example.data.model.UserSettings
import com.example.pk.DataPoint
import com.example.pk.PharmacokineticsEngine
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

data class LiveWidgetData(
    val hasData: Boolean,
    val e2ValueStr: String,
    val e2UnitStr: String,
    val tValueStr: String,
    val tUnitStr: String,
    val statusText: String,
    val statusColor: Int,
    val lastDoseStr: String,
    val nextDoseStr: String,
    val nextSiteStr: String,
    val subtitleGraphStr: String,
    val timelinePoints: List<DataPoint>,
    val targetE2Min: Double,
    val targetE2Max: Double,
    val e2Unit: E2Unit,
    val tUnit: TUnit,
    val isOled: Boolean = true,
    val isNight: Boolean = true
)

object WidgetDataCalculator {

    suspend fun calculate(context: Context): LiveWidgetData {
        val db = AppDatabase.getInstance(context)
        val dao = db.injectionDao()
        val prefs = context.getSharedPreferences("estrocurve_prefs", Context.MODE_PRIVATE)

        // Load settings
        val e2UnitStr = prefs.getString("e2Unit", "PG_ML") ?: "PG_ML"
        val tUnitStr = prefs.getString("tUnit", "NG_DL") ?: "NG_DL"
        val e2Unit = if (e2UnitStr == "PMOL_L") E2Unit.PMOL_L else E2Unit.PG_ML
        val tUnit = if (tUnitStr == "NMOL_L") TUnit.NMOL_L else TUnit.NG_DL
        val targetMinE2 = prefs.getFloat("targetE2Min", 200f).toDouble()
        val targetMaxE2 = prefs.getFloat("targetE2Max", 350f).toDouble()
        val calibrationFactor = prefs.getFloat("calibrationFactor", 1.0f).toDouble()
        val themeModeStr = prefs.getString("themeMode", "OLED_BLACK") ?: "OLED_BLACK"
        val isOled = themeModeStr == "OLED_BLACK"
        val isNight = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES

        // Active rotation sites
        val sitesStr = prefs.getString("enabledSites", null)
        val enabledSites: Set<InjectionSite> = if (sitesStr.isNullOrEmpty()) {
            InjectionSite.entries.toSet()
        } else {
            sitesStr.split(",").mapNotNull { name ->
                try { InjectionSite.valueOf(name.trim()) } catch (_: Exception) { null }
            }.toSet().ifEmpty { InjectionSite.entries.toSet() }
        }

        // Regimen
        val regEsterCode = prefs.getString("reg_ester", "EV") ?: "EV"
        val regDose = prefs.getFloat("reg_dose", 4.0f).toDouble()
        val regInterval = prefs.getFloat("reg_interval", 5.0f).toDouble()
        val regHour = prefs.getInt("reg_hour", 9)
        val regMinute = prefs.getInt("reg_minute", 0)
        val regimen = RegimenProfile(
            ester = EstradiolEster.fromCode(regEsterCode),
            doseMg = regDose,
            intervalDays = regInterval,
            preferredHour = regHour,
            preferredMinute = regMinute
        )

        // Injections
        val injections = dao.getAllInjectionsAscending()
        val now = System.currentTimeMillis()

        if (injections.isEmpty()) {
            val activeSites = InjectionSite.entries.filter { it in enabledSites }.ifEmpty { InjectionSite.entries }
            val firstSite = activeSites.first().shortCode
            return LiveWidgetData(
                hasData = false,
                e2ValueStr = "---",
                e2UnitStr = e2Unit.label,
                tValueStr = "---",
                tUnitStr = tUnit.label,
                statusText = "NO LOGS",
                statusColor = Color.parseColor("#757575"),
                lastDoseStr = "Last: --",
                nextDoseStr = "Next: --",
                nextSiteStr = "Site: $firstSite",
                subtitleGraphStr = "No injection logs yet • Tap to track dose",
                timelinePoints = emptyList(),
                targetE2Min = targetMinE2,
                targetE2Max = targetMaxE2,
                e2Unit = e2Unit,
                tUnit = tUnit,
                isOled = isOled,
                isNight = isNight
            )
        }

        // Injections exist
        val latest = injections.maxByOrNull { it.timestamp }
        val currentE2 = PharmacokineticsEngine.calculateSuperpositionE2(injections, now, calibrationFactor)
        val currentT = PharmacokineticsEngine.estimateTestosteroneNgDl(currentE2)
        val status = TSuppressionStatus.fromTestosteroneNgDl(currentT)

        val statusColor = when (status) {
            TSuppressionStatus.SUPPRESSED -> Color.parseColor("#00E676")
            TSuppressionStatus.BORDERLINE -> Color.parseColor("#FFD600")
            TSuppressionStatus.ELEVATED -> Color.parseColor("#FF5252")
        }

        // Formatted E2 & T
        val convertedE2 = e2Unit.fromPgMl(currentE2)
        val e2ValueStr = if (convertedE2 >= 100) {
            String.format(Locale.US, "%.0f", convertedE2)
        } else {
            String.format(Locale.US, "%.1f", convertedE2)
        }

        val convertedT = tUnit.fromNgDl(currentT)
        val tValueStr = if (tUnit == TUnit.NMOL_L) {
            String.format(Locale.US, "%.2f", convertedT)
        } else {
            String.format(Locale.US, "%.0f", convertedT)
        }

        // Last dose elapsed
        val lastDoseStr = if (latest != null) {
            val elapsedHours = (now - latest.timestamp) / (3600.0 * 1000.0)
            if (elapsedHours < 0) {
                "Last: just now"
            } else if (elapsedHours < 24.0) {
                String.format(Locale.US, "Last: %.0fh ago", elapsedHours)
            } else {
                String.format(Locale.US, "Last: %.1fd ago", elapsedHours / 24.0)
            }
        } else {
            "Last: --"
        }

        // Next dose due
        val intervalMillis = (regimen.intervalDays * 24 * 3600 * 1000).toLong()
        val nextTimestamp = if (latest != null) {
            val rawNext = latest.timestamp + intervalMillis
            val cal = Calendar.getInstance().apply {
                timeInMillis = rawNext
                set(Calendar.HOUR_OF_DAY, regimen.preferredHour)
                set(Calendar.MINUTE, regimen.preferredMinute)
                set(Calendar.SECOND, 0)
            }
            cal.timeInMillis
        } else null

        val nextDoseStr = if (nextTimestamp != null) {
            val hoursToNext = (nextTimestamp - now) / (3600.0 * 1000.0)
            if (hoursToNext < 0) {
                val overdueHours = abs(hoursToNext)
                if (overdueHours < 24.0) {
                    String.format(Locale.US, "Next: Overdue (%.0fh)", overdueHours)
                } else {
                    String.format(Locale.US, "Next: Overdue (%.1fd)", overdueHours / 24.0)
                }
            } else if (hoursToNext < 24.0) {
                String.format(Locale.US, "Next: in %.0fh", hoursToNext)
            } else {
                String.format(Locale.US, "Next: in %.1fd", hoursToNext / 24.0)
            }
        } else {
            "Next: --"
        }

        // Next injection rotation site
        val activeSites = InjectionSite.entries.filter { it in enabledSites }.ifEmpty { InjectionSite.entries }
        val nextSite = if (latest?.site == null || latest.site !in activeSites) {
            activeSites.first()
        } else {
            val nextIdx = (activeSites.indexOf(latest.site) + 1) % activeSites.size
            activeSites[nextIdx]
        }
        val nextSiteStr = "Site: ${nextSite.shortCode}"

        // Subtitle summary for graph widget
        val subtitleGraphStr = String.format(
            Locale.US,
            "Now: %s %s • T: %s %s (%s)",
            e2ValueStr,
            e2Unit.label,
            tValueStr,
            tUnit.label,
            if (status == TSuppressionStatus.SUPPRESSED) "Suppressed" else if (status == TSuppressionStatus.BORDERLINE) "Borderline" else "Elevated"
        )

        // 10-day timeline (-7d past + 3d future)
        val timeline = PharmacokineticsEngine.generateTimelineDataPoints(
            injections = injections,
            nowMillis = now,
            pastDays = 7,
            futureDays = 3,
            stepHours = 2.0,
            calibrationFactor = calibrationFactor
        )

        return LiveWidgetData(
            hasData = true,
            e2ValueStr = e2ValueStr,
            e2UnitStr = e2Unit.label,
            tValueStr = tValueStr,
            tUnitStr = tUnit.label,
            statusText = when (status) {
                TSuppressionStatus.SUPPRESSED -> "SUPPRESSED"
                TSuppressionStatus.BORDERLINE -> "BORDERLINE"
                TSuppressionStatus.ELEVATED -> "ELEVATED"
            },
            statusColor = statusColor,
            lastDoseStr = lastDoseStr,
            nextDoseStr = nextDoseStr,
            nextSiteStr = nextSiteStr,
            subtitleGraphStr = subtitleGraphStr,
            timelinePoints = timeline,
            targetE2Min = targetMinE2,
            targetE2Max = targetMaxE2,
            e2Unit = e2Unit,
            tUnit = tUnit,
            isOled = isOled,
            isNight = isNight
        )
    }
}
