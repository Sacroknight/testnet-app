package com.qos.testnet.ui.home

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.qos.testnet.databinding.FragmentHomeBinding
import com.qos.testnet.ui.home.HomeViewModel.Companion.downloadMeasurement
import com.qos.testnet.ui.home.HomeViewModel.Companion.downloadScore
import com.qos.testnet.ui.home.HomeViewModel.Companion.instantMeasurements
import com.qos.testnet.ui.home.HomeViewModel.Companion.jitterBonus
import com.qos.testnet.ui.home.HomeViewModel.Companion.jitterMeasurement
import com.qos.testnet.ui.home.HomeViewModel.Companion.locationProgress
import com.qos.testnet.ui.home.HomeViewModel.Companion.overallRating
import com.qos.testnet.ui.home.HomeViewModel.Companion.pingMeasurement
import com.qos.testnet.ui.home.HomeViewModel.Companion.pingScore
import com.qos.testnet.ui.home.HomeViewModel.Companion.progress
import com.qos.testnet.ui.home.HomeViewModel.Companion.signalStrength
import com.qos.testnet.ui.home.HomeViewModel.Companion.signalStrengthBonus
import com.qos.testnet.ui.home.HomeViewModel.Companion.uploadMeasurement
import com.qos.testnet.ui.home.HomeViewModel.Companion.uploadScore
import com.qos.testnet.ui.home.HomeViewModel.Companion.visibilityOfDownload
import com.qos.testnet.ui.home.HomeViewModel.Companion.visibilityOfJitter
import com.qos.testnet.ui.home.HomeViewModel.Companion.visibilityOfLocationProgress
import com.qos.testnet.ui.home.HomeViewModel.Companion.visibilityOfPing
import com.qos.testnet.ui.home.HomeViewModel.Companion.visibilityOfProgress
import com.qos.testnet.ui.home.HomeViewModel.Companion.visibilityOfScore
import com.qos.testnet.ui.home.HomeViewModel.Companion.visibilityOfUpload

/**
 * The Home fragment.
 */
