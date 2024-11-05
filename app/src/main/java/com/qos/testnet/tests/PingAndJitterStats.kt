package com.qos.testnet.tests

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.MutableLiveData
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.Random
import kotlin.math.pow
import kotlin.math.sqrt

class PingAndJitterStats : InternetTest, TestCallback {
    companion object {
        const val MAX_PING_TIMES = 40
        const val TIMEOUT_MS = 5000 // Tiempo de espera en milisegundos
        const val ERROR_MEASURING_PING = -1
    }

    private var failedPings = 0
    private val instantPing = MutableLiveData<String>()
    private val finalPing = MutableLiveData<String>()
    private val jitterLiveData = MutableLiveData<String>()
    private val progress = MutableLiveData<Int>()
    private val host: MutableMap<String, Int> = HashMap()
    private var pingMeasure = 0
    private var jitterMeasure = 0
    private var finished = false

    init {
        getMostVisitedWebsites()
        progress.value = 0
        finished = false
    }

    fun isFinished(): Boolean = finished

    fun setFinished(finished: Boolean) {
        this.finished = finished
    }

    fun getPingMeasured(): Int = pingMeasure

    private fun setFinalPing(finalPing: Int) {
        pingMeasure = finalPing
    }

    fun getJitterMeasured(): Int = jitterMeasure

    private fun setJitterMeasure(jitterMeasure: Int) {
        this.jitterMeasure = jitterMeasure
    }

    fun getJitterLivedata(): MutableLiveData<String> = jitterLiveData

    fun setJitterLivedata(jitterMeasured: String) {
        jitterLiveData.postValue(jitterMeasured)
    }

    private fun getMostVisitedWebsites() {
        host["google.com"] = 1
        host["youtube.com"] = 2
        host["whatsapp.com"] = 3
        host["facebook.com"] = 4
        host["instagram.com"] = 5
        host["live.com"] = 6
        host["openai.com"] = 7
        host["office.com"] = 8
        host["wikipedia.org"] = 9
        host["mercadolibre.com.co"] = 10
    }

    fun chooseHost(): String {
        val hostList = host.keys.toList()
        return hostList[Random().nextInt(hostList.size)]
    }

    fun measuringPingJitter(chosenHost: String, testCallback: TestCallback) {
        Thread {
            val pingList = mutableListOf<Int>()
            var i = 0
            while (i < MAX_PING_TIMES && pingList.size <= MAX_PING_TIMES / 2) {
                var ping = measuringPing(chosenHost, testCallback)

                if (ping == ERROR_MEASURING_PING || ping > TIMEOUT_MS) {
                    failedPings++
                    ping = measuringPing(chosenHost, testCallback)
                } else {
                    pingList.add(ping)
                }

                val pingProgress = i * (100 / MAX_PING_TIMES)
                val pingResult = "$ping ms"
                runOnUiThread {
                    setProgress(pingProgress)
                    testCallback.OnTestBackground(
                        if (pingResult.contains("-1")) {
                            if (pingList.isNotEmpty()) {
                                "${pingList.last()} ms"
                            } else {
                                ""
                            }
                        } else {
                            pingResult
                        }, pingProgress
                    )
                }

                // Espera un tiempo antes de enviar el siguiente ping
                try {
                    Thread.sleep(200) // 150 ms de espera entre pings
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    Log.e(this::class.java.name, "El hilo fue interrumpido", e)
                    testCallback.OnTestFailed("El hilo fue interrumpido: ${e.message}")
                    return@Thread
                }

                i++
            }
            calculateAndSetStatistics(pingList, testCallback)
            Log.d(
                this::class.java.name,
                "Finished measuring ping and jitter, the number of packets lost is: $failedPings"
            )
        }.start()
    }

    private fun runOnUiThread(action: () -> Unit) {
        Handler(Looper.getMainLooper()).post(action)
    }

    private fun calculateAndSetStatistics(pingList: List<Int>, testCallback: TestCallback) {
        try {
            val depuredPingList = pingList.filter { it > 0 }
            val averagePing = depuredPingList.average()
            val variance = depuredPingList.map { (it - averagePing).pow(2) }.average()
            val jitter = sqrt(variance).toInt()
            setFinalPing(averagePing.toInt())
            setJitterMeasure(jitter)
            setFinished(true)
        } finally {
            setJitterLivedata("${getJitterMeasured()} ms")
            setFinalMeasurement("${getPingMeasured()} ms")
            testCallback.OnTestSuccess("${getJitterMeasured()} ms")
        }
    }

    private fun measuringPing(chosenHost: String, testCallback: TestCallback): Int {
        var ping = 0
        if (chosenHost.isEmpty()) {
            OnTestFailed(chosenHost)
            return ping
        }

        var process: Process? = null
        try {
            process =
                ProcessBuilder("ping", "-c", "1", "-W", "${TIMEOUT_MS / 1000}", chosenHost).start()

            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                var line: String?
                Log.d(this::class.java.name, reader.readLine() )
                while (reader.readLine().also { line = it } != null) {
                    if (line!!.contains("time=")) {
                        val startIndex = line!!.indexOf("time=") + 5
                        val endIndex = line!!.indexOf(" ms", startIndex)
                        ping = line!!.substring(startIndex, endIndex).toFloat().toInt()
                        break
                    }
                }
            }

            // Espera a que el proceso termine
            if (process.waitFor() != 0) {
                ping = ERROR_MEASURING_PING
            }
        } catch (e: IOException) {
            Log.e(this::class.java.name, "Error al ejecutar el comando ping", e)
            testCallback.OnTestFailed(e.message ?: "Error desconocido")
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            Log.e(this::class.java.name, "El hilo fue interrumpido", e)
            testCallback.OnTestFailed("El hilo fue interrumpido: ${e.message}")
        } catch (e: RuntimeException) {
            Log.e(this::class.java.name, "Error inesperado al ejecutar el comando ping", e)
            testCallback.OnTestFailed("Error inesperado: ${e.message}")
        } finally {
            process?.destroy()
        }

        return ping
    }

    override fun getInstantMeasurement(): MutableLiveData<String> = instantPing

    override fun setInstantMeasurement(instantMeasurement: String) {
        instantPing.postValue(instantMeasurement)
    }

    override fun getFinalMeasurement(): MutableLiveData<String> = finalPing

    override fun setFinalMeasurement(finalMeasurement: String) {
        finalPing.postValue(finalMeasurement)
    }

    override fun getProgress(): MutableLiveData<Int> = progress

    override fun setProgress(currentProgress: Int) {
        progress.postValue(currentProgress)
    }

    override fun OnTestStart() {
        // No implementado
    }

    override fun OnTestSuccess(jitter: String) {
        // No implementado
    }

    override fun OnTestBackground(currentPing: String, currentProgress: Int) {
        // No implementado
    }
}