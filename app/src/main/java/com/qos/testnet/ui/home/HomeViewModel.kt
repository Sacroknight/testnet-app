package com.qos.testnet.ui.home;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

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
    private static MutableLiveData<String> deviceInfo = new MutableLiveData<>();
    private static MutableLiveData<String> pingMeasurement = new MutableLiveData<>();
    private static MutableLiveData<String> instantMeasurements = new MutableLiveData<>();
    private static MutableLiveData<String> jitterMeasurement = new MutableLiveData<>();
    private static MutableLiveData<Integer> progress = new MutableLiveData<>();
    private static MutableLiveData<String> finalDownloadRate = new MutableLiveData<>();
    private static MutableLiveData<String> availableServers = new MutableLiveData<>();
    private static MutableLiveData<Boolean> isFinished = new MutableLiveData<>();
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

    public static MutableLiveData<String> getPingMeasurement() {
        return pingMeasurement;
    }

    public static MutableLiveData<String> getJitterMeasurement() {
        return jitterMeasurement;
    }

    public static MutableLiveData<String> getDeviceInfo() {
        return deviceInfo;
    }

    public static MutableLiveData<String> getInstantMeasurements() {
        return instantMeasurements;
    }

    public static MutableLiveData<Integer> getProgress() {
        return progress;
    }
    public static MutableLiveData<Boolean> isFinished() {
        return isFinished;
    }
    private String location;
    public String getLocation() {
        return location;
    }
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * Start the location retrieval process
     */
    private void startLocationRetrieval() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    locationInfo.retrieveLocation(dontAskAgain, dontAskAgainDenied, new LocationCallback() {
                        @Override
                        public void onLocationSuccess(String gpsLocation) {
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    if (!success) {
                                        setLocation(gpsLocation);
                                        getBestHostForNetworkTests();
                                        success = true;
                                    }
                                }
                            }, 100);
                        }

                        @Override
                        public void onApproxLocationSuccess(String networkLocation) {
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    if (!success) {
                                        setLocation(networkLocation);
                                        getBestHostForNetworkTests();
                                        success = true;
                                    }
                                }
                            }, 100);
                        }

                        @Override
                        public void onLocationFailed(String error) {
                            Toast.makeText(context, "Gps location unavailable: %s" + error, Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onApproxLocationFailed(String error) {
                            deviceInfo.postValue("Network location unavailable: %s" + error);
                        }

                        @Override
                        public void onLocationRetrievalException(String e) {
                            deviceInfo.postValue("Location retrieval failed: " + e);
                        }
                    });
                } finally {
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (!success) {
                                getBestHostForNetworkTestsWithApi();
                            } else {
                                success = false;
                            }
                        }
                    }, 100);

                }
            }
        }).start();
    }

    /**
     * Start ping and jitter test
     */
    public void startPingAndJitterTest() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                pingAndJitterTest.startPingAndJitterTest(new TestCallback() {
                    @Override
                    public void OnTestStart() {
                        deviceInfo.postValue("");
                    }

                    @Override
                    public void OnTestSuccess(String jitter) {
                        pingMeasurement.postValue(pingAndJitterTest.getPingMeasurement().getValue());
                        jitterMeasurement.postValue(pingAndJitterTest.getJitterMeasurement().getValue());
                        instantMeasurements.postValue("");
                        progress.postValue(0);
                        new Handler(Looper.getMainLooper()).postDelayed(
                                HomeViewModel.this::startDownloadSpeedTest, 500);
                    }

                    @Override
                    public void OnTestBackground(String currentBackgroundMeasurement,
                                                 int currentBackgroundProgress) {
                        instantMeasurements.postValue(currentBackgroundMeasurement);
                        progress.postValue(currentBackgroundProgress);
                    }

                    @Override
                    public void OnTestFailure() {
                        TestCallback.super.OnTestFailure();
                    }
                });
            }
        }).start();
    }



    /**
     * Start download speed test
     */
    public void startDownloadSpeedTest() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    downloadSpeedTest.startSpeedTest(getBetterHost.getUrlAddress(), new
                            TestCallback() {
                                @Override
                                public void OnTestStart() {
                                    deviceInfo.postValue("");
                                }

                                @Override
                                public void OnTestSuccess(String downloadSpeed) {
                                    finalDownloadRate.postValue(downloadSpeed);
                                }

                                @Override
                                public void OnTestBackground(String currentBackgroundMeasurement,
                                                             int currentBackgroundProgress) {
                                    progress.postValue(currentBackgroundProgress);
                                    instantMeasurements.postValue(currentBackgroundMeasurement);
                                }


                                @Override
                                public void OnTestFailure() {
                                    TestCallback.super.OnTestFailure();
                                }

                            });
                } finally {
                    progress.postValue(0);
                    instantMeasurements.postValue("");
                    deviceInfo.postValue(getDeviceInformation());
                    isFinished.postValue(true);
                }
            }
        }).start();
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
            availableServers.postValue(response);
        }

        @Override
        public void onRequestFailure(String error) {
            deviceInfo.postValue(error);
            Toast.makeText(context, "Failure on the request of the best host", Toast.LENGTH_LONG).show();
        }
    };


    /**
     * Gets device info text.
     *
     * @return the device info text
     */
    public String getDeviceInformation() {

        return "Manufacturer: " + deviceInformation.getManufacturer() +
                "\nModel: " + deviceInformation.getModel() +
                "\nAndroid Version: " + deviceInformation.getAndroidVersion() +
                "\nActual Location: " + locationInfo.getCurrentLocation() +
                "\nApproximate Location: " + locationInfo.getApproximateLocation() +
                "\nSignal Strength: " + deviceInformation.getCarrier() + " " +
                deviceInformation.getSignalStrength() + " dBm" +
                "\nCurrent Host: " + pingAndJitterTest.getCurrentHost() +
                "\nPing: " + pingAndJitterTest.getPingMeasured() + " ms" +
                "\nJitter: " + pingAndJitterTest.getJitterMeasured() + " ms" +
                "\nBest server: " + getBetterHost.getUrlAddress() +
                "\nDownload Speed: " + downloadSpeedTest.getFinalDownloadSpeed() + " Mb/s";
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
        startLocationRetrieval();
    }

    public void getBetterHost(){
        getBetterHost.getBestHost(locationInfo.getCurrentLocation(),deviceInformation.getCarrier(),networkCallback);
    }
    private void getBetterHostAPI(){
        getBetterHost.getBestHost(deviceInformation.getCarrier(), networkCallback);
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
     *
     * **Key Functionality:**
     * - Asynchronously identifies the optimal host for network tests.
     * - Initiates ping and jitter measurements on the chosen host.
     *
     */

    private void getBestHostForNetworkTests() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                getBetterHost();
                new Handler(Looper.getMainLooper()).postDelayed(
                        HomeViewModel.this::startPingAndJitterTest, 100);
            }

        }).start();
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
    private void getBestHostForNetworkTestsWithApi() {
        new Thread(() -> {
            getBetterHostAPI();
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    startPingAndJitterTest();
                }
            }, 100);
        }).start();
    }
}

