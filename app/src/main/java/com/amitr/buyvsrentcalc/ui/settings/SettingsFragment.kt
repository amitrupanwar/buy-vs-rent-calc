package com.amitr.buyvsrentcalc.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
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
        setupPrivacyPolicyButton()
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
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, options)
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

    private fun setupPrivacyPolicyButton() {
        binding.btnViewPrivacyPolicy.setOnClickListener {
            showPrivacyPolicyDialog()
        }
    }

    private fun showPrivacyPolicyDialog() {
        val policyText = """
            Privacy Policy — Buy vs Rent Calculator
            Effective Date: October 24, 2024
            Developer: Amit Rupanwar
            
            1. On-Device Data Storage:
            Your financial privacy is 100% guaranteed. All financial inputs, loan parameters, rent calculations, and saved decision profiles are stored strictly on your device using Android's Room Database. Your financial data is NEVER transmitted, uploaded, or shared with external servers.
            
            2. Google AdMob & Advertising Data:
            Our application uses Google AdMob to display banner advertisements. Google AdMob and Google Play Services may collect non-personally identifiable technical information (such as Android Advertising ID, IP address, device model, and ad interaction metrics) to serve relevant ads and measure performance.
            
            3. User Choices & Ad Opt-Out:
            You can opt-out of personalized ads or reset your Advertising ID at any time via Android System Settings (Settings -> Google -> Ads -> Opt out of Ads Personalization).
            
            4. Managing & Deleting Saved Profiles:
            You can delete saved decision checks anytime from the History tab, or clear all app data permanently in Android System Settings.
            
            5. Children's Privacy:
            This app does not knowingly collect personal data from children under 13.
            
            6. Contact & Support:
            Email: amitrupanwar@gmail.com
            Repository: https://github.com/amitrupanwar/buy-vs-rent-calc
        """.trimIndent()

        AlertDialog.Builder(requireContext())
            .setTitle("Privacy Policy & Data Security")
            .setMessage(policyText)
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
