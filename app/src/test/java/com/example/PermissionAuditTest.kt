package com.example

import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PermissionAuditTest {

  @Test
  fun `verify network and nearby device permissions are completely excluded`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val packageInfo = context.packageManager.getPackageInfo(
      context.packageName,
      PackageManager.GET_PERMISSIONS
    )
    val requestedPermissions = packageInfo.requestedPermissions?.toSet() ?: emptySet()

    // Assert network permissions are absent
    assertFalse("INTERNET permission must not be present", requestedPermissions.contains("android.permission.INTERNET"))
    assertFalse("ACCESS_NETWORK_STATE permission must not be present", requestedPermissions.contains("android.permission.ACCESS_NETWORK_STATE"))
    assertFalse("READ_GSERVICES permission must not be present", requestedPermissions.contains("com.google.android.providers.gsf.permission.READ_GSERVICES"))

    // Assert Bluetooth and nearby device permissions are absent
    assertFalse("BLUETOOTH permission must not be present", requestedPermissions.contains("android.permission.BLUETOOTH"))
    assertFalse("BLUETOOTH_ADMIN permission must not be present", requestedPermissions.contains("android.permission.BLUETOOTH_ADMIN"))
    assertFalse("BLUETOOTH_SCAN permission must not be present", requestedPermissions.contains("android.permission.BLUETOOTH_SCAN"))
    assertFalse("BLUETOOTH_CONNECT permission must not be present", requestedPermissions.contains("android.permission.BLUETOOTH_CONNECT"))
    assertFalse("BLUETOOTH_ADVERTISE permission must not be present", requestedPermissions.contains("android.permission.BLUETOOTH_ADVERTISE"))
    assertFalse("NEARBY_WIFI_DEVICES permission must not be present", requestedPermissions.contains("android.permission.NEARBY_WIFI_DEVICES"))

    // Verify legitimate local permissions remain intact
    assertTrue("POST_NOTIFICATIONS should remain for injection reminders", requestedPermissions.contains("android.permission.POST_NOTIFICATIONS"))
    assertTrue("RECEIVE_BOOT_COMPLETED should remain for rescheduling alarms", requestedPermissions.contains("android.permission.RECEIVE_BOOT_COMPLETED"))
    assertTrue("VIBRATE should remain for alarms/alerts", requestedPermissions.contains("android.permission.VIBRATE"))
  }
}
