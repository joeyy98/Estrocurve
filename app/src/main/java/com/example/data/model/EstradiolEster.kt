package com.example.data.model

/**
 * Standard estradiol esters calibrated to injectable pharmacokinetic benchmarks.
 *
 * Pharmacokinetic parameters derived from empirical studies and 2-compartment depot model:
 * C(t) = doseMg * scale * (ka / (ka - ke)) * (exp(-ke * t) - exp(-ka * t))
 */
enum class EstradiolEster(
    val code: String,
    val displayName: String,
    val typicalDoseMg: Double,
    val typicalIntervalDays: Double,
    val tMaxDays: Double,
    val halfLifeDays: Double,
    val ka: Double,          // depot absorption rate constant (1/day)
    val ke: Double,          // terminal elimination rate constant (1/day)
    val scaleFactor: Double, // concentration scale multiplier (pg/mL per mg administered)
    val description: String
) {
    EV(
        code = "EV",
        displayName = "Estradiol Valerate",
        typicalDoseMg = 4.0,
        typicalIntervalDays = 5.0,
        tMaxDays = 1.3,
        halfLifeDays = 4.0,
        ka = 1.40,
        ke = 0.1733,
        scaleFactor = 65.0,
        description = "Short-to-medium acting ester. Common in oil (IM/SubQ). Fast absorption with marked peaks; 4mg/5d yields Peak ~361 pg/mL and Trough ~215 pg/mL."
    ),
    EC(
        code = "EC",
        displayName = "Estradiol Cypionate",
        typicalDoseMg = 4.0,
        typicalIntervalDays = 7.0,
        tMaxDays = 3.5,
        halfLifeDays = 8.0,
        ka = 0.40,
        ke = 0.0866,
        scaleFactor = 42.0,
        description = "Medium-to-long acting ester. Smoother curve and flatter peak than Valerate; typically injected weekly."
    ),
    EEn(
        code = "EEn",
        displayName = "Estradiol Enanthate",
        typicalDoseMg = 5.0,
        typicalIntervalDays = 7.0,
        tMaxDays = 3.2,
        halfLifeDays = 7.0,
        ka = 0.48,
        ke = 0.0990,
        scaleFactor = 46.0,
        description = "Long-acting ester. Very stable sustained levels with gentle peaks and troughs; popular for 7-day to 10-day cycles."
    ),
    EB(
        code = "EB",
        displayName = "Estradiol Benzoate",
        typicalDoseMg = 2.0,
        typicalIntervalDays = 3.0,
        tMaxDays = 0.9,
        halfLifeDays = 1.5,
        ka = 2.00,
        ke = 0.4620,
        scaleFactor = 110.0,
        description = "Rapid short-acting ester. Produces sharp high peaks within 24h and clears quickly; requires dosing every 2 to 3 days."
    ),
    EU(
        code = "EU",
        displayName = "Estradiol Undecylate",
        typicalDoseMg = 16.0,
        typicalIntervalDays = 21.0,
        tMaxDays = 12.0,
        halfLifeDays = 28.0,
        ka = 0.075,
        ke = 0.0247,
        scaleFactor = 15.0,
        description = "Ultra-long-acting depot ester. Very flat, stable release spanning multiple weeks to a month between injections."
    );

    companion object {
        fun fromCode(code: String): EstradiolEster =
            entries.find { it.code.equals(code, ignoreCase = true) } ?: EV
    }
}
