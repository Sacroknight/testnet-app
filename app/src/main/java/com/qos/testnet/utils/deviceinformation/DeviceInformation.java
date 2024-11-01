package com.qos.testnet.utils.deviceinformation;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoNr;
import android.telephony.CellInfoWcdma;
import android.telephony.CellSignalStrengthCdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthNr;
import android.telephony.CellSignalStrengthWcdma;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.Nullable;

import com.qos.testnet.permissionmanager.RequestPermissions;

import java.util.List;

/*
  Class responsible for obtaining and storing device information.
  <p>
  This class allows access to the following device data:
  - Manufacturer (manufacturer)
  - Model (model)
  - Android version (androidVersion)
  - Carrier (carrier)
  - Signal strength (signalStrength)
 */

public class DeviceInformation {
    public static final int DENIED_PERMISSIONS = -2;
    private static final int NETWORK_TYPE_UNKNOWN_OR_INACTIVE = -1;
    private final Context context;
    private final RequestPermissions requestPermissions;
    private String manufacturer;
    private String model;
    private String androidVersion;
    private String carrier;
    private String networkType;
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
        setNetworkType(getActiveNetworkType(context));
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer() {
        this.manufacturer = Build.MANUFACTURER;
    }

    public String getNetworkType() {
        return networkType;
    }

    public void setNetworkType(String network) {
        this.networkType = network;
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

    public String getCarrier() {
        return carrier;
    }

    private void setCarrier(String carrier) {
        this.carrier = carrier;
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
        if (!requestPermissions.hasReadPhonePermissions() || !requestPermissions.hasNetworkStatePermissions()) {
            requestPermissions.requestReadPhonePermissions();
            return;
        }
        if (telephonyManager != null) {
            try {
                CellInfo activeCellInfo = getCellInfo(telephonyManager);

                if (activeCellInfo != null) {
                    setCarrier(telephonyManager.getNetworkOperatorName());
                    measureSignalStrength(activeCellInfo);
                } else {
                    setSignalStrength(NETWORK_TYPE_UNKNOWN_OR_INACTIVE);
                }

                ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                if (connectivityManager != null) {
                    NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                    if (networkCapabilities == null || !networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                        setSignalStrength(NETWORK_TYPE_UNKNOWN_OR_INACTIVE);
                    }
                }
            } catch (Exception e) {
                Log.e("DeviceInformation", "Error al obtener la señal de red: " + e.getMessage());
            }
        }
    }

    @Nullable
    private static CellInfo getCellInfo(TelephonyManager telephonyManager) {
        @SuppressLint("MissingPermission") List<CellInfo> cellInfoList = telephonyManager.getAllCellInfo();
        CellInfo activeCellInfo = null;

        if (cellInfoList != null) {
            for (CellInfo info : cellInfoList) {
                if (info.isRegistered()) {
                    activeCellInfo = info;
                    break;
                }
            }
        }
        return activeCellInfo;
    }

    private void measureSignalStrength(CellInfo cellInfo) {
        int currentStrength = NETWORK_TYPE_UNKNOWN_OR_INACTIVE;
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
            CellSignalStrengthWcdma cellSignalStrengthWcdma = ((CellInfoWcdma) cellInfo).getCellSignalStrength();
            currentStrength = cellSignalStrengthWcdma.getDbm();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && cellInfo instanceof CellInfoNr) {
            @SuppressLint({"NewApi", "LocalSuppress"}) CellSignalStrengthNr cellSignalStrengthNr = (CellSignalStrengthNr) cellInfo.getCellSignalStrength();
            currentStrength = cellSignalStrengthNr.getDbm();
        }
        setSignalStrength(currentStrength);
    }

    public String getActiveNetworkType(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

        if (connectivityManager != null && telephonyManager != null) {
            Network activeNetwork = connectivityManager.getActiveNetwork();
            if (activeNetwork != null) {
                NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork);

                if (networkCapabilities != null && networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    @SuppressLint("MissingPermission") int networkTypeNumber = telephonyManager.getDataNetworkType(); // Use getDataNetworkType() instead
                    return getNetworkTypeName(networkTypeNumber);
                } else if (networkCapabilities != null && networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    return "WiFi";
                }
            }
        }
        return String.valueOf(NETWORK_TYPE_UNKNOWN_OR_INACTIVE);
    }

    private String getNetworkTypeName(int networkType) {
        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
                return "2G (GPRS)";
            case TelephonyManager.NETWORK_TYPE_EDGE:
                return "2G (EDGE)";
            case TelephonyManager.NETWORK_TYPE_UMTS:
                return "3G (UMTS)";
            case TelephonyManager.NETWORK_TYPE_HSDPA:
                return "3G (HSDPA)";
            case TelephonyManager.NETWORK_TYPE_HSUPA:
                return "3G (HSUPA)";
            case TelephonyManager.NETWORK_TYPE_HSPA:
                return "3G (HSPA)";
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return "3G (HSPA+)";
            case TelephonyManager.NETWORK_TYPE_LTE:
                return "4G (LTE)";
            case TelephonyManager.NETWORK_TYPE_NR:
                return "5G";
            default:
                return String.valueOf(NETWORK_TYPE_UNKNOWN_OR_INACTIVE);
        }
    }
}

