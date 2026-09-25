package com.amitr.buyvsrentcalc.domain.model

data class HomeCalculationResult(
    val monthlySchedule: List<MonthlyComparisonResult>,
    val yearlySummaries: List<YearlyComparisonSummary>,
    val breakEvenMonth: Int?,
    val finalBuyNetWorth: Double,
    val finalRentNetWorth: Double,
    val netDifferenceFinal: Double,
    val financialWinner: FinancialWinner,
    val totalPurchaseCost: Double,
    val initialBuyOutflow: Double,
    val loanAmount: Double,
    val monthlyEmi: Double
)
