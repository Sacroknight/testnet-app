package com.qos.testnet.utils.deviceInformation

import android.annotation.SuppressLint
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.auth.AuthenticationException
import com.qos.testnet.permissionmanager.RequestPermissions
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume

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

    var currentLocation: String? = null

    @SuppressLint("MissingPermission")
    fun locationFlow(context: Context)=  callbackFlow<Location> {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            // Comprobar permisos
            if (!requestPermissions.hasLocationPermissions()) {
                close(Exception("Permisos de ubicación no otorgados"))
                return@callbackFlow
            }
            var locationProvided = false
            val gpsLocationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    if (!locationProvided) {
                        trySend(location)
                        locationProvided = true
                        locationManager.removeUpdates(this)
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String, status: Int, extras: Bundle?) {
                    // Ignorar cambios de estado
                }

            }

            // Solicitar ubicación por GPS con temporizador
            val timer = Timer()
            val timerTask = object : TimerTask() {
                override fun run() {
                    // Solicitar ubicación por red si no se obtiene por GPS en 10 segundos
                    val networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    if (networkLocation != null) {
                        trySend(networkLocation)
                    } else {
                        close(Exception("Ubicación no encontrada"))
                    }
                    timer.cancel()
                    locationManager.removeUpdates(gpsLocationListener)
                }
            }

            // Solicitar ubicación por GPS
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0F, gpsLocationListener)
            timer.schedule(timerTask, 10000)

            // Limpiar recursos al cancelar el flujo
            awaitClose {
                timer.cancel()
                locationManager.removeUpdates(gpsLocationListener)
            }
        }
//        @SuppressLint("MissingPermission")
//    fun locationFlow() = callbackFlow<Location> {
//        val errorMessage = AtomicReference("All in order")
//        val locationManagerFuture = CompletableFuture.supplyAsync {
//            context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
//        }
//
//        if (requestPermissions.hasLocationPermissions()) {
//            locationManagerFuture.thenAccept { locationManager ->
//                if (locationManager != null) {
//                    val gpsLocationFuture = CompletableFuture<Location>()
//                    val networkLocationFuture = CompletableFuture<Location>()
//
//                    locationManager.requestSingleUpdate(
//                        LocationManager.GPS_PROVIDER,
//                        gpsLocationFuture::complete,
//                        context.mainLooper
//                    )
//
//                    locationManager.requestSingleUpdate(
//                        LocationManager.NETWORK_PROVIDER,
//                        networkLocationFuture::complete,
//                        context.mainLooper
//                    )
//
//                    var exception = false
//                    try {
//                        val gpsLocation = gpsLocationFuture.get(10, TimeUnit.SECONDS)
//                        val networkLocation = networkLocationFuture.get(10, TimeUnit.SECONDS)
//
//                        when {
//                            gpsLocation != null -> {
//                                currentLatitudeGPS = gpsLocation.latitude
//                                currentLongitudeGPS = gpsLocation.longitude
//                                trySendBlocking(Location(gpsLocation.provider))
//                                currentLocation = "$currentLatitudeGPS, $currentLongitudeGPS"
//                                return@thenAccept
//                            }
//                            networkLocation != null -> {
//                                currentLongitudeNetwork = networkLocation.longitude
//                                currentLatitudeNetwork = networkLocation.latitude
//                                trySendBlocking(Location(networkLocation.provider))
//                                currentLocation = "$currentLatitudeNetwork, $currentLongitudeNetwork"
//                                return@thenAccept
//                            }
//                        }
//                    } catch (e: InterruptedException) {
//                        errorMessage.set("Interrupted: ${e.message}")
//                        exception = true
//                    } catch (e: ExecutionException) {
//                        errorMessage.set("Execution failed: ${e.message}")
//                        exception = true
//                    } catch (e: TimeoutException) {
//                        errorMessage.set("Timeout: ${e.message}")
//                        exception = true
//                    } finally {
//                        if (exception) {
//                            close(Exception(errorMessage.get()))
//                        }
//                    }
//                } else {
//                    getFallbackLocation { location ->
//                        if (location != null) {
//                            trySendBlocking(location)
//                        } else {
//                            close(Exception(errorMessage.get()))
//                        }
//                    }
//                }
//            }.exceptionally {
//                close(Exception(it.message))
//                null
//            }
//        }
//
//        awaitClose {
//            client.dispatcher.executorService.shutdown()
//        }
//    }

    private fun getFallbackLocation(callback: (Location?) -> Unit) {
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
            callback(Location("API"))
        } catch (e: JSONException) {
            callback(null)
        } catch (e: IOException) {
            callback(null)
        } catch (e: AuthenticationException) {
            callback(null)
        }
    }


    //    fun locationFlow() = callbackFlow {
//        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as
//                LocationManager?: throw IllegalStateException("LocationManager service not available")
//        if (requestPermissions.hasLocationPermissions()){
//            val gpsTimeoutMillis = 10_000L
//            val networkTimeoutMillis = 12_000L
//            try{
//                val gpsLocation = async {requestLocation(LocationManager.GPS_PROVIDER, gpsTimeoutMillis, locationManager)}
//                val networkLocation = async {requestLocation(LocationManager.NETWORK_PROVIDER, networkTimeoutMillis, locationManager)}
//                // Esperar el resultado de la ubicación GPS o de red
//                val selectedLocation = gpsLocation.await() ?: networkLocation.await()
//                        when (selectedLocation?.provider) {
//                            LocationManager.GPS_PROVIDER -> {
//                                // Handle GPS location
//                                currentLatitudeGPS = selectedLocation.latitude
//                                currentLongitudeGPS = selectedLocation.longitude
//                                trySend(selectedLocation.provider)
//                                currentLocation = "$currentLatitudeGPS, $currentLongitudeGPS"
//                            }
//                            LocationManager.NETWORK_PROVIDER -> {
//                                // Handle network location
//                                currentLatitudeNetwork = selectedLocation.latitude
//                                currentLongitudeNetwork = selectedLocation.longitude
//                                trySend(selectedLocation.provider)
//                                currentLocation = "$currentLatitudeNetwork, $currentLongitudeNetwork"
//                            }
//                            null -> {
//                                // Handle null selectedLocation (failed to retrieve location)
//                                throw IOException("Failed to retrieve location")
//                            }
//                            else -> {
//                                // Unknown provider
//                                throw IOException("Unknown location provider")
//                            }
//                        }
//
//                }catch(e: Exception){
//                    close(e)
//                }
//        }else{
//            close(SecurityException("Location permissions not granted"))        }
//    }
    @SuppressLint("MissingPermission")
    suspend fun requestLocation(provider: String, timeoutMillis: Long, locationManager: LocationManager): Location? = withContext(Dispatchers.Main) {
        return@withContext suspendCancellableCoroutine {
                cont: CancellableContinuation<Location?> ->
            val locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    cont.resume(location)
                    locationManager.removeUpdates(this) // Remove updates after receiving location
                }

                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            }

            locationManager.requestSingleUpdate(provider, { location ->
                if(cont.isActive){
                    cont.resume(location)
                }
            }, null)

            val timeoutJob = CoroutineScope(Dispatchers.Default).launch {
                delay(timeoutMillis)
                if (cont.isActive) {
                    cont.resume(null)
                }
                locationManager.removeUpdates(locationListener) // Remove updates after timeout
            }
            cont.invokeOnCancellation {
                timeoutJob.cancel() // Cancelar el temporizador si la solicitud se cancela
                locationManager.removeUpdates(locationListener)
            }
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
