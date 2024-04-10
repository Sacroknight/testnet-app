package com.qos.testnet.utils.deviceInformation

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.auth.AuthenticationException
import com.qos.testnet.permissionmanager.RequestPermissions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class LocationInfo(private val context: Context){

    private var client: OkHttpClient = OkHttpClient()
    private var apiUrl: String =
        "https://api.ip2location.io/?key=30ABFB42A85F6E2C877172679CC6DD48&format=json"
    var currentLongitudeGPS: Double? = null
    var currentLatitudeGPS: Double? = null
    var currentLatitudeNetwork:Double? = null
    var currentLongitudeNetwork:Double? = null
    var currentLatitudeApi:Double? = null
    var currentLongitudeApi:Double? = null
    private val requestPermissions = RequestPermissions(context)

    @JvmField
    var currentLocation: String? = null

    @SuppressLint("MissingPermission")
    fun locationFlow() = callbackFlow<Location> {
        val errorMessage = AtomicReference("All in order")
        val locationManagerFuture = CompletableFuture.supplyAsync {
            context.getSystemService(
                Context.LOCATION_SERVICE) as LocationManager
        }
        if (requestPermissions.hasLocationPermissions()) {
            locationManagerFuture.thenAccept { locationManager ->
                if (locationManager != null) {
                    val gpsLocationFuture = CompletableFuture<Location>()
                    locationManager.requestSingleUpdate(
                        LocationManager.GPS_PROVIDER,
                        gpsLocationFuture::complete,
                        context.mainLooper)

                    val networkLocationFuture = CompletableFuture<Location>()
                    locationManager.requestSingleUpdate(
                        LocationManager.NETWORK_PROVIDER,
                        networkLocationFuture::complete,
                        context.mainLooper)
                    var exception = false
                    try{
                        val networkLocation = networkLocationFuture.get(10 , TimeUnit.SECONDS)
                        val gpsLocation = gpsLocationFuture.get(10, TimeUnit.SECONDS)
                        when {
                            gpsLocation != null -> {
                                currentLatitudeGPS = gpsLocation.latitude
                                currentLongitudeGPS = gpsLocation.longitude
                                trySend(Location(gpsLocation.provider))
                                currentLocation = "$currentLatitudeGPS, $currentLongitudeGPS"
                                return@thenAccept
                            }
                            networkLocation != null -> {
                                currentLongitudeNetwork = networkLocation.longitude
                                currentLatitudeNetwork = networkLocation.latitude
                                trySend(Location(networkLocation.provider))
                                currentLocation = "$currentLatitudeNetwork, $currentLongitudeNetwork"
                                return@thenAccept
                            }
                        }
                    }catch (e: Exception){
                        errorMessage.set("$ERROR_RETRIEVING_LOCATION ${e.message}")
                        exception = true
                    }finally {
                        if(exception){
                            close(Exception(errorMessage.get()))
                        }
                    }
                }else{
                    client = OkHttpClient()
                    val request: Request = Request.Builder().url(apiUrl).build()
                    try {
                        var responseData: String
                        client.newCall(request).execute().use { response ->
                            if (!response.isSuccessful) throw IOException("Unexpected code $response")
                            if (response.code == 401) throw AuthenticationException("Unauthorized API request")
                            checkNotNull(response.body)
                            responseData = response.body!!.string()
                        }
                        val locationInfo = JSONObject(responseData)
                        currentLongitudeApi = locationInfo.getDouble("longitude")
                        currentLatitudeApi = locationInfo.getDouble("latitude")
                    } catch (e: JSONException) {
                        throw RuntimeException(e.toString())
                    } catch (e: IOException) {
                        throw RuntimeException(e.toString())
                    } catch (e: AuthenticationException) {
                        throw RuntimeException(e.toString())
                    }
                }
            }.exceptionally {
                close(Exception(it.message))
                null
            }
        }
        awaitClose{
            client.dispatcher.executorService.shutdown()
        }

    }
    @SuppressLint("MissingPermission")
    fun locationWithoutGPSFlow()= callbackFlow<Location>{
        val errorMessage = AtomicReference("All in order")
        val locationManagerFuture = CompletableFuture.supplyAsync {
            context.getSystemService(
                Context.LOCATION_SERVICE) as LocationManager
        }
        if (requestPermissions.hasLocationPermissions()) {
            locationManagerFuture.thenAccept { locationManager ->
                if (locationManager != null) {
                    val networkLocationFuture = CompletableFuture<Location>()
                    locationManager.requestSingleUpdate(
                        LocationManager.NETWORK_PROVIDER,
                        networkLocationFuture::complete,
                        context.mainLooper)
                    var exception = false
                    try{
                        val networkLocation = networkLocationFuture.get(15, TimeUnit.SECONDS)
                        currentLongitudeNetwork = networkLocation.longitude
                        currentLatitudeNetwork = networkLocation.latitude
                        trySend(Location(networkLocation.provider))
                        currentLocation = "$currentLatitudeNetwork, $currentLongitudeNetwork"
                        return@thenAccept

                    }catch (e: Exception){
                        errorMessage.set("$ERROR_RETRIEVING_LOCATION ${e.message}")
                        exception = true
                    }finally {
                        if(exception){
                            close(Exception(errorMessage.get()))
                        }
                    }
                }else{
                    client = OkHttpClient()
                    val request: Request = Request.Builder().url(apiUrl).build()
                    try {
                        var responseData: String
                        client.newCall(request).execute().use { response ->
                            if (!response.isSuccessful) throw IOException("Unexpected code $response")
                            if (response.code == 401) throw AuthenticationException("Unauthorized API request")
                            checkNotNull(response.body)
                            responseData = response.body!!.string()
                        }
                        val locationInfo = JSONObject(responseData)
                        currentLongitudeApi = locationInfo.getDouble("longitude")
                        currentLatitudeApi = locationInfo.getDouble("latitude")
                    } catch (e: JSONException) {
                        throw RuntimeException(e.toString())
                    } catch (e: IOException) {
                        throw RuntimeException(e.toString())
                    } catch (e: AuthenticationException) {
                        throw RuntimeException(e.toString())
                    }
                }
            }.exceptionally {
                close(Exception(it.message))
                null
            }
        }
        awaitClose{
            client.dispatcher.executorService.shutdown()
        }
    }

    companion object {
        private const val LOCATION_NOT_FOUND = "Location not found"
        private const val NETWORK_LOCATION_NOT_FOUND = "Network location not found"
        private const val GPS_LOCATION_NOT_FOUND = "GPS location not found"
        private const val ERROR_RETRIEVING_LOCATION = "Error retrieving location"
    }
}
