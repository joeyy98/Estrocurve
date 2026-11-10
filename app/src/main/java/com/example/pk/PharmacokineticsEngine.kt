package com.example.pk

import com.example.data.model.EstradiolEster
import com.example.data.model.InjectionEntity
import com.example.data.model.TSuppressionStatus
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow

data class SteadyStateMetrics(
    val peakPgMl: Double,
    val troughPgMl: Double,
    val averagePgMl: Double,
    val peakDayInCycle: Double,
    val fluctuationRatioPercent: Double,
    val daysTo90PercentSteadyState: Double,
    val peakTestosteroneNgDl: Double,
    val troughTestosteroneNgDl: Double
)

data class DataPoint(
    val timeMillis: Long,
    val hoursFromNow: Double,
    val e2PgMl: Double,
    val testosteroneNgDl: Double,
    val isPast: Boolean
)

object PharmacokineticsEngine {

    // Physiological baseline endogenous/adrenal background E2 (pg/mL)
    const val BASELINE_E2_PG_ML = 5.0

    // Monotherapy Testosterone Suppression Model Parameters (Hill equation)
    // T(E2) = T_min + (T_baseline - T_min) / (1 + (E2 / IC50)^h)
    const val T_BASELINE_NG_DL = 550.0  // Typical adult male baseline gonadal T
    const val T_MIN_NG_DL = 15.0       // Adrenal residual androgen production
    const val T_IC50_PG_ML = 75.0      // E2 level providing 50% suppression
    const val T_HILL_EXPONENT = 2.8    // Hill cooperativity coefficient

    /**
     * Estimates serum Testosterone (ng/dL) given circulating Estradiol (pg/mL)
     * based on gonadal negative feedback inhibition during estradiol monotherapy.
     */
    fun estimateTestosteroneNgDl(e2PgMl: Double): Double {
        if (e2PgMl <= 0.0) return T_BASELINE_NG_DL
        val effectiveE2 = max(0.0, e2PgMl)
        val ratio = effectiveE2 / T_IC50_PG_ML
        val denominator = 1.0 + ratio.pow(T_HILL_EXPONENT)
        return T_MIN_NG_DL + (T_BASELINE_NG_DL - T_MIN_NG_DL) / denominator
    }

    /**
     * Calculates the single-dose serum concentration (pg/mL) for an ester at t days post-injection.
     * Uses the 2-compartment depot Bateman function:
     * C(t) = Dose * scale * [ka / (ka - ke)] * (exp(-ke * t) - exp(-ka * t)) * calibrationFactor
     */
    fun singleDoseE2PgMl(
        ester: EstradiolEster,
        doseMg: Double,
        timeDays: Double,
        calibrationFactor: Double = 1.0
    ): Double {
        if (timeDays <= 0.0 || doseMg <= 0.0) return 0.0
        val ka = ester.ka
        val ke = ester.ke
        val diff = ka - ke
        val multiplier = if (kotlin.math.abs(diff) < 1e-6) {
            doseMg * ester.scaleFactor * ka * timeDays * exp(-ke * timeDays)
        } else {
            val factor = (ester.scaleFactor * doseMg * ka) / diff
            factor * (exp(-ke * timeDays) - exp(-ka * timeDays))
        }
        return max(0.0, multiplier * calibrationFactor)
    }

    /**
     * Superposition: sums all past injections to calculate total circulating E2 at a given timestamp.
     * Returns 0.0 if no injections have been logged (clean blank-slate).
     */
    fun calculateSuperpositionE2(
        injections: List<InjectionEntity>,
        targetTimestampMillis: Long,
        calibrationFactor: Double = 1.0
    ): Double {
        if (injections.isEmpty()) return 0.0
        var total = BASELINE_E2_PG_ML
        for (inj in injections) {
            val deltaDays = (targetTimestampMillis - inj.timestamp) / (1000.0 * 60.0 * 60.0 * 24.0)
            if (deltaDays >= 0.0) {
                total += singleDoseE2PgMl(inj.ester, inj.doseMg, deltaDays, calibrationFactor)
            }
        }
        return total
    }

    /**
     * Closed-form analytical solution for infinite repeating periodic injections at steady state:
     * C_ss(t) = Dose * scale * [ka / (ka - ke)] * [ exp(-ke*t)/(1 - exp(-ke*tau)) - exp(-ka*t)/(1 - exp(-ka*tau)) ] * calibrationFactor
     * where tau is intervalDays, and 0 <= t <= tau.
     */
    fun steadyStateE2PgMl(
        ester: EstradiolEster,
        doseMg: Double,
        intervalDays: Double,
        timeInCycleDays: Double,
        calibrationFactor: Double = 1.0
    ): Double {
        if (doseMg <= 0.0 || intervalDays <= 0.0) return 0.0
        val t = timeInCycleDays.coerceIn(0.0, intervalDays)
        val ka = ester.ka
        val ke = ester.ke
        val tau = intervalDays

        val denomKe = 1.0 - exp(-ke * tau)
        val denomKa = 1.0 - exp(-ka * tau)

        val termE = exp(-ke * t) / max(1e-9, denomKe)
        val termA = exp(-ka * t) / max(1e-9, denomKa)

        val diff = ka - ke
        val factor = if (kotlin.math.abs(diff) < 1e-6) {
            ester.scaleFactor * doseMg
        } else {
            (ester.scaleFactor * doseMg * ka) / diff
        }

        val level = factor * (termE - termA) * calibrationFactor
        return max(0.0, level) + BASELINE_E2_PG_ML
    }

