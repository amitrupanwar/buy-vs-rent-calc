package com.amitr.buyvsrentcalc.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.amitr.buyvsrentcalc.R
import com.amitr.buyvsrentcalc.databinding.FragmentHistoryBinding
import com.amitr.buyvsrentcalc.domain.model.HomeDecisionConfig
import com.amitr.buyvsrentcalc.ui.home.HomeCalculatorViewModel
import com.amitr.buyvsrentcalc.util.CurrencyFormatter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeCalculatorViewModel by activityViewModels()
    private lateinit var historyAdapter: HistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, layoutInstanceState: Bundle?) {
        super.onViewCreated(view, layoutInstanceState)

        setupRecyclerView()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        val symbol = CurrencyFormatter.getSavedCurrency(requireContext())
        historyAdapter.currencySymbol = symbol
        val currentList = viewModel.savedDecisions.value
        updateHistoryUi(currentList)
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter(
            onItemClick = { config ->
                viewModel.loadDecision(config.id)
                Toast.makeText(requireContext(), "Loaded '${config.profileTitle}' into Calculator", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.nav_home_calc)
            },
            onDeleteClick = { config ->
                showDeleteConfirmDialog(config)
            }
        )

        val symbol = CurrencyFormatter.getSavedCurrency(requireContext())
        historyAdapter.currencySymbol = symbol

        binding.rvHistoryList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historyAdapter
        }
    }

    private fun updateHistoryUi(list: List<HomeDecisionConfig>) {
        val count = list.size
        if (count == 0) {
            binding.layoutHistoryEmpty.visibility = View.VISIBLE
            binding.rvHistoryList.visibility = View.GONE
            binding.tvHistoryCountHeader.text = "No saved decision checks found"
        } else {
            binding.layoutHistoryEmpty.visibility = View.GONE
            binding.rvHistoryList.visibility = View.VISIBLE
            binding.tvHistoryCountHeader.text = "$count saved decision check(s) available"
            historyAdapter.submitList(ArrayList(list))
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.savedDecisions.collectLatest { list ->
                    updateHistoryUi(list)
                }
            }
        }
    }

    private fun showDeleteConfirmDialog(config: HomeDecisionConfig) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Decision Check")
            .setMessage("Are you sure you want to delete '${config.profileTitle}' from History?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteDecision(config)
                Toast.makeText(requireContext(), "Deleted '${config.profileTitle}'", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
