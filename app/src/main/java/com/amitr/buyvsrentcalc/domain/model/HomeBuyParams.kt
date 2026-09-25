package com.amitr.buyvsrentcalc.domain.model

import kotlin.math.max

data class HomeBuyParams(
    val propertyPrice: Double,
    val downPaymentValue: Double = 20.0,
    val isDownPaymentPercent: Boolean = true,
    val loanInterestRateAnnual: Double = 8.5,
    val loanTenureYears: Int = 20,
    val upfrontCostPercent: Double = 5.0,
    val isUpfrontCostInclusive: Boolean = false,
    val partPrepaymentAmount: Double = 0.0,
    val partPrepaymentFrequency: PrepaymentFrequency = PrepaymentFrequency.NONE,
    val propertyAppreciationRateAnnual: Double = 5.0,
    val monthlySocietyMaintenance: Double = 3000.0,
    val maintenanceEscalationRateAnnual: Double = 5.0,
    val propertyUpkeepAnnualAmount: Double = 12_000.0,
    val propertyTaxAnnualAmount: Double = 12_000.0,
    val isLoanSelected: Boolean = true
) {
    val upfrontCostsAmount: Double
        get() = if (isUpfrontCostInclusive) 0.0 else propertyPrice * (upfrontCostPercent / 100.0)

    val totalPurchaseCost: Double
        get() = propertyPrice + upfrontCostsAmount

    val basePriceForDownPayment: Double
        get() = propertyPrice

    val downPaymentAmount: Double
        get() = if (isDownPaymentPercent) {
            basePriceForDownPayment * (downPaymentValue / 100.0)
        } else {
            downPaymentValue
        }

    val downPaymentPercent: Double
        get() = if (isDownPaymentPercent) {
            downPaymentValue
        } else {
            if (basePriceForDownPayment > 0) (downPaymentValue / basePriceForDownPayment) * 100.0 else 0.0
        }

    val loanPrincipal: Double
        get() {
            if (!isLoanSelected) return 0.0
            return max(0.0, basePriceForDownPayment - downPaymentAmount)
        }

    val initialOutflow: Double
        get() {
            return if (isLoanSelected) {
                downPaymentAmount + upfrontCostsAmount
            } else {
                totalPurchaseCost
            }
        }

    fun getPartPrepaymentForMonth(m: Int): Double {
        if (partPrepaymentAmount <= 0.0 || partPrepaymentFrequency == PrepaymentFrequency.NONE) {
            return 0.0
        }
        val interval = partPrepaymentFrequency.intervalMonths
        return if (interval > 0 && m % interval == 0) partPrepaymentAmount else 0.0
    }
}
