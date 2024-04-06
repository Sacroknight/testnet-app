package com.qos.testnet.ui.home

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qos.testnet.permissionmanager.PermissionPreferences
import com.qos.testnet.tests.DownloadSpeedTest
import com.qos.testnet.tests.PingAndJitterTest
import com.qos.testnet.tests.TestCallback
import com.qos.testnet.utils.deviceInformation.DeviceInformation
import com.qos.testnet.utils.deviceInformation.LocationInfo
import com.qos.testnet.utils.networkInformation.GetBetterHost
import com.qos.testnet.utils.networkInformation.NetworkCallback
import kotlinx.coroutines.launch

/**
 * The Home view model
 * Creates the view model for the MVVM architecture
 */
class HomeViewModel(homeContext: Context) : ViewModel() {

    @SuppressLint("StaticFieldLeak")
    private val context = homeContext
    private val getBetterHost = GetBetterHost()
    private val downloadSpeedTest = DownloadSpeedTest()
    val pingAndJitterTest = PingAndJitterTest()
    private val deviceInformation = DeviceInformation(context)
    private val locationInfo = LocationInfo(context)

    //private val uploadSpeedStats = UploadSpeedStats()

    private var dontAskAgain = false
    private var dontAskAgainDenied = false
    private var success: Boolean = false

    private var currentLocation: String? = null

    /**
     * Start the location retrieval process
     */
    private fun startLocationRetrieval() {
        viewModelScope.launch {
            locationInfo.locationFlow()
                .collect { location ->
                    // Manejar la ubicación obtenida
                    when (location.provider) {
                        LocationManager.GPS_PROVIDER -> {
                            // Ubicación GPS obtenida
                            currentLocation =
                                "${locationInfo.currentLatitudeGPS} , ${locationInfo.currentLongitudeGPS}"
                            getBestHostAndStartTasks()
                        }

                        LocationManager.NETWORK_PROVIDER -> {
                            // Ubicación de red obtenida
                            currentLocation =
                                "${locationInfo.currentLatitudeNetwork} , ${locationInfo.currentLongitudeNetwork}"
                            getBestHostAndStartTasks()
                        }

                        else -> {
                            currentLocation =
                                "${locationInfo.currentLongitudeApi} , ${locationInfo.currentLatitudeApi}"
                            getBestHostAndStartTasks()
                        }
                    }
                }

        }
    }

    /**
     * Start ping and jitter test
     */
    private fun startPingAndJitterTest() {
        Thread {
            pingAndJitterTest.startPingAndJitterTest(object : TestCallback {
                override fun OnTestStart() {
                    deviceInfo.postValue("")
                }

                override fun OnTestSuccess(jitter: String) {
                    pingMeasurement.postValue(pingAndJitterTest.pingMeasurement.value)
                    jitterMeasurement.postValue(pingAndJitterTest.jitterMeasurement.value)
                    instantMeasurements.postValue("")
                    progress.postValue(0)
                    Handler(Looper.getMainLooper()).postDelayed(
                        { this@HomeViewModel.startDownloadSpeedTest() }, 500
                    )
                }

                override fun OnTestBackground(
                    currentBackgroundMeasurement: String,
                    currentBackgroundProgress: Int
                ) {
                    instantMeasurements.postValue(currentBackgroundMeasurement)
                    progress.postValue(currentBackgroundProgress)
                }

            })
        }.start()
    }


    /**
     * Start download speed test
     */
    fun startDownloadSpeedTest() {
        Thread {
            try {
                downloadSpeedTest.startSpeedTest(
                    getBetterHost.getUrlAddress(),
                    object : TestCallback {
                        override fun OnTestStart() {
                            deviceInfo.postValue("")
                        }

                        override fun OnTestSuccess(downloadSpeed: String) {
                            finalDownloadRate.postValue(downloadSpeed)
                        }

                        override fun OnTestBackground(
                            currentBackgroundMeasurement: String,
                            currentBackgroundProgress: Int
                        ) {
                            progress.postValue(currentBackgroundProgress)
                            instantMeasurements.postValue(currentBackgroundMeasurement)
                        }


                    })
            } finally {
                progress.postValue(0)
                instantMeasurements.postValue("")
                deviceInfo.postValue(getDeviceInformation())
                isFinished.postValue(true)
            }
        }.start()
    }

