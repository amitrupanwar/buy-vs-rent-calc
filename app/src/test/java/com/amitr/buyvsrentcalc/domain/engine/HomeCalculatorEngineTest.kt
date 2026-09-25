package com.amitr.buyvsrentcalc.domain.engine

import com.amitr.buyvsrentcalc.domain.model.FinancialWinner
import com.amitr.buyvsrentcalc.domain.model.HomeBuyParams
import com.amitr.buyvsrentcalc.domain.model.HomeDecisionConfig
import com.amitr.buyvsrentcalc.domain.model.HomeRentParams
import com.amitr.buyvsrentcalc.domain.model.InvestmentParams
import com.amitr.buyvsrentcalc.domain.model.InvestmentType
import com.amitr.buyvsrentcalc.domain.model.PrepaymentFrequency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeCalculatorEngineTest {

    @Test
    fun testStandardHomeCalculation_LoanVsRent() {
        val config = HomeDecisionConfig(
            id = 1,
            profileTitle = "50L Home Buy vs 20k Rent",
            evaluationHorizonYears = 20,
            buyParams = HomeBuyParams(
                propertyPrice = 5_000_000.0,
                downPaymentValue = 20.0, // 10L down payment
                isDownPaymentPercent = true,
                loanInterestRateAnnual = 8.5,
                loanTenureYears = 20,
                upfrontCostPercent = 5.0, // 2.5L upfront
                isUpfrontCostInclusive = false,
                propertyAppreciationRateAnnual = 5.0,
                monthlySocietyMaintenance = 3000.0,
                maintenanceEscalationRateAnnual = 5.0,
                propertyUpkeepAnnualAmount = 25_000.0,
                propertyTaxAnnualAmount = 12_000.0,
                isLoanSelected = true
            ),
            rentParams = HomeRentParams(
                initialMonthlyRent = 20_000.0,
                rentEscalationRateAnnual = 5.0,
                securityDepositValue = 6.0, // 1.2L deposit
                isSecurityDepositFixed = false,
                annualRelocationCost = 15_000.0,
                annualRentalBrokerage = 20_000.0
            ),
            investmentParams = InvestmentParams(
                investmentType = InvestmentType.MUTUAL_FUNDS,
                annualReturnRatePercent = 10.0,
                taxRatePercent = 12.5
            )
        )

        val result = HomeCalculatorEngine.calculate(config)

        // Verify monthly schedule length: 20 years * 12 = 240 months
        assertEquals(240, result.monthlySchedule.size)
        assertEquals(20, result.yearlySummaries.size)

        // Verify first month values
        val firstMonth = result.monthlySchedule.first()
        assertEquals(1, firstMonth.monthIndex)
        assertEquals(1, firstMonth.yearIndex)
        assertTrue(firstMonth.emiPaid > 30000.0) // Expected ~34,700 EMI for 40L loan @ 8.5% for 20 yrs
        assertTrue(firstMonth.remainingLoanBalance < 4_000_000.0)

        // Verify property value at end of 20 years
        val lastMonth = result.monthlySchedule.last()
        assertEquals(240, lastMonth.monthIndex)
        assertEquals(20, lastMonth.yearIndex)
        assertEquals(0.0, lastMonth.remainingLoanBalance, 0.01) // Fully amortized

        // Verify final property value appreciated ~5% p.a. compound
        assertTrue(lastMonth.propertyValue > 13_000_000.0)

        // Verify result summary objects
        assertNotNull(result.financialWinner)
    }

    @Test
    fun testFullCashPayment_NoLoan() {
        val config = HomeDecisionConfig(
            id = 2,
            profileTitle = "Full Cash Buy vs Rent",
            evaluationHorizonYears = 10,
            buyParams = HomeBuyParams(
                propertyPrice = 3_000_000.0,
                downPaymentValue = 100.0,
                isDownPaymentPercent = true,
                loanInterestRateAnnual = 8.5, // preserved value
                loanTenureYears = 20, // preserved value
                upfrontCostPercent = 5.0,
                propertyAppreciationRateAnnual = 4.0,
                monthlySocietyMaintenance = 2000.0,
                maintenanceEscalationRateAnnual = 5.0,
                propertyUpkeepAnnualAmount = 15_000.0,
                propertyTaxAnnualAmount = 8_000.0,
                isLoanSelected = false
            ),
            rentParams = HomeRentParams(
                initialMonthlyRent = 15_000.0,
                rentEscalationRateAnnual = 6.0,
                securityDepositValue = 45000.0, // Fixed 45k deposit
                isSecurityDepositFixed = true,
                annualRelocationCost = 10_000.0,
                annualRentalBrokerage = 15_000.0
            ),
            investmentParams = InvestmentParams(
                investmentType = InvestmentType.FIXED_DEPOSIT,
                annualReturnRatePercent = 7.0,
                taxRatePercent = 20.0
            )
        )

        val result = HomeCalculatorEngine.calculate(config)

        assertEquals(120, result.monthlySchedule.size)
        val firstMonth = result.monthlySchedule.first()
        assertEquals(0.0, firstMonth.emiPaid, 0.001)
        assertEquals(0.0, firstMonth.remainingLoanBalance, 0.001)

        // Verify relocation cost included at month 12
        val month12 = result.monthlySchedule[11]
        assertTrue(month12.monthlyRentOutflow > month12.monthlyRentPaid)
    }

    @Test
    fun testFixedDownPaymentAndInclusiveRegistration() {
        val buy = HomeBuyParams(
            propertyPrice = 4_000_000.0, // 40L price inclusive of reg/stamp duty
            downPaymentValue = 1_000_000.0, // Fixed 10L down payment
            isDownPaymentPercent = false,
            upfrontCostPercent = 5.0,
            isUpfrontCostInclusive = true,
            loanInterestRateAnnual = 8.0,
            loanTenureYears = 15,
            isLoanSelected = true
        )

        assertEquals(0.0, buy.upfrontCostsAmount, 0.01)
        assertEquals(4_000_000.0, buy.totalPurchaseCost, 0.01)
        assertEquals(1_000_000.0, buy.downPaymentAmount, 0.01)
        assertEquals(3_000_000.0, buy.loanPrincipal, 0.01)
        assertEquals(1_000_000.0, buy.initialOutflow, 0.01)
    }

    @Test
    fun testLoanPartPrepaymentAndAnnualBrokerage() {
        val config = HomeDecisionConfig(
            id = 3,
            profileTitle = "Part Prepayment Test",
            evaluationHorizonYears = 10,
            buyParams = HomeBuyParams(
                propertyPrice = 5_000_000.0,
                downPaymentValue = 20.0,
                loanInterestRateAnnual = 8.5,
                loanTenureYears = 20,
                partPrepaymentAmount = 100_000.0, // Extra 1L prepay
                partPrepaymentFrequency = PrepaymentFrequency.ANNUAL, // Annual lump-sum
                isLoanSelected = true
            ),
            rentParams = HomeRentParams(
                initialMonthlyRent = 20_000.0,
                rentEscalationRateAnnual = 5.0,
                annualRentalBrokerage = 20_000.0 // Recurring annual brokerage
            ),
            investmentParams = InvestmentParams(
                investmentType = InvestmentType.MUTUAL_FUNDS,
                annualReturnRatePercent = 10.0,
                taxRatePercent = 12.5
            )
        )

        val result = HomeCalculatorEngine.calculate(config)

        // With extra prepayments, remaining loan balance at year 10 should be lower than standard 20-yr amortization
        val month120 = result.monthlySchedule.last()
        assertTrue(month120.remainingLoanBalance < 2_000_000.0)

        // Month 12 rent outflow should include escalated annual brokerage
        val month12 = result.monthlySchedule[11]
        assertTrue(month12.monthlyRentOutflow > month12.monthlyRentPaid + 15000.0)
    }
}
