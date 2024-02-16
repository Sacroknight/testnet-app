package com.qos.myapplication.tests;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoNr;
import android.telephony.CellInfoWcdma;
import android.telephony.CellSignalStrength;
import android.telephony.CellSignalStrengthCdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthNr;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.List;

public class DeviceInformation implements PermissionCallback {
    private static final int REQUEST_READ_PHONE_PERMISSION = 1001;
    private static final int NOT_ACTIVE_MOBILE_NETWORK = 0010;
    private String manufacturer;
    private String model;
    private String androidVersion;
    private String carrier;

    private int signalStrength;
    private String location;
    private final Context context;
    public static final int REQUEST_LOCATION_PERMISSION = 404;
    private static final long MIN_TIME_BW_UPDATES = 60000; // 1 minute
    private static final float MIN_DISTANCE_CHANGE_FOR_UPDATES = 20; // 10 meters
    private final int DENIED_PERMISSIONS = 1;
    private final int NOT_SIGNAL = 404;

    public DeviceInformation(Context context) {
        this.context = context;
        setManufacturer();
        setModel();
        setAndroidVersion();

    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer() {
        this.manufacturer = Build.MANUFACTURER;
    }

    public String getModel() {
        return model;
    }

    public void setModel() {
        this.model = Build.MODEL;
    }

    public String getAndroidVersion() {
        return androidVersion;
    }

    public void setAndroidVersion() {
        this.androidVersion = Build.VERSION.RELEASE;
    }

    public int getSignalStrength() {
        return signalStrength;
    }

    public void setSignalStrength(int signalStrength) {
        this.signalStrength = signalStrength;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
    private boolean hasLocationPermissions() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasReadPhonePermissions() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_BASIC_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
    }

    public void retrieveSignalStrength() {
        if (hasLocationPermissions() && hasReadPhonePermissions()) {
            gettingSignalStrength();
        } else {
            requestLocationPermission(REQUEST_LOCATION_PERMISSION);
            requestReadPhoneStatePermission(REQUEST_READ_PHONE_PERMISSION);
            showPermissionDeniedWarning();
            if (!hasLocationPermissions()) {
                setSignalStrength(DENIED_PERMISSIONS);
            }
        }
    }

    private void gettingSignalStrength() {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        SubscriptionManager subscriptionManager = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        CellInfo cellInfo;
        if (telephonyManager != null && subscriptionManager != null) {
            try {
                @SuppressLint("MissingPermission")
                List<CellInfo> cellInfoList = telephonyManager.getAllCellInfo();
                NetworkInfo activeNetworkInfo = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
                cellInfo = null;
                if (activeNetworkInfo == null || activeNetworkInfo.getType() != ConnectivityManager.TYPE_MOBILE) {
                    setSignalStrength(NOT_ACTIVE_MOBILE_NETWORK);
                } else if (activeNetworkInfo != null && activeNetworkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                    if (cellInfoList != null)
                        for (CellInfo info : cellInfoList) {
                            if (info.isRegistered()) {
                                cellInfo = info;
                            }
                        }
                    measureSignalStrength(cellInfo);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }


        }
    }

    private void measureSignalStrength(CellInfo cellInfo) {
        int currentStrength = NOT_SIGNAL;
        if (cellInfo instanceof CellInfoGsm) {
            CellSignalStrengthGsm cellSignalStrengthGsm = ((CellInfoGsm) cellInfo).getCellSignalStrength();
            currentStrength = cellSignalStrengthGsm.getDbm();
        } else if (cellInfo instanceof CellInfoCdma) {
            CellSignalStrengthCdma cellSignalStrengthCdma = ((CellInfoCdma) cellInfo).getCellSignalStrength();
            currentStrength = cellSignalStrengthCdma.getDbm();
        } else if (cellInfo instanceof CellInfoLte) {
            CellSignalStrengthLte cellSignalStrengthLte = ((CellInfoLte) cellInfo).getCellSignalStrength();
            currentStrength = cellSignalStrengthLte.getDbm(); // Use RSRQ for LTE
        } else if (cellInfo instanceof CellInfoWcdma) {
            CellSignalStrength cellSignalStrengthWcdma = ((CellInfoWcdma) cellInfo).getCellSignalStrength();
            currentStrength = cellSignalStrengthWcdma.getDbm();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (cellInfo instanceof CellInfoNr) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    CellSignalStrengthNr cellSignalStrengthNr = (CellSignalStrengthNr) ((CellInfoNr) cellInfo).getCellSignalStrength();
                    currentStrength = cellSignalStrengthNr.getDbm();
                }
            }
        }
        setSignalStrength(currentStrength);

    }

