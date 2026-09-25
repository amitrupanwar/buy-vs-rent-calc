package com.amitr.buyvsrentcalc.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.amitr.buyvsrentcalc.domain.model.CompoundingFrequency
import com.amitr.buyvsrentcalc.domain.model.HomeBuyParams
import com.amitr.buyvsrentcalc.domain.model.HomeDecisionConfig
import com.amitr.buyvsrentcalc.domain.model.HomeRentParams
import com.amitr.buyvsrentcalc.domain.model.InvestmentParams
import com.amitr.buyvsrentcalc.domain.model.InvestmentType
import com.amitr.buyvsrentcalc.domain.model.PrepaymentFrequency

@Entity(tableName = "home_decisions")
data class HomeDecisionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val profileTitle: String,
    val createdTimestamp: Long = System.currentTimeMillis(),
    val evaluationHorizonYears: Int,
    // Buy params
    val propertyPrice: Double,
    val downPaymentValue: Double = 20.0,
    val isDownPaymentPercent: Boolean = true,
    val loanInterestRateAnnual: Double,
    val loanTenureYears: Int,
    val upfrontCostPercent: Double,
    val isUpfrontCostInclusive: Boolean = false,
    val partPrepaymentAmount: Double = 0.0,
    val partPrepaymentFrequency: String = "NONE",
    val propertyAppreciationRateAnnual: Double,
    val monthlySocietyMaintenance: Double,
    val maintenanceEscalationRateAnnual: Double,
    val propertyUpkeepAnnualAmount: Double = 12_000.0,
    val propertyTaxAnnualAmount: Double = 12_000.0,
    val isLoanSelected: Boolean = true,
    // Rent params
    val initialMonthlyRent: Double,
    val rentEscalationRateAnnual: Double,
    val securityDepositValue: Double = 2.0,
    val isSecurityDepositFixed: Boolean = false,
    val annualRelocationCost: Double = 0.0,
    val annualRentalBrokerage: Double = 20_000.0,
    val monthlyTenantMaintenance: Double = 0.0,
    // Investment params
    val investmentType: String,
    val annualReturnRatePercent: Double,
    val taxRatePercent: Double,
    val investmentCompoundingFrequency: String = "MONTHLY"
) {
    fun toDomainConfig(): HomeDecisionConfig {
        return HomeDecisionConfig(
            id = id,
            profileTitle = profileTitle,
            evaluationHorizonYears = evaluationHorizonYears,
            buyParams = HomeBuyParams(
                propertyPrice = propertyPrice,
                downPaymentValue = downPaymentValue,
                isDownPaymentPercent = isDownPaymentPercent,
                loanInterestRateAnnual = loanInterestRateAnnual,
                loanTenureYears = loanTenureYears,
                upfrontCostPercent = upfrontCostPercent,
                isUpfrontCostInclusive = isUpfrontCostInclusive,
                partPrepaymentAmount = partPrepaymentAmount,
                partPrepaymentFrequency = try {
                    PrepaymentFrequency.valueOf(partPrepaymentFrequency)
                } catch (e: Exception) {
                    PrepaymentFrequency.NONE
                },
                propertyAppreciationRateAnnual = propertyAppreciationRateAnnual,
                monthlySocietyMaintenance = monthlySocietyMaintenance,
                maintenanceEscalationRateAnnual = maintenanceEscalationRateAnnual,
                propertyUpkeepAnnualAmount = propertyUpkeepAnnualAmount,
                propertyTaxAnnualAmount = propertyTaxAnnualAmount,
                isLoanSelected = isLoanSelected
            ),
            rentParams = HomeRentParams(
                initialMonthlyRent = initialMonthlyRent,
                rentEscalationRateAnnual = rentEscalationRateAnnual,
                securityDepositValue = securityDepositValue,
                isSecurityDepositFixed = isSecurityDepositFixed,
                annualRelocationCost = annualRelocationCost,
                annualRentalBrokerage = annualRentalBrokerage,
                monthlyTenantMaintenance = monthlyTenantMaintenance
            ),
            investmentParams = InvestmentParams(
                investmentType = try {
                    InvestmentType.valueOf(investmentType)
                } catch (e: Exception) {
                    InvestmentType.MUTUAL_FUNDS
                },
                annualReturnRatePercent = annualReturnRatePercent,
                taxRatePercent = taxRatePercent,
                compoundingFrequency = try {
                    CompoundingFrequency.valueOf(investmentCompoundingFrequency)
                } catch (e: Exception) {
                    CompoundingFrequency.MONTHLY
                }
            )
        )
    }

    companion object {
        fun fromDomainConfig(config: HomeDecisionConfig): HomeDecisionEntity {
            return HomeDecisionEntity(
                id = config.id,
                profileTitle = config.profileTitle,
                evaluationHorizonYears = config.evaluationHorizonYears,
                propertyPrice = config.buyParams.propertyPrice,
                downPaymentValue = config.buyParams.downPaymentValue,
                isDownPaymentPercent = config.buyParams.isDownPaymentPercent,
                loanInterestRateAnnual = config.buyParams.loanInterestRateAnnual,
                loanTenureYears = config.buyParams.loanTenureYears,
                upfrontCostPercent = config.buyParams.upfrontCostPercent,
                isUpfrontCostInclusive = config.buyParams.isUpfrontCostInclusive,
                partPrepaymentAmount = config.buyParams.partPrepaymentAmount,
                partPrepaymentFrequency = config.buyParams.partPrepaymentFrequency.name,
                propertyAppreciationRateAnnual = config.buyParams.propertyAppreciationRateAnnual,
                monthlySocietyMaintenance = config.buyParams.monthlySocietyMaintenance,
                maintenanceEscalationRateAnnual = config.buyParams.maintenanceEscalationRateAnnual,
                propertyUpkeepAnnualAmount = config.buyParams.propertyUpkeepAnnualAmount,
                propertyTaxAnnualAmount = config.buyParams.propertyTaxAnnualAmount,
                isLoanSelected = config.buyParams.isLoanSelected,
                initialMonthlyRent = config.rentParams.initialMonthlyRent,
                rentEscalationRateAnnual = config.rentParams.rentEscalationRateAnnual,
                securityDepositValue = config.rentParams.securityDepositValue,
                isSecurityDepositFixed = config.rentParams.isSecurityDepositFixed,
                annualRelocationCost = config.rentParams.annualRelocationCost,
                annualRentalBrokerage = config.rentParams.annualRentalBrokerage,
                monthlyTenantMaintenance = config.rentParams.monthlyTenantMaintenance,
                investmentType = config.investmentParams.investmentType.name,
                annualReturnRatePercent = config.investmentParams.annualReturnRatePercent,
                taxRatePercent = config.investmentParams.taxRatePercent,
                investmentCompoundingFrequency = config.investmentParams.compoundingFrequency.name
            )
        }
    }
}
