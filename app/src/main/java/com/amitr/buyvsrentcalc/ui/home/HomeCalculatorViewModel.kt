package com.amitr.buyvsrentcalc.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.amitr.buyvsrentcalc.data.local.AppDatabase
import com.amitr.buyvsrentcalc.data.repository.HomeDecisionRepository
import com.amitr.buyvsrentcalc.domain.engine.HomeCalculatorEngine
import com.amitr.buyvsrentcalc.domain.model.CompoundingFrequency
import com.amitr.buyvsrentcalc.domain.model.HomeBuyParams
import com.amitr.buyvsrentcalc.domain.model.HomeCalculationResult
import com.amitr.buyvsrentcalc.domain.model.HomeDecisionConfig
import com.amitr.buyvsrentcalc.domain.model.HomeRentParams
import com.amitr.buyvsrentcalc.domain.model.InvestmentParams
import com.amitr.buyvsrentcalc.domain.model.InvestmentType
import com.amitr.buyvsrentcalc.domain.model.PrepaymentFrequency
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class HomeCalculatorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: HomeDecisionRepository by lazy {
        val dao = AppDatabase.getDatabase(application).homeDecisionDao()
        HomeDecisionRepository(dao)
    }

    val savedDecisions: StateFlow<List<HomeDecisionConfig>> = repository.allDecisions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    private val defaultBuyParams = HomeBuyParams(
        propertyPrice = 5_000_000.0,
        downPaymentValue = 20.0,
        isDownPaymentPercent = true,
        loanInterestRateAnnual = 8.5,
        loanTenureYears = 20,
        upfrontCostPercent = 5.0,
        isUpfrontCostInclusive = false,
        partPrepaymentAmount = 0.0,
        partPrepaymentFrequency = PrepaymentFrequency.NONE,
        propertyAppreciationRateAnnual = 5.0,
        monthlySocietyMaintenance = 3000.0,
        maintenanceEscalationRateAnnual = 5.0,
        propertyUpkeepAnnualAmount = 12_000.0,
        propertyTaxAnnualAmount = 12_000.0,
        isLoanSelected = true
    )

    private val defaultRentParams = HomeRentParams(
        initialMonthlyRent = 20_000.0,
        rentEscalationRateAnnual = 5.0,
        securityDepositValue = 2.0,
        isSecurityDepositFixed = false,
        annualRelocationCost = 15_000.0,
        annualRentalBrokerage = 20_000.0,
        monthlyTenantMaintenance = 0.0
    )

    private val defaultInvestmentParams = InvestmentParams(
        investmentType = InvestmentType.MUTUAL_FUNDS,
        annualReturnRatePercent = 7.0,
        taxRatePercent = 20.0,
        compoundingFrequency = CompoundingFrequency.MONTHLY
    )

    private val _currentConfig = MutableStateFlow(
        HomeDecisionConfig(
            id = 0,
            profileTitle = "Home Buy vs Rent Check 1",
            evaluationHorizonYears = 20,
            buyParams = defaultBuyParams,
            rentParams = defaultRentParams,
            investmentParams = defaultInvestmentParams
        )
    )
    val currentConfig: StateFlow<HomeDecisionConfig> = _currentConfig.asStateFlow()

    private val _calculationResult = MutableStateFlow<HomeCalculationResult?>(null)
    val calculationResult: StateFlow<HomeCalculationResult?> = _calculationResult.asStateFlow()

    init {
        recalculate()
    }

    fun evaluateCurrentConfig() {
        recalculate()
    }

    fun updateConfig(newConfig: HomeDecisionConfig) {
        _currentConfig.value = newConfig
    }

    fun updateBuyParams(transform: (HomeBuyParams) -> HomeBuyParams) {
        val curr = _currentConfig.value
        _currentConfig.value = curr.copy(buyParams = transform(curr.buyParams))
    }

    fun updateRentParams(transform: (HomeRentParams) -> HomeRentParams) {
        val curr = _currentConfig.value
        _currentConfig.value = curr.copy(rentParams = transform(curr.rentParams))
    }

    fun updateInvestmentParams(transform: (InvestmentParams) -> InvestmentParams) {
        val curr = _currentConfig.value
        _currentConfig.value = curr.copy(investmentParams = transform(curr.investmentParams))
    }

    fun updateEvaluationHorizon(years: Int) {
        val curr = _currentConfig.value
        _currentConfig.value = curr.copy(evaluationHorizonYears = years)
    }

    fun updateProfileTitle(title: String) {
        val curr = _currentConfig.value
        _currentConfig.value = curr.copy(profileTitle = title)
    }

    private fun recalculate() {
        val config = _currentConfig.value
        val result = HomeCalculatorEngine.calculate(config)
        _calculationResult.value = result
    }

    fun saveCurrentDecision(onSaved: (Long) -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val config = _currentConfig.value
                val id = repository.saveDecision(config)
                _currentConfig.value = config.copy(id = id)
                onSaved(id)
            } catch (e: Exception) {
                onError(e.message ?: "Failed to save decision check")
            }
        }
    }

    fun loadDecision(id: Long, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val loaded = repository.getDecisionById(id)
                if (loaded != null) {
                    _currentConfig.value = loaded
                    recalculate()
                }
            } catch (e: Exception) {
                onError(e.message ?: "Failed to load decision check")
            }
        }
    }

    fun deleteDecision(config: HomeDecisionConfig, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.deleteDecision(config)
            } catch (e: Exception) {
                onError(e.message ?: "Failed to delete decision check")
            }
        }
    }
}
