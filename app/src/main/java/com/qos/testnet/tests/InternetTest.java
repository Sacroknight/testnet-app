package com.qos.testnet.tests;

import androidx.lifecycle.MutableLiveData;

public interface InternetTest {
    MutableLiveData<String> getInstantMeasurement();
    void setInstantMeasurement(String instantMeasurement);

    MutableLiveData<String> getFinalMeasurement();
    void setFinalMeasurement(String finalMeasurement);
    MutableLiveData<Integer> getProgress();
    void setProgress(int progress);
}
