package com.qos.testnet.ui.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import com.qos.testnet.tests.DownloadSpeedStats;
import com.qos.testnet.tests.PingAndJitterMeasurements;
import com.qos.testnet.tests.UploadSpeedStats;
import com.qos.testnet.utils.deviceInformation.DeviceInformation;
import com.qos.testnet.utils.deviceInformation.LocationInfo;
import com.qos.testnet.utils.networkInformation.GetBetterHost;

/**
 * The Home view model
 * Creates the view model for the MVVM architecture
 */
public class HomeViewModel implements ViewModelProvider.Factory {

    private final MutableLiveData<String> mText;
    private final MutableLiveData<String> dText;
    private final MutableLiveData<String> downloadText;
    private final DeviceInformation deviceInformation;
    private final PingAndJitterMeasurements pingAndJitterMeasurements;
    private final DownloadSpeedStats downloadSpeedStats;
    private final UploadSpeedStats uploadSpeedStats;
    private final GetBetterHost getBetterHost;
    private final LocationInfo locationInfo;
    private final Context context;
    private final Handler handler;
    private final ProgressBar progressBar;
    private final Button button;
    private String chosenHost;
    private boolean dontAskAgain;
    private boolean dontAskAgainDenied;
    private boolean success;
    /**
     * The Location callback.
     */
    LocationInfo.LocationCallback locationCallback = new LocationInfo.LocationCallback() {
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
            }
            mText.setValue(String.format("Gps location unavailable: %s", error));
            Toast.makeText(context, "Error getting location: " + error, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onApproxLocationFailed(String error) {
            if (!success) {
                getBestHostAndStartTasks();
            }
            mText.setValue(String.format("Network location unavailable: %s", error));
            downloadText.setValue(getDeviceInfoText());
            Toast.makeText(context, "Error getting location: " + error, Toast.LENGTH_LONG).show();
        }

        @Override
        public void onLocationRetrievalException(Exception e) {
            if (!success) {
                getBestHostAndStartTasks();
            }
            e.printStackTrace();
            downloadText.setValue("Location retrieval failed: " + e.getMessage());
        }
    };


    /**
     * Instantiates a new Home view model.
     *
     * @param deviceInformation         the device information.
     * @param pingAndJitterMeasurements the ping jitter stats.
     * @param downloadSpeedStats        the download speed stats.
     * @param getBetterHost             the get better host.
     * @param locationInfo              the location info.
     * @param context                   the context.
     * @param handler                   the handler.
     * @param button                    The button.
     * @param progressBar               The progress bar.
     */
    public HomeViewModel(DeviceInformation deviceInformation, PingAndJitterMeasurements pingAndJitterMeasurements,
                         DownloadSpeedStats downloadSpeedStats, UploadSpeedStats uploadSpeedStats, GetBetterHost getBetterHost, LocationInfo locationInfo,
                         Context context, Handler handler, Button button, ProgressBar progressBar) {
        this.uploadSpeedStats = uploadSpeedStats;
        this.context = context;
        this.handler = handler;
        this.locationInfo = locationInfo;
        this.deviceInformation = deviceInformation;
        this.getBetterHost = getBetterHost;
        this.pingAndJitterMeasurements = pingAndJitterMeasurements;
        this.downloadSpeedStats = downloadSpeedStats;
        this.button = button;
        this.progressBar = progressBar;
        updatePreferences();
        mText = new MutableLiveData<>();
        dText = new MutableLiveData<>();
        downloadText = new MutableLiveData<>();
        downloadText.setValue("Let's see if works");
    }

    /**
     * Gets text.
     *
     * @return the text
     */
    public LiveData<String> getText() {
        return mText;
    }

    /**
     * Gets device info.
     *
     * @return the device info
     */
    public LiveData<String> getDeviceInfo() {
        return dText;
    }

