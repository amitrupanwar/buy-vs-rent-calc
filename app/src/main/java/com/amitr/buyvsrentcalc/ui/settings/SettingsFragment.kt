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
            Privacy Policy for Buy vs Rent Calculator
            Effective Date: 24 Oct 2024
            
            1. Overview & Data Storage:
            Your privacy is our priority. All financial parameters, profile titles, and saved decision checks are stored 100% locally on your device using Android's Room Database. We do not transmit or store your financial data on external servers.
            
            2. Advertising (Google AdMob):
            Our application uses Google AdMob to serve non-intrusive banner advertisements. Google AdMob and Google Play Services may collect non-personally identifiable technical information (such as Advertising IDs, device model, IP address, and ad interaction metrics) to serve relevant ads and measure ad performance.
            
            3. Privacy Choices & Advertising Opt-Out:
            You can opt-out of personalized ads or reset your Advertising ID at any time in your Android system settings under Settings -> Google -> Ads -> Opt out of Ads Personalization.
            
            4. Local Data Management:
            You can delete saved decision checks anytime from the History tab or clear all app data in Android system settings.
            
            5. Contact Us:
            For privacy questions or support, contact developer Amit Rupanwar at:
            Email: amitrupanwar@gmail.com
            GitHub: https://github.com/amitrupanwar/buy-vs-rent-calc
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
