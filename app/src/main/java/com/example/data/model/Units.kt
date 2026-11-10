package com.example.data.model

import java.util.Locale

/**
 * Units of measurement for Estradiol (E2)
 * 1 pg/mL = 3.671 pmol/L
 */
enum class E2Unit(val label: String) {
    PG_ML("pg/mL"),
    PMOL_L("pmol/L");

    fun fromPgMl(pgMl: Double): Double = when (this) {
        PG_ML -> pgMl
        PMOL_L -> pgMl * 3.67128
    }

    fun toPgMl(value: Double): Double = when (this) {
        PG_ML -> value
        PMOL_L -> value / 3.67128
    }

    fun format(pgMl: Double): String {
        val converted = fromPgMl(pgMl)
        return String.format(Locale.US, "%.1f %s", converted, label)
    }

    fun formatInteger(pgMl: Double): String {
        val converted = fromPgMl(pgMl)
        return String.format(Locale.US, "%.0f %s", converted, label)
    }
}

/**
 * Units of measurement for Testosterone (T)
 * 1 ng/dL = 0.0347 nmol/L
 */
enum class TUnit(val label: String) {
    NG_DL("ng/dL"),
    NMOL_L("nmol/L");

    fun fromNgDl(ngDl: Double): Double = when (this) {
        NG_DL -> ngDl
        NMOL_L -> ngDl * 0.03467
    }

    fun toNgDl(value: Double): Double = when (this) {
        NG_DL -> value
        NMOL_L -> value / 0.03467
    }

    fun format(ngDl: Double): String {
        val converted = fromNgDl(ngDl)
        return String.format(Locale.US, "%.1f %s", converted, label)
    }

    fun formatInteger(ngDl: Double): String {
        val converted = fromNgDl(ngDl)
        return if (this == NMOL_L) {
            String.format(Locale.US, "%.2f %s", converted, label)
        } else {
            String.format(Locale.US, "%.0f %s", converted, label)
        }
    }
}

/**
 * Gonadal Testosterone suppression classification for Estradiol Monotherapy.
 */
enum class TSuppressionStatus(
    val title: String,
    val description: String
) {
    SUPPRESSED("FULL SUPPRESSION", "T is fully suppressed into typical female castrate range (< 50 ng/dL)"),
    BORDERLINE("BORDERLINE SUPPRESSION", "T is partially suppressed (50 - 100 ng/dL); monitor trough E2"),
    ELEVATED("UNSUPPRESSED / ELEVATED", "T is above monotherapy target (> 100 ng/dL); trough E2 may be too low");

    companion object {
        fun fromTestosteroneNgDl(tNgDl: Double): TSuppressionStatus = when {
            tNgDl < 50.0 -> SUPPRESSED
            tNgDl <= 100.0 -> BORDERLINE
            else -> ELEVATED
        }
    }
}
