package com.qos.testnet.ui.home

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.qos.testnet.databinding.FragmentHomeBinding
import com.qos.testnet.ui.home.HomeViewModel.Companion.deviceInfo
import com.qos.testnet.ui.home.HomeViewModel.Companion.instantMeasurements
import com.qos.testnet.ui.home.HomeViewModel.Companion.jitterMeasurement
import com.qos.testnet.ui.home.HomeViewModel.Companion.progress
import com.qos.testnet.ui.home.HomeViewModel.Companion.visibilityOfProgress

/**
 * The Home fragment.
 */
class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var handler: Handler? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        handler = Handler(Looper.getMainLooper())
        val root: View = binding.root

        val factory = HomeViewModelFactory(requireContext())
        val homeViewModel = ViewModelProvider(this, factory).get(HomeViewModel::class.java)

        binding.startButton.setOnClickListener {
            binding.startButton.isEnabled = false
            homeViewModel.startTasks()
            binding.deviceInformation.gravity = View.TEXT_ALIGNMENT_CENTER
            binding.deviceInformation.text = "Corriendo test de velocidad"
        }

        // Observe the instant measurements and update the UI accordingly
        instantMeasurements.observe(viewLifecycleOwner) { s: String? ->
            binding.instantMeasurements.text = s
        }

        // Observe the changes on the button and update the UI accordingly
        HomeViewModel.isFinished.observe(viewLifecycleOwner) { testFinished: Boolean? ->
            binding.startButton.isEnabled = testFinished!!
        }

        // Observe the device info and update the UI accordingly
        deviceInfo.observe(viewLifecycleOwner) { deviceInfo: String? ->
            binding.deviceInformation.text = deviceInfo
        }

        // Observe the progress and update the UI accordingly
        progress.observe(viewLifecycleOwner) { progress: Int? ->
            binding.testProgressIndicator.progress = progress!!
        }

        jitterMeasurement.observe(viewLifecycleOwner) {}

        visibilityOfProgress.observe(viewLifecycleOwner) { visibility: Int? ->
            binding.testProgressIndicator.visibility = visibility ?: View.INVISIBLE
        }
        return root

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
