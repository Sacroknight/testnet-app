package com.qos.testnet.utils.deviceinformation

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.auth.AuthenticationException
import com.qos.testnet.permissionmanager.RequestPermissions
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

class LocationInfo(private val context: Context) {
    private val client: OkHttpClient = OkHttpClient()
    var currentLongitudeGPS: Double? = LOCATION_NOT_FOUND
    var currentLatitudeGPS: Double? = LOCATION_NOT_FOUND
    var currentLatitudeNetwork: Double? = LOCATION_NOT_FOUND
    var currentLongitudeNetwork: Double? = LOCATION_NOT_FOUND
    var currentLatitudeApi: Double? = LOCATION_NOT_FOUND
    var currentLongitudeApi: Double? = LOCATION_NOT_FOUND
    private val requestPermissions = RequestPermissions(context)

    @JvmField
    var currentLocation: String? = null

    @OptIn(DelicateCoroutinesApi::class)
    @SuppressLint("MissingPermission")
    fun locationFlow(timeout: Long) = callbackFlow {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (!requestPermissions.hasLocationPermissions()) {
            close(Exception("Location permissions not granted"))
            return@callbackFlow
        }

        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                trySend(location)
                currentLatitudeGPS = (location.latitude as? Double) ?: -1.0
                currentLongitudeGPS = (location.longitude as? Double) ?: -1.0
                locationManager.removeUpdates(this)
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                // Implements if is necessary
            }
        }

        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            MIN_TIME_BW_UPDATES,
            MIN_DISTANCE_CHANGE_FOR_UPDATES,
            locationListener
        )

        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            if (!isClosedForSend) {
                // If we didn't get a GPS location, try from the network provider.
                val lastKnownLocation =
                    locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                lastKnownLocation?.let {
                    trySend(it).isSuccess
                    currentLatitudeNetwork = (it.latitude as? Double) ?: -1.0
                    currentLongitudeNetwork = (it.longitude as? Double) ?: -1.0
                } ?: close(Exception("No location found"))
                locationManager.removeUpdates(locationListener)
            }
        }, timeout)

        // Canceling and clear
        awaitClose {
            locationManager.removeUpdates(locationListener)
        }
    }

    fun fetchLocationFromApi(apiUrl: String): Pair<Double, Double> {
        val client = OkHttpClient()
        val request: Request = Request.Builder().url(apiUrl).build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            if (response.code == 401) throw AuthenticationException("Unauthorized API request")

            val responseData =
                response.body?.string() ?: throw NullPointerException("Response body is null")

            val locationInfo = JSONObject(responseData)
            val longitude = locationInfo.getDouble("longitude")
            val latitude = locationInfo.getDouble("latitude")
            currentLongitudeApi = longitude
            currentLatitudeGPS = latitude

            return Pair(latitude, longitude)
        }
    }

    companion object {
        private const val LOCATION_NOT_FOUND = -1.0
        private const val NETWORK_LOCATION_NOT_FOUND = "Network location not found"
        private const val GPS_LOCATION_NOT_FOUND = "GPS location not found"
        private const val ERROR_RETRIEVING_LOCATION = "Error retrieving location"
        const val API_URL: String =
            "https://api.ip2location.io/?key=30ABFB42A85F6E2C877172679CC6DD48&format=json"
        private const val MIN_TIME_BW_UPDATES = 1000L
        private const val MIN_DISTANCE_CHANGE_FOR_UPDATES = 200F
    }
}
