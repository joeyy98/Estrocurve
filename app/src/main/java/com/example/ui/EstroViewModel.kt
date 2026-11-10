package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.AppThemeMode
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
import com.example.pk.SteadyStateMetrics
import com.example.reminder.ReminderScheduler
import com.example.widget.WidgetUpdateHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.util.BackupCryptoManager
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

data class DashboardState(
    val currentE2PgMl: Double = 0.0,
    val currentTestosteroneNgDl: Double = 550.0,
    val suppressionStatus: TSuppressionStatus = TSuppressionStatus.ELEVATED,
    val hoursSinceLastDose: Double? = null,
    val hoursToNextDose: Double? = null,
    val nextDoseTimestamp: Long? = null,
    val latestInjection: InjectionEntity? = null,
    val timelinePoints: List<DataPoint> = emptyList(),
    val totalInjectionsCount: Int = 0,
    val nextSuggestedSite: InjectionSite = InjectionSite.LEFT_THIGH
)

data class SimulatorState(
    val ester: EstradiolEster = EstradiolEster.EV,
    val doseMg: Double = 4.0,
    val intervalDays: Double = 5.0,
    val isSteadyState: Boolean = true,
    val totalDays: Double = 28.0,
    val points: List<DataPoint> = emptyList(),
    val metrics: SteadyStateMetrics = PharmacokineticsEngine.calculateSteadyStateMetrics(
        EstradiolEster.EV, 4.0, 5.0
    ),
    // Comparison regimen
    val isComparing: Boolean = false,
    val compareEster: EstradiolEster = EstradiolEster.EEn,
    val compareDoseMg: Double = 5.0,
    val compareIntervalDays: Double = 7.0,
    val comparePoints: List<DataPoint> = emptyList(),
    val compareMetrics: SteadyStateMetrics = PharmacokineticsEngine.calculateSteadyStateMetrics(
        EstradiolEster.EEn, 5.0, 7.0
    )
)

class EstroViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val dao = db.injectionDao()
    private val prefs = application.getSharedPreferences("estrocurve_prefs", Context.MODE_PRIVATE)

    // User settings
    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<UserSettings> = _settings.asStateFlow()

    // Active Regimen
    private val _regimen = MutableStateFlow(loadRegimen())
    val regimen: StateFlow<RegimenProfile> = _regimen.asStateFlow()

    // All injections flow
    val injectionsList: StateFlow<List<InjectionEntity>> = dao.getAllInjectionsFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Simulator State
    private val _simulatorState = MutableStateFlow(
        SimulatorState(
            isSteadyState = prefs.getBoolean("sim_steady_state", true)
        )
    )
    val simulatorState: StateFlow<SimulatorState> = _simulatorState.asStateFlow()

    // Dashboard State derived from injections, regimen, and settings
    val dashboardState: StateFlow<DashboardState> = combine(
        injectionsList,
        _regimen,
        _settings
    ) { injections, activeRegimen, userSettings ->
        val now = System.currentTimeMillis()
        val latest = injections.maxByOrNull { it.timestamp }

        val currentE2 = if (injections.isEmpty()) {
            0.0
        } else {
            PharmacokineticsEngine.calculateSuperpositionE2(injections, now, userSettings.calibrationFactor)
        }
        val currentT = PharmacokineticsEngine.estimateTestosteroneNgDl(currentE2)
        val status = if (injections.isEmpty()) {
            TSuppressionStatus.ELEVATED
        } else {
            TSuppressionStatus.fromTestosteroneNgDl(currentT)
        }

        val hoursSince = if (latest != null) {
            (now - latest.timestamp) / (3600.0 * 1000.0)
        } else null

        val intervalMillis = (activeRegimen.intervalDays * 24 * 3600 * 1000).toLong()
        val nextTimestamp = if (latest != null) latest.timestamp + intervalMillis else null
        val hoursToNext = if (nextTimestamp != null) {
            (nextTimestamp - now) / (3600.0 * 1000.0)
        } else null

        val timeline = PharmacokineticsEngine.generateTimelineDataPoints(
            injections = injections,
            nowMillis = now,
            pastDays = 14,
            futureDays = 7,
            stepHours = 3.0,
            calibrationFactor = userSettings.calibrationFactor
        )

        // Suggest next site based on user's active rotation sites
        val nextSite = getNextSiteInRotation(latest?.site, userSettings.enabledSites)

        DashboardState(
            currentE2PgMl = currentE2,
            currentTestosteroneNgDl = currentT,
            suppressionStatus = status,
            hoursSinceLastDose = hoursSince,
            hoursToNextDose = hoursToNext,
            nextDoseTimestamp = nextTimestamp,
            latestInjection = latest,
            timelinePoints = timeline,
            totalInjectionsCount = injections.size,
            nextSuggestedSite = nextSite
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, DashboardState())

    init {
        // One-time clear of any previous dummy/sample data to ensure a pristine blank slate
        val hasClearedInitial = prefs.getBoolean("has_cleared_initial_sample_data", false)
        if (!hasClearedInitial) {
            viewModelScope.launch(Dispatchers.IO) {
                dao.clearAll()
                prefs.edit().putBoolean("has_cleared_initial_sample_data", true).apply()
            }
        }
        recalculateSimulator()
    }

    fun logInjection(
        timestamp: Long,
        ester: EstradiolEster,
        doseMg: Double,
        site: InjectionSite,
        notes: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val entity = InjectionEntity(
                timestamp = timestamp,
                esterCode = ester.code,
                doseMg = doseMg,
                siteId = site.idName,
                notes = notes
            )
            dao.insert(entity)
            scheduleNotificationForNextDose()
            WidgetUpdateHelper.updateAllWidgets(getApplication())
        }
    }

    fun updateInjection(injection: InjectionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.update(injection)
            scheduleNotificationForNextDose()
            WidgetUpdateHelper.updateAllWidgets(getApplication())
        }
    }

    fun deleteInjection(injection: InjectionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.delete(injection)
            scheduleNotificationForNextDose()
            WidgetUpdateHelper.updateAllWidgets(getApplication())
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            dao.clearAll()
            ReminderScheduler.cancelReminder(getApplication())
            WidgetUpdateHelper.updateAllWidgets(getApplication())
        }
    }

    fun updateRegimen(
        ester: EstradiolEster,
        doseMg: Double,
        intervalDays: Double,
        preferredHour: Int,
        preferredMinute: Int
    ) {
        val updated = _regimen.value.copy(
            ester = ester,
            doseMg = doseMg,
            intervalDays = intervalDays,
            preferredHour = preferredHour,
            preferredMinute = preferredMinute
        )
        _regimen.value = updated
        saveRegimen(updated)
        scheduleNotificationForNextDose()
        WidgetUpdateHelper.updateAllWidgets(getApplication())
    }

    fun updateSettings(newSettings: UserSettings) {
        _settings.value = newSettings
        saveSettings(newSettings)
        recalculateSimulator()
        scheduleNotificationForNextDose()
        WidgetUpdateHelper.updateAllWidgets(getApplication())
    }

    // Simulator parameter changes
    fun setSimulatorEster(ester: EstradiolEster) {
        _simulatorState.value = _simulatorState.value.copy(
            ester = ester,
            doseMg = ester.typicalDoseMg,
            intervalDays = ester.typicalIntervalDays
        )
        recalculateSimulator()
    }

    fun setSimulatorDose(doseMg: Double) {
        _simulatorState.value = _simulatorState.value.copy(doseMg = doseMg.coerceAtLeast(0.5))
        recalculateSimulator()
    }

    fun setSimulatorInterval(intervalDays: Double) {
        _simulatorState.value = _simulatorState.value.copy(intervalDays = intervalDays.coerceAtLeast(1.0))
        recalculateSimulator()
    }

    fun setSimulatorSteadyState(isSteadyState: Boolean) {
        prefs.edit().putBoolean("sim_steady_state", isSteadyState).apply()
        _simulatorState.value = _simulatorState.value.copy(isSteadyState = isSteadyState)
        recalculateSimulator()
    }

    fun toggleSimulatorComparison(enable: Boolean) {
        _simulatorState.value = _simulatorState.value.copy(isComparing = enable)
        recalculateSimulator()
    }

    fun setCompareEster(ester: EstradiolEster) {
        _simulatorState.value = _simulatorState.value.copy(
            compareEster = ester,
            compareDoseMg = ester.typicalDoseMg,
            compareIntervalDays = ester.typicalIntervalDays
        )
        recalculateSimulator()
    }

    fun setCompareDose(doseMg: Double) {
        _simulatorState.value = _simulatorState.value.copy(compareDoseMg = doseMg.coerceAtLeast(0.5))
        recalculateSimulator()
    }

    fun setCompareInterval(intervalDays: Double) {
        _simulatorState.value = _simulatorState.value.copy(compareIntervalDays = intervalDays.coerceAtLeast(1.0))
        recalculateSimulator()
    }

    private fun recalculateSimulator() {
        val sim = _simulatorState.value
        val factor = _settings.value.calibrationFactor

        val pts = PharmacokineticsEngine.generateSimulationPoints(
            ester = sim.ester,
            doseMg = sim.doseMg,
            intervalDays = sim.intervalDays,
            totalDays = sim.totalDays,
            stepHours = 2.0,
            steadyStateOnly = sim.isSteadyState,
            calibrationFactor = factor
        )
        val metrics = PharmacokineticsEngine.calculateSteadyStateMetrics(
            ester = sim.ester,
            doseMg = sim.doseMg,
            intervalDays = sim.intervalDays,
            calibrationFactor = factor
        )

        var compPts = emptyList<DataPoint>()
        var compMetrics = sim.compareMetrics
        if (sim.isComparing) {
            compPts = PharmacokineticsEngine.generateSimulationPoints(
                ester = sim.compareEster,
                doseMg = sim.compareDoseMg,
                intervalDays = sim.compareIntervalDays,
                totalDays = sim.totalDays,
                stepHours = 2.0,
                steadyStateOnly = sim.isSteadyState,
                calibrationFactor = factor
            )
            compMetrics = PharmacokineticsEngine.calculateSteadyStateMetrics(
                ester = sim.compareEster,
                doseMg = sim.compareDoseMg,
                intervalDays = sim.compareIntervalDays,
                calibrationFactor = factor
            )
        }

        _simulatorState.value = sim.copy(
            points = pts,
            metrics = metrics,
            comparePoints = compPts,
            compareMetrics = compMetrics
        )
    }

    private fun getNextSiteInRotation(lastSite: InjectionSite?, enabledSites: Set<InjectionSite>): InjectionSite {
        val activeSites = InjectionSite.entries.filter { it in enabledSites }.ifEmpty { InjectionSite.entries }
        if (lastSite == null || lastSite !in activeSites) {
            return activeSites.first()
        }
        val nextIdx = (activeSites.indexOf(lastSite) + 1) % activeSites.size
        return activeSites[nextIdx]
    }

    fun scheduleNotificationForNextDose() {
        val currentSettings = _settings.value
        if (!currentSettings.reminderEnabled) {
            ReminderScheduler.cancelReminder(getApplication())
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val latest = dao.getLatestInjection()
            val reg = _regimen.value
            if (latest != null) {
                val intervalMillis = (reg.intervalDays * 24 * 3600 * 1000).toLong()
                var targetTime = latest.timestamp + intervalMillis

                // Adjust to user's preferred hour and minute
                val cal = Calendar.getInstance().apply {
                    timeInMillis = targetTime
                    set(Calendar.HOUR_OF_DAY, reg.preferredHour)
                    set(Calendar.MINUTE, reg.preferredMinute)
                    set(Calendar.SECOND, 0)
                }
                targetTime = cal.timeInMillis

                // Subtract advance notice if configured
                val triggerTime = targetTime - (currentSettings.advanceReminderHours * 3600L * 1000L)

                ReminderScheduler.scheduleNextReminder(
                    context = getApplication(),
                    triggerTimeMillis = triggerTime,
                    esterName = reg.ester.displayName,
                    doseMg = reg.doseMg
                )
            } else {
                ReminderScheduler.cancelReminder(getApplication())
            }
        }
    }

    // File-based Backup & Restore with Optional AES-GCM Encryption
    fun createBackupFileBytes(password: String?): ByteArray {
        val injections = injectionsList.value
        val settings = _settings.value
        val regimen = _regimen.value
        return BackupCryptoManager.createBackupBytes(injections, settings, regimen, password)
    }

    suspend fun restoreBackupFileBytes(bytes: ByteArray, password: String?): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                val payloadResult = BackupCryptoManager.decryptAndParse(bytes, password)
                if (payloadResult.isFailure) {
                    return@withContext Result.failure(payloadResult.exceptionOrNull() ?: Exception("Failed to decrypt backup"))
                }
                val payload = payloadResult.getOrThrow()
                if (payload.injections.isNotEmpty()) {
                    dao.insertAll(payload.injections)
                }
                saveSettings(payload.settings)
                _settings.value = payload.settings
                saveRegimen(payload.regimen)
                _regimen.value = payload.regimen

                scheduleNotificationForNextDose()
                WidgetUpdateHelper.updateAllWidgets(getApplication())
                Result.success(payload.injections.size)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // JSON Export (Legacy)
    fun exportBackupJson(): String {
        val injections = injectionsList.value
        val root = JSONObject()
        root.put("version", 1)
        root.put("appName", "EstroCurve")
        root.put("exportedAt", System.currentTimeMillis())

        val arr = JSONArray()
        for (inj in injections) {
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

    // JSON Import
    fun importBackupJson(jsonString: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val root = JSONObject(jsonString)
                val arr = root.getJSONArray("injections")
                val imported = mutableListOf<InjectionEntity>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    imported.add(
                        InjectionEntity(
                            id = 0L, // re-generate ID to avoid clashes
                            timestamp = obj.getLong("timestamp"),
                            esterCode = obj.getString("esterCode"),
                            doseMg = obj.getDouble("doseMg"),
                            siteId = obj.getString("siteId"),
                            notes = obj.optString("notes", "")
                        )
                    )
                }
                if (imported.isNotEmpty()) {
                    dao.insertAll(imported)
                    scheduleNotificationForNextDose()
                    WidgetUpdateHelper.updateAllWidgets(getApplication())
                }
            } catch (_: Exception) {
                // Ignore parse errors gracefully
            }
        }
    }

    private fun loadSettings(): UserSettings {
        val e2 = prefs.getString("e2Unit", "PG_ML") ?: "PG_ML"
        val t = prefs.getString("tUnit", "NG_DL") ?: "NG_DL"
        val minE2 = prefs.getFloat("targetE2Min", 200f).toDouble()
        val maxE2 = prefs.getFloat("targetE2Max", 350f).toDouble()
        val rem = prefs.getBoolean("reminderEnabled", true)
        val adv = prefs.getInt("advanceHours", 0)
        val cal = prefs.getFloat("calibrationFactor", 1.0f).toDouble()
        val sitesStr = prefs.getString("enabledSites", null)
        val enabledSites = if (sitesStr.isNullOrEmpty()) {
            InjectionSite.entries.toSet()
        } else {
            sitesStr.split(",").mapNotNull { name ->
                try { InjectionSite.valueOf(name.trim()) } catch (_: Exception) { null }
            }.toSet().ifEmpty { InjectionSite.entries.toSet() }
        }

        val themeModeStr = prefs.getString("themeMode", "OLED_BLACK") ?: "OLED_BLACK"
        val themeMode = try {
            AppThemeMode.valueOf(themeModeStr)
        } catch (_: Exception) {
            AppThemeMode.OLED_BLACK
        }

        return UserSettings(
            e2Unit = if (e2 == "PMOL_L") E2Unit.PMOL_L else E2Unit.PG_ML,
            tUnit = if (t == "NMOL_L") TUnit.NMOL_L else TUnit.NG_DL,
            targetE2Min = minE2,
            targetE2Max = maxE2,
            reminderEnabled = rem,
            advanceReminderHours = adv,
            calibrationFactor = cal,
            enabledSites = enabledSites,
            themeMode = themeMode
        )
    }

    private fun saveSettings(s: UserSettings) {
        prefs.edit()
            .putString("e2Unit", s.e2Unit.name)
            .putString("tUnit", s.tUnit.name)
            .putFloat("targetE2Min", s.targetE2Min.toFloat())
            .putFloat("targetE2Max", s.targetE2Max.toFloat())
            .putBoolean("reminderEnabled", s.reminderEnabled)
            .putInt("advanceHours", s.advanceReminderHours)
            .putFloat("calibrationFactor", s.calibrationFactor.toFloat())
            .putString("enabledSites", s.enabledSites.joinToString(",") { it.name })
            .putString("themeMode", s.themeMode.name)
            .apply()
    }

    private fun loadRegimen(): RegimenProfile {
        val code = prefs.getString("reg_ester", "EV") ?: "EV"
        val dose = prefs.getFloat("reg_dose", 4.0f).toDouble()
        val interval = prefs.getFloat("reg_interval", 5.0f).toDouble()
        val hour = prefs.getInt("reg_hour", 9)
        val minute = prefs.getInt("reg_minute", 0)

        return RegimenProfile(
            ester = EstradiolEster.fromCode(code),
            doseMg = dose,
            intervalDays = interval,
            preferredHour = hour,
            preferredMinute = minute
        )
    }

    private fun saveRegimen(r: RegimenProfile) {
        prefs.edit()
            .putString("reg_ester", r.ester.code)
            .putFloat("reg_dose", r.doseMg.toFloat())
            .putFloat("reg_interval", r.intervalDays.toFloat())
            .putInt("reg_hour", r.preferredHour)
            .putInt("reg_minute", r.preferredMinute)
            .apply()
    }
}
