package com.amitr.buyvsrentcalc.domain.model

data class MonthlyComparisonResult(
    val monthIndex: Int,
    val yearIndex: Int,
    val emiPaid: Double,
    val principalPaid: Double,
    val interestPaid: Double,
    val partPrepaymentPaid: Double,
    val remainingLoanBalance: Double,
    val propertyValue: Double,
    val monthlyBuyOutflow: Double,
    val cumulativeBuyOutflow: Double,
    val buyNetWorth: Double,
    val monthlyRentPaid: Double,
    val monthlyRentOutflow: Double,
    val cumulativeRentOutflow: Double,
    val investmentPortfolioValue: Double,
    val rentNetWorth: Double,
    val netDifference: Double
)