    /**
     * The network callback.
     */
    private var networkCallback: NetworkCallback = object : NetworkCallback {
        override fun onRequestSuccess(response: String) {
            availableServers.postValue(response)
        }

        override fun onRequestFailure(error: String) {
            deviceInfo.postValue(error)
            Toast.makeText(context, "Failure on the request of the best host", Toast.LENGTH_LONG)
                .show()
        }
    }


    /**
     * new constructor
     */
    init {
        viewModelScope.launch {
            updatePreferences()
        }
    }

    /**
     * Gets device info text.
     *
     * @return the device info text
     */
    private fun getDeviceInformation(): String {
        return """Manufacturer: ${deviceInformation.manufacturer}
            Model: ${deviceInformation.model}
            Android Version: ${deviceInformation.androidVersion}
            Actual Location: ${locationInfo.currentLatitudeGPS} , ${locationInfo.currentLongitudeGPS}
            Approximate Location: ${locationInfo.currentLongitudeNetwork} , ${locationInfo.currentLatitudeNetwork}
            Api Location: ${locationInfo.currentLongitudeApi} , ${locationInfo.currentLatitudeApi}
            Signal Strength: ${deviceInformation.carrier} ${deviceInformation.signalStrength} dBm
            Current Host: ${pingAndJitterTest.currentHost}
            Ping: ${pingAndJitterTest.pingMeasured} ms
            Jitter: ${pingAndJitterTest.jitterMeasured} ms
            Best server: ${getBetterHost.getUrlAddress()}
            Download Speed: ${downloadSpeedTest.finalDownloadSpeed} Mb/s"""
    }

    /**
     * Update preferences.
     */
    private suspend fun updatePreferences() {
        val permissionPreferences = PermissionPreferences.getInstance()
        dontAskAgain = permissionPreferences.getPermissionPreference(
            context,
            PermissionPreferences.PermissionPreferencesKeys.DONT_ASK_AGAIN_LOCATION_PERMISSION
        )
        dontAskAgainDenied = permissionPreferences.getPermissionPreference(
            context,
            PermissionPreferences.PermissionPreferencesKeys.DONT_ASK_AGAIN_PHONE_PERMISSION
        )
    }

    /**
     * Start tasks.
     */
    fun startTasks() {
        deviceInformation.retrieveSignalStrength(dontAskAgain, dontAskAgainDenied)
        startLocationRetrieval()
    }

    private fun getBestHost() {
        getBetterHost.getBestHost(
            locationInfo.currentLocation ?: "0.0,0.0",
            deviceInformation.carrier ?: "Carrier NULL",
            networkCallback
        )
    }

    /**
     * Initiates a new thread to asynchronously determine the best host for conducting network tests,
     * including ping and jitter measurements.
     *
     *
     * The process involves querying a service to obtain the optimal host based on location and carrier information.
     * Once the best host is identified, the ping and jitter tasks are initiated,
     * and the user interface is updated with the results upon completion.
     *
     *
     *
     *
     *
     * **Key Functionality:**
     * - Asynchronously identifies the optimal host for network tests.
     * - Initiates ping and jitter measurements on the chosen host.
     *
     */
    private fun getBestHostAndStartTasks() {
        Thread {
            getBestHost()
            Handler(Looper.getMainLooper()).postDelayed({
                startPingAndJitterTest()
            }, 100)
        }.start()
    }


    companion object {
        @JvmStatic
        var deviceInfo: MutableLiveData<String> = MutableLiveData()
            private set
        var pingMeasurement: MutableLiveData<String?> = MutableLiveData()
            private set

        @JvmStatic
        var instantMeasurements: MutableLiveData<String> = MutableLiveData()
            private set

        @JvmStatic
        var jitterMeasurement: MutableLiveData<String?> = MutableLiveData()
            private set

        @JvmStatic
        var progress: MutableLiveData<Int> = MutableLiveData()
            private set
        private var finalDownloadRate = MutableLiveData<String>()
        private val availableServers = MutableLiveData<String>()

        @JvmField
        val isFinished: MutableLiveData<Boolean> = MutableLiveData()
    }
}

