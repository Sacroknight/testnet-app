package com.qos.testnet.utils.deviceInformation;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;

import com.qos.testnet.permissionmanager.RequestPermissions;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

public class LocationInfo implements LocationCallback {
    private static final String LOCATION_NOT_FOUND = "Location not found";
    private static final String NETWORK_LOCATION_NOT_FOUND = "Network location not found";
    private static final String GPS_LOCATION_NOT_FOUND = "GPS location not found";
    private static final String ERROR_RETRIEVING_LOCATION = "Error retrieving location";
    public final int DENIED_PERMISSIONS = -2;
    private final Context context;
    private final RequestPermissions requestPermissions;
    private String currentLocation;
    private String approximateLocation;
    public LocationInfo(Context context) {
        this.context = context;
        this.requestPermissions = new RequestPermissions(context);
    }

    public String getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(String currentLocation) {
        this.currentLocation = currentLocation;
    }
    public String getApproximateLocation() {
        return approximateLocation;
    }
    public void setApproximateLocation(String approximateLocation) {
        this.approximateLocation = approximateLocation;
    }

    public void retrieveLocation(boolean dontAskAgain, boolean dontAskAgainDenied, LocationCallback callback) {
        if (requestPermissions.hasLocationPermissions()) {
            try {
                getLocation(callback, dontAskAgain, dontAskAgainDenied);
            } catch (Exception e) {
                // Handle exceptions centrally (e.g., log the error, inform the user)
                e.printStackTrace();
                callback.onLocationFailed("Location retrieval failed: " + e.getMessage());
            }
        } else if (!dontAskAgain) {
            requestPermissions.showPermissionDeniedWarning();
        } else if (!dontAskAgainDenied) {
            requestPermissions.requestLocationPermissionsDialog();
        } else {
            callback.onLocationFailed(LOCATION_NOT_FOUND);
        }
    }

    @SuppressLint("MissingPermission")
    private void getLocation(LocationCallback callback, boolean dontAskAgain, boolean dontAskAgainDenied) {
        AtomicReference<String> errorMessage = new AtomicReference<>("All in order");
        CompletableFuture<LocationManager> locationManagerFuture = CompletableFuture.supplyAsync(() ->
                (LocationManager) context.getSystemService(Context.LOCATION_SERVICE));
        if (requestPermissions.hasLocationPermissions()) {
            locationManagerFuture.thenAccept(locationManager -> {
                if (locationManager != null) {
                    //Location source: gps
                    CompletableFuture<Location> gpsLocationFuture = new CompletableFuture<>();
                    locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER,
                            gpsLocationFuture::complete,
                            context.getMainLooper());
                    //Location source: network
                    CompletableFuture<Location> networkLocationFuture = new CompletableFuture<>();
                    locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER,
                            networkLocationFuture::complete,
                            context.getMainLooper());
                    boolean exception = false;
                    try {
                        Location networkLocation = networkLocationFuture.get();
                        Location gpsLocation = gpsLocationFuture.get(10, TimeUnit.SECONDS);
                        if (gpsLocation != null) {
                            // Update location with GPS if received within timeout
                            double latitude = gpsLocation.getLatitude();
                            double longitude = gpsLocation.getLongitude();
                            setCurrentLocation(latitude + ", " + longitude);
                            callback.onLocationSuccess(getCurrentLocation());
                            return; // Exit the thenAccept block if GPS location is obtained
                        } else {
                            setCurrentLocation(LOCATION_NOT_FOUND);
                            callback.onLocationFailed(GPS_LOCATION_NOT_FOUND);
                        }
                        if (networkLocation!=null){
                            double latitude = networkLocation.getLatitude();
                            double longitude = networkLocation.getLongitude();
                            setApproximateLocation(latitude + ", " + longitude);
                            callback.onApproxLocationSuccess(getApproximateLocation());
                        }else{
                            callback.onApproxLocationFailed(NETWORK_LOCATION_NOT_FOUND);
                            setApproximateLocation(LOCATION_NOT_FOUND);
                        }
                    } catch (TimeoutException | InterruptedException | ExecutionException e) {
                        errorMessage.set(ERROR_RETRIEVING_LOCATION + e.getMessage());
                        exception =true;
                    } finally {
                        if (exception) {
                            callback.onLocationRetrievalException(errorMessage.get());
                        }
                    }
                }
            }).exceptionally(e -> {
                callback.onLocationFailed(e.getMessage());
                return null;
            });
        }
    }

    public void onLocationSuccess(String location) {

    }

    public void onApproxLocationSuccess(String location) {

    }

    public void onLocationFailed(String error) {

    }

    public void onApproxLocationFailed(String error) {

    }

    public void onLocationRetrievalException(String e) {

    }

}
