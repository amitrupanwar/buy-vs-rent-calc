package com.amitr.buyvsrentcalc.ui.home

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.amitr.buyvsrentcalc.R
import com.amitr.buyvsrentcalc.databinding.ItemTabularRowBinding
import com.amitr.buyvsrentcalc.databinding.ItemYearlySummaryBinding
import com.amitr.buyvsrentcalc.domain.model.ScheduleDisplayRow
import com.amitr.buyvsrentcalc.util.CurrencyFormatter

class ProgressionAdapter : ListAdapter<ScheduleDisplayRow, RecyclerView.ViewHolder>(DiffCallback) {

    enum class ViewStyle {
        CARD,
        TABULAR
    }

    var viewStyle: ViewStyle = ViewStyle.TABULAR
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var currencySymbol: String = "₹"
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var onItemClickListener: ((ScheduleDisplayRow) -> Unit)? = null

    override fun getItemViewType(position: Int): Int {
        return if (viewStyle == ViewStyle.TABULAR) VIEW_TYPE_TABULAR else VIEW_TYPE_CARD
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_TABULAR) {
            val binding = ItemTabularRowBinding.inflate(inflater, parent, false)
            TabularViewHolder(binding, onItemClickListener)
        } else {
            val binding = ItemYearlySummaryBinding.inflate(inflater, parent, false)
            CardViewHolder(binding, onItemClickListener)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        if (holder is TabularViewHolder) {
            holder.bind(item, currencySymbol)
        } else if (holder is CardViewHolder) {
            holder.bind(item, currencySymbol)
        }
    }

    class CardViewHolder(
        private val binding: ItemYearlySummaryBinding,
        private val onClickListener: ((ScheduleDisplayRow) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ScheduleDisplayRow, symbol: String) {
            binding.tvYearHeader.text = item.periodLabel
            binding.tvPropertyValue.text = CurrencyFormatter.format(item.propertyValue, symbol)
            binding.tvRemainingLoan.text = CurrencyFormatter.format(item.remainingLoan, symbol)
            binding.tvBuyNetWorth.text = CurrencyFormatter.format(item.buyNetWorth, symbol)
            binding.tvInvestmentPortfolio.text = CurrencyFormatter.format(item.investmentPortfolioValue, symbol)
            binding.tvRentNetWorth.text = CurrencyFormatter.format(item.rentNetWorth, symbol)

            val diff = item.netAdvantage
            if (diff >= 0) {
                binding.tvNetAdvantageBadge.text = "+${CurrencyFormatter.format(diff, symbol)} (Buy Wins)"
                binding.tvNetAdvantageBadge.setBackgroundResource(R.drawable.shape_badge_rounded)
            } else {
                binding.tvNetAdvantageBadge.text = "+${CurrencyFormatter.format(-diff, symbol)} (Rent Wins)"
                binding.tvNetAdvantageBadge.setBackgroundResource(R.drawable.shape_badge_rounded)
            }

            if (item.isYearly) {
                binding.root.setOnClickListener { onClickListener?.invoke(item) }
            } else {
                binding.root.setOnClickListener(null)
            }
        }
    }

    class TabularViewHolder(
        private val binding: ItemTabularRowBinding,
        private val onClickListener: ((ScheduleDisplayRow) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ScheduleDisplayRow, symbol: String) {
            binding.tvTabPeriod.text = item.periodLabel

            // Background distinguishing for Year Tally vs Monthly sub-rows
            when {
                item.isTally -> {
                    binding.layoutRowRoot.setBackgroundColor(Color.parseColor("#200284C7"))
                }
                !item.isYearly && item.periodIndex > 0 -> {
                    binding.layoutRowRoot.setBackgroundColor(Color.parseColor("#0A000000"))
                }
                else -> {
                    binding.layoutRowRoot.setBackgroundColor(Color.TRANSPARENT)
                }
            }

            // Buy Section
            binding.layoutTabLoanFields.visibility = if (item.isLoanSelected) View.VISIBLE else View.GONE
            binding.tvTabEmi.text = CurrencyFormatter.formatExact(item.emiPaid, symbol)
            binding.tvTabPrincipal.text = CurrencyFormatter.formatExact(item.principalPaid, symbol)
            binding.tvTabInterest.text = CurrencyFormatter.formatExact(item.interestPaid, symbol)
            binding.tvTabPendingLoan.text = CurrencyFormatter.formatExact(item.remainingLoan, symbol)
            binding.tvTabBuyExpenses.text = CurrencyFormatter.formatExact(item.buyExpenses, symbol)
            binding.tvTabPropertyValue.text = CurrencyFormatter.formatExact(item.propertyValue, symbol)
            binding.tvTabMoneyOutflowBuy.text = CurrencyFormatter.formatExact(item.moneyOutflowBuy, symbol)
            binding.tvTabCumBuyOutflow.text = CurrencyFormatter.formatExact(item.cumulativeBuyOutflow, symbol)
            binding.tvTabBuyNetWorth.text = CurrencyFormatter.formatExact(item.buyNetWorth, symbol)

            // Rent Section
            binding.tvTabRentPaid.text = CurrencyFormatter.formatExact(item.monthlyRentPaid, symbol)
            binding.tvTabMoneyOutflowRent.text = CurrencyFormatter.formatExact(item.moneyOutflowRent, symbol)
            binding.tvTabInvestInjected.text = CurrencyFormatter.formatExact(item.moneyInjected, symbol)
            binding.tvTabPortfolioValue.text = CurrencyFormatter.formatExact(item.investmentPortfolioValue, symbol)
            binding.tvTabCumRentOutflow.text = CurrencyFormatter.formatExact(item.cumulativeRentOutflow, symbol)
            binding.tvTabRentNetWorth.text = CurrencyFormatter.formatExact(item.rentNetWorth, symbol)

            // Advantage
            val diff = item.netAdvantage
            val formattedDiff = CurrencyFormatter.formatExact(Math.abs(diff), symbol)
            val winnerText = if (diff >= 0) "+$formattedDiff Buy" else "+$formattedDiff Rent"
            binding.tvTabAdvantage.text = winnerText

            if (item.isYearly) {
                binding.root.setOnClickListener { onClickListener?.invoke(item) }
            } else {
                binding.root.setOnClickListener(null)
            }
        }
    }

    companion object {
        private const val VIEW_TYPE_CARD = 1
        private const val VIEW_TYPE_TABULAR = 2

        object DiffCallback : DiffUtil.ItemCallback<ScheduleDisplayRow>() {
            override fun areItemsTheSame(oldItem: ScheduleDisplayRow, newItem: ScheduleDisplayRow): Boolean {
                return oldItem.periodLabel == newItem.periodLabel &&
                        oldItem.isYearly == newItem.isYearly &&
                        oldItem.yearIndex == newItem.yearIndex &&
                        oldItem.periodIndex == newItem.periodIndex &&
                        oldItem.isTally == newItem.isTally
            }

            override fun areContentsTheSame(oldItem: ScheduleDisplayRow, newItem: ScheduleDisplayRow): Boolean {
                return oldItem == newItem
            }
        }
    }
}
