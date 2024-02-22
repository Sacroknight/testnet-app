package com.qos.myapplication.ui.home;

import android.os.AsyncTask;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import com.qos.myapplication.tests.DeviceInformation;
import com.qos.myapplication.tests.PingJitterStats;

public class HomeViewModel implements ViewModelProvider.Factory {

    private final MutableLiveData<String> mText;
    private final MutableLiveData<String> dText;
    private final DeviceInformation deviceInformation;
    private final PingJitterStats pingJitterStats;
    private String chosenHost;

    public HomeViewModel(DeviceInformation deviceInformation, PingJitterStats pingJitterStats) {

        this.deviceInformation = deviceInformation;
        this.pingJitterStats = pingJitterStats;
        mText = new MutableLiveData<>();
        dText = new MutableLiveData<>();
        mText.setValue("This is home fragment");

    }


    public LiveData<String> getText() {
        return mText;
    }

    public LiveData<String> getDeviceInfo() {
        return dText;
    }

    public void startPingJitterMeasurement(Button button, ProgressBar progressBar) {
        chosenHost = pingJitterStats.chooseHost();
        button.setEnabled(false);
        new PingJitterTask(pingJitterStats, chosenHost, button, progressBar).execute();
    }


    public String getDeviceInfoText() {

        return "Manufacturer: " + deviceInformation.getManufacturer() +
                "\nModel: " + deviceInformation.getModel() +
                "\nAndroid Version: " + deviceInformation.getAndroidVersion() +
                "\nActual Location: " + deviceInformation.getLocation() +
                "\nSignal Strenght: " + deviceInformation.getCarrier() + " " + deviceInformation.getSignalStrength() + " dBm" +
                "\nCurrent Host: " + chosenHost +
                "\nPing: " + pingJitterStats.getPingMeasure() +
                "\nJitter: " + pingJitterStats.getJitterMeasure();
    }

    private class PingJitterTask extends AsyncTask<Void, Integer, Void> {

        private final PingJitterStats pingJitterStats;
        private final String chosenHost;
        private final Button button;
        private final ProgressBar progressBar;

        public PingJitterTask(PingJitterStats pingJitterStats, String chosenHost, Button button, ProgressBar progressBar) {
            this.pingJitterStats = pingJitterStats;
            this.chosenHost = chosenHost;
            this.button = button;
            this.progressBar = progressBar;
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