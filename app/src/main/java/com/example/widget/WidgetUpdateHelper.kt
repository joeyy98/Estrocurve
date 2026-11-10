package com.example.widget

import android.content.Context

object WidgetUpdateHelper {

    /**
     * Broadcasts and triggers refresh on both home screen widgets.
     */
    fun updateAllWidgets(context: Context) {
        try {
            LiveHormoneWidgetProvider.updateAll(context)
            DynamicsGraphWidgetProvider.updateAll(context)
        } catch (_: Exception) {
            // Gracefully ignore any widget update exceptions
        }
    }
}
