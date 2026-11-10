package com.example

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.reminder.NotificationHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NotificationHelperTest {

    @Test
    fun `reminder notification has expected title and body text`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        NotificationHelper.showReminderNotification(context)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadowManager = Shadows.shadowOf(notificationManager)
        val notifications = shadowManager.allNotifications

        assertEquals(1, notifications.size)
        val notification = notifications[0]
        assertNotNull(notification)

        val extras = notification.extras
        assertEquals("E Injection due", extras.getString("android.title"))
        assertEquals("Time for your E injection", extras.getString("android.text"))
    }
}
