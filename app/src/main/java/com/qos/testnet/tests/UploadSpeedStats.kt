import android.icu.math.BigDecimal
import android.icu.math.BigDecimal.ROUND_HALF_UP
import java.io.DataOutputStream
import java.math.RoundingMode
import java.math.RoundingMode.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class HttpUploadTest(private val fileURL: String) : Thread() {
    companion object {
        var uploadedKByte = 0
    }

    private var uploadElapsedTime = 0.0
    private var finished = false
    private var elapsedTime = 0.0
    private var finalUploadRate = 0.0
    private var startTime: Long = 0
    private fun round(value: Double, places: Int): Double {
        if (places < 0) throw IllegalArgumentException()
        val bd = BigDecimal.valueOf(value)
        return bd.setScale(places, ROUND_HALF_UP).toDouble()
    }

    fun isFinished(): Boolean = finished

    fun getInstantUploadRate(): Double {
        if (uploadedKByte >= 0) {
            val now = System.currentTimeMillis()
            val elapsedTime = (now - startTime) / 1000.0
            return round(((uploadedKByte / 1000.0) * 8) / elapsedTime, 2)
        } else {
            return 0.0
        }
    }

    fun getFinalUploadRate(): Double = round(finalUploadRate, 2)

    override fun run() {
        try {
            val url = URL(fileURL)
            uploadedKByte = 0
            startTime = System.currentTimeMillis()
            val executor: ExecutorService = Executors.newFixedThreadPool(4)
            for (i in 0 until 4) {
                executor.execute(HandlerUpload(url))
            }
            executor.shutdown()
            while (!executor.isTerminated) {
                try {
                    Thread.sleep(100)
                } catch (ex: InterruptedException) {
                    ex.printStackTrace()
                    Thread.currentThread().interrupt()}
            }
            val now = System.currentTimeMillis()
            uploadElapsedTime = (now - startTime) / 1000.0
            finalUploadRate = ((uploadedKByte / 1000.0) * 8) / uploadElapsedTime
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        finished = true
    }
}

class HandlerUpload(private val url: URL) : Thread() {
    override fun run() {
        val buffer = ByteArray(150 * 1024)
        val startTime = System.currentTimeMillis()
        val timeout = 10
        while (true) {
            try {
                val conn = url.openConnection() as HttpURLConnection
                conn.doOutput = true
                conn.requestMethod = "POST"
                conn.setRequestProperty("Connection", "Keep-Alive")
                val dos = DataOutputStream(conn.outputStream)
                dos.write(buffer, 0,buffer.size)
                dos.flush()
                conn.responseCode
                var uploadedKByte =+ buffer.size / 1024.0
                val endTime = System.currentTimeMillis()
                val uploadElapsedTime = (endTime - startTime) / 1000.0
                if (uploadElapsedTime >= timeout) {
                    break
                }
                dos.close()
                conn.disconnect()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }
}