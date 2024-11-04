package com.qos.testnet.ui.home

import android.annotation.SuppressLint
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
import com.qos.testnet.R
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
    private val mobileScore = MobileNetworkQualityScoreCalculator()
    private val downloadSpeedTest = DownloadSpeedTest(context)
    private val pingAndJitterTest = PingAndJitterTest()
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
        updateVisibility(
            visibilityOfScore to View.GONE,
            visibilityOfJitter to View.GONE,
            visibilityOfDownload to View.GONE,
            visibilityOfUpload to View.GONE
        )
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
        updateVisibility(
            visibilityOfProgress to View.VISIBLE,
            visibilityOfPing to View.VISIBLE,
        )
        Thread {
            pingAndJitterTest.startPingAndJitterTest(object : TestCallback {
                override fun OnTestStart() {
                    pingMeasurement.postValue("")
                }

                override fun OnTestSuccess(jitter: String) {
                    updateVisibility(
                        visibilityOfJitter to View.VISIBLE
                    )
                    pingMeasurement.postValue("${pingAndJitterTest.pingMeasured} ms")
                    jitterMeasurement.postValue("${pingAndJitterTest.jitterMeasured} ms")
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
                    pingMeasurement.postValue(currentBackgroundMeasurement)
                    progress.postValue(currentBackgroundProgress)
                }

                override fun OnTestFailed(errorMessage: String) {
                    updateVisibility(
                        visibilityOfProgress to View.GONE,
                        visibilityOfJitter to View.GONE
                    )
                    pingMeasurement.postValue(errorMessage)
                    Log.e("PingAndJitterTest", "Test failed: $errorMessage")
                }
            })
        }.start()
    }

    /**
     * Start download speed test
     */
    fun startDownloadSpeedTest() {
        updateVisibility(
            visibilityOfDownload to View.VISIBLE
        )
        Thread {
            try {
                downloadSpeedTest.startSpeedTest(
                    getBetterHost.urlAddress,
                    object : TestCallback {
                        override fun OnTestStart() {
                            downloadMeasurement.postValue("")
                            updateVisibility(
                                visibilityOfDownload to View.VISIBLE
                            )
                        }

                        override fun OnTestSuccess(downloadSpeed: String) {
                            downloadMeasurement.postValue("${downloadSpeedTest.finalDownloadSpeed} Mb/s")
                            Handler(Looper.getMainLooper()).postDelayed(
                                { this@HomeViewModel.startUploadSpeedTest() }, 500
                            )
                        }

                        override fun OnTestBackground(
                            currentBackgroundMeasurement: String,
                            currentBackgroundProgress: Int
                        ) {
                            progress.postValue(currentBackgroundProgress)
                            downloadMeasurement.postValue(currentBackgroundMeasurement)
                        }

                        override fun OnTestFailed(errorMessage: String) {
                            Log.e("DownloadSpeedTest", "Test failed: $errorMessage")
                            visibilityOfProgress.postValue(View.GONE)
                            downloadMeasurement.postValue("Download failed")
                        }


                    })
            } finally {
                progress.postValue(0)
            }
        }.start()
    }

    /**
     * Start Upload Test
     */
    fun startUploadSpeedTest() {
        updateVisibility(visibilityOfUpload to View.VISIBLE)
        Thread {
            try {
                uploadSpeedStats.runUploadSpeedTest(object : TestCallback {
                    override fun OnTestStart() {
                        uploadMeasurement.postValue("")
                    }

                    override fun OnTestSuccess(uploadSpeed: String) {
                        uploadMeasurement.postValue("${uploadSpeedStats.finalUploadRate} Mb/s")
                    }

                    override fun OnTestBackground(
                        currentBackgroundMeasurement: String,
                        currentBackgroundProgress: Int
                    ) {
                        progress.postValue(currentBackgroundProgress)
                        uploadMeasurement.postValue(currentBackgroundMeasurement)
                    }

                    override fun OnTestFailed(errorMessage: String) {
                        // Handle any errors that occur during the test
                        Log.e("UploadSpeedTest", "Test failed: $errorMessage")
                        visibilityOfProgress.postValue(View.GONE)
                        visibilityOfUpload.postValue(View.GONE)
                        uploadMeasurement.postValue("Upload failed")
                    }
                }, getBetterHost.urlUploadAddress)
            } finally {
                calculateOverallRating()
                progress.postValue(0)
                instantMeasurements.postValue("")
                isFinished.postValue(true)
                updateVisibility(
                    visibilityOfScore to View.VISIBLE,
                    visibilityOfProgress to View.GONE
                )
                scoreVisualizer()
                val newTestData = TestData(
                    dispositivo = deviceInformation.model,
                    fecha = deviceInformation.getCurrentDateTime(),
                    idVersionAndroid = deviceInformation.androidVersion.toInt(),
                    intensidadDeSenal = deviceInformation.signalStrength,
                    jitter = pingAndJitterTest.jitterMeasured,
                    operadorDeRed = deviceInformation.carrier ?: "-1",
                    ping = pingAndJitterTest.pingMeasured,
                    redScore = score,
                    servidor = getBetterHost.urlAddress,
                    tipoDeRed = deviceInformation.getActiveNetworkType(context) ?: "-1",
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
        visibilityOfScore.postValue(View.GONE)
        isFinished.postValue(false)
        deviceInformation.retrieveSignalStrength(dontAskAgain, dontAskAgainDenied)
        deviceInformation.updateNetworkInfo()
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

        score = mobileScore.calculateOverallScore(
            ping, jitter, downloadSpeed, uploadSpeed, signalStrength.toInt()
        )

        overallRating.postValue(score)
    }

    @SuppressLint("DefaultLocale")
    private fun scoreVisualizer() {
        visibilityOfScore.postValue(View.VISIBLE)
        val pingScoreAux: Double = mobileScore.pingScore
        val formatedPingScore = String.format("%.2f", pingScoreAux)
        pingScore.postValue(context.getString(R.string.indiviual_score) + " $formatedPingScore/40")
        val jitterAdjustmentFactor: Double = mobileScore.jitterAdjustmentFactor
        val formattedJitterAdjustmentFactor = String.format("%.2f", jitterAdjustmentFactor)
        jitterBonus.postValue(context.getString(R.string.bonus_score) + " $formattedJitterAdjustmentFactor")
        val downloadScoreAux: Double = mobileScore.downloadScore
        val formatedDownloadScore = String.format("%.2f", downloadScoreAux)
        downloadScore.postValue(context.getString(R.string.indiviual_score) + " $formatedDownloadScore/35")
        val uploadScoreAux: Double = mobileScore.uploadScore
        val formatedUploadScore = String.format("%.2f", uploadScoreAux)
        uploadScore.postValue(context.getString(R.string.indiviual_score) + " $formatedUploadScore/25")
        val signalStrengthAdjustmentFactor: Double = mobileScore.signalStrengthAdjustmentFactor
        val formattedSignalStrengthAdjustmentFactor =
            String.format("%.2f", signalStrengthAdjustmentFactor)
        signalStrengthBonus.postValue(context.getString(R.string.bonus_score) + " $formattedSignalStrengthAdjustmentFactor")
        signalStrength.postValue(context.getString(R.string.signal_strength) + " ${deviceInformation.signalStrength} dBm")
    }

    private fun updateVisibility(vararg visibilityStates: Pair<MutableLiveData<Int>, Int>) {
        visibilityStates.forEach { (liveData, visibility) ->
            liveData.postValue(visibility)
        }
    }

    companion object {
        @JvmStatic
        var deviceInfo: MutableLiveData<String> = MutableLiveData()
            private set
        var pingMeasurement: MutableLiveData<String?> = MutableLiveData()
            private set
        var signalStrength: MutableLiveData<String> = MutableLiveData()
            private set

        @JvmStatic
        var instantMeasurements: MutableLiveData<String> = MutableLiveData()
            private set

        @JvmStatic
        var jitterMeasurement: MutableLiveData<String?> = MutableLiveData()
            private set

        @JvmStatic
        var downloadMeasurement: MutableLiveData<String?> = MutableLiveData()
            private set

        @JvmStatic
        var uploadMeasurement: MutableLiveData<String?> = MutableLiveData()
            private set

        @JvmStatic
        var progress: MutableLiveData<Int> = MutableLiveData()
            private set
        private val availableServers = MutableLiveData<String>()
        val overallRating: MutableLiveData<Double> = MutableLiveData()
        val visibilityOfProgress: MutableLiveData<Int> = MutableLiveData()
        val visibilityOfPing: MutableLiveData<Int> = MutableLiveData()
        val visibilityOfJitter: MutableLiveData<Int> = MutableLiveData()
        val visibilityOfDownload: MutableLiveData<Int> = MutableLiveData()
        val visibilityOfUpload: MutableLiveData<Int> = MutableLiveData()
        val visibilityOfScore: MutableLiveData<Int> = MutableLiveData()

        val pingScore: MutableLiveData<String> = MutableLiveData()
        val jitterBonus: MutableLiveData<String> = MutableLiveData()
        val downloadScore: MutableLiveData<String> = MutableLiveData()
        val uploadScore: MutableLiveData<String> = MutableLiveData()
        val signalStrengthBonus: MutableLiveData<String> = MutableLiveData()

        @JvmField
        val isFinished: MutableLiveData<Boolean> = MutableLiveData()
    }
}

