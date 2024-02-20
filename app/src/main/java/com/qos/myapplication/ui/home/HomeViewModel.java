package com.qos.myapplication.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.qos.myapplication.tests.DeviceInformation;
import com.qos.myapplication.tests.PingJitterStats;

public class HomeViewModel extends ViewModel {

    private final MutableLiveData<String> mText;
    private final DeviceInformation deviceInformation;
    private final PingJitterStats pingJitterStats;

    public HomeViewModel(DeviceInformation deviceInformation, PingJitterStats pingJitterStats) {
        this.deviceInformation = deviceInformation;
        this.pingJitterStats = pingJitterStats;
        mText = new MutableLiveData<>();
        mText.setValue("This is home fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
    public String getDeviceInfoText(String chosenHost){
        return "Manufacturer: " + deviceInformation.getManufacturer() +
                "\nModel: " + deviceInformation.getModel() +
                "\nAndroid Version: " + deviceInformation.getAndroidVersion() +
                "\nActual Location: " + deviceInformation.getLocation() +
                "\nSignal Strenght: " + deviceInformation.getCarrier() +" "+ deviceInformation.getSignalStrength() +" dBm" +
                "\nCurrent Host: " + chosenHost +
                "\nPing: " + pingJitterStats.getPingMeasure() +
                "\nJitter: " + pingJitterStats.getJitterMeasure();
    }
}