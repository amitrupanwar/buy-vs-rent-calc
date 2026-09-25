package com.amitr.buyvsrentcalc.domain.model

data class HomeRentParams(
    val initialMonthlyRent: Double,
    val rentEscalationRateAnnual: Double = 5.0,
    val securityDepositValue: Double = 2.0,
    val isSecurityDepositFixed: Boolean = false,
    val annualRelocationCost: Double = 0.0,
    val annualRentalBrokerage: Double = 20_000.0,
    val monthlyTenantMaintenance: Double = 0.0
) {
    val securityDepositAmount: Double
        get() = if (isSecurityDepositFixed) {
            securityDepositValue
        } else {
            initialMonthlyRent * securityDepositValue
        }

    val securityDepositMonths: Int
        get() = if (!isSecurityDepositFixed) {
            securityDepositValue.toInt()
        } else if (initialMonthlyRent > 0) {
            (securityDepositValue / initialMonthlyRent).toInt()
        } else 0

    val initialOutflow: Double
        get() = securityDepositAmount + annualRentalBrokerage
}
