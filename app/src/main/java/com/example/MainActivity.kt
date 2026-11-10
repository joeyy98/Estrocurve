package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.InjectionEntity
import com.example.reminder.NotificationHelper
import com.example.ui.EstroViewModel
import com.example.ui.components.ExportImportDialog
import com.example.ui.components.QuickLogDialog
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SimulatorScreen
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EstroCurveTheme
import com.example.ui.theme.EstradiolPink
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

enum class AppTab(val title: String) {
    DASHBOARD("Tracker"),
    SIMULATOR("Simulator"),
    HISTORY("Logs"),
    SETTINGS("Settings")
}

class MainActivity : ComponentActivity() {

    private val selectedTabState = mutableStateOf(AppTab.DASHBOARD)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.createNotificationChannel(this)
        handleWidgetIntent(intent)

        setContent {
            val viewModel: EstroViewModel = viewModel()
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            EstroCurveTheme(themeMode = settings.themeMode) {
                EstroCurveApp(
                    viewModel = viewModel,
                    initialTab = selectedTabState.value,
                    onTabChanged = { selectedTabState.value = it }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWidgetIntent(intent)
    }

    private fun handleWidgetIntent(intent: Intent?) {
        val target = intent?.getStringExtra("TARGET_TAB")
        if (target == "TRACKER") {
            selectedTabState.value = AppTab.DASHBOARD
        }
    }
}

@Composable
fun EstroCurveApp(
    viewModel: EstroViewModel = viewModel(),
    initialTab: AppTab = AppTab.DASHBOARD,
    onTabChanged: (AppTab) -> Unit = {}
) {
    val context = LocalContext.current
    var currentTab by rememberSaveable { mutableStateOf(initialTab) }

    LaunchedEffect(currentTab) {
        onTabChanged(currentTab)
    }

    // Dialog States
    var showLogDialog by remember { mutableStateOf(false) }
    var editingInjection by remember { mutableStateOf<InjectionEntity?>(null) }
    var showExportImportDialog by remember { mutableStateOf(false) }

    // State collections
    val dashboardState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val simulatorState by viewModel.simulatorState.collectAsStateWithLifecycle()
    val injectionsList by viewModel.injectionsList.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val regimen by viewModel.regimen.collectAsStateWithLifecycle()

    // Push notification runtime permission (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.scheduleNotificationForNextDose()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Predictive back / BackHandler: sub-screens go back to Dashboard
    if (currentTab != AppTab.DASHBOARD) {
        BackHandler {
            currentTab = AppTab.DASHBOARD
        }
    }

    val isOled = settings.themeMode == com.example.data.model.AppThemeMode.OLED_BLACK
    val scaffoldBg = if (isOled) Color.Black else androidx.compose.material3.MaterialTheme.colorScheme.background
    val navBarBg = if (isOled) DarkSurface else androidx.compose.material3.MaterialTheme.colorScheme.surface
    val navBorder = if (isOled) DarkBorder else androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant
    val navIndicator = if (isOled) Color(0xFF28101F) else androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer
    val navSelectedColor = if (isOled) EstradiolPink else androidx.compose.material3.MaterialTheme.colorScheme.primary
    val navUnselectedColor = if (isOled) TextMuted else androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant

    Scaffold(
        containerColor = scaffoldBg,
        modifier = Modifier
            .fillMaxSize()
            .background(scaffoldBg),
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(navBarBg)
            ) {
                HorizontalDivider(color = navBorder, thickness = 1.dp)
                NavigationBar(
                    containerColor = navBarBg,
                    tonalElevation = 0.dp,
                    windowInsets = NavigationBarDefaults.windowInsets,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bottom_nav_bar")
                ) {
                    NavigationBarItem(
                        selected = currentTab == AppTab.DASHBOARD,
                        onClick = { currentTab = AppTab.DASHBOARD },
                        icon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ShowChart,
                                contentDescription = "Tracker",
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        label = { Text("Tracker", fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = navSelectedColor,
                            selectedTextColor = navSelectedColor,
                            indicatorColor = navIndicator,
                            unselectedIconColor = navUnselectedColor,
                            unselectedTextColor = navUnselectedColor
                        ),
                        modifier = Modifier.testTag("nav_tab_dashboard")
                    )

                    NavigationBarItem(
                        selected = currentTab == AppTab.SIMULATOR,
                        onClick = { currentTab = AppTab.SIMULATOR },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.AutoGraph,
                                contentDescription = "Simulator",
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        label = { Text("Simulator", fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = navSelectedColor,
                            selectedTextColor = navSelectedColor,
                            indicatorColor = navIndicator,
                            unselectedIconColor = navUnselectedColor,
                            unselectedTextColor = navUnselectedColor
                        ),
                        modifier = Modifier.testTag("nav_tab_simulator")
                    )

                    NavigationBarItem(
                        selected = currentTab == AppTab.HISTORY,
                        onClick = { currentTab = AppTab.HISTORY },
                        icon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.FormatListBulleted,
                                contentDescription = "Logs",
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        label = { Text("Logs", fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = navSelectedColor,
                            selectedTextColor = navSelectedColor,
                            indicatorColor = navIndicator,
                            unselectedIconColor = navUnselectedColor,
                            unselectedTextColor = navUnselectedColor
                        ),
                        modifier = Modifier.testTag("nav_tab_history")
                    )

                    NavigationBarItem(
                        selected = currentTab == AppTab.SETTINGS,
                        onClick = { currentTab = AppTab.SETTINGS },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        label = { Text("Settings", fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = navSelectedColor,
                            selectedTextColor = navSelectedColor,
                            indicatorColor = navIndicator,
                            unselectedIconColor = navUnselectedColor,
                            unselectedTextColor = navUnselectedColor
                        ),
                        modifier = Modifier.testTag("nav_tab_settings")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                AppTab.DASHBOARD -> {
                    DashboardScreen(
                        dashboardState = dashboardState,
                        regimen = regimen,
                        settings = settings,
                        onOpenLogDialog = {
                            editingInjection = null
                            showLogDialog = true
                        },
                        onEditInjection = { inj ->
                            editingInjection = inj
                            showLogDialog = true
                        }
                    )
                }

                AppTab.SIMULATOR -> {
                    SimulatorScreen(
                        simulatorState = simulatorState,
                        settings = settings,
                        onEsterChanged = { viewModel.setSimulatorEster(it) },
                        onDoseChanged = { viewModel.setSimulatorDose(it) },
                        onIntervalChanged = { viewModel.setSimulatorInterval(it) },
                        onSteadyStateToggled = { viewModel.setSimulatorSteadyState(it) },
                        onComparisonToggled = { viewModel.toggleSimulatorComparison(it) },
                        onCompareEsterChanged = { viewModel.setCompareEster(it) },
                        onCompareDoseChanged = { viewModel.setCompareDose(it) },
                        onCompareIntervalChanged = { viewModel.setCompareInterval(it) },
                        onApplyToRegimen = { ester, dose, interval ->
                            viewModel.updateRegimen(
                                ester = ester,
                                doseMg = dose,
                                intervalDays = interval,
                                preferredHour = regimen.preferredHour,
                                preferredMinute = regimen.preferredMinute
                            )
                        }
                    )
                }

                AppTab.HISTORY -> {
                    HistoryScreen(
                        injections = injectionsList,
                        settings = settings,
                        onOpenLogDialog = {
                            editingInjection = null
                            showLogDialog = true
                        },
                        onEditInjection = { inj ->
                            editingInjection = inj
                            showLogDialog = true
                        },
                        onDeleteInjection = { inj ->
                            viewModel.deleteInjection(inj)
                        }
                    )
                }

                AppTab.SETTINGS -> {
                    SettingsScreen(
                        settings = settings,
                        regimen = regimen,
                        onSettingsChanged = { viewModel.updateSettings(it) },
                        onRegimenChanged = {
                            viewModel.updateRegimen(
                                ester = it.ester,
                                doseMg = it.doseMg,
                                intervalDays = it.intervalDays,
                                preferredHour = it.preferredHour,
                                preferredMinute = it.preferredMinute
                            )
                        },
                        onOpenExportImportDialog = {
                            showExportImportDialog = true
                        },
                        onRequestNotificationPermission = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        onClearAllLogs = {
                            viewModel.clearAllLogs()
                        }
                    )
                }
            }
        }
    }

    // Quick Log Dialog
    if (showLogDialog) {
        QuickLogDialog(
            initialEster = regimen.ester,
            initialDoseMg = regimen.doseMg,
            initialSite = dashboardState.nextSuggestedSite,
            enabledSites = settings.enabledSites,
            editingInjection = editingInjection,
            settings = settings,
            onDismiss = {
                showLogDialog = false
                editingInjection = null
            },
            onSave = { timestamp, ester, doseMg, site, notes ->
                if (editingInjection != null) {
                    viewModel.updateInjection(
                        editingInjection!!.copy(
                            timestamp = timestamp,
                            esterCode = ester.code,
                            doseMg = doseMg,
                            siteId = site.idName,
                            notes = notes
                        )
                    )
                } else {
                    viewModel.logInjection(
                        timestamp = timestamp,
                        ester = ester,
                        doseMg = doseMg,
                        site = site,
                        notes = notes
                    )
                }
                showLogDialog = false
                editingInjection = null
            }
        )
    }

    // Export / Import Dialog
    if (showExportImportDialog) {
        ExportImportDialog(
            settings = settings,
            onDismiss = { showExportImportDialog = false },
            onExportBytes = { password -> viewModel.createBackupFileBytes(password) },
            onRestoreBytes = { bytes, password -> viewModel.restoreBackupFileBytes(bytes, password) }
        )
    }
}
