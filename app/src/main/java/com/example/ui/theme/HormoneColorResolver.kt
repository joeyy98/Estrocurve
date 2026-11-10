package com.example.ui.theme

import android.content.Context
import android.graphics.Color as AndroidColor
import android.os.Build
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils

data class ResolvedHormoneColors(
    val e2Argb: Int,
    val tArgb: Int,
    val compareArgb: Int
) {
    val e2Color: ComposeColor get() = ComposeColor(e2Argb)
    val tColor: ComposeColor get() = ComposeColor(tArgb)
    val compareColor: ComposeColor get() = ComposeColor(compareArgb)
}

object HormoneColorResolver {

    /**
     * Resolves distinct curve and indicator colors for Estradiol (E2), Testosterone (T),
     * and comparison regimens across OLED Black, colorful dynamic, monochrome, and low-contrast palettes.
     */
    fun resolve(
        context: Context,
        isOled: Boolean,
        isNight: Boolean
    ): ResolvedHormoneColors {
        if (isOled) {
            return ResolvedHormoneColors(
                e2Argb = AndroidColor.parseColor("#FF2D78"),      // EstradiolPink
                tArgb = AndroidColor.parseColor("#00E5FF"),       // TestosteroneCyan
                compareArgb = AndroidColor.parseColor("#AB47BC")  // Comparison Purple
            )
        }

        // High-contrast vibrant fallback colors
        val fallbackE2 = if (isNight) AndroidColor.parseColor("#FF80AB") else AndroidColor.parseColor("#C2185B")
        val fallbackT = if (isNight) AndroidColor.parseColor("#80D8FF") else AndroidColor.parseColor("#0097A7")
        val fallbackComp = if (isNight) AndroidColor.parseColor("#EA80FC") else AndroidColor.parseColor("#7B1FA2")

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return ResolvedHormoneColors(fallbackE2, fallbackT, fallbackComp)
        }

        var rawE2 = fallbackE2
        var rawT = fallbackT
        var rawComp = fallbackComp

        try {
            val e2Res = if (isNight) android.R.color.system_accent1_200 else android.R.color.system_accent1_600
            val tRes = if (isNight) android.R.color.system_accent2_200 else android.R.color.system_accent2_600
            val compRes = if (isNight) android.R.color.system_accent3_200 else android.R.color.system_accent3_600

            rawE2 = ContextCompat.getColor(context, e2Res)
            rawT = ContextCompat.getColor(context, tRes)
            rawComp = ContextCompat.getColor(context, compRes)
        } catch (_: Exception) {
            return ResolvedHormoneColors(fallbackE2, fallbackT, fallbackComp)
        }

        // Analyze saturation in HSV
        val hsvE2 = FloatArray(3).also { AndroidColor.colorToHSV(rawE2, it) }
        val hsvT = FloatArray(3).also { AndroidColor.colorToHSV(rawT, it) }

        // Analyze perceptual distance in CIE-Lab space
        val labE2 = DoubleArray(3)
        val labT = DoubleArray(3)
        ColorUtils.colorToLAB(rawE2, labE2)
        ColorUtils.colorToLAB(rawT, labT)
        val deltaE = ColorUtils.distanceEuclidean(labE2, labT)

        // Low saturation threshold (monochrome theme)
        val isMonochromeE2 = hsvE2[1] < 0.12f
        val isMonochromeT = hsvT[1] < 0.12f

        // Near-identical hue & low deltaE threshold (collision in pastel/single-hue themes)
        val hueDiff = kotlin.math.abs(hsvE2[0] - hsvT[0])
        val areHuesVeryClose = hueDiff < 30f || hueDiff > 330f
        val areColorsTooSimilar = deltaE < 24.0 || (areHuesVeryClose && kotlin.math.abs(hsvE2[1] - hsvT[1]) < 0.20f)

        return when {
            // Case 1: Both accents are monochrome/grayscale
            isMonochromeE2 && isMonochromeT -> {
                ResolvedHormoneColors(
                    e2Argb = fallbackE2,
                    tArgb = fallbackT,
                    compareArgb = fallbackComp
                )
            }

            // Case 2: One accent is monochrome or the two accents are perceptually indistinguishable
            areColorsTooSimilar || isMonochromeE2 || isMonochromeT -> {
                // Keep E2 as rawE2 if it has color, otherwise use fallbackE2
                val chosenE2 = if (isMonochromeE2) fallbackE2 else rawE2
                // Use distinct cyan/teal for T to guarantee high contrast
                val chosenT = fallbackT
                ResolvedHormoneColors(
                    e2Argb = chosenE2,
                    tArgb = chosenT,
                    compareArgb = fallbackComp
                )
            }

            // Case 3: Accents have sufficient perceptual separation
            else -> {
                ResolvedHormoneColors(
                    e2Argb = rawE2,
                    tArgb = rawT,
                    compareArgb = rawComp
                )
            }
        }
    }
}
