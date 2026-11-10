package com.example.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class InjectionReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val ester = intent?.getStringExtra("ester_name") ?: "Estradiol"
        val dose = intent?.getDoubleExtra("dose_mg", 4.0) ?: 4.0
        NotificationHelper.showReminderNotification(context, ester, dose)
    }
}
