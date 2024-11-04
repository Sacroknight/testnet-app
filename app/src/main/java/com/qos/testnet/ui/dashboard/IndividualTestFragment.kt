package com.qos.testnet.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.qos.testnet.databinding.FragmentIndividualTestBinding
import com.qos.testnet.utils.AdapterResults
import kotlinx.coroutines.launch

class IndividualTestFragment : Fragment() {

    private var _binding: FragmentIndividualTestBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: AdapterResults

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentIndividualTestBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val factory = IndividualTestViewModelFactory(requireContext())
        val individualTestViewModel =
            ViewModelProvider(this, factory)[IndividualTestViewModel::class.java]

        lifecycleScope.launch {
            individualTestViewModel.fetchData()
        }

        adapter = AdapterResults()
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(context)

        individualTestViewModel.fetchedData.observe(viewLifecycleOwner) { dataList ->
            adapter.submitList(dataList)
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}