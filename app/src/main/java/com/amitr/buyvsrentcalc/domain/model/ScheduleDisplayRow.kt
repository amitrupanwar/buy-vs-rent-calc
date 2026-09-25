package com.amitr.buyvsrentcalc.domain.model

data class ScheduleDisplayRow(
    val periodLabel: String,
    val isYearly: Boolean,
    val isLoanSelected: Boolean,
    val periodIndex: Int,
    val yearIndex: Int,
    val isExpanded: Boolean = false,
    val isTally: Boolean = false,
    // Buy Section
    val emiPaid: Double,
    val principalPaid: Double,
    val interestPaid: Double,
    val remainingLoan: Double,
    val buyExpenses: Double,
    val propertyValue: Double,
    val moneyOutflowBuy: Double,
    val cumulativeBuyOutflow: Double,
    val buyNetWorth: Double,
    // Rent & Invest Section
    val monthlyRentPaid: Double,
    val moneyOutflowRent: Double,
    val moneyInjected: Double,
    val investmentPortfolioValue: Double,
    val cumulativeRentOutflow: Double,
    val rentNetWorth: Double,
    // Comparison
    val netAdvantage: Double
)
