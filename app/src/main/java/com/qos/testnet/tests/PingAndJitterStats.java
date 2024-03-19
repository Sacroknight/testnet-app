package com.qos.testnet.tests;


import androidx.lifecycle.MutableLiveData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;


public class PingAndJitterStats implements InternetTest, TestCallback {
    public static final int MAX_PING_TIMES = 40;
    private static final int TIMEOUT_MS = 5000;
    private static final int ERROR_MEASURING_PING = -1;
    private int probes = 0;

    private MutableLiveData<String> instantPing = null;
    private MutableLiveData<String> finalPing = null;
    private MutableLiveData<String> jitter = null;
    private MutableLiveData<Integer> progress = null;
    Map<String, Integer> host;
    private int pingMeasure;
    private int jitterMeasure;
    private boolean finished;

    /**
     * Return if the ping and jitter test is finished.
     * @return boolean finished
     */
    public boolean isFinished() {
        return finished;
    }
    /**
     * Set if the ping and jitter test is finished.
     */
    public void setFinished(boolean finished) {
        this.finished = finished;
    }
    /**
     * Return the ping measured.
     * @return Measured ping
     */
    public int getPingMeasured(){
        return pingMeasure;
    }
    /**
     * Set the ping measured.
     */
    private void setFinalPing(int finalPing) {
        this.pingMeasure = finalPing;
    }
    /**
     * Return the jitter measured.
     * @return jitter measured
     */
    public int getJitterMeasure() {
        return jitterMeasure;
    }

    /**
     * Set the jitter measured.
     */
    private void setJitterMeasure(int jitterMeasure) {
        this.jitterMeasure = jitterMeasure;
    }

    /**
     * Return the jitter measured for the ui updates.
     * @return jitter measured
     */
    public MutableLiveData<String> getJitterLivedata() {
        return jitter;
    }
    /**
     * Set the jitter measured for the ui updates.
     */
    public void setJitterLivedata(String jitterMeasured) {
        jitter.postValue(jitterMeasured);
    }

    public PingAndJitterStats() {
        getMostVisitedWebsites();
        progress = new MutableLiveData<>();
        instantPing = new MutableLiveData<>();
        finalPing = new MutableLiveData<>();
        jitter = new MutableLiveData<>();
        finished = false;
    }
    private void getMostVisitedWebsites() {
        host = new HashMap<>();
        host.put("google.com", 1);
        host.put("youtube.com", 2);
        host.put("whatsapp.com", 3);
        host.put("facebook.com", 4);
        host.put("instagram.com", 5);
        host.put("live.com", 6);
        host.put("openai.com", 7);
        host.put("office.com", 8);
        host.put("microsoft.com", 9);
        host.put("lan.leagueoflegends.com", 10);
    }

    public String chooseHost() {
        List<String> hostList = new ArrayList<>(host.keySet());
        Random random = new Random();
        return hostList.get(random.nextInt(hostList.size()));
    }

    public void measuringPingJitter(String chosenHost, TestCallback testCallback) {
        List<Integer> pingList = new ArrayList<>();
        for (int i = 0; i < MAX_PING_TIMES; i++) {
            int ping = measuringPing(chosenHost, testCallback);
            pingList.add(ping);
            setInstantMeasurement(ping + " ms");
            setProgress(i*(100/MAX_PING_TIMES));
            testCallback.OnTestBackground(ping + " ms", i*(100/MAX_PING_TIMES));
        }
        calculateAndSetStatistics(pingList, testCallback);
    }



    private void calculateAndSetStatistics(List<Integer> pingList, TestCallback testCallback) {
        int averagePing = (int) pingList.stream().mapToInt(Integer::intValue).average().orElse(0);
        double variance = pingList.stream().mapToDouble(ping -> Math.pow(ping - averagePing, 2)).average().orElse(0);
        int jitter = (int) Math.sqrt(variance);
        setFinalPing(averagePing);
        setFinalMeasurement(averagePing + " ms");
        setJitterMeasure(jitter);
        setJitterLivedata(jitter + " ms");
        setFinished(true);
        testCallback.OnTestSuccess(jitter + " ms");
    }


    private int measuringPing(String chosenHost, TestCallback testCallback) {
        int probes = 0;
        int ping = 0;
        if (chosenHost == null || chosenHost.isEmpty()) {
            OnTestFailed(chosenHost);
            ping = ERROR_MEASURING_PING;
        }
        try {
            Process process = new ProcessBuilder("ping", "-c", "1", "-w", String.valueOf(TIMEOUT_MS), chosenHost).start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("time=") && probes <= 3) {
                    int startIndex = line.indexOf("time=") + 5;
                    int endIndex = line.indexOf(" ms", startIndex);
                    ping = (int) Float.parseFloat(line.substring(startIndex, endIndex));
                    break;
                } else {
                    probes++;
                }
            }
            if (probes >= 3) {
                ping = ERROR_MEASURING_PING;
            }
            reader.close();
            process.destroy();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return ping;
    }
    /**
     * Set the instant ping measured.
     */
    @Override
    public MutableLiveData<String> getInstantMeasurement() {
        return instantPing;
    }
    /**
     * Set the instant ping measured for the ui updates.
     */
    @Override
    public void setInstantMeasurement(String instantMeasurement) {
        instantPing.postValue(instantMeasurement);
    }


    /**
     * Return the ping measured in string for the ui updates.
     * @return Measured ping in string
     */
    @Override
    public MutableLiveData<String> getFinalMeasurement() {
        return finalPing;
    }

    @Override
    public void setFinalMeasurement(String finalMeasurement) {
        finalPing.postValue(finalMeasurement);
    }

    /**
     * Return the progress of the ping and jitter test for the ui updates.
     * @return Progress
     */
    @Override
    public MutableLiveData<Integer> getProgress() {
        return progress;
    }

    @Override
    public void setProgress(int currentProgress) {
        progress.postValue(currentProgress);
    }

    @Override
    public void OnTestStart() {

    }

    @Override
    public void OnTestSuccess(String jitter) {

    }

    @Override
    public void OnTestBackground(String currentPing, int currentProgress) {

    }
}