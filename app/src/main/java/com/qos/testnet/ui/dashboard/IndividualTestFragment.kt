package com.qos.testnet.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.qos.testnet.databinding.FragmentIndividualTestBinding

class IndividualTestFragment : Fragment() {
    private var binding: FragmentIndividualTestBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val individualTestViewModel =
            ViewModelProvider(this).get(IndividualTestViewModel::class.java)

        binding = FragmentIndividualTestBinding.inflate(inflater, container, false)
        val root: View = binding!!.root

        val textView = binding!!.textDashboard
        individualTestViewModel.text.observe(viewLifecycleOwner) { text: CharSequence? ->
            textView.text = text
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}