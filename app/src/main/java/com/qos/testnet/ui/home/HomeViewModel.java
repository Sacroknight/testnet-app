package com.qos.testnet.ui.home;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.qos.testnet.tests.DownloadSpeedTest;
import com.qos.testnet.tests.PingAndJitterTest;
import com.qos.testnet.tests.TestCallback;
import com.qos.testnet.tests.UploadSpeedStats;
import com.qos.testnet.utils.deviceInformation.DeviceInformation;
import com.qos.testnet.utils.deviceInformation.LocationCallback;
import com.qos.testnet.utils.deviceInformation.LocationInfo;
import com.qos.testnet.utils.networkInformation.GetBetterHost;
import com.qos.testnet.utils.networkInformation.NetworkCallback;

/**
 * The Home view model
 * Creates the view model for the MVVM architecture
 */
public class HomeViewModel extends ViewModel {
    private static MutableLiveData<String> deviceInfo = null;
    private static MutableLiveData<String> pingMeasurement = null;
    private static MutableLiveData<String> instantMeasurements = null;
    private static MutableLiveData<String> jitterMeasurement = null;
    private static MutableLiveData<Integer> progress = null;
    private static MutableLiveData<String> instantDownloadRate = null;
    private DeviceInformation deviceInformation = null;
    private GetBetterHost getBetterHost = null;
    private PingAndJitterTest pingAndJitterTest = null;
    private DownloadSpeedTest downloadSpeedTest = null;
    private UploadSpeedStats uploadSpeedStats = null;
    private LocationInfo locationInfo = null;
    @SuppressLint("StaticFieldLeak")
    private Context context = null;
    private boolean dontAskAgain;
    private boolean dontAskAgainDenied;
    private boolean success;
    private MutableLiveData<String> finalDownloadRate = null;

    /**
     * Start the location retrieval process
     */
    private void retrieveLocation() {
        try {
            locationInfo.retrieveLocation(locationCallback, dontAskAgain, dontAskAgainDenied);
        } catch (Exception e) {
            e.printStackTrace();
            locationCallback.onLocationRetrievalException(e);
        }
    }

    /**
     * Start ping and jitter test
     */
    public void startPingJitterTest() {
        pingAndJitterTest.startPingAndJitterTest(new TestCallback() {
            @Override
            public void OnTestStart() {
                deviceInfo.postValue("");
            }

            @Override
            public void OnTestSuccess(String jitter) {
                pingMeasurement.postValue(pingAndJitterTest.getPingMeasurement().getValue());
                jitterMeasurement.postValue(pingAndJitterTest.getJitterMeasurement().getValue());
                deviceInfo.postValue(getDeviceInfoText());
                progress.postValue(0);
            }

            @Override
            public void OnTestBackground(String currentBackgroundMeasurement, int currentBackgroundProgress) {
                    instantMeasurements.postValue(currentBackgroundMeasurement);
                    progress.postValue(currentBackgroundProgress);
            }

            @Override
            public void OnTestFailure() {
                TestCallback.super.OnTestFailure();
            }
        });
    }

    public static LiveData<String> getPingMeasurement() {
        return pingMeasurement;
    }

    public static LiveData<String> getJitterMeasurement() {
        return jitterMeasurement;
    }

    public static MutableLiveData<String> getDeviceInfo() {
        return deviceInfo;
    }

    public static MutableLiveData<String> getInstantMeasurements() {
        return instantMeasurements;
    }

    public static LiveData<Integer> getProgress() {
        return progress;
    }

    /**
     * Start download speed test
     */
    public void startDownloadSpeedTest() {
        downloadSpeedTest.startSpeedTest(getBetterHost.getUrlAddress(), new TestCallback() {
            @Override
            public void OnTestStart() {
                deviceInfo.postValue("");
            }

            @Override
            public void OnTestSuccess(String downloadSpeed) {
                finalDownloadRate.postValue(downloadSpeed);
                deviceInfo.postValue(getDeviceInfoText());
                progress.postValue(0);
            }
            @Override
            public void OnTestBackground(String currentBackgroundMeasurement, int currentBackgroundProgress) {
                    progress.postValue(downloadSpeedTest.getInstantDownloadRateProgress().getValue());
                    instantDownloadRate.postValue(downloadSpeedTest.getInstantDownloadRate().getValue());
            }


            @Override
            public void OnTestFailure() {
                TestCallback.super.OnTestFailure();
            }
        });
    }

    /**
     * new constructor
     */
    public HomeViewModel(Context homeContext) {
        this.context = homeContext;
        getBetterHost = new GetBetterHost();
        downloadSpeedTest = new DownloadSpeedTest();
        pingAndJitterTest = new PingAndJitterTest();
        deviceInformation = new DeviceInformation(context);
        locationInfo = new LocationInfo(context);
        pingMeasurement = new MutableLiveData<>();
        instantMeasurements = new MutableLiveData<>();
        jitterMeasurement = new MutableLiveData<>();
        progress = new MutableLiveData<>();
        deviceInfo = new MutableLiveData<>();
        instantDownloadRate = new MutableLiveData<>();
        finalDownloadRate = new MutableLiveData<>();
        success = false;
        updatePreferences(context);
    }

