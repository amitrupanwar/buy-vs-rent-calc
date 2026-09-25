package com.amitr.buyvsrentcalc.util

import android.content.Context
import java.util.Currency
import java.util.Locale

object CurrencyFormatter {

    private const val PREF_NAME = "buy_vs_rent_prefs"
    private const val KEY_CURRENCY_SYMBOL = "currency_symbol"

    val CURRENCY_OPTIONS = listOf(
        "₹ (INR)" to "₹",
        "$ (USD)" to "$",
        "€ (EUR)" to "€",
        "£ (GBP)" to "£",
        "A$ (AUD)" to "A$",
        "C$ (CAD)" to "C$",
        "¥ (JPY)" to "¥"
    )

    fun getDefaultRegionCurrencySymbol(): String {
        return try {
            val locale = Locale.getDefault()
            val country = locale.country.uppercase(Locale.ROOT)
            when (country) {
                "IN" -> "₹"
                "US" -> "$"
                "GB" -> "£"
                "JP" -> "¥"
                "AU" -> "A$"
                "CA" -> "C$"
                "DE", "FR", "IT", "ES", "NL", "BE", "AT", "GR", "FI", "PT", "IE", "LU", "SK", "SI", "EE", "LV", "LT", "CY", "MT" -> "€"
                else -> {
                    val currency = Currency.getInstance(locale)
                    val symbol = currency.symbol
                    when {
                        symbol.contains("₹") -> "₹"
                        symbol.contains("$") -> "$"
                        symbol.contains("€") -> "€"
                        symbol.contains("£") -> "£"
                        symbol.contains("¥") -> "¥"
                        else -> "₹"
                    }
                }
            }
        } catch (e: Exception) {
            "₹"
        }
    }

    fun getSavedCurrency(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_CURRENCY_SYMBOL)) {
            val defaultSymbol = getDefaultRegionCurrencySymbol()
            prefs.edit().putString(KEY_CURRENCY_SYMBOL, defaultSymbol).apply()
            return defaultSymbol
        }
        return prefs.getString(KEY_CURRENCY_SYMBOL, "₹") ?: "₹"
    }

    fun saveCurrency(context: Context, symbol: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CURRENCY_SYMBOL, symbol).apply()
    }

    fun getOptionLabelForSymbol(symbol: String): String {
        return CURRENCY_OPTIONS.find { it.second == symbol }?.first ?: "$symbol (Currency)"
    }

    fun getSymbolFromOptionLabel(label: String): String {
        return CURRENCY_OPTIONS.find { it.first == label }?.second ?: label.takeWhile { !it.isWhitespace() }
    }

    fun format(value: Double, symbol: String): String {
        val absValue = Math.abs(value)
        val sign = if (value < 0) "-" else ""

        return when (symbol) {
            "₹" -> {
                when {
                    absValue >= 10_000_000 -> String.format(Locale.getDefault(), "%s₹%.2f Cr", sign, absValue / 10_000_000.0)
                    absValue >= 100_000 -> String.format(Locale.getDefault(), "%s₹%.2f L", sign, absValue / 100_000.0)
                    else -> String.format(Locale.getDefault(), "%s₹%,.0f", sign, absValue)
                }
            }
            else -> {
                when {
                    absValue >= 1_000_000 -> String.format(Locale.getDefault(), "%s%s%.2f M", sign, symbol, absValue / 1_000_000.0)
                    absValue >= 1_000 -> String.format(Locale.getDefault(), "%s%s%.1f k", sign, symbol, absValue / 1_000.0)
                    else -> String.format(Locale.getDefault(), "%s%s%,.0f", sign, symbol, absValue)
                }
            }
        }
    }

    fun formatExact(value: Double, symbol: String): String {
        val absValue = Math.abs(value)
        val sign = if (value < 0) "-" else ""
        val rounded = Math.round(absValue)
        val formattedNumber = String.format(Locale.US, "%,d", rounded)
        return "$sign$symbol$formattedNumber"
    }
}
