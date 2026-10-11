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
            Privacy Policy & Financial Disclaimer
            Effective Date: October 24, 2024 | Developer: Amit Rupanwar
            
            1. 100% Free & Zero Personal Data Requested:
            This app is completely free. We do NOT ask for or collect any personal information (no sign-up or registration required).
            
            2. Financial Calculation & Risk Disclaimer:
            All calculations, net worth projections, and break-even crossover points are mathematical estimations derived strictly from user-specified input parameters. This app is for informational scenario comparison only and does NOT constitute binding financial, investment, legal, or tax advice. Real-world property appreciation, interest rates, and tax rates vary substantially by region and market conditions. The final financial decision rests solely with the user. Users should consult certified financial planners before making investment decisions.
            
            3. On-Device Data Storage (Room DB):
            Your financial privacy is 100% guaranteed. All financial inputs, loan parameters, rent calculations, and saved decision profiles are stored strictly on your device using Android's Room Database. Financial data is NEVER transmitted to external servers.
            
            4. Google AdMob Advertising & Policy Links:
            Our application uses Google AdMob to display banner advertisements. Google AdMob and Google Play Services may collect non-personally identifiable technical information (such as Android Advertising ID, IP address, device model, and ad interaction metrics) to serve relevant ads.
            
            Official Google Policy Links:
            • Google Privacy Policy: https://policies.google.com/privacy
            • Google Ad Services: https://policies.google.com/technologies/partner-sites
            • Google AdMob Policy: https://support.google.com/admob/answer/7676680
            • Google Ads Settings: https://adssettings.google.com
            
            5. Ad Opt-Out & Data Management:
            You can opt-out of personalized ads via Android System Settings (Settings -> Google -> Ads). You can delete saved decision profiles anytime from the History tab or clear all app data in Android settings.
            
            6. Contact & Support:
            Email: amitrupanwar@gmail.com
            Repository: https://github.com/amitrupanwar/buy-vs-rent-calc
        """.trimIndent()

        AlertDialog.Builder(requireContext())
            .setTitle("Privacy Policy & Financial Disclaimer")
            .setMessage(policyText)
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