    /**
     * The network callback.
     */
    NetworkCallback networkCallback = new NetworkCallback() {

        @Override
        public void onRequestSuccess(String response) {
            startPingJitterTest();
        }

        @Override
        public void onRequestFailure(String error) {
            deviceInfo.postValue(error);
            Toast.makeText(context, "Failure on the request of the best host", Toast.LENGTH_LONG).show();
        }
    };
    /**
     * The Location callback.
     */
    LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationSuccess(String location) {
            if (!success) {
                getBestHostAndStartTasks(location);
                success = true;
            }
        }

        @Override
        public void onApproxLocationSuccess(String location) {
            if (!success) {
                getBestHostAndStartTasks(location);
                success = true;
            }
        }

        @Override
        public void onLocationFailed(String error) {
            if (!success) {
                getBestHostAndStartTasks();
                success = true;
            }
            Toast.makeText(context, "Gps location unavailable: %s" + error, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onApproxLocationFailed(String error) {
            if (!success) {
                getBestHostAndStartTasks();
                success = true;
            }
            deviceInfo.postValue(getDeviceInfoText());
            Toast.makeText(context, "Network location unavailable: %s" + error, Toast.LENGTH_LONG).show();
        }

        @Override
        public void onLocationRetrievalException(Exception e) {
            if (!success) {
                getBestHostAndStartTasks();
                success = true;
            }
            e.printStackTrace();
            deviceInfo.postValue("Location retrieval failed: " + e.getMessage());
        }
    };

    /**
     * Gets device info text.
     *
     * @return the device info text
     */
    public String getDeviceInfoText() {

        return "Manufacturer: " + deviceInformation.getManufacturer() +
                "\nModel: " + deviceInformation.getModel() +
                "\nAndroid Version: " + deviceInformation.getAndroidVersion() +
                "\nActual Location: " + locationInfo.getCurrentLocation() +
                "\nApproximate Location: " + locationInfo.getApproximateLocation() +
                "\nSignal Strength: " + deviceInformation.getCarrier() + " " +
                deviceInformation.getSignalStrength() + " dBm" +
                "\nCurrent Host: " + pingAndJitterTest.getCurrentHost() +
                "\nPing: " + pingAndJitterTest.getPingMeasurement().getValue() +
                "\nJitter: " + pingAndJitterTest.getJitterMeasurement().getValue() +
                "\nBest server: " + getBetterHost.getUrlAddress() +
                "\nDownload Speed" + downloadSpeedTest.getFinalDownloadRate().getValue() ;
    }

    /**
     * Update preferences.
     */
    public void updatePreferences(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("MyPrefs",
                Context.MODE_PRIVATE);
        dontAskAgain = sharedPreferences.getBoolean("dontAskAgain", false);
        dontAskAgainDenied = sharedPreferences.getBoolean("dontAskAgainDenied", false);
    }

    /**
     * Start tasks.
     */
    public void startTasks() {
        deviceInformation.retrieveSignalStrength(dontAskAgain, dontAskAgainDenied);
        retrieveLocation();
    }

    /**
     * Initiates a new thread to asynchronously determine the best host for conducting network tests,
     * including ping and jitter measurements.
     * <p>
     * The process involves querying a service to obtain the optimal host based on location and carrier information.
     * Once the best host is identified, the ping and jitter tasks are initiated,
     * and the user interface is updated with the results upon completion.
     * <p>
     * note: This method is called only if the retrieve of the location is successful
     * <p>
     * **Key Functionality:**
     * - Asynchronously identifies the optimal host for network tests.
     * - Initiates ping and jitter measurements on the chosen host.
     *
     * @param location the location retrieved by the user device
     */

    private void getBestHostAndStartTasks(String location) {
        getBetterHost.getBestHost(location, deviceInformation.getCarrier(), networkCallback);
    }

    /**
     * Initiates a new thread to asynchronously determine the best host for conducting network tests,
     * including ping and jitter measurements.
     * <p>
     * The process involves querying a service to obtain the optimal host based on device and carrier information.
     * Once the best host is identified, the ping and jitter tasks are initiated,
     * <p>
     * Note: This method is called only if the retrieval of the location is unsuccessful or times out.
     * Note: The location is provided by the ip2location API service.
     * <p>
     * **Key Functionality:**
     * - Asynchronously identifies the optimal host for network tests.
     * - Initiates ping and jitter measurements on the chosen host.
     */
    private void getBestHostAndStartTasks() {
        getBetterHost.getBestHost(deviceInformation.getCarrier(), networkCallback);
    }
}

