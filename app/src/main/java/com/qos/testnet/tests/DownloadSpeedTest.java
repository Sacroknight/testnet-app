package com.qos.testnet.tests;

import android.content.Context;

import androidx.databinding.BaseObservable;
import androidx.lifecycle.MutableLiveData;

public class DownloadSpeedTest extends BaseObservable {
    private final DownloadSpeedStats downloadSpeedStats;
    public DownloadSpeedTest(Context context) {
        downloadSpeedStats = new DownloadSpeedStats(context);
    }
    public void startSpeedTest(String url, TestCallback callback) {
        downloadSpeedStats.setFinished(false);
        downloadSpeedStats.runDownloadSpeedTest(callback, url);
    }

    public boolean isSpeedTestComplete() {
        return downloadSpeedStats.isFinished();
    }

    public MutableLiveData<String> getFinalDownloadRate() {
        return downloadSpeedStats.getFinalMeasurement();
    }
    public MutableLiveData<String> getInstantDownloadRate() {
        return downloadSpeedStats.getInstantMeasurement();
    }
    public MutableLiveData<Integer> getInstantDownloadRateProgress(){
        return downloadSpeedStats.getProgress();
    }
    public double getFinalDownloadSpeed(){
        return downloadSpeedStats.getFinalDownloadRate();
    }
}
