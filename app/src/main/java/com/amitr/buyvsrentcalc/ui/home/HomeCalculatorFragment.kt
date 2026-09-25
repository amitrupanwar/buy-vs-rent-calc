package com.amitr.buyvsrentcalc.ui.home

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Filter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.amitr.buyvsrentcalc.R
import com.amitr.buyvsrentcalc.databinding.FragmentHomeCalculatorBinding
import com.amitr.buyvsrentcalc.domain.model.CompoundingFrequency
import com.amitr.buyvsrentcalc.domain.model.FinancialWinner
import com.amitr.buyvsrentcalc.domain.model.HomeCalculationResult
import com.amitr.buyvsrentcalc.domain.model.HomeDecisionConfig
import com.amitr.buyvsrentcalc.domain.model.PrepaymentFrequency
import com.amitr.buyvsrentcalc.domain.model.ScheduleDisplayRow
import com.amitr.buyvsrentcalc.util.CurrencyFormatter
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeCalculatorFragment : Fragment() {

    private var _binding: FragmentHomeCalculatorBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeCalculatorViewModel by viewModels()
    private val progressionAdapter = ProgressionAdapter()
    private var currentCurrencySymbol: String = "₹"
    private val expandedYearIndices = mutableSetOf<Int>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeCalculatorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, layoutInstanceState: Bundle?) {
        super.onViewCreated(view, layoutInstanceState)

        setupRecyclerView()
        setupCurrencyDropdown()
        setupInputListeners()
        setupActions()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        binding.rvProgressionSchedule.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = progressionAdapter
        }

        progressionAdapter.onItemClickListener = { row ->
            if (row.isYearly) {
                if (expandedYearIndices.contains(row.yearIndex)) {
                    expandedYearIndices.remove(row.yearIndex)
                } else {
                    expandedYearIndices.add(row.yearIndex)
                }
                viewModel.calculationResult.value?.let { renderResult(it) }
            }
        }
    }

    private fun setupCurrencyDropdown() {
        val options = CurrencyFormatter.CURRENCY_OPTIONS.map { it.first }

        currentCurrencySymbol = CurrencyFormatter.getSavedCurrency(requireContext())
        val currentLabel = CurrencyFormatter.getOptionLabelForSymbol(currentCurrencySymbol)
        binding.actvCurrency.setText(currentLabel, false)
        progressionAdapter.currencySymbol = currentCurrencySymbol

        setupNonFilteringDropdown(binding.actvCurrency, options) { position ->
            val selectedLabel = options[position]
            val symbol = CurrencyFormatter.getSymbolFromOptionLabel(selectedLabel)
            currentCurrencySymbol = symbol
            CurrencyFormatter.saveCurrency(requireContext(), symbol)
            progressionAdapter.currencySymbol = symbol
            viewModel.calculationResult.value?.let { renderResult(it) }
        }
    }

    private fun setupActions() {
        binding.btnEvaluate.setOnClickListener {
            binding.btnEvaluate.isEnabled = false
            binding.btnEvaluate.text = "Evaluating Financial Engine..."
            binding.progressEvaluating.visibility = View.VISIBLE

            viewLifecycleOwner.lifecycleScope.launch {
                delay(650)

                syncUiToViewModel()
                viewModel.evaluateCurrentConfig()

                binding.progressEvaluating.visibility = View.GONE
                binding.btnEvaluate.text = "Evaluate Buy vs Rent"
                binding.btnEvaluate.isEnabled = true

                binding.cardResultSummary.post {
                    binding.root.smoothScrollTo(0, binding.cardResultSummary.top - 16)
                    animateResultCard()
                }
            }
        }

        binding.btnSaveDecision.setOnClickListener {
            showSaveConfirmDialog()
        }

        binding.btnLoadSaved.setOnClickListener {
            showSavedDecisionsDialog()
        }

        binding.horizonSliderView.slider.setLabelFormatter { value ->
            "${value.toInt()} Yrs"
        }

        binding.horizonSliderView.slider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val years = value.toInt()
                binding.tvHorizonLabel.text = "Evaluation Horizon: $years Years"
                viewModel.updateEvaluationHorizon(years)
            }
        }

        binding.toggleStrategy.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val isLoan = checkedId == R.id.btnStrategyLoan
                binding.cardLoanParameters.visibility = if (isLoan) View.VISIBLE else View.GONE
                viewModel.updateBuyParams { it.copy(isLoanSelected = isLoan) }
            }
        }

        binding.toggleDownPaymentUnit.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val isPercent = checkedId == R.id.btnDpPercent
                val currentBuyParams = viewModel.currentConfig.value.buyParams
                val propertyPrice = currentBuyParams.propertyPrice

                val newValue: Double = if (isPercent != currentBuyParams.isDownPaymentPercent) {
                    if (isPercent) {
                        if (propertyPrice > 0) {
                            val calcPercent = (currentBuyParams.downPaymentValue / propertyPrice) * 100.0
                            if (calcPercent in 0.1..100.0) calcPercent else 20.0
                        } else {
                            20.0
                        }
                    } else {
                        if (propertyPrice > 0) {
                            val calcAmount = propertyPrice * (currentBuyParams.downPaymentValue / 100.0)
                            if (calcAmount > 0) calcAmount else 1_000_000.0
                        } else {
                            1_000_000.0
                        }
                    }
                } else {
                    currentBuyParams.downPaymentValue
                }

                binding.tilDownPayment.hint = if (isPercent) "Down Payment %" else "Down Payment Amount ($currentCurrencySymbol)"
                binding.etDownPaymentValue.setText(formatNumberForInput(newValue))

                viewModel.updateBuyParams {
                    it.copy(
                        downPaymentValue = newValue,
                        isDownPaymentPercent = isPercent
                    )
                }
            }
        }

        binding.cbUpfrontCostInclusive.setOnCheckedChangeListener { _, isChecked ->
            updateUpfrontCostState(isChecked)
            viewModel.updateBuyParams { it.copy(isUpfrontCostInclusive = isChecked) }
        }
        updateUpfrontCostState(binding.cbUpfrontCostInclusive.isChecked)

        binding.toggleDepositUnit.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val isFixed = checkedId == R.id.btnDepositAmount
                val currentRentParams = viewModel.currentConfig.value.rentParams
                val initialRent = currentRentParams.initialMonthlyRent

                val newValue: Double = if (isFixed != currentRentParams.isSecurityDepositFixed) {
                    if (isFixed) {
                        if (initialRent > 0) {
                            val calcAmount = initialRent * currentRentParams.securityDepositValue
                            if (calcAmount > 0) calcAmount else 120_000.0
                        } else {
                            120_000.0
                        }
                    } else {
                        if (initialRent > 0) {
                            val calcMonths = (currentRentParams.securityDepositValue / initialRent)
                            if (calcMonths in 0.1..60.0) calcMonths else 6.0
                        } else {
                            6.0
                        }
                    }
                } else {
                    currentRentParams.securityDepositValue
                }

                binding.tilSecurityDeposit.hint = if (isFixed) "Rent Deposit Amount ($currentCurrencySymbol)" else "Rent Deposit (Months)"
                binding.etSecurityDepositValue.setText(formatNumberForInput(newValue))

                viewModel.updateRentParams {
                    it.copy(
                        securityDepositValue = newValue,
                        isSecurityDepositFixed = isFixed
                    )
                }
            }
        }

        // Chart Mode Toggle Listener
        binding.toggleChartMode.addOnButtonCheckedListener { _, _, isChecked ->
            if (isChecked) {
                viewModel.calculationResult.value?.let { renderResult(it) }
            }
        }

        // Progression Format Toggle Listener (Cards vs Table)
        binding.toggleProgressionFormat.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val isTabular = checkedId == R.id.btnStyleTabular
                binding.layoutTabularHeader.visibility = if (isTabular) View.VISIBLE else View.GONE
                progressionAdapter.viewStyle = if (isTabular) ProgressionAdapter.ViewStyle.TABULAR else ProgressionAdapter.ViewStyle.CARD
                viewModel.calculationResult.value?.let { renderResult(it) }
            }
        }
    }

    private fun showSaveConfirmDialog() {
        val currentTitle = binding.etProfileTitle.text.toString().ifBlank { "My Home Buy vs Rent Check" }
        val input = EditText(requireContext()).apply {
            setText(currentTitle)
            setSelection(currentTitle.length)
            setPadding(32, 24, 32, 24)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Save Decision Check")
            .setMessage("Enter profile name to save your calculation:")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val title = input.text.toString().ifBlank { "Home Buy vs Rent Check" }
                binding.etProfileTitle.setText(title)
                viewModel.updateProfileTitle(title)
                viewModel.saveCurrentDecision(
                    onSaved = { id ->
                        showSaveSuccessAlert(title)
                    },
                    onError = { errorMsg ->
                        Toast.makeText(requireContext(), "Save failed: $errorMsg", Toast.LENGTH_LONG).show()
                    }
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSaveSuccessAlert(title: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Check Saved Successfully!")
            .setMessage("Decision check '$title' has been saved. You can access or load it anytime from 'Saved Checks'.")
            .setPositiveButton("OK", null)
            .setNeutralButton("View Saved Checks") { _, _ ->
                showSavedDecisionsDialog()
            }
            .show()
    }

    private fun setupInputListeners() {
        // Buy params
        bindDoubleEditText(binding.etPropertyPrice) { valNum ->
            viewModel.updateBuyParams { it.copy(propertyPrice = valNum) }
        }
        bindDoubleEditText(binding.etDownPaymentValue) { valNum ->
            viewModel.updateBuyParams { it.copy(downPaymentValue = valNum) }
        }
        bindDoubleEditText(binding.etUpfrontCost) { valNum ->
            viewModel.updateBuyParams { it.copy(upfrontCostPercent = valNum) }
        }
        bindDoubleEditText(binding.etInterestRate) { valNum ->
            viewModel.updateBuyParams { it.copy(loanInterestRateAnnual = valNum) }
        }
        bindIntEditText(binding.etLoanTenure) { valNum ->
            viewModel.updateBuyParams { it.copy(loanTenureYears = valNum) }
        }

        // Part Prepayment
        val frequencies = PrepaymentFrequency.entries
        val freqNames = frequencies.map { it.displayName }
        setupNonFilteringDropdown(binding.actvPrepaymentFrequency, freqNames) { position ->
            val selectedFreq = frequencies[position]
            viewModel.updateBuyParams { it.copy(partPrepaymentFrequency = selectedFreq) }
        }

        bindDoubleEditText(binding.etPartPrepaymentAmount) { valNum ->
            viewModel.updateBuyParams { it.copy(partPrepaymentAmount = valNum) }
        }
        bindDoubleEditText(binding.etPropertyAppreciation) { valNum ->
            viewModel.updateBuyParams { it.copy(propertyAppreciationRateAnnual = valNum) }
        }
        bindDoubleEditText(binding.etSocietyMaintenance) { valNum ->
            viewModel.updateBuyParams { it.copy(monthlySocietyMaintenance = valNum) }
        }
        bindDoubleEditText(binding.etMaintenanceEscalation) { valNum ->
            viewModel.updateBuyParams { it.copy(maintenanceEscalationRateAnnual = valNum) }
        }
        bindDoubleEditText(binding.etPropertyTaxAmount) { valNum ->
            viewModel.updateBuyParams { it.copy(propertyTaxAnnualAmount = valNum) }
        }
        bindDoubleEditText(binding.etPropertyUpkeepAmount) { valNum ->
            viewModel.updateBuyParams { it.copy(propertyUpkeepAnnualAmount = valNum) }
        }

        // Rent params
        bindDoubleEditText(binding.etInitialRent) { valNum ->
            viewModel.updateRentParams { it.copy(initialMonthlyRent = valNum) }
        }
        bindDoubleEditText(binding.etRentEscalation) { valNum ->
            viewModel.updateRentParams { it.copy(rentEscalationRateAnnual = valNum) }
        }
        bindDoubleEditText(binding.etSecurityDepositValue) { valNum ->
            viewModel.updateRentParams { it.copy(securityDepositValue = valNum) }
        }
        bindDoubleEditText(binding.etAnnualRelocationCost) { valNum ->
            viewModel.updateRentParams { it.copy(annualRelocationCost = valNum) }
        }
        bindDoubleEditText(binding.etBrokerage) { valNum ->
            viewModel.updateRentParams { it.copy(annualRentalBrokerage = valNum) }
        }

        // Investment params
        val compoundingFreqs = CompoundingFrequency.entries
        val compoundingFreqNames = compoundingFreqs.map { it.displayName }
        setupNonFilteringDropdown(binding.actvInvestmentCompoundingFrequency, compoundingFreqNames) { position ->
            val selectedFreq = compoundingFreqs[position]
            viewModel.updateInvestmentParams { it.copy(compoundingFrequency = selectedFreq) }
        }

        bindDoubleEditText(binding.etInvestmentReturn) { valNum ->
            viewModel.updateInvestmentParams { it.copy(annualReturnRatePercent = valNum) }
        }
        bindDoubleEditText(binding.etTaxRate) { valNum ->
            viewModel.updateInvestmentParams { it.copy(taxRatePercent = valNum) }
        }

        binding.etProfileTitle.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (binding.etProfileTitle.hasFocus()) {
                    viewModel.updateProfileTitle(s.toString())
                }
            }
        })
    }

    private fun bindDoubleEditText(editText: EditText, onValueChange: (Double) -> Unit) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (editText.hasFocus()) {
                    val value = s.toString().toDoubleOrNull() ?: 0.0
                    onValueChange(value)
                }
            }
        })
    }

    private fun bindIntEditText(editText: EditText, onValueChange: (Int) -> Unit) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (editText.hasFocus()) {
                    val value = s.toString().toIntOrNull() ?: 0
                    onValueChange(value)
                }
            }
        })
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.calculationResult.collectLatest { result ->
                        if (result != null) {
                            renderResult(result)
                        }
                    }
                }
                launch {
                    viewModel.currentConfig.collectLatest { config ->
                        binding.tvHorizonLabel.text = "Evaluation Horizon: ${config.evaluationHorizonYears} Years"
                        val clampedYears = config.evaluationHorizonYears.toFloat().coerceIn(1f, 30f)
                        if (binding.horizonSliderView.slider.value != clampedYears) {
                            binding.horizonSliderView.slider.value = clampedYears
                        }
                    }
                }
                launch {
                    viewModel.savedDecisions.collectLatest {
                        // Keeps Flow actively subscribed
                    }
                }
            }
        }
    }

    private fun renderResult(result: HomeCalculationResult) {
        val isLoan = viewModel.currentConfig.value.buyParams.isLoanSelected

        val winnerText = when (result.financialWinner) {
            FinancialWinner.BUYING -> "BUYING IS FINANCIAL WINNER"
            FinancialWinner.RENTING -> "RENTING IS FINANCIAL WINNER"
            FinancialWinner.EQUIVALENT -> "BUYING & RENTING ARE EQUIVALENT"
        }
        binding.tvWinnerBadge.text = winnerText

        val absDiff = Math.abs(result.netDifferenceFinal)
        val formattedDiff = CurrencyFormatter.format(absDiff, currentCurrencySymbol)
        val prefix = if (result.netDifferenceFinal >= 0) "Buy Advantage: " else "Rent Advantage: "
        binding.tvNetDifferenceSummary.text = "$prefix$formattedDiff"

        val breakEvenText = if (result.breakEvenMonth != null) {
            val yr = ((result.breakEvenMonth - 1) / 12) + 1
            val mth = ((result.breakEvenMonth - 1) % 12) + 1
            "Break-Even Crossover: Year $yr, Month $mth"
        } else {
            "No Break-Even Crossover during horizon"
        }
        binding.tvBreakEvenSummary.text = breakEvenText

        binding.tvFinalBuyNetWorth.text = CurrencyFormatter.format(result.finalBuyNetWorth, currentCurrencySymbol)
        binding.tvFinalRentNetWorth.text = CurrencyFormatter.format(result.finalRentNetWorth, currentCurrencySymbol)

        // Bind Summary Derived Metrics
        binding.tvSummaryActualPropertyCost.text = CurrencyFormatter.format(result.totalPurchaseCost, currentCurrencySymbol)
        binding.tvSummaryInitialOutflow.text = CurrencyFormatter.format(result.initialBuyOutflow, currentCurrencySymbol)
        binding.layoutSummaryLoanRow.visibility = if (isLoan) View.VISIBLE else View.GONE
        binding.layoutHeaderLoanFields.visibility = if (isLoan) View.VISIBLE else View.GONE
        binding.tvSummaryLoanAmount.text = CurrencyFormatter.format(result.loanAmount, currentCurrencySymbol)
        binding.tvSummaryMonthlyEmi.text = CurrencyFormatter.format(result.monthlyEmi, currentCurrencySymbol)

        // Render Chart
        val isNetWorthMode = binding.toggleChartMode.checkedButtonId == R.id.btnChartNetWorth
        val buySeries: List<Double>
        val rentSeries: List<Double>
        val chartLabels: List<String>

        if (isNetWorthMode) {
            buySeries = result.yearlySummaries.map { it.buyNetWorthEndYear }
            rentSeries = result.yearlySummaries.map { it.rentNetWorthEndYear }
            chartLabels = result.yearlySummaries.map { "Y${it.yearIndex}" }
            binding.chartComparison.mode = ComparisonLineChartView.ChartMode.NET_WORTH
        } else {
            buySeries = result.yearlySummaries.map { it.annualBuyOutflow }
            rentSeries = result.yearlySummaries.map { it.annualRentOutflow }
            chartLabels = result.yearlySummaries.map { "Y${it.yearIndex}" }
            binding.chartComparison.mode = ComparisonLineChartView.ChartMode.CUMULATIVE_OUTFLOW
        }

        binding.chartComparison.currencySymbol = currentCurrencySymbol
        binding.chartComparison.setSeriesData(buySeries, rentSeries, chartLabels)

        // Render Progression Schedule
        val monthlyGrouped = result.monthlySchedule.groupBy { it.yearIndex }

        val buyParams = viewModel.currentConfig.value.buyParams
        val rentParams = viewModel.currentConfig.value.rentParams
        val initialLumpSum = Math.max(0.0, result.initialBuyOutflow - rentParams.initialOutflow)
        val initialRentNW = initialLumpSum + rentParams.securityDepositAmount
        val initialBuyNW = buyParams.propertyPrice - (if (isLoan) buyParams.loanPrincipal else 0.0)

        val initialRow = ScheduleDisplayRow(
            periodLabel = "Initial",
            isYearly = false,
            isLoanSelected = isLoan,
            periodIndex = 0,
            yearIndex = 0,
            isExpanded = false,
            isTally = false,
            emiPaid = 0.0,
            principalPaid = 0.0,
            interestPaid = 0.0,
            partPrepaymentPaid = 0.0,
            remainingLoan = if (isLoan) buyParams.loanPrincipal else 0.0,
            buyExpenses = buyParams.upfrontCostsAmount,
            propertyValue = buyParams.propertyPrice,
            moneyOutflowBuy = result.initialBuyOutflow,
            cumulativeBuyOutflow = result.initialBuyOutflow,
            buyNetWorth = initialBuyNW,
            monthlyRentPaid = 0.0,
            moneyOutflowRent = rentParams.initialOutflow,
            moneyInjected = initialLumpSum,
            investmentPortfolioValue = initialLumpSum,
            cumulativeRentOutflow = rentParams.initialOutflow,
            rentNetWorth = initialRentNW,
            netAdvantage = initialBuyNW - initialRentNW
        )

        val fullList = mutableListOf<ScheduleDisplayRow>()
        fullList.add(initialRow)

        result.yearlySummaries.forEach { ySummary ->
            val monthsInYear = monthlyGrouped[ySummary.yearIndex] ?: emptyList()
            val lastMonth = monthsInYear.lastOrNull() ?: result.monthlySchedule.first()

            val isExpanded = expandedYearIndices.contains(ySummary.yearIndex)

            val sumEmi = monthsInYear.sumOf { it.emiPaid }
            val sumPrincipal = monthsInYear.sumOf { it.principalPaid }
            val sumInterest = monthsInYear.sumOf { it.interestPaid }
            val sumPartPrepayments = monthsInYear.sumOf { it.partPrepaymentPaid }
            val sumExpenses = monthsInYear.sumOf { it.monthlyBuyOutflow - it.emiPaid - it.partPrepaymentPaid }
            val sumRentPaid = monthsInYear.sumOf { it.monthlyRentPaid }

            if (!isExpanded) {
                // Collapsed State: Single compact Year row
                val yearRow = ScheduleDisplayRow(
                    periodLabel = "Year ${ySummary.yearIndex} ▼",
                    isYearly = true,
                    isLoanSelected = isLoan,
                    periodIndex = ySummary.yearIndex,
                    yearIndex = ySummary.yearIndex,
                    isExpanded = false,
                    isTally = false,
                    emiPaid = sumEmi,
                    principalPaid = sumPrincipal,
                    interestPaid = sumInterest,
                    partPrepaymentPaid = sumPartPrepayments,
                    remainingLoan = lastMonth.remainingLoanBalance,
                    buyExpenses = sumExpenses,
                    propertyValue = lastMonth.propertyValue,
                    moneyOutflowBuy = ySummary.annualBuyOutflow,
                    cumulativeBuyOutflow = lastMonth.cumulativeBuyOutflow,
                    buyNetWorth = lastMonth.buyNetWorth,
                    monthlyRentPaid = sumRentPaid,
                    moneyOutflowRent = ySummary.annualRentOutflow,
                    moneyInjected = ySummary.annualBuyOutflow - ySummary.annualRentOutflow,
                    investmentPortfolioValue = lastMonth.investmentPortfolioValue,
                    cumulativeRentOutflow = lastMonth.cumulativeRentOutflow,
                    rentNetWorth = lastMonth.rentNetWorth,
                    netAdvantage = lastMonth.netDifference
                )
                fullList.add(yearRow)
            } else {
                // Expanded State: 12 Monthly rows first, followed by the Year Annual Tally Summary Row
                monthsInYear.forEach { mItem ->
                    val monthRow = ScheduleDisplayRow(
                        periodLabel = "│  Mo ${mItem.monthIndex}",
                        isYearly = false,
                        isLoanSelected = isLoan,
                        periodIndex = mItem.monthIndex,
                        yearIndex = ySummary.yearIndex,
                        isExpanded = false,
                        isTally = false,
                        emiPaid = mItem.emiPaid,
                        principalPaid = mItem.principalPaid,
                        interestPaid = mItem.interestPaid,
                        partPrepaymentPaid = mItem.partPrepaymentPaid,
                        remainingLoan = mItem.remainingLoanBalance,
                        buyExpenses = mItem.monthlyBuyOutflow - mItem.emiPaid - mItem.partPrepaymentPaid,
                        propertyValue = mItem.propertyValue,
                        moneyOutflowBuy = mItem.monthlyBuyOutflow,
                        cumulativeBuyOutflow = mItem.cumulativeBuyOutflow,
                        buyNetWorth = mItem.buyNetWorth,
                        monthlyRentPaid = mItem.monthlyRentPaid,
                        moneyOutflowRent = mItem.monthlyRentOutflow,
                        moneyInjected = mItem.monthlyBuyOutflow - mItem.monthlyRentOutflow,
                        investmentPortfolioValue = mItem.investmentPortfolioValue,
                        cumulativeRentOutflow = mItem.cumulativeRentOutflow,
                        rentNetWorth = mItem.rentNetWorth,
                        netAdvantage = mItem.netDifference
                    )
                    fullList.add(monthRow)
                }

                // Year Annual Tally Summary Row placed right after Month 12
                val tallyRow = ScheduleDisplayRow(
                    periodLabel = "★ Yr ${ySummary.yearIndex} Total ▲",
                    isYearly = true,
                    isLoanSelected = isLoan,
                    periodIndex = ySummary.yearIndex,
                    yearIndex = ySummary.yearIndex,
                    isExpanded = true,
                    isTally = true,
                    emiPaid = sumEmi,
                    principalPaid = sumPrincipal,
                    interestPaid = sumInterest,
                    partPrepaymentPaid = sumPartPrepayments,
                    remainingLoan = lastMonth.remainingLoanBalance,
                    buyExpenses = sumExpenses,
                    propertyValue = lastMonth.propertyValue,
                    moneyOutflowBuy = ySummary.annualBuyOutflow,
                    cumulativeBuyOutflow = lastMonth.cumulativeBuyOutflow,
                    buyNetWorth = lastMonth.buyNetWorth,
                    monthlyRentPaid = sumRentPaid,
                    moneyOutflowRent = ySummary.annualRentOutflow,
                    moneyInjected = ySummary.annualBuyOutflow - ySummary.annualRentOutflow,
                    investmentPortfolioValue = lastMonth.investmentPortfolioValue,
                    cumulativeRentOutflow = lastMonth.cumulativeRentOutflow,
                    rentNetWorth = lastMonth.rentNetWorth,
                    netAdvantage = lastMonth.netDifference
                )
                fullList.add(tallyRow)
            }
        }

        val isTabular = binding.toggleProgressionFormat.checkedButtonId == R.id.btnStyleTabular
        binding.layoutTabularHeader.visibility = if (isTabular) View.VISIBLE else View.GONE
        progressionAdapter.viewStyle = if (isTabular) ProgressionAdapter.ViewStyle.TABULAR else ProgressionAdapter.ViewStyle.CARD

        progressionAdapter.currencySymbol = currentCurrencySymbol
        progressionAdapter.submitList(fullList)
    }

    private fun setupNonFilteringDropdown(
        autoCompleteTextView: MaterialAutoCompleteTextView,
        items: List<String>,
        onItemSelected: (Int) -> Unit
    ) {
        val adapter = object : ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            items
        ) {
            override fun getFilter(): Filter {
                return object : Filter() {
                    override fun performFiltering(constraint: CharSequence?): FilterResults {
                        val results = FilterResults()
                        results.values = items
                        results.count = items.size
                        return results
                    }
                    override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                        notifyDataSetChanged()
                    }
                }
            }
        }
        autoCompleteTextView.setAdapter(adapter)
        autoCompleteTextView.setOnItemClickListener { _, _, position, _ ->
            onItemSelected(position)
        }
    }

    private fun showSavedDecisionsDialog() {
        val list = viewModel.savedDecisions.value
        if (list.isEmpty()) {
            AlertDialog.Builder(requireContext())
                .setTitle("No Saved Checks Found")
                .setMessage("You haven't saved any decision checks yet. Enter your parameters and tap 'Save Check' to save a profile.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        val items = list.map { "${it.profileTitle} (${it.evaluationHorizonYears} yrs)" }.toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle("Saved Decision Checks (${list.size})")
            .setItems(items) { _, which ->
                val selected = list[which]
                showSavedCheckOptionsDialog(selected)
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showSavedCheckOptionsDialog(config: HomeDecisionConfig) {
        val options = arrayOf("Load Profile", "Delete Profile")
        AlertDialog.Builder(requireContext())
            .setTitle(config.profileTitle)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        viewModel.loadDecision(config.id)
                        populateUiFromConfig(config)
                        Toast.makeText(requireContext(), "Loaded '${config.profileTitle}'", Toast.LENGTH_SHORT).show()
                    }
                    1 -> {
                        AlertDialog.Builder(requireContext())
                            .setTitle("Delete Check")
                            .setMessage("Are you sure you want to delete '${config.profileTitle}'?")
                            .setPositiveButton("Delete") { _, _ ->
                                viewModel.deleteDecision(config)
                                Toast.makeText(requireContext(), "Deleted '${config.profileTitle}'", Toast.LENGTH_SHORT).show()
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                }
            }
            .setNegativeButton("Back", null)
            .show()
    }

    private fun populateUiFromConfig(config: HomeDecisionConfig) {
        binding.etProfileTitle.setText(config.profileTitle)
        binding.etPropertyPrice.setText(formatNumberForInput(config.buyParams.propertyPrice))
        binding.etDownPaymentValue.setText(formatNumberForInput(config.buyParams.downPaymentValue))
        binding.etUpfrontCost.setText(formatNumberForInput(config.buyParams.upfrontCostPercent))
        binding.cbUpfrontCostInclusive.isChecked = config.buyParams.isUpfrontCostInclusive
        updateUpfrontCostState(config.buyParams.isUpfrontCostInclusive)
        binding.etInterestRate.setText(formatNumberForInput(config.buyParams.loanInterestRateAnnual))
        binding.etLoanTenure.setText(config.buyParams.loanTenureYears.toString())
        binding.etPartPrepaymentAmount.setText(formatNumberForInput(config.buyParams.partPrepaymentAmount))
        binding.actvPrepaymentFrequency.setText(config.buyParams.partPrepaymentFrequency.displayName, false)
        binding.etPropertyAppreciation.setText(formatNumberForInput(config.buyParams.propertyAppreciationRateAnnual))
        binding.etSocietyMaintenance.setText(formatNumberForInput(config.buyParams.monthlySocietyMaintenance))
        binding.etMaintenanceEscalation.setText(formatNumberForInput(config.buyParams.maintenanceEscalationRateAnnual))
        binding.etPropertyTaxAmount.setText(formatNumberForInput(config.buyParams.propertyTaxAnnualAmount))
        binding.etPropertyUpkeepAmount.setText(formatNumberForInput(config.buyParams.propertyUpkeepAnnualAmount))

        binding.etInitialRent.setText(formatNumberForInput(config.rentParams.initialMonthlyRent))
        binding.etRentEscalation.setText(formatNumberForInput(config.rentParams.rentEscalationRateAnnual))
        binding.etSecurityDepositValue.setText(formatNumberForInput(config.rentParams.securityDepositValue))
        binding.etAnnualRelocationCost.setText(formatNumberForInput(config.rentParams.annualRelocationCost))
        binding.etBrokerage.setText(formatNumberForInput(config.rentParams.annualRentalBrokerage))

        binding.etInvestmentReturn.setText(formatNumberForInput(config.investmentParams.annualReturnRatePercent))
        binding.etTaxRate.setText(formatNumberForInput(config.investmentParams.taxRatePercent))
        binding.actvInvestmentCompoundingFrequency.setText(config.investmentParams.compoundingFrequency.displayName, false)

        binding.toggleStrategy.check(if (config.buyParams.isLoanSelected) R.id.btnStrategyLoan else R.id.btnStrategyCash)
        binding.cardLoanParameters.visibility = if (config.buyParams.isLoanSelected) View.VISIBLE else View.GONE
        binding.toggleDownPaymentUnit.check(if (config.buyParams.isDownPaymentPercent) R.id.btnDpPercent else R.id.btnDpAmount)
        binding.toggleDepositUnit.check(if (config.rentParams.isSecurityDepositFixed) R.id.btnDepositAmount else R.id.btnDepositMonths)
    }

    private fun syncUiToViewModel() {
        val title = binding.etProfileTitle.text.toString().ifBlank { "My Home Buy vs Rent Check" }
        viewModel.updateProfileTitle(title)

        val propertyPrice = binding.etPropertyPrice.text.toString().toDoubleOrNull() ?: 0.0
        val downPaymentValue = binding.etDownPaymentValue.text.toString().toDoubleOrNull() ?: 0.0
        val upfrontCostPercent = binding.etUpfrontCost.text.toString().toDoubleOrNull() ?: 0.0
        val isInclusive = binding.cbUpfrontCostInclusive.isChecked
        val interestRate = binding.etInterestRate.text.toString().toDoubleOrNull() ?: 0.0
        val tenureYears = binding.etLoanTenure.text.toString().toIntOrNull() ?: 0
        val prepayAmount = binding.etPartPrepaymentAmount.text.toString().toDoubleOrNull() ?: 0.0

        val prepayFreqText = binding.actvPrepaymentFrequency.text.toString()
        val prepayFreq = PrepaymentFrequency.entries.find { it.displayName.equals(prepayFreqText, ignoreCase = true) } ?: PrepaymentFrequency.NONE

        val propertyAppreciation = binding.etPropertyAppreciation.text.toString().toDoubleOrNull() ?: 0.0
        val societyMaintenance = binding.etSocietyMaintenance.text.toString().toDoubleOrNull() ?: 0.0
        val maintenanceEscalation = binding.etMaintenanceEscalation.text.toString().toDoubleOrNull() ?: 0.0
        val propertyTax = binding.etPropertyTaxAmount.text.toString().toDoubleOrNull() ?: 0.0
        val propertyUpkeep = binding.etPropertyUpkeepAmount.text.toString().toDoubleOrNull() ?: 0.0
        val isLoanSelected = binding.toggleStrategy.checkedButtonId == R.id.btnStrategyLoan
        val isDpPercent = binding.toggleDownPaymentUnit.checkedButtonId == R.id.btnDpPercent

        viewModel.updateBuyParams {
            it.copy(
                propertyPrice = propertyPrice,
                downPaymentValue = downPaymentValue,
                isDownPaymentPercent = isDpPercent,
                loanInterestRateAnnual = interestRate,
                loanTenureYears = tenureYears,
                upfrontCostPercent = upfrontCostPercent,
                isUpfrontCostInclusive = isInclusive,
                partPrepaymentAmount = prepayAmount,
                partPrepaymentFrequency = prepayFreq,
                propertyAppreciationRateAnnual = propertyAppreciation,
                monthlySocietyMaintenance = societyMaintenance,
                maintenanceEscalationRateAnnual = maintenanceEscalation,
                propertyUpkeepAnnualAmount = propertyUpkeep,
                propertyTaxAnnualAmount = propertyTax,
                isLoanSelected = isLoanSelected
            )
        }

        val initialRent = binding.etInitialRent.text.toString().toDoubleOrNull() ?: 0.0
        val rentEscalation = binding.etRentEscalation.text.toString().toDoubleOrNull() ?: 0.0
        val depositValue = binding.etSecurityDepositValue.text.toString().toDoubleOrNull() ?: 0.0
        val isDepositFixed = binding.toggleDepositUnit.checkedButtonId == R.id.btnDepositAmount
        val relocationCost = binding.etAnnualRelocationCost.text.toString().toDoubleOrNull() ?: 0.0
        val brokerage = binding.etBrokerage.text.toString().toDoubleOrNull() ?: 0.0

        viewModel.updateRentParams {
            it.copy(
                initialMonthlyRent = initialRent,
                rentEscalationRateAnnual = rentEscalation,
                securityDepositValue = depositValue,
                isSecurityDepositFixed = isDepositFixed,
                annualRelocationCost = relocationCost,
                annualRentalBrokerage = brokerage
            )
        }

        val returnRate = binding.etInvestmentReturn.text.toString().toDoubleOrNull() ?: 0.0
        val taxRate = binding.etTaxRate.text.toString().toDoubleOrNull() ?: 0.0

        val compoundingFreqText = binding.actvInvestmentCompoundingFrequency.text.toString()
        val compoundingFreq = CompoundingFrequency.entries.find { it.displayName.equals(compoundingFreqText, ignoreCase = true) } ?: CompoundingFrequency.MONTHLY

        viewModel.updateInvestmentParams {
            it.copy(
                annualReturnRatePercent = returnRate,
                taxRatePercent = taxRate,
                compoundingFrequency = compoundingFreq
            )
        }
    }

    private fun animateResultCard() {
        binding.cardResultSummary.alpha = 0f
        binding.cardResultSummary.scaleX = 0.95f
        binding.cardResultSummary.scaleY = 0.95f
        binding.cardResultSummary.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(400)
            .start()
    }

    private fun updateUpfrontCostState(isInclusive: Boolean) {
        binding.tilUpfrontCost.isEnabled = !isInclusive
        binding.etUpfrontCost.isEnabled = !isInclusive
        binding.tilUpfrontCost.alpha = if (isInclusive) 0.5f else 1.0f
    }

    private fun formatNumberForInput(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toLong().toString()
        } else {
            value.toString()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