    /**
     * Computes comprehensive steady-state metrics for a repeating regimen.
     */
    fun calculateSteadyStateMetrics(
        ester: EstradiolEster,
        doseMg: Double,
        intervalDays: Double,
        calibrationFactor: Double = 1.0
    ): SteadyStateMetrics {
        val tau = max(0.5, intervalDays)
        val ka = ester.ka
        val ke = ester.ke

        // Analytical peak time in cycle
        val denomKe = 1.0 - exp(-ke * tau)
        val denomKa = 1.0 - exp(-ka * tau)
        val arg = (ka * denomKe) / (ke * denomKa)
        val rawPeakTime = if (arg > 0.0 && kotlin.math.abs(ka - ke) > 1e-6) {
            ln(arg) / (ka - ke)
        } else {
            ester.tMaxDays
        }
        val peakDay = rawPeakTime.coerceIn(0.0, tau)

        val peakE2 = steadyStateE2PgMl(ester, doseMg, tau, peakDay, calibrationFactor)
        val troughE2 = steadyStateE2PgMl(ester, doseMg, tau, tau, calibrationFactor)

        // Average level across cycle is integrated dose divided by clearance:
        // C_avg = (dose * scaleFactor * calibrationFactor) / (tau * ke) + baseline
        val averageE2 = ((doseMg * ester.scaleFactor * calibrationFactor) / (tau * ke)) + BASELINE_E2_PG_ML

        val fluctuation = if (averageE2 > 0.0) {
            ((peakE2 - troughE2) / averageE2) * 100.0
        } else 0.0

        val daysTo90Percent = ester.halfLifeDays * 3.322

        // Testosterone suppression levels at peak and trough E2
        val tAtPeak = estimateTestosteroneNgDl(peakE2)
        val tAtTrough = estimateTestosteroneNgDl(troughE2)

        return SteadyStateMetrics(
            peakPgMl = peakE2,
            troughPgMl = troughE2,
            averagePgMl = averageE2,
            peakDayInCycle = peakDay,
            fluctuationRatioPercent = fluctuation,
            daysTo90PercentSteadyState = daysTo90Percent,
            peakTestosteroneNgDl = tAtPeak,
            troughTestosteroneNgDl = tAtTrough
        )
    }

    /**
     * Generates a timeline of calculated E2 & T data points for the actual injection log
     * from (now - pastDays) to (now + futureDays).
     */
    fun generateTimelineDataPoints(
        injections: List<InjectionEntity>,
        nowMillis: Long,
        pastDays: Int = 14,
        futureDays: Int = 7,
        stepHours: Double = 3.0,
        calibrationFactor: Double = 1.0
    ): List<DataPoint> {
        val points = mutableListOf<DataPoint>()
        val startMillis = nowMillis - (pastDays * 24L * 3600L * 1000L)
        val endMillis = nowMillis + (futureDays * 24L * 3600L * 1000L)
        val stepMillis = (stepHours * 3600L * 1000L).toLong()

        var current = startMillis
        while (current <= endMillis) {
            val e2 = calculateSuperpositionE2(injections, current, calibrationFactor)
            val t = estimateTestosteroneNgDl(e2)
            val hoursFromNow = (current - nowMillis) / (3600.0 * 1000.0)

            points.add(
                DataPoint(
                    timeMillis = current,
                    hoursFromNow = hoursFromNow,
                    e2PgMl = e2,
                    testosteroneNgDl = t,
                    isPast = current <= nowMillis
                )
            )
            current += stepMillis
        }
        return points
    }

    /**
     * Generates simulation data points over a custom day range for a repeating regimen.
     */
    fun generateSimulationPoints(
        ester: EstradiolEster,
        doseMg: Double,
        intervalDays: Double,
        totalDays: Double = 28.0,
        stepHours: Double = 2.0,
        steadyStateOnly: Boolean = true,
        calibrationFactor: Double = 1.0
    ): List<DataPoint> {
        val points = mutableListOf<DataPoint>()
        val totalHours = totalDays * 24.0
        var hour = 0.0

        if (steadyStateOnly) {
            while (hour <= totalHours) {
                val dayInCycle = (hour / 24.0) % intervalDays
                val e2 = steadyStateE2PgMl(ester, doseMg, intervalDays, dayInCycle, calibrationFactor)
                val t = estimateTestosteroneNgDl(e2)
                points.add(
                    DataPoint(
                        timeMillis = (hour * 3600.0 * 1000.0).toLong(),
                        hoursFromNow = hour,
                        e2PgMl = e2,
                        testosteroneNgDl = t,
                        isPast = false
                    )
                )
                hour += stepHours
            }
        } else {
            // Sequential multi-dose accumulation starting from dose 0 at day 0
            val numDoses = (totalDays / intervalDays).toInt() + 1
            val doseTimestamps = (0..numDoses).map { it * intervalDays * 24.0 }

            while (hour <= totalHours) {
                var e2 = BASELINE_E2_PG_ML
                for (doseTime in doseTimestamps) {
                    if (hour >= doseTime) {
                        val daysSinceDose = (hour - doseTime) / 24.0
                        e2 += singleDoseE2PgMl(ester, doseMg, daysSinceDose, calibrationFactor)
                    }
                }
                val t = estimateTestosteroneNgDl(e2)
                points.add(
                    DataPoint(
                        timeMillis = (hour * 3600.0 * 1000.0).toLong(),
                        hoursFromNow = hour,
                        e2PgMl = e2,
                        testosteroneNgDl = t,
                        isPast = false
                    )
                )
                hour += stepHours
            }
        }
        return points
    }
}
