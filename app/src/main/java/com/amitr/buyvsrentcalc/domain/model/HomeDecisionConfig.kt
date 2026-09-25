package com.amitr.buyvsrentcalc.domain.model

data class HomeDecisionConfig(
    val id: Long = 0,
    val profileTitle: String,
    val evaluationHorizonYears: Int,
    val buyParams: HomeBuyParams,
    val rentParams: HomeRentParams,
    val investmentParams: InvestmentParams
)
