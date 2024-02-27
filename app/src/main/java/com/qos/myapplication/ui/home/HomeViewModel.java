package com.qos.myapplication.ui.home;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import com.qos.myapplication.tests.DeviceInformation;
import com.qos.myapplication.tests.DownloadSpeedStats;
import com.qos.myapplication.tests.PingJitterStats;

public class HomeViewModel implements ViewModelProvider.Factory {

    private final MutableLiveData<String> mText;
    private final MutableLiveData<String> dText;
    private final MutableLiveData<String> downloadText;
    private final DeviceInformation deviceInformation;
    private final PingJitterStats pingJitterStats;
    private final DownloadSpeedStats downloadSpeedStats;
    private String chosenHost;

    public HomeViewModel(DeviceInformation deviceInformation, PingJitterStats pingJitterStats, DownloadSpeedStats downloadSpeedStats) {
        this.downloadSpeedStats = downloadSpeedStats;
        this.deviceInformation = deviceInformation;
        this.pingJitterStats = pingJitterStats;
        mText = new MutableLiveData<>();
        dText = new MutableLiveData<>();
        mText.setValue("This is home fragment");
        downloadText =  new MutableLiveData<>();
        downloadText.setValue("A ver si mide");
    }


    public LiveData<String> getText() {
        return mText;
    }

    public LiveData<String> getDeviceInfo() {
        return dText;
    }
    public LiveData<String> getDownloadSpeed() {
        return downloadText;
    }

    public void startPingJitterMeasurement(Button button, ProgressBar progressBar,
                                           SharedPreferences sharedPreferences) {
        chosenHost = pingJitterStats.chooseHost();
        button.setEnabled(false);
        new PingJitterTask(pingJitterStats, deviceInformation, chosenHost, button,
                progressBar, sharedPreferences).execute();
    }
    public String getDeviceInfoText() {

        return "Manufacturer: " + deviceInformation.getManufacturer() +
                "\nModel: " + deviceInformation.getModel() +
                "\nAndroid Version: " + deviceInformation.getAndroidVersion() +
                "\nActual Location: " + deviceInformation.getLocation() +
                "\nSignal Strenght: " + deviceInformation.getCarrier() + " " +
                deviceInformation.getSignalStrength() + " dBm" +
                "\nCurrent Host: " + chosenHost +
                "\nPing: " + pingJitterStats.getPingMeasure() +
                "\nJitter: " + pingJitterStats.getJitterMeasure() +
                "\nDownload Speed" + downloadSpeedStats.getDownloadSpeed();
    }
    public void showDownloadSpeed(){
        while (!downloadSpeedStats.finished){
            downloadText.setValue(String.valueOf(downloadSpeedStats.getDownloadSpeed()));
        }
        //downloadText.setValue(String.valueOf(downloadSpeedStats.measureDownloadSpeed()));
    }
    private class PingJitterTask extends AsyncTask<Void, Integer, Void> {

        private final PingJitterStats pingJitterStats;
        private final DeviceInformation deviceInformation;
        private final String chosenHost;
        private final Button button;
        private final ProgressBar progressBar;
        private final SharedPreferences sharedPreferences;

        public PingJitterTask(PingJitterStats pingJitterStats, DeviceInformation deviceInformation,
                              String chosenHost, Button button, ProgressBar progressBar, SharedPreferences sharedPreferences) {
            this.pingJitterStats = pingJitterStats;
            this.deviceInformation = deviceInformation;
            this.sharedPreferences = sharedPreferences;
            this.chosenHost = chosenHost;
            this.button = button;
            this.progressBar = progressBar;
        }
        @Override
        protected void onPreExecute(){
            deviceInformation.updateDeviceLocationAndSignal(sharedPreferences.getBoolean("dontAskAgain", false)
                    ,sharedPreferences.getBoolean("dontAskAgainDenied",false));
        }

        @Override
        protected Void doInBackground(Void... voids) {
            pingJitterStats.measuringPingJitter(chosenHost);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            progressBar.setProgress(0);
            getDeviceInfoText();
            dText.setValue(getDeviceInfoText());
            button.setEnabled(true);
        }
    }
}