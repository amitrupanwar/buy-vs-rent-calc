package com.amitr.buyvsrentcalc.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.amitr.buyvsrentcalc.R
import com.amitr.buyvsrentcalc.databinding.ItemHistoryCardBinding
import com.amitr.buyvsrentcalc.domain.engine.HomeCalculatorEngine
import com.amitr.buyvsrentcalc.domain.model.FinancialWinner
import com.amitr.buyvsrentcalc.domain.model.HomeDecisionConfig
import com.amitr.buyvsrentcalc.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter(
    private val onItemClick: (HomeDecisionConfig) -> Unit,
    private val onDeleteClick: (HomeDecisionConfig) -> Unit
) : ListAdapter<HomeDecisionConfig, HistoryAdapter.ViewHolder>(DiffCallback) {

    var currencySymbol: String = "₹"
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoryCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, currencySymbol, onItemClick, onDeleteClick)
    }

    class ViewHolder(private val binding: ItemHistoryCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            item: HomeDecisionConfig,
            symbol: String,
            onItemClick: (HomeDecisionConfig) -> Unit,
            onDeleteClick: (HomeDecisionConfig) -> Unit
        ) {
            binding.tvHistoryTitle.text = item.profileTitle

            val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            val dateStr = dateFormat.format(Date(item.createdTimestamp))
            binding.tvHistoryDate.text = "Saved: $dateStr"

            binding.tvHistoryHorizon.text = "${item.evaluationHorizonYears} Yrs Horizon"

            val priceFormatted = CurrencyFormatter.format(item.buyParams.propertyPrice, symbol)
            val rentFormatted = CurrencyFormatter.format(item.rentParams.initialMonthlyRent, symbol)
            binding.tvHistoryDetails.text = "Price: $priceFormatted | Rent: $rentFormatted/mo"

            val result = HomeCalculatorEngine.calculate(item)
            val winnerText = when (result.financialWinner) {
                FinancialWinner.BUYING -> "BUY WINS"
                FinancialWinner.RENTING -> "RENT WINS"
                FinancialWinner.EQUIVALENT -> "EQUIVALENT"
            }
            binding.tvHistoryWinnerBadge.text = winnerText

            binding.root.setOnClickListener { onItemClick(item) }
            binding.ibHistoryDelete.setOnClickListener { onDeleteClick(item) }
        }
    }

    companion object {
        object DiffCallback : DiffUtil.ItemCallback<HomeDecisionConfig>() {
            override fun areItemsTheSame(oldItem: HomeDecisionConfig, newItem: HomeDecisionConfig): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: HomeDecisionConfig, newItem: HomeDecisionConfig): Boolean {
                return oldItem == newItem
            }
        }
    }
}
