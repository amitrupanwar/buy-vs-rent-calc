package com.amitr.buyvsrentcalc.ui.settings

import android.R
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.amitr.buyvsrentcalc.databinding.FragmentSettingsBinding
import com.amitr.buyvsrentcalc.util.CurrencyFormatter
import com.google.android.material.snackbar.Snackbar
import java.util.Locale

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRegionDisplay()
        setupCurrencyDropdown()
    }

    private fun setupRegionDisplay() {
        val locale = Locale.getDefault()
        val displayCountry = locale.displayCountry.ifBlank { "Default" }
        val countryCode = locale.country.ifBlank { "Global" }
        val defaultSymbol = CurrencyFormatter.getDefaultRegionCurrencySymbol()

        binding.tvDetectedRegion.text =
            "Detected Region: $displayCountry ($countryCode) → Auto-selected $defaultSymbol"
    }

    private fun setupCurrencyDropdown() {
        val options = CurrencyFormatter.CURRENCY_OPTIONS.map { it.first }
        val adapter = ArrayAdapter(requireContext(), R.layout.simple_dropdown_item_1line, options)
        binding.actvSettingsCurrency.setAdapter(adapter)

        val currentSymbol = CurrencyFormatter.getSavedCurrency(requireContext())
        val currentLabel = CurrencyFormatter.getOptionLabelForSymbol(currentSymbol)
        binding.actvSettingsCurrency.setText(currentLabel, false)

        binding.actvSettingsCurrency.setOnItemClickListener { _, _, position, _ ->
            val selectedLabel = options[position]
            val symbol = CurrencyFormatter.getSymbolFromOptionLabel(selectedLabel)
            CurrencyFormatter.saveCurrency(requireContext(), symbol)

            Snackbar.make(
                binding.root,
                "App-wide currency updated to $symbol",
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
