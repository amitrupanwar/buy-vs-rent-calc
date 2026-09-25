package com.amitr.buyvsrentcalc.domain.model

enum class CompoundingFrequency(val intervalMonths: Int, val displayName: String) {
    MONTHLY(1, "Monthly"),
    QUARTERLY(3, "Quarterly"),
    SEMI_ANNUAL(6, "Semi-Annual"),
    ANNUAL(12, "Annual")
}
