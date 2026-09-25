package com.amitr.buyvsrentcalc.domain.engine

import com.amitr.buyvsrentcalc.domain.model.FinancialWinner
import com.amitr.buyvsrentcalc.domain.model.HomeCalculationResult
import com.amitr.buyvsrentcalc.domain.model.HomeDecisionConfig
import com.amitr.buyvsrentcalc.domain.model.MonthlyComparisonResult
import com.amitr.buyvsrentcalc.domain.model.YearlyComparisonSummary
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

object HomeCalculatorEngine {

    fun calculate(config: HomeDecisionConfig): HomeCalculationResult {
        val buy = config.buyParams
        val rent = config.rentParams
        val invest = config.investmentParams

        val totalMonths = config.evaluationHorizonYears * 12
        val loanTenureMonths = buy.loanTenureYears * 12

        val monthlyInterestRateLoan = (buy.loanInterestRateAnnual / 100.0) / 12.0
        val monthlyEmi = if (buy.isLoanSelected && buy.loanPrincipal > 0 && loanTenureMonths > 0) {
            calculateEmi(buy.loanPrincipal, monthlyInterestRateLoan, loanTenureMonths)
        } else {
            0.0
        }

        var remainingLoanPrincipal = if (buy.isLoanSelected) buy.loanPrincipal else 0.0
        var currentPropertyValue = buy.propertyPrice

        val monthlyPropertyAppreciationRate = (buy.propertyAppreciationRateAnnual / 100.0) / 12.0

        val compoundingIntervalMonths = maxOf(1, invest.compoundingFrequency.intervalMonths)
        val compoundingPeriodRate = (invest.annualReturnRatePercent / 100.0) / (12.0 / compoundingIntervalMonths)

        val initialSavingsLumpSum = max(0.0, buy.initialOutflow - rent.initialOutflow)
        var portfolioValue = initialSavingsLumpSum
        var totalInvestedCapital = initialSavingsLumpSum

        var cumulativeBuyOutflow = buy.initialOutflow
        var cumulativeRentOutflow = rent.initialOutflow

        val monthlySchedule = ArrayList<MonthlyComparisonResult>(totalMonths)
        var breakEvenMonth: Int? = null
        var initialBuyHigher: Boolean? = null

        for (m in 1..totalMonths) {
            val yearIndex = ((m - 1) / 12) + 1

            // 1. Loan Amortization & Part Prepayments
            val interestForMonth: Double
            val principalForMonth: Double
            val emiPaidForMonth: Double

            if (buy.isLoanSelected && m <= loanTenureMonths && remainingLoanPrincipal > 0) {
                interestForMonth = remainingLoanPrincipal * monthlyInterestRateLoan
                val nominalPrincipal = monthlyEmi - interestForMonth

                val extraPrepaymentThisMonth = buy.getPartPrepaymentForMonth(m)

                val totalPrincipalAttempt = max(0.0, nominalPrincipal) + extraPrepaymentThisMonth
                principalForMonth = min(remainingLoanPrincipal, totalPrincipalAttempt)
                emiPaidForMonth = principalForMonth + interestForMonth
                remainingLoanPrincipal = max(0.0, remainingLoanPrincipal - principalForMonth)
            } else {
                interestForMonth = 0.0
                principalForMonth = 0.0
                emiPaidForMonth = 0.0
            }

            // 2. Property Value Growth
            currentPropertyValue *= (1.0 + monthlyPropertyAppreciationRate)

            // 3. Maintenance, Upkeep, Taxes
            val maintenanceEscalationFactor = (1.0 + (buy.maintenanceEscalationRateAnnual / 100.0)).pow(yearIndex - 1)
            val currentMonthlyMaintenance = buy.monthlySocietyMaintenance * maintenanceEscalationFactor
            val currentMonthlyUpkeep = (buy.propertyUpkeepAnnualAmount * maintenanceEscalationFactor) / 12.0
            val currentMonthlyTaxAndInsurance = (buy.propertyTaxAnnualAmount * maintenanceEscalationFactor) / 12.0

            val monthlyBuyOutflow = emiPaidForMonth + currentMonthlyMaintenance + currentMonthlyUpkeep + currentMonthlyTaxAndInsurance
            cumulativeBuyOutflow += monthlyBuyOutflow

            val buyNetWorth = currentPropertyValue - remainingLoanPrincipal

            // 4. Rent Outflow, Relocation & Annual Brokerage
            val rentEscalationFactor = (1.0 + (rent.rentEscalationRateAnnual / 100.0)).pow(yearIndex - 1)
            val currentMonthlyRent = rent.initialMonthlyRent * rentEscalationFactor

            val relocationChargeForMonth = if (m % 12 == 0 && rent.annualRelocationCost > 0) {
                rent.annualRelocationCost * rentEscalationFactor
            } else {
                0.0
            }

            val annualBrokerageForMonth = if (m % 12 == 0 && m < totalMonths && rent.annualRentalBrokerage > 0) {
                rent.annualRentalBrokerage * rentEscalationFactor
            } else {
                0.0
            }

            val monthlyRentOutflow = currentMonthlyRent + rent.monthlyTenantMaintenance + relocationChargeForMonth + annualBrokerageForMonth
            cumulativeRentOutflow += monthlyRentOutflow

            // 5. Investment Portfolio Growth with Compounding Frequency
            if (m % compoundingIntervalMonths == 0) {
                val portfolioGrowth = portfolioValue * compoundingPeriodRate
                portfolioValue += portfolioGrowth
            }

            val monthlyCashDifference = monthlyBuyOutflow - monthlyRentOutflow
            portfolioValue += monthlyCashDifference
            totalInvestedCapital += monthlyCashDifference

            val investmentGains = max(0.0, portfolioValue - totalInvestedCapital)
            val estimatedTax = investmentGains * (invest.taxRatePercent / 100.0)
            val postTaxPortfolioValue = max(0.0, portfolioValue - estimatedTax)

            val rentNetWorth = postTaxPortfolioValue + rent.securityDepositAmount
            val netDifference = buyNetWorth - rentNetWorth

            if (initialBuyHigher == null) {
                initialBuyHigher = buyNetWorth >= rentNetWorth
            } else if (breakEvenMonth == null && (buyNetWorth >= rentNetWorth) != initialBuyHigher) {
                breakEvenMonth = m
            }

            monthlySchedule.add(
                MonthlyComparisonResult(
                    monthIndex = m,
                    yearIndex = yearIndex,
                    emiPaid = emiPaidForMonth,
                    principalPaid = principalForMonth,
                    interestPaid = interestForMonth,
                    remainingLoanBalance = remainingLoanPrincipal,
                    propertyValue = currentPropertyValue,
                    monthlyBuyOutflow = monthlyBuyOutflow,
                    cumulativeBuyOutflow = cumulativeBuyOutflow,
                    buyNetWorth = buyNetWorth,
                    monthlyRentPaid = currentMonthlyRent,
                    monthlyRentOutflow = monthlyRentOutflow,
                    cumulativeRentOutflow = cumulativeRentOutflow,
                    investmentPortfolioValue = postTaxPortfolioValue,
                    rentNetWorth = rentNetWorth,
                    netDifference = netDifference
                )
            )
        }

        // 6. Yearly Summaries
        val yearlySummaries = monthlySchedule.groupBy { it.yearIndex }.map { (yIndex, monthsInYear) ->
            val lastMonth = monthsInYear.last()
            val annualBuyOutflow = monthsInYear.sumOf { it.monthlyBuyOutflow }
            val annualRentOutflow = monthsInYear.sumOf { it.monthlyRentOutflow }

            YearlyComparisonSummary(
                yearIndex = yIndex,
                annualBuyOutflow = annualBuyOutflow,
                annualRentOutflow = annualRentOutflow,
                propertyValueEndYear = lastMonth.propertyValue,
                remainingLoanBalanceEndYear = lastMonth.remainingLoanBalance,
                buyNetWorthEndYear = lastMonth.buyNetWorth,
                investmentPortfolioEndYear = lastMonth.investmentPortfolioValue,
                rentNetWorthEndYear = lastMonth.rentNetWorth,
                netAdvantage = lastMonth.netDifference
            )
        }

        val lastRecord = monthlySchedule.last()
        val finalBuyNW = lastRecord.buyNetWorth
        val finalRentNW = lastRecord.rentNetWorth
        val netDiffFinal = finalBuyNW - finalRentNW

        val winner = when {
            netDiffFinal > 1.0 -> FinancialWinner.BUYING
            netDiffFinal < -1.0 -> FinancialWinner.RENTING
            else -> FinancialWinner.EQUIVALENT
        }

        return HomeCalculationResult(
            monthlySchedule = monthlySchedule,
            yearlySummaries = yearlySummaries,
            breakEvenMonth = breakEvenMonth,
            finalBuyNetWorth = finalBuyNW,
            finalRentNetWorth = finalRentNW,
            netDifferenceFinal = netDiffFinal,
            financialWinner = winner,
            totalPurchaseCost = buy.totalPurchaseCost,
            initialBuyOutflow = buy.initialOutflow,
            loanAmount = if (buy.isLoanSelected) buy.loanPrincipal else 0.0,
            monthlyEmi = monthlyEmi
        )
    }

    private fun calculateEmi(principal: Double, monthlyRate: Double, tenureMonths: Int): Double {
        if (monthlyRate == 0.0) return principal / tenureMonths
        val numerator = principal * monthlyRate * (1.0 + monthlyRate).pow(tenureMonths)
        val denominator = (1.0 + monthlyRate).pow(tenureMonths) - 1.0
        return numerator / denominator
    }
}
