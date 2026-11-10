package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("EstroCurve", appName)
  }

  @Test
  fun `simulator steady state preference persists across viewmodel instances`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val vm1 = com.example.ui.EstroViewModel(app)
    
    // Toggle steady state to false
    vm1.setSimulatorSteadyState(false)
    org.junit.Assert.assertFalse(vm1.simulatorState.value.isSteadyState)

    // Recreate ViewModel (simulating app relaunch)
    val vm2 = com.example.ui.EstroViewModel(app)
    org.junit.Assert.assertFalse(
      "Steady state preference must persist as false upon relaunch",
      vm2.simulatorState.value.isSteadyState
    )

    // Toggle back to true
    vm2.setSimulatorSteadyState(true)
    val vm3 = com.example.ui.EstroViewModel(app)
    org.junit.Assert.assertTrue(
      "Steady state preference must persist as true upon relaunch",
      vm3.simulatorState.value.isSteadyState
    )
  }
}
