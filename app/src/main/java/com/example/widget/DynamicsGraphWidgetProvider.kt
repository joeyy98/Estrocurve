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

class DynamicsGraphWidgetProvider : AppWidgetProvider() {

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

                val textMutedColor: Int
                val textSecondaryColor: Int
                val e2Color: Int
                val tColor: Int

                if (data.isOled) {
                    textMutedColor = Color.parseColor("#757575")
                    textSecondaryColor = Color.parseColor("#B0B0B0")
                    e2Color = Color.parseColor("#FF4081")
                    tColor = Color.parseColor("#00E5FF")
                } else if (data.isNight) {
                    textMutedColor = Color.parseColor("#929094")
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
                    textMutedColor = Color.parseColor("#79747E")
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

                // Render graph bitmap with theme colors
                val bitmap = WidgetGraphRenderer.render(
                    context = context,
                    dataPoints = data.timelinePoints,
                    targetE2Min = data.targetE2Min,
                    targetE2Max = data.targetE2Max,
                    e2Unit = data.e2Unit,
                    tUnit = data.tUnit,
                    isOled = data.isOled,
                    isNight = data.isNight,
                    width = 760,
                    height = 360
                )

                withContext(Dispatchers.Main) {
                    for (appWidgetId in appWidgetIds) {
                        val views = RemoteViews(context.packageName, R.layout.widget_dynamics_graph)

                        // Apply adaptive theme background
                        views.setInt(R.id.widget_graph_root, "setBackgroundResource", bgRes)

                        // Apply adaptive theme colors to labels
                        views.setTextColor(R.id.widget_graph_title, textMutedColor)
                        views.setTextColor(R.id.widget_graph_subtitle, textSecondaryColor)
                        views.setTextColor(R.id.widget_legend_e2, e2Color)
                        views.setTextColor(R.id.widget_legend_t, tColor)
                        views.setTextColor(R.id.widget_legend_now, textMutedColor)

                        views.setTextViewText(R.id.widget_graph_subtitle, data.subtitleGraphStr)
                        views.setImageViewBitmap(R.id.widget_graph_image, bitmap)

                        // Launch Tracker screen on tap
                        val intent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra("TARGET_TAB", "TRACKER")
                        }
                        val pendingIntent = PendingIntent.getActivity(
                            context,
                            appWidgetId + 1000,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(R.id.widget_graph_root, pendingIntent)

                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                }
            }
        }

        fun updateAll(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, DynamicsGraphWidgetProvider::class.java)
            val ids = appWidgetManager.getAppWidgetIds(component)
            if (ids.isNotEmpty()) {
                updateWidgets(context, appWidgetManager, ids)
            }
        }
    }
}
