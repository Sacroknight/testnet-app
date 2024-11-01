package com.qos.testnet.ui.home

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.qos.testnet.data.local.TestData
import com.qos.testnet.data.repository.RepositoryCRUD
import com.qos.testnet.permissionmanager.PermissionPreferences
import com.qos.testnet.tests.DownloadSpeedTest
import com.qos.testnet.tests.PingAndJitterTest
import com.qos.testnet.tests.TestCallback
import com.qos.testnet.tests.UploadSpeedStats
import com.qos.testnet.utils.deviceinformation.DeviceInformation
import com.qos.testnet.utils.deviceinformation.LocationInfo
import com.qos.testnet.utils.network.GetBetterHost
import com.qos.testnet.utils.network.MobileNetworkQualityScoreCalculator
import com.qos.testnet.utils.network.NetworkCallback
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The Home view model
 * Creates the view model for the MVVM architecture
 */
class HomeViewModel(homeContext: Context) : ViewModel() {

    private val context by lazy { homeContext }
    private val getBetterHost = GetBetterHost()
    private val uploadSpeedStats = UploadSpeedStats()
    private val downloadSpeedTest = DownloadSpeedTest(context)
    val pingAndJitterTest = PingAndJitterTest()
    private val deviceInformation = DeviceInformation(context)
    private val locationInfo = LocationInfo(context)
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    private val firestore = FirebaseFirestore.getInstance()
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val userRepository: RepositoryCRUD = RepositoryCRUD(firestore, firebaseAuth, context)

    private var dontAskAgain = false
    private var dontAskAgainDenied = false

    private var userId: String = ""
    private var currentLocation: String? = null
    private var score: Double = 0.0

    /**
     * Start the location retrieval process
     */
    private fun startLocationRetrieval() {
        viewModelScope.launch {
            val locationResult =
                locationInfo.locationFlow(timeout = 10000L) // 10 segundos de tiempo de espera
                    .catch { exception ->
                        // Log the error, no need to handle it here since the catch block below will handle all exceptions
                        Log.e("LocationFlow", "Error occurred: ${exception.message}")
                    }
                    .firstOrNull() // Use firstOrNull to get the location if available, or null if the flow completes without a location

            if (locationResult != null) {
                // We got a location from GPS or network, handle it
                currentLocation = "${locationResult.latitude}, ${locationResult.longitude}"
                getBestHostAndStartTasks()
            } else {
                // GPS and network location failed, fallback to fetching location from API
                try {
                    val (latitude, longitude) = withContext(ioDispatcher) {
                        locationInfo.fetchLocationFromApi(LocationInfo.API_URL)
                    }
                    currentLocation = "$latitude, $longitude"
                    getBestHostAndStartTasks()
                } catch (e: Exception) {
                    Log.e("LocationFlow", "Error fetching location from API: ${e.message}")
                }
            }
        }
    }

