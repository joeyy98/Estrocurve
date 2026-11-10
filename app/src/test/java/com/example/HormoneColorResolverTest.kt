package com.example

import android.content.Context
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.example.ui.theme.HormoneColorResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HormoneColorResolverTest {

    @Test
    fun `oled mode resolves pure neon pink and cyan`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val colors = HormoneColorResolver.resolve(context, isOled = true, isNight = true)
        assertEquals(Color.parseColor("#FF2D78"), colors.e2Argb)
        assertEquals(Color.parseColor("#00E5FF"), colors.tArgb)
        assertNotEquals(colors.e2Argb, colors.tArgb)
    }

    @Test
    fun `dynamic dark mode guarantees distinct colors between E2 and T`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val colors = HormoneColorResolver.resolve(context, isOled = false, isNight = true)
        assertNotEquals("E2 and T must not share the same color in dark mode", colors.e2Argb, colors.tArgb)
        assertNotEquals("Comparison curve must not collide with E2", colors.e2Argb, colors.compareArgb)
        assertNotEquals("Comparison curve must not collide with T", colors.tArgb, colors.compareArgb)
    }

    @Test
    fun `dynamic light mode guarantees distinct colors between E2 and T`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val colors = HormoneColorResolver.resolve(context, isOled = false, isNight = false)
        assertNotEquals("E2 and T must not share the same color in light mode", colors.e2Argb, colors.tArgb)
        assertNotEquals("Comparison curve must not collide with E2 in light mode", colors.e2Argb, colors.compareArgb)
        assertNotEquals("Comparison curve must not collide with T in light mode", colors.tArgb, colors.compareArgb)
    }
}
