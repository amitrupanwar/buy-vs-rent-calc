package com.amitr.buyvsrentcalc.domain.model

data class InvestmentParams(
    val investmentType: InvestmentType = InvestmentType.MUTUAL_FUNDS,
    val annualReturnRatePercent: Double = 7.0,
    val taxRatePercent: Double = 20.0,
    val compoundingFrequency: CompoundingFrequency = CompoundingFrequency.MONTHLY
)
