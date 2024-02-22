package com.qos.myapplication.tests;
/*
  Class responsible for obtaining and storing device information.
  <p>
  This class allows access to the following device data:
  - Manufacturer (manufacturer)
  - Model (model)
  - Android version (androidVersion)
  - Carrier (carrier)
  - Location (location)
  - Signal strength (signalStrength)
 */

import android.annotation.SuppressLint;
import android.content.Context;
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
import android.telephony.TelephonyManager;

import com.qos.myapplication.permissionmanager.RequestPermissions;

import java.util.List;

public class DeviceInformation {
    private static final int NOT_ACTIVE_MOBILE_NETWORK = -1;
    private static final String LOCATION_NOT_FOUND = "Location not found";

    public final int DENIED_PERMISSIONS = -2;
    private final int NOT_SIGNAL = 404;
    private final Context context;
    private final RequestPermissions requestPermissions;
    private String manufacturer;
    private String model;
    private String androidVersion;
    private String carrier;
    private String location;
    private int signalStrength;

    /**
     * Constructor for the device information
     *
     * @param context requires the app context.
     **/
    public DeviceInformation(Context context) {
        this.context = context;
        this.requestPermissions = new RequestPermissions(context);
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

    private void setCarrier(String carrier) {
        this.carrier = carrier;
    }

    public String getCarrier() {
        return carrier;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void updateDeviceLocationAndSignal(boolean dontAskAgain, boolean dontAskAgainDenied) {
        retrieveLocation(dontAskAgain);
        retrieveSignalStrength(dontAskAgainDenied, dontAskAgain);
    }

    public void retrieveSignalStrength(boolean requested, boolean request) {

        if (requestPermissions.hasAllNecessaryPermissions()) {
            gettingSignalStrength();
        } else if (requestPermissions.hasLocationPermissions()) {
            gettingSignalStrength();
        } else if (!requested) {
            requestPermissions.showPermissionDeniedWarning();
        } else if (!request) {
            requestPermissions.requestLocationPermissionsDialog();
        } else {
            setSignalStrength(DENIED_PERMISSIONS);
        }
    }

    private void gettingSignalStrength() {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        CellInfo cellInfo;
        if (telephonyManager != null) {
            try {
                @SuppressLint("MissingPermission")
                List<CellInfo> cellInfoList = telephonyManager.getAllCellInfo();
                NetworkInfo activeNetworkInfo = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
                cellInfo = null;
                if (cellInfoList != null) {
                    for (CellInfo info : cellInfoList) {
                        if (info.isRegistered()) {
                            cellInfo = info;
                        }
                    }
                    setCarrier(telephonyManager.getNetworkOperatorName());
                }
                if (activeNetworkInfo == null || activeNetworkInfo.getType() != ConnectivityManager.TYPE_MOBILE) {
                    setSignalStrength(NOT_ACTIVE_MOBILE_NETWORK);
                } else if (activeNetworkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                    measureSignalStrength(cellInfo);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void measureSignalStrength(CellInfo cellInfo) {
        int currentStrength = NOT_SIGNAL;
        if (cellInfo instanceof CellInfoLte) {
            CellSignalStrengthLte cellSignalStrengthLte = ((CellInfoLte) cellInfo).getCellSignalStrength();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                currentStrength = cellSignalStrengthLte.getRssi(); // The signal for LTE must be RSSI
            } else {
                currentStrength = cellSignalStrengthLte.getDbm();
            }
        } else if (cellInfo instanceof CellInfoGsm) {
            CellSignalStrengthGsm cellSignalStrengthGsm = ((CellInfoGsm) cellInfo).getCellSignalStrength();
            currentStrength = cellSignalStrengthGsm.getDbm();
        } else if (cellInfo instanceof CellInfoCdma) {
            CellSignalStrengthCdma cellSignalStrengthCdma = ((CellInfoCdma) cellInfo).getCellSignalStrength();
            currentStrength = cellSignalStrengthCdma.getDbm();
        } else if (cellInfo instanceof CellInfoWcdma) {
            CellSignalStrength cellSignalStrengthWcdma = ((CellInfoWcdma) cellInfo).getCellSignalStrength();
            currentStrength = cellSignalStrengthWcdma.getDbm();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (cellInfo instanceof CellInfoNr) {
                CellSignalStrengthNr cellSignalStrengthNr = (CellSignalStrengthNr) ((CellInfoNr) cellInfo).getCellSignalStrength();
                currentStrength = cellSignalStrengthNr.getDbm();
            }
        }
        setSignalStrength(currentStrength);
    }

    @SuppressLint("MissingPermission")
    public void retrieveLocation(boolean requested) {
        if (requestPermissions.hasLocationPermissions()) {
            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (locationManager != null) {
                gettingLocation(locationManager);
            } else if (!requestPermissions.hasLocationPermissions()) {
                if (!requested) {
                    requestPermissions.showPermissionDeniedWarning();
                } else {
                    setLocation(String.valueOf(DENIED_PERMISSIONS));
                }
            }
        } else {
            setLocation(LOCATION_NOT_FOUND);
        }
    }

    @SuppressLint("MissingPermission")
    public void gettingLocation(LocationManager locationManager) {
        locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER,
                actualLocation -> {
                    double latitude = actualLocation.getLatitude();
                    double longitude = actualLocation.getLongitude();
//                        // Update the location
                    setLocation(latitude + ", " + longitude);
                }, context.getMainLooper());
    }
}



