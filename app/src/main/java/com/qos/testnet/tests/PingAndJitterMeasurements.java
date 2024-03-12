package com.qos.testnet.tests;


import android.os.Handler;

import com.qos.myapplication.databinding.FragmentHomeBinding;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;


public class PingAndJitterMeasurements {
    public static final int MAX_PING_TIMES = 40;
    private static final int TIMEOUT_MS = 5000;
    private static final int ERROR_MEASURING_PING = -1;
    private final FragmentHomeBinding binding;
    private final Handler handler;

    Map<String, Integer> host;
    AtomicInteger progress = new AtomicInteger(0);
    private int pingMeasure;
    private int jitterMeasure;

    public PingAndJitterMeasurements(FragmentHomeBinding fragmentHomeBinding, Handler handler) {
        this.binding = fragmentHomeBinding;
        this.handler = handler;
        getMostVisitedWebsites();
    }

    public int getPingMeasure() {
        return pingMeasure;
    }

    private void setPingMeasure(int pingMeasure) {
        this.pingMeasure = pingMeasure;
    }

    public int getJitterMeasure() {
        return jitterMeasure;
    }

    private void setJitterMeasure(int jitterMeasure) {
        this.jitterMeasure = jitterMeasure;
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

    public void measuringPingJitter(String chosenHost) {
        progress.set(0);
        List<Integer> pingList = new ArrayList<>();
        for (int i = 0; i < MAX_PING_TIMES; i++) {
            int ping = measuringPing(chosenHost);
            pingList.add(ping);
            handler.post(() -> {
                binding.textHome.setText(String.valueOf(ping));
                binding.testProgressIndicator.setProgress(progress.addAndGet(100 / MAX_PING_TIMES));
            });
        }
        calculateAndSetStatistics(pingList);
    }

    private void calculateAndSetStatistics(List<Integer> pingList) {
        int averagePing = (int) pingList.stream().mapToInt(Integer::intValue).average().orElse(0);
        double variance = pingList.stream().mapToDouble(ping -> Math.pow(ping - averagePing, 2)).average().orElse(0);
        int jitter = (int) Math.sqrt(variance);
        setJitterMeasure(jitter);
        setPingMeasure(averagePing);
    }


    private int measuringPing(String chosenHost) {
        int probes = 0;
        int ping = 0;
        if (chosenHost == null || chosenHost.isEmpty()) {
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
}