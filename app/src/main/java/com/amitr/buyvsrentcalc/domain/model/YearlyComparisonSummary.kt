package com.amitr.buyvsrentcalc.domain.model

data class YearlyComparisonSummary(
    val yearIndex: Int,
    val annualBuyOutflow: Double,
    val annualRentOutflow: Double,
    val propertyValueEndYear: Double,
    val remainingLoanBalanceEndYear: Double,
    val buyNetWorthEndYear: Double,
    val investmentPortfolioEndYear: Double,
    val rentNetWorthEndYear: Double,
    val netAdvantage: Double
)