    /**
     * Gets download speed.
     *
     * @return the download speed
     */
    public LiveData<String> getDownloadSpeed() {
        return downloadText;
    }

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
                "\nCurrent Host: " + chosenHost +
                "\nPing: " + pingAndJitterMeasurements.getPingMeasure() +
                "\nJitter: " + pingAndJitterMeasurements.getJitterMeasure() +
                "\nBest server: " + getBetterHost.getUrlAddress() +
                "\nDownload Speed" + downloadSpeedStats.getFinalDownloadRate();
    }

    private void retrieveLocation() {
        try {
            locationInfo.retrieveLocation(locationCallback, dontAskAgain, dontAskAgainDenied);
        } catch (Exception e) {
            e.printStackTrace();
            locationCallback.onLocationRetrievalException(e);
        }
    }

    private void testDownloadSpeed(String url) {
        try {
            downloadSpeedStats.getDownloadSpeed(url);
        } catch (Exception e) {
            e.printStackTrace();
            downloadSpeedStats.setFinalDownloadRate(DownloadSpeedStats.ERROR_MEASURING_DOWNLOADING_RATE);
        }
    }

    private void testUploadSpeed(String url) {
        try {
            uploadSpeedStats.getUploadSpeed(url);
        } catch (Exception e) {
            e.printStackTrace();
            uploadSpeedStats.setFinalUploadRate(DownloadSpeedStats.ERROR_MEASURING_DOWNLOADING_RATE);
        }
    }

    /**
     * Update preferences.
     */
    public void updatePreferences() {
        SharedPreferences sharedPreferences = context.getSharedPreferences("MyPrefs",
                Context.MODE_PRIVATE);
        dontAskAgain = sharedPreferences.getBoolean("dontAskAgain", false);
        dontAskAgainDenied = sharedPreferences.getBoolean("dontAskAgainDenied", false);
    }

    /**
     * Start tasks.
     */
    public void startTasks() {
        chosenHost = pingAndJitterMeasurements.chooseHost();
        deviceInformation.retrieveSignalStrength(dontAskAgain, dontAskAgainDenied);
        button.setEnabled(false);
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
        new Thread(new Runnable() {
            @Override
            public void run() {
                getBetterHost.getBestHost(location, deviceInformation.getCarrier());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        startPingJitterTask(chosenHost);
                    }
                });
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
    private void getBestHostAndStartTasks() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                getBetterHost.getBestHost(deviceInformation.getCarrier());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        startPingJitterTask(chosenHost);
                    }
                });
            }
        }).start();
    }

    /**
     * Initiates a new thread to asynchronously perform ping and jitter measurements,
     * refreshing the UI with results upon completion.
     * <p>
     * **Key Functionality:**
     * - Launches a separate thread to prevent blocking the UI thread for network operations.
     * - Measures ping and jitter using `pingAndJitterMeasurements.measuringPingJitter(chosenHost)`.
     * - Updates UI with results, enables the button, and hides the progress bar using a `Handler` to execute tasks on the UI thread.
     * <p>
     * **Memory Leak Prevention:**
     * - **Avoids static inner classes:** By using anonymous inner classes, we prevent implicit static references to the enclosing fragment or activity, mitigating memory leaks.
     * - **Utilizes a Handler for UI updates:** Employs a `Handler` to schedule UI updates on the main thread, ensuring proper thread management and avoiding leaks.
     *
     * @param chosenHost The host to measure ping and jitter against.
     */
    private void startPingJitterTask(final String chosenHost) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                pingAndJitterMeasurements.measuringPingJitter(chosenHost);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setProgress(0);
                        startDownloadSpeedTask();
                        dText.postValue(getDeviceInfoText());
                    }
                });
            }
        }).start();
    }

    /**
     * Initiates a task to measure the download speed asynchronously.
     * <p>
     * This method triggers the download speed test using the URL address of the determined optimal
     * host or user selected host.
     * Upon completion of the test, it updates the UI elements accordingly, making the progress bar invisible,
     * enabling a previously disabled button, and updating the displayed device information text.
     */
    public void startDownloadSpeedTask() {
        new Thread(() -> {
            testDownloadSpeed(getBetterHost.getUrlAddress());
            handler.post(() -> {
                dText.postValue(getDeviceInfoText());
                startUploadSpeedTask();
            });
        }).start();
    }

    public void startUploadSpeedTask() {
        new Thread(() -> {
            testUploadSpeed(getBetterHost.getUrlUploadAddress());
            handler.post(() -> {
                button.setEnabled(true);
                dText.postValue(getDeviceInfoText());
                success =false;
            });
        }).start();
    }
}