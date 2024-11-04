package com.qos.testnet.ui.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.qos.testnet.R
import com.qos.testnet.data.local.TestData
import com.qos.testnet.data.repository.RepositoryCRUD
import com.qos.testnet.databinding.FragmentTestResultsBinding
import com.qos.testnet.utils.AdapterHistory
import com.qos.testnet.viewmodel.TestResultsViewModel

class TestResultsFragment : Fragment() {

    private var _binding: FragmentTestResultsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: AdapterHistory
    private lateinit var testResultsViewModel: TestResultsViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTestResultsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val firestore = FirebaseFirestore.getInstance()
        val firebaseAuth = FirebaseAuth.getInstance()
        val repository = RepositoryCRUD(firestore, firebaseAuth, requireContext())

        testResultsViewModel = ViewModelProvider(
            this,
            TestResultsViewModelFactory(repository)
        )[TestResultsViewModel::class.java]

        adapter = AdapterHistory { testData ->
            showTestDetails(testData)
        }

        binding.historyRecyclerView.adapter = adapter
        binding.historyRecyclerView.layoutManager = LinearLayoutManager(context)

        testResultsViewModel.fetchData()
        testResultsViewModel.fetchedData.observe(viewLifecycleOwner) { dataList ->
            adapter.submitList(dataList)
        }

        return root
    }

    private fun showTestDetails(testData: TestData) {
        val bundle = Bundle().apply {
            putSerializable("testData", testData)
        }
        findNavController().navigate(R.id.testDetailsFragment, bundle)
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}