class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var handler: Handler? = null

    @SuppressLint("DefaultLocale")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        handler = Handler(Looper.getMainLooper())
        val root: View = binding.root

        val factory = HomeViewModelFactory(requireContext())
        val homeViewModel = ViewModelProvider(this, factory)[HomeViewModel::class.java]

        setupInitialVisibility()
        setupButtonListeners(homeViewModel)
        observeViewModel()

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupInitialVisibility() {
        binding.cancelButton.visibility = View.GONE
        binding.locationProgressBar.visibility = View.GONE
        binding.testProgressIndicator.visibility = View.GONE
        binding.measurementPing.visibility = View.GONE
        binding.measurementJitter.visibility = View.GONE
        binding.downloadSpeedMeasurement.visibility = View.GONE
        binding.uploadSpeedMeasurement.visibility = View.GONE
        binding.signalStrength.visibility = View.GONE
        binding.signalStrengthBonus.visibility = View.GONE
        binding.score.visibility = View.GONE
    }

    private fun setupButtonListeners(homeViewModel: HomeViewModel) {
        binding.startButton.setOnClickListener {
            homeViewModel.startTasks()
        }

        binding.cancelButton.setOnClickListener {
            homeViewModel.cancelTasks()
        }
    }

    private fun observeViewModel() {

        // Observe the instant measurements and update the UI accordingly
        instantMeasurements.observe(viewLifecycleOwner) { s: String? ->
            binding.instantMeasurements.text = s
        }

        // Observe the changes on the button and update the UI accordingly
        HomeViewModel.isFinished.observe(viewLifecycleOwner) { testFinished: Boolean? ->
            binding.startButton.isEnabled = testFinished!!
            binding.cancelButton.isEnabled = !testFinished
            if (!testFinished) {
                binding.startButton.visibility = View.GONE
                binding.cancelButton.visibility = View.VISIBLE
                binding.testProgressIndicator.visibility = View.GONE
            } else {
                binding.startButton.visibility = View.VISIBLE
                binding.cancelButton.visibility = View.GONE
                binding.testProgressIndicator.visibility = View.GONE
            }
        }

        // Observe the progress and update the UI accordingly
        progress.observe(viewLifecycleOwner) { progress: Int? ->
            binding.testProgressIndicator.progress = progress!!
        }

        pingMeasurement.observe(viewLifecycleOwner) { s: String? ->
            binding.pingTextView.text = s
        }

        jitterMeasurement.observe(viewLifecycleOwner) { s: String? ->
            binding.jitterTextView.text = s
        }

        downloadMeasurement.observe(viewLifecycleOwner) { s: String? ->
            binding.downloadSpeedTextView.text = s
        }

        uploadMeasurement.observe(viewLifecycleOwner) { s: String? ->
            binding.uploadSpeedTextView.text = s
        }

        overallRating.observe(viewLifecycleOwner) { rating: Double? ->
            val formattedRating = rating?.let { String.format("%.2f", it) } ?: "0.00"
            binding.scoreTextView.text = formattedRating
        }

        visibilityOfProgress.observe(viewLifecycleOwner) { visibility: Int? ->
            binding.testProgressIndicator.visibility = visibility ?: View.INVISIBLE
        }

        visibilityOfPing.observe(viewLifecycleOwner) { visibility: Int? ->
            binding.measurementPing.visibility = visibility ?: View.INVISIBLE
        }

        visibilityOfJitter.observe(viewLifecycleOwner) { visibility: Int? ->
            binding.measurementJitter.visibility = visibility ?: View.INVISIBLE
        }

        visibilityOfDownload.observe(viewLifecycleOwner) { visibility: Int? ->
            binding.downloadSpeedMeasurement.visibility = visibility ?: View.INVISIBLE
        }

        visibilityOfUpload.observe(viewLifecycleOwner) { visibility: Int? ->
            binding.uploadSpeedMeasurement.visibility = visibility ?: View.INVISIBLE
        }

        visibilityOfLocationProgress.observe(viewLifecycleOwner) { visibility ->
            binding.locationProgressBar.visibility = visibility
        }

        visibilityOfScore.observe(viewLifecycleOwner) { visibility ->
            val actualVisibility = visibility ?: View.INVISIBLE
            binding.score.visibility = actualVisibility
            binding.individualPingScore.visibility = actualVisibility
            binding.individualJitterScore.visibility = actualVisibility
            binding.individualDownloadScore.visibility = actualVisibility
            binding.individualUploadScore.visibility = actualVisibility
            binding.signalStrengthBonus.visibility = actualVisibility
            binding.signalStrength.visibility = actualVisibility
        }

        pingScore.observe(viewLifecycleOwner) { s: String? ->
            binding.individualPingScore.text = s
        }
        jitterBonus.observe(viewLifecycleOwner) { s: String? ->
            binding.individualJitterScore.text = s
        }
        downloadScore.observe(viewLifecycleOwner) { s: String? ->
            binding.individualDownloadScore.text = s
        }
        uploadScore.observe(viewLifecycleOwner) { s: String? ->
            binding.individualUploadScore.text = s
        }
        signalStrengthBonus.observe(viewLifecycleOwner) { s: String? ->
            binding.signalStrengthBonus.text = s
        }
        signalStrength.observe(viewLifecycleOwner) { s: String? ->
            binding.signalStrength.text = s
        }

        locationProgress.observe(viewLifecycleOwner) { progress ->
            binding.locationProgressBar.progress = progress
        }
    }

    private fun startInternetCheck() {
        handler?.postDelayed(object : Runnable {
            override fun run() {
                val isConnected = isInternetAvailable(requireContext())
                binding.startButton.isEnabled = isConnected
                if (!isConnected) {
                    binding.instantMeasurements.text = "No internet access"
                } else {
                    binding.instantMeasurements.text = "Internet access"
                    // Usa postDelayed para limpiar el texto después de 200 milisegundos
                    binding.instantMeasurements.postDelayed({
                        binding.instantMeasurements.text = ""
                    }, 200)
                }
                handler?.postDelayed(this, 5000) // Repite la verificación cada 5 segundos
            }
        }, 0)
    }

    fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities =
            connectivityManager.getNetworkCapabilities(network) ?: return false
        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

}