    @SuppressLint("MissingPermission")
    public void retrieveLocation() {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        boolean permissionGranted = false;
        if (locationManager != null) {
            // Check both fine and coarse permissions
            if (hasLocationPermissions()) {
                permissionGranted = true;
                class LocationContainer {
                    double latitude;
                    double longitude;

                    public double getLatitude() {
                        return latitude;
                    }

                    public double getLongitude() {
                        return longitude;
                    }
                }
                LocationContainer locationContainer = new LocationContainer();
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        MIN_TIME_BW_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES,
                        (Location location) -> {
                           locationContainer.latitude = location.getLatitude();
                           locationContainer.longitude = location.getLongitude();
                            setLocation(locationContainer.getLatitude() + ", " + locationContainer.getLongitude());

                        });
            } else {
                requestLocationPermission(REQUEST_LOCATION_PERMISSION);
                showPermissionDeniedWarning();
            }

            // Combine flag and value to represent missing data due to permission denial
            if (!permissionGranted) {
                setLocation(String.valueOf(DENIED_PERMISSIONS)); // or any suitable placeholder
            }
        }
    }

    public void requestLocationPermission(int requestCode) {
        if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, Manifest.permission.ACCESS_FINE_LOCATION) ||
                ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, Manifest.permission.ACCESS_COARSE_LOCATION)) {
            // Explain why permissions are needed before requesting
            // ... show explanation dialog here

            // Example dialog:
            new AlertDialog.Builder(context)
                    .setTitle("Location Permission Needed")
                    .setMessage("This app needs access to your location to retrieve cell information and provide accurate results.")
                    .setPositiveButton("OK", (dialog, which) -> {
                        // Proceed with permission request
                        ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, requestCode);
                    })
                    .create()
                    .show();
        } else {
            // No explanation needed, request permissions directly
            ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, requestCode);
        }
    }

    public void requestReadPhoneStatePermission(int requestCode) {
        if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, Manifest.permission.READ_PHONE_STATE) ||
                ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, Manifest.permission.READ_BASIC_PHONE_STATE)) {
            new AlertDialog.Builder(context)
                    .setTitle("Read Phone State Permission Needed")
                    .setMessage("This app needs access to your phone configuration to retrieve cell information and provide accurate results.")
                    .setPositiveButton("OK", (dialog, which) -> {
                        // Proceed with permission request
                        ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_BASIC_PHONE_STATE}, requestCode);
                    })
                    .create()
                    .show();
        } else {
            // No explanation needed, request permissions directly
            ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.READ_BASIC_PHONE_STATE, Manifest.permission.READ_PHONE_STATE}, requestCode);
        }
    }
    private void showPermissionDeniedWarning() {
        // Use a dialog, Toast, or other method to display a message
        new AlertDialog.Builder(context)
                .setTitle("Permission Warning")
                .setMessage("Cell information retrieval requires location permission. " +
                        "Test results will have lower precision without it. " +
                        "The experiment can still be conducted.")
                .setPositiveButton("Continue", (dialog, which) -> dialog.dismiss())
                .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel()) // Optionally, close app if user wants
                .show();
    }

    @Override
    public void onPermissionGranted() {
        retrieveSignalStrength(); // Pass this instance as callback
        retrieveLocation();
    }

    @Override
    public void onPermissionDenied() {
    }
}
