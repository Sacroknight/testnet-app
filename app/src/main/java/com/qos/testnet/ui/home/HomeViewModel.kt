package com.qos.testnet.ui.home

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.CountDownTimer
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
    private var isTestRunning = false

    /**
     * Start the location retrieval process
     */
    private fun startLocationRetrieval() {
        updateVisibility(
            visibilityOfLocationProgress to View.VISIBLE,
            visibilityOfScore to View.GONE,
            visibilityOfPing to View.GONE,
            visibilityOfJitter to View.GONE,
            visibilityOfDownload to View.GONE,
            visibilityOfUpload to View.GONE
        )

        startLocationProgressTimer(10000L, 100L)

        viewModelScope.launch {
            val locationResult = retrieveLocationWithTimeout(10000L)
            handleLocationResult(locationResult)
        }
    }

    private fun startLocationProgressTimer(totalTime: Long, interval: Long) {
        val progressIncrement = 100 / (totalTime / interval).toInt()

        val timer = object : CountDownTimer(totalTime, interval) {
            var progressValue = 0

            override fun onTick(millisUntilFinished: Long) {
                progressValue += progressIncrement
                locationProgress.postValue(progressValue)
            }

            override fun onFinish() {
                locationProgress.postValue(100)
                updateVisibility(visibilityOfLocationProgress to View.GONE)
            }
        }

        timer.start()
    }

    private suspend fun retrieveLocationWithTimeout(timeout: Long): Location? {
        return locationInfo.locationFlow(timeout = timeout)
            .catch { exception ->
                Log.e("LocationFlow", "Error occurred: ${exception.message}")
            }
            .firstOrNull()
    }

    private suspend fun handleLocationResult(locationResult: Location?) {
        if (locationResult != null) {
            currentLocation = "${locationResult.latitude}, ${locationResult.longitude}"
            getBestHostAndStartTasks()
            updateVisibility(visibilityOfLocationProgress to View.GONE)
        } else {
            fetchLocationFromApi(ioDispatcher)
        }
    }

    private suspend fun fetchLocationFromApi(dispatcher: CoroutineDispatcher) {
        try {
            val (latitude, longitude) = withContext(dispatcher) {
                locationInfo.fetchLocationFromApi(LocationInfo.API_URL)
            }
            currentLocation = "$latitude, $longitude"
            updateVisibility(visibilityOfLocationProgress to View.GONE)
            getBestHostAndStartTasks()
        } catch (e: Exception) {
            Log.e("LocationFlow", "Error fetching location from API: ${e.message}")
        } finally {
            updateVisibility(visibilityOfLocationProgress to View.GONE)
        }
    }
    /**
     * Start ping and jitter test
     */
    private fun startPingAndJitterTest() {
        updateVisibility(
            visibilityOfProgress to View.VISIBLE,
            visibilityOfPing to View.VISIBLE
        )

        Thread {
            if (!isTestRunning) return@Thread
            try {
                pingAndJitterTest.startPingAndJitterTest(createPingAndJitterTestCallback())
            } finally {
                Log.d("PingAndJitterTest", "Test finished")
            }
        }.start()
    }

    private fun createPingAndJitterTestCallback(): TestCallback {
        return object : TestCallback {
            override fun onTestStart() {
                if (!isTestRunning) return
                pingMeasurement.postValue("")
            }

            override fun onTestSuccess(jitter: String) {
                if (!isTestRunning) return
                handleTestSuccess()
            }

            override fun onTestBackground(
                currentBackgroundMeasurement: String,
                currentBackgroundProgress: Int
            ) {
                if (!isTestRunning) return
                pingMeasurement.postValue(currentBackgroundMeasurement)
                progress.postValue(currentBackgroundProgress)
            }

            override fun onTestFailed(errorMessage: String) {
                if (!isTestRunning) return
                handleTestFailure(errorMessage)
            }

            override fun onTestFailure(error: String?) {
                Log.e("PingAndJitterTest", "Test failed: $error")
                handleTestSuccess()
            }
        }
    }

    private fun handleTestSuccess() {
        updateVisibility(visibilityOfJitter to View.VISIBLE)
        pingMeasurement.postValue("${pingAndJitterTest.pingMeasured} ms")
        jitterMeasurement.postValue("${pingAndJitterTest.jitterMeasured} ms")
        progress.postValue(0)

        viewModelScope.launch {
            userId = userRepository.getUserId()
        }

        Handler(Looper.getMainLooper()).postDelayed(
            { if (isTestRunning) startDownloadSpeedTest() },
            500
        )
    }

    private fun handleTestFailure(errorMessage: String) {
        updateVisibility(
            visibilityOfProgress to View.GONE,
            visibilityOfJitter to View.GONE
        )
        pingMeasurement.postValue(errorMessage)
        Log.e("PingAndJitterTest", "Test failed: $errorMessage")
    }

    /**
     * Starts the download speed test
     */
    private fun startDownloadSpeedTest() {
        updateVisibility(visibilityOfDownload to View.VISIBLE)

        Thread {
            if (!isTestRunning) return@Thread

            try {
                downloadSpeedTest.startSpeedTest(getBetterHost.urlAddress, downloadSpeedCallback())
            } finally {
                progress.postValue(0)
            }
        }.start()
    }

    private fun downloadSpeedCallback(): TestCallback {
        return object : TestCallback {
            override fun onTestStart() {
                if (!isTestRunning) return
                downloadMeasurement.postValue("")
                updateVisibility(visibilityOfDownload to View.VISIBLE)
            }

            override fun onTestSuccess(downloadSpeed: String) {
                if (!isTestRunning) return
                downloadMeasurement.postValue("${downloadSpeedTest.finalDownloadSpeed} Mb/s")
                Handler(Looper.getMainLooper()).postDelayed(
                    { if (isTestRunning) startUploadSpeedTest() },
                    500
                )
            }

            override fun onTestBackground(
                currentBackgroundMeasurement: String,
                currentBackgroundProgress: Int
            ) {
                if (!isTestRunning) return
                progress.postValue(currentBackgroundProgress)
                downloadMeasurement.postValue(currentBackgroundMeasurement)
            }

            override fun onTestFailed(errorMessage: String) {
                if (!isTestRunning) return
                Log.e("DownloadSpeedTest", "Test failed: $errorMessage")
                visibilityOfProgress.postValue(View.GONE)
                downloadMeasurement.postValue("Download failed")
            }

            override fun onTestFailure(error: String?) {
                if (!isTestRunning) return
                Log.e("DownloadSpeedTest", "Test failed: $error")
            }
        }
    }

    /**
     * Starts the upload speed test
     */
    fun startUploadSpeedTest() {
        updateVisibility(visibilityOfUpload to View.VISIBLE)

        Thread {
            if (!isTestRunning) return@Thread
            try {
                uploadSpeedStats.runUploadSpeedTest(
                    uploadTestCallback(),
                    getBetterHost.urlUploadAddress
                )
            } finally {
                if (isTestRunning) finalizeUploadTest()
            }
        }.start()
    }

    private fun uploadTestCallback(): TestCallback {
        return object : TestCallback {
            override fun onTestStart() {
                if (!isTestRunning) return
                uploadMeasurement.postValue("")
            }

            override fun onTestSuccess(uploadSpeed: String) {
                if (!isTestRunning) return
                uploadMeasurement.postValue("${uploadSpeedStats.finalUploadRate} Mb/s")
            }

            override fun onTestBackground(
                currentBackgroundMeasurement: String,
                currentBackgroundProgress: Int
            ) {
                if (!isTestRunning) return
                progress.postValue(currentBackgroundProgress)
                uploadMeasurement.postValue(currentBackgroundMeasurement)
            }

            override fun onTestFailed(errorMessage: String) {
                if (!isTestRunning) return
                Log.e("UploadSpeedTest", "Test failed: $errorMessage")
                visibilityOfProgress.postValue(View.GONE)
                visibilityOfUpload.postValue(View.GONE)
                uploadMeasurement.postValue("Upload failed")
            }

            override fun onTestFailure(error: String?) {
                if (!isTestRunning) return
                Log.e("UploadSpeedTest", "Test failed: $error")
            }
        }
    }

    private fun finalizeUploadTest() {
        calculateOverallRating()
        progress.postValue(0)
        instantMeasurements.postValue("")
        isFinished.postValue(true)
        updateVisibility(
            visibilityOfScore to View.VISIBLE,
            visibilityOfProgress to View.GONE
        )
        scoreVisualizer()

        val newTestData = createTestData()
        userRepository.sendData(newTestData)
    }

    private fun createTestData(): TestData {
        return TestData(
            dispositivo = deviceInformation.model,
            fecha = deviceInformation.getCurrentDateTime(),
            idVersionAndroid = deviceInformation.androidVersion.toInt(),
            intensidadDeSenal = deviceInformation.signalStrength,
            jitter = pingAndJitterTest.jitterMeasured,
            operadorDeRed = deviceInformation.carrier ?: "-1",
            ping = pingAndJitterTest.pingMeasured,
            pingHost = pingAndJitterTest.currentHost,
            redScore = score,
            servidor = getBetterHost.urlAddress,
            tipoDeRed = deviceInformation.getActiveNetworkType(context) ?: "-1",
            ubicacion = currentLocation ?: "-1, -1",
            userId = userId,
            velocidadDeCarga = downloadSpeedTest.finalDownloadSpeed,
            velocidadDeDescarga = uploadSpeedStats.finalUploadRate
        )
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
        isTestRunning = true
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
     * Método para cancelar la prueba
     */
    fun cancelTasks() {
        if (isTestRunning) {
            updateVisibility(
                visibilityOfProgress to View.GONE,
                visibilityOfPing to View.VISIBLE,
                visibilityOfJitter to View.VISIBLE,
                visibilityOfDownload to View.VISIBLE,
                visibilityOfUpload to View.VISIBLE,
                visibilityOfScore to View.GONE
            )

            // Mostrar mensaje de cancelación
            instantMeasurements.postValue("Test Cancelado")
            isFinished.postValue(true)
            // Marcar la prueba como no en curso
            isTestRunning = false
        }
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

    /**
     * Método para restablecer todas las variables de medición
     */
    private fun resetMeasurements() {
        pingMeasurement.postValue("")
        jitterMeasurement.postValue("")
        downloadMeasurement.postValue("")
        uploadMeasurement.postValue("")
        progress.postValue(0)
        overallRating.postValue(0.0)
        pingScore.postValue("")
        jitterBonus.postValue("")
        downloadScore.postValue("")
        uploadScore.postValue("")
        signalStrengthBonus.postValue("")
        signalStrength.postValue("")
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
        val visibilityOfLocationProgress: MutableLiveData<Int> = MutableLiveData()

        val pingScore: MutableLiveData<String> = MutableLiveData()
        val jitterBonus: MutableLiveData<String> = MutableLiveData()
        val downloadScore: MutableLiveData<String> = MutableLiveData()
        val uploadScore: MutableLiveData<String> = MutableLiveData()
        val signalStrengthBonus: MutableLiveData<String> = MutableLiveData()

        @JvmField
        val isFinished: MutableLiveData<Boolean> = MutableLiveData()
        val locationProgress = MutableLiveData<Int>()
    }
}

