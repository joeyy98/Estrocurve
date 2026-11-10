package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LiveHormoneWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        if (appWidgetIds.isEmpty()) return
        updateWidgets(context, appWidgetManager, appWidgetIds)
    }

    companion object {
        fun updateWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                val data = WidgetDataCalculator.calculate(context)

                val bgRes = when {
                    data.isOled -> R.drawable.widget_card_background_oled
                    data.isNight -> R.drawable.widget_card_background_dynamic_dark
                    else -> R.drawable.widget_card_background_dynamic_light
                }
                val boxRes = when {
                    data.isOled -> R.drawable.widget_metric_box_oled
                    data.isNight -> R.drawable.widget_metric_box_dynamic_dark
                    else -> R.drawable.widget_metric_box_dynamic_light
                }

                val textMutedColor: Int
                val textPrimaryColor: Int
                val textSecondaryColor: Int
                val e2Color: Int
                val tColor: Int

                if (data.isOled) {
                    textMutedColor = Color.parseColor("#757575")
                    textPrimaryColor = Color.parseColor("#FFFFFF")
                    textSecondaryColor = Color.parseColor("#B0B0B0")
                    e2Color = Color.parseColor("#FF4081")
                    tColor = Color.parseColor("#00E5FF")
                } else if (data.isNight) {
                    textMutedColor = Color.parseColor("#929094")
                    textPrimaryColor = Color.parseColor("#E6E1E5")
                    textSecondaryColor = Color.parseColor("#CAC4D0")
                    e2Color = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        try {
                            ContextCompat.getColor(context, android.R.color.system_accent1_200)
                        } catch (_: Exception) {
                            Color.parseColor("#FF80AB")
                        }
                    } else Color.parseColor("#FF80AB")
                    tColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        try {
                            ContextCompat.getColor(context, android.R.color.system_accent2_200)
                        } catch (_: Exception) {
                            Color.parseColor("#80D8FF")
                        }
                    } else Color.parseColor("#80D8FF")
                } else {
                    // Dynamic Light Mode
                    textMutedColor = Color.parseColor("#79747E")
                    textPrimaryColor = Color.parseColor("#1C1B1F")
                    textSecondaryColor = Color.parseColor("#49454F")
                    e2Color = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        try {
                            ContextCompat.getColor(context, android.R.color.system_accent1_600)
                        } catch (_: Exception) {
                            Color.parseColor("#C2185B")
                        }
                    } else Color.parseColor("#C2185B")
                    tColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        try {
                            ContextCompat.getColor(context, android.R.color.system_accent2_600)
                        } catch (_: Exception) {
                            Color.parseColor("#0097A7")
                        }
                    } else Color.parseColor("#0097A7")
                }

                withContext(Dispatchers.Main) {
                    for (appWidgetId in appWidgetIds) {
                        val views = RemoteViews(context.packageName, R.layout.widget_live_data)

                        // Apply adaptive theme backgrounds
                        views.setInt(R.id.widget_root, "setBackgroundResource", bgRes)
                        views.setInt(R.id.widget_e2_box, "setBackgroundResource", boxRes)
                        views.setInt(R.id.widget_t_box, "setBackgroundResource", boxRes)

                        // Apply adaptive theme typography colors
                        views.setTextColor(R.id.widget_title, textMutedColor)
                        views.setTextColor(R.id.widget_e2_label, e2Color)
                        views.setTextColor(R.id.widget_e2_val, e2Color)
                        views.setTextColor(R.id.widget_e2_unit, textMutedColor)

                        views.setTextColor(R.id.widget_t_label, tColor)
                        views.setTextColor(R.id.widget_t_val, tColor)
                        views.setTextColor(R.id.widget_t_unit, textMutedColor)

                        views.setTextColor(R.id.widget_since_val, textSecondaryColor)
                        views.setTextColor(R.id.widget_next_due_val, textSecondaryColor)
                        views.setTextColor(R.id.widget_next_site_val, textPrimaryColor)

                        // Bind text values
                        views.setTextViewText(R.id.widget_e2_val, data.e2ValueStr)
                        views.setTextViewText(R.id.widget_e2_unit, data.e2UnitStr)
                        views.setTextViewText(R.id.widget_t_val, data.tValueStr)
                        views.setTextViewText(R.id.widget_t_unit, data.tUnitStr)

                        views.setTextViewText(R.id.widget_status_pill, data.statusText)
                        views.setTextColor(R.id.widget_status_pill, data.statusColor)

                        views.setTextViewText(R.id.widget_since_val, data.lastDoseStr)
                        views.setTextViewText(R.id.widget_next_due_val, data.nextDoseStr)
                        views.setTextViewText(R.id.widget_next_site_val, data.nextSiteStr)

                        // Launch Tracker screen on tap
                        val intent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra("TARGET_TAB", "TRACKER")
                        }
                        val pendingIntent = PendingIntent.getActivity(
                            context,
                            appWidgetId,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                }
            }
        }

        fun updateAll(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, LiveHormoneWidgetProvider::class.java)
            val ids = appWidgetManager.getAppWidgetIds(component)
            if (ids.isNotEmpty()) {
                updateWidgets(context, appWidgetManager, ids)
            }
        }
    }
}
