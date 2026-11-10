package com.example.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getInstance(context)
                    val latest = db.injectionDao().getLatestInjection()
                    if (latest != null) {
                        val intervalMillis = (latest.ester.typicalIntervalDays * 24 * 3600 * 1000).toLong()
                        val nextTime = latest.timestamp + intervalMillis
                        if (nextTime > System.currentTimeMillis()) {
                            ReminderScheduler.scheduleNextReminder(
                                context,
                                nextTime,
                                latest.ester.displayName,
                                latest.doseMg
                            )
                        }
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
