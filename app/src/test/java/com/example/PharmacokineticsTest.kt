package com.example

import com.example.data.model.EstradiolEster
import com.example.data.model.InjectionEntity
import com.example.data.model.TSuppressionStatus
import com.example.pk.PharmacokineticsEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PharmacokineticsTest {

    @Test
    fun testEstradiolValerateSingleDoseKinetics() {
        val doseMg = 4.0
        val ester = EstradiolEster.EV

        // Day 0 should be 0 (or baseline)
        val day0 = PharmacokineticsEngine.singleDoseE2PgMl(ester, doseMg, 0.0)
        assertEquals(0.0, day0, 0.01)

        // Peak should be around 1.5 - 2.5 days
        val day1 = PharmacokineticsEngine.singleDoseE2PgMl(ester, doseMg, 1.0)
        val day2 = PharmacokineticsEngine.singleDoseE2PgMl(ester, doseMg, 2.0)
        val day5 = PharmacokineticsEngine.singleDoseE2PgMl(ester, doseMg, 5.0)
        val day10 = PharmacokineticsEngine.singleDoseE2PgMl(ester, doseMg, 10.0)

        assertTrue("Day 2 level should be positive and substantial", day2 > 150.0)
        assertTrue("Day 2 should exceed day 5 level", day2 > day5)
        assertTrue("Day 5 should exceed day 10 level", day5 > day10)
    }

    @Test
    fun testTestosteroneMonotherapySuppression() {
        // At 0 pg/mL E2, T should be unsuppressed baseline (~550 ng/dL)
        val tZero = PharmacokineticsEngine.estimateTestosteroneNgDl(0.0)
        assertEquals(550.0, tZero, 5.0)
        assertEquals(TSuppressionStatus.ELEVATED, TSuppressionStatus.fromTestosteroneNgDl(tZero))

        // At 100 pg/mL E2, T should be partially suppressed (~200-300 ng/dL)
        val t100 = PharmacokineticsEngine.estimateTestosteroneNgDl(100.0)
        assertTrue("T at 100 pg/mL should be between 150 and 280 ng/dL", t100 in 150.0..280.0)

        // At 200 pg/mL E2 (clinical monotherapy threshold), T should be suppressed below 50 ng/dL
        val t200 = PharmacokineticsEngine.estimateTestosteroneNgDl(200.0)
        assertTrue("T at 200 pg/mL must be < 50 ng/dL for monotherapy target", t200 < 50.0)
        assertEquals(TSuppressionStatus.SUPPRESSED, TSuppressionStatus.fromTestosteroneNgDl(t200))

        // At 300+ pg/mL E2, T should be tightly suppressed near castrate minimum (~15-25 ng/dL)
        val t350 = PharmacokineticsEngine.estimateTestosteroneNgDl(350.0)
        assertTrue("T at 350 pg/mL should approach castrate minimum (~15-25 ng/dL)", t350 < 25.0)
    }

    @Test
    fun testSteadyStateAnalyticalCalculation() {
        val metrics = PharmacokineticsEngine.calculateSteadyStateMetrics(
            ester = EstradiolEster.EV,
            doseMg = 4.0,
            intervalDays = 5.0
        )

        assertTrue("Peak E2 should be higher than trough E2", metrics.peakPgMl > metrics.troughPgMl)
        assertTrue("Peak E2 should be greater than average E2", metrics.peakPgMl > metrics.averagePgMl)
        assertTrue("Average E2 should be greater than trough E2", metrics.averagePgMl > metrics.troughPgMl)
        assertTrue("Peak day should be between 0 and 5 days", metrics.peakDayInCycle in 0.5..3.0)
        assertTrue("Fluctuation should be positive", metrics.fluctuationRatioPercent > 0.0)

        // Calibrated to injectable pharmacokinetic benchmarks: Peak ~360 pg/mL, Trough ~215 pg/mL
        assertTrue("Calibrated peak E2 for 4mg/5d EV must be ~350-375 pg/mL (got ${metrics.peakPgMl})",
            metrics.peakPgMl in 350.0..375.0)
        assertTrue("Calibrated trough E2 for 4mg/5d EV must be ~200-225 pg/mL (got ${metrics.troughPgMl})",
            metrics.troughPgMl in 200.0..225.0)
    }

    @Test
    fun testEmptyInjectionsBlankSlate() {
        val emptyResult = PharmacokineticsEngine.calculateSuperpositionE2(emptyList(), System.currentTimeMillis())
        assertEquals(0.0, emptyResult, 0.0)
    }

    @Test
    fun testSuperpositionPrinciple() {
        val now = System.currentTimeMillis()
        val dayMillis = 24L * 3600L * 1000L

        val injections = listOf(
            InjectionEntity(
                timestamp = now - (10L * dayMillis),
                esterCode = "EV",
                doseMg = 4.0,
                siteId = "LEFT_THIGH"
            ),
            InjectionEntity(
                timestamp = now - (5L * dayMillis),
                esterCode = "EV",
                doseMg = 4.0,
                siteId = "RIGHT_THIGH"
            )
        )

        val totalNow = PharmacokineticsEngine.calculateSuperpositionE2(injections, now)
        // Should include contributions from both injections plus baseline
        val single5Days = PharmacokineticsEngine.singleDoseE2PgMl(EstradiolEster.EV, 4.0, 5.0)
        val single10Days = PharmacokineticsEngine.singleDoseE2PgMl(EstradiolEster.EV, 4.0, 10.0)
        val expected = PharmacokineticsEngine.BASELINE_E2_PG_ML + single5Days + single10Days

        assertEquals(expected, totalNow, 0.001)
    }

    @Test
    fun testInjectionSiteRotation() {
        val thighOnly = setOf(com.example.data.model.InjectionSite.LEFT_THIGH, com.example.data.model.InjectionSite.RIGHT_THIGH)
        val activeSites = com.example.data.model.InjectionSite.entries.filter { it in thighOnly }

        // Cycle from LEFT_THIGH -> RIGHT_THIGH
        val nextAfterLeft = activeSites[(activeSites.indexOf(com.example.data.model.InjectionSite.LEFT_THIGH) + 1) % activeSites.size]
        assertEquals(com.example.data.model.InjectionSite.RIGHT_THIGH, nextAfterLeft)

        // Cycle from RIGHT_THIGH -> LEFT_THIGH
        val nextAfterRight = activeSites[(activeSites.indexOf(com.example.data.model.InjectionSite.RIGHT_THIGH) + 1) % activeSites.size]
        assertEquals(com.example.data.model.InjectionSite.LEFT_THIGH, nextAfterRight)
    }

    @Test
    fun testTimelineDataPointGenerationForWidget() {
        val now = System.currentTimeMillis()
        val dayMillis = 24L * 3600L * 1000L
        val injections = listOf(
            InjectionEntity(
                timestamp = now - (3L * dayMillis),
                esterCode = "EV",
                doseMg = 4.0,
                siteId = "LEFT_THIGH"
            )
        )
        val points = PharmacokineticsEngine.generateTimelineDataPoints(
            injections = injections,
            nowMillis = now,
            pastDays = 7,
            futureDays = 3,
            stepHours = 2.0
        )
        assertTrue("Timeline should contain points covering 10 days", points.size > 100)
        val firstPoint = points.first()
        val lastPoint = points.last()
        assertTrue("First point should be in the past", firstPoint.isPast)
        assertTrue("Last point should be in the future", !lastPoint.isPast)
    }
}

