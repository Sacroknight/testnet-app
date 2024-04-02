package com.qos.testnet.tests;

import androidx.lifecycle.MutableLiveData;

public class PingAndJitterTest {
    private final PingAndJitterStats pingAndJitterStats;
    private String url ="";
    public String getCurrentHost(){
        return url;
    }
    public PingAndJitterTest(){
        pingAndJitterStats = new PingAndJitterStats();
        this.url = pingAndJitterStats.chooseHost();
    }
    public void startPingAndJitterTest(TestCallback testCallback) {
        pingAndJitterStats.measuringPingJitter(url, testCallback);
    }
    public boolean isPingAndJitterTestComplete() {
        return pingAndJitterStats.isFinished();
    }
    public MutableLiveData<String> getPingMeasurement() {
        return pingAndJitterStats.getFinalMeasurement();
    }
    public MutableLiveData<String> getJitterMeasurement() {
        return pingAndJitterStats.getJitterLivedata();
    }
    public MutableLiveData<String> getInstantPingMeasurement() {
        return pingAndJitterStats.getInstantMeasurement();
    }
    public int getPingMeasured(){
        return pingAndJitterStats.getPingMeasured();
    }
    public int getJitterMeasured(){
        return pingAndJitterStats.getJitterMeasured();
    }
    public MutableLiveData<Integer> getProgress(){
        return pingAndJitterStats.getProgress();
    }
}