    /**
     * Start ping and jitter test
     */
    private fun startPingAndJitterTest() {
        visibilityOfProgress.postValue(View.VISIBLE)
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
                    viewModelScope.launch {
                        userId = userRepository.getUserId()
                    }
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

                override fun OnTestFailed(errorMessage: String) {
                    visibilityOfProgress.postValue(View.GONE)
                    Log.e("PingAndJitterTest", "Test failed: $errorMessage")
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
                    getBetterHost.urlAddress,
                    object : TestCallback {
                        override fun OnTestStart() {
                            deviceInfo.postValue("")
                        }

                        override fun OnTestSuccess(downloadSpeed: String) {
                            finalDownloadRate.postValue(downloadSpeed)
                            Handler(Looper.getMainLooper()).postDelayed(
                                { this@HomeViewModel.startUploadSpeedTest() }, 500
                            )
                        }

                        override fun OnTestBackground(
                            currentBackgroundMeasurement: String,
                            currentBackgroundProgress: Int
                        ) {
                            progress.postValue(currentBackgroundProgress)
                            instantMeasurements.postValue(currentBackgroundMeasurement)
                        }

                        override fun OnTestFailed(errorMessage: String) {
                            Log.e("DownloadSpeedTest", "Test failed: $errorMessage")
                            visibilityOfProgress.postValue(View.GONE)
                        }


                    })
            } finally {
                progress.postValue(0)
                instantMeasurements.postValue("")
            }
        }.start()
    }

    /**
     * Start Upload Test
     */
    fun startUploadSpeedTest() {
        Thread {
            try {
                uploadSpeedStats.runUploadSpeedTest(object : TestCallback {
                    override fun OnTestStart() {
                        deviceInfo.postValue("")
                    }

                    override fun OnTestSuccess(uploadSpeed: String) {
                        finalUploadRate.postValue(uploadSpeed)
                        calculateOverallRating()
                    }

                    override fun OnTestBackground(
                        currentBackgroundMeasurement: String,
                        currentBackgroundProgress: Int
                    ) {
                        progress.postValue(currentBackgroundProgress)
                        instantMeasurements.postValue(currentBackgroundMeasurement)
                    }

                    override fun OnTestFailed(errorMessage: String) {
                        // Handle any errors that occur during the test
                        Log.e("UploadSpeedTest", "Test failed: $errorMessage")
                        visibilityOfProgress.postValue(View.GONE)
                    }
                }, getBetterHost.urlUploadAddress)
            } finally {
                progress.postValue(0)
                visibilityOfProgress.postValue(View.GONE)
                instantMeasurements.postValue("")
                deviceInfo.postValue(getDeviceInformation())
                isFinished.postValue(true)
                val newTestData = TestData(
                    dispositivo = deviceInformation.model,
                    fecha = "2024-10-11T15:00:00Z",
                    idVersionAndroid = deviceInformation.androidVersion.toInt(),
                    intensidadDeSenal = deviceInformation.signalStrength,
                    jitter = pingAndJitterTest.jitterMeasured,
                    operadorDeRed = deviceInformation.carrier,
                    ping = pingAndJitterTest.pingMeasured,
                    redScore = score,
                    servidor = getBetterHost.urlAddress,
                    tipoDeRed = deviceInformation.networkType,
                    ubicacion = currentLocation ?: "-1, -1",
                    userId = userId,
                    velocidadDeCarga = downloadSpeedTest.finalDownloadSpeed,
                    velocidadDeDescarga = uploadSpeedStats.finalUploadRate
                )
                userRepository.sendData(newTestData)
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
            |Model: ${deviceInformation.model}
            |Android Version: ${deviceInformation.androidVersion}
            |Actual Location: $currentLocation
            |GPS Location: ${locationInfo.currentLatitudeGPS} , ${locationInfo.currentLongitudeGPS}
            |Approximate Location: ${locationInfo.currentLatitudeNetwork} , ${locationInfo.currentLongitudeNetwork}
            |Api Location: ${locationInfo.currentLatitudeApi} , ${locationInfo.currentLongitudeApi}
            |Signal Strength: ${deviceInformation.carrier} ${deviceInformation.signalStrength} dBm
            |Current Host: ${pingAndJitterTest.currentHost}
            |Ping: ${pingAndJitterTest.pingMeasured} ms
            |Jitter: ${pingAndJitterTest.jitterMeasured} ms
            |Best server: ${getBetterHost.urlAddress}
            |Download Speed: ${downloadSpeedTest.finalDownloadSpeed} Mb/s
            |Upload Speed: ${uploadSpeedStats.finalUploadRate} Mb/s
            |Score: $score""".trimMargin()
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

    private fun calculateOverallRating() {
        val ping = pingAndJitterTest.pingMeasured
        val jitter = pingAndJitterTest.jitterMeasured
        val downloadSpeed = downloadSpeedTest.finalDownloadSpeed
        val uploadSpeed = uploadSpeedStats.finalUploadRate
        val signalStrength = deviceInformation.signalStrength.toDouble()

        score = MobileNetworkQualityScoreCalculator.calculateOverallScore(
            ping, jitter, downloadSpeed, uploadSpeed, signalStrength.toInt()
        )

        overallRating.postValue(score)
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
        private var finalUploadRate = MutableLiveData<String>()
        private val availableServers = MutableLiveData<String>()
        private val overallRating: MutableLiveData<Double> = MutableLiveData()
        val visibilityOfProgress: MutableLiveData<Int> = MutableLiveData()

        @JvmField
        val isFinished: MutableLiveData<Boolean> = MutableLiveData()
    }
}

