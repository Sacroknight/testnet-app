package com.qos.myapplication.tests;


import android.os.Handler;
import android.util.Log;

import com.qos.myapplication.databinding.FragmentHomeBinding;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;



public class PingJitterStats {
    private static final int TIMEOUT_MS = 5000;
    public static final int MAX_PING_TIMES = 20;
    private static final int ERROR_MEASURING_PING = -1;

    Map<String, Integer> host;
    public InetAddress inetAddress;
    private int pingMeasure;
    private int jitterMeasure;
    AtomicInteger progress = new AtomicInteger(0);

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

    public PingJitterStats() {
        getMostVisitedWebsites();
    }
    private void getMostVisitedWebsites(){
        host = new HashMap<>();
        host.put("google.com",1);
        host.put("youtube.com",2);
        host.put("whatsapp.com",3);
        host.put("facebook.com",4);
        host.put("instagram.com",5);
        host.put("live.com",6);
        host.put("openai.com",7);
        host.put("office.com",8);
        host.put("microsoft.com",9);
        host.put("lan.leagueoflegends.com",10);
    }
    public String chooseHost() {
        List<String> hostList = new ArrayList<>(host.keySet());
        Random random = new Random();
        return hostList.get(random.nextInt(hostList.size()));
    }
    public void measuringPingJitter(String chosenHost, Handler handler, FragmentHomeBinding binding) {
        progress.set(0);
        List<Integer> pingList = new ArrayList<>();
        for (int i = 0; i < MAX_PING_TIMES; i++) {
            try {
                int ping = measuringPing(chosenHost);
//                if(ping == ERROR_MEASURING_PING){
//                    setJitterMeasure(ERROR_MEASURING_PING);
//                    setPingMeasure(ERROR_MEASURING_PING);
//                    break;
//                }else
                pingList.add(ping);
                // Consider asynchronous updates if UI updates can be delayed
                handler.post(() -> {
                    binding.textHome.setText(String.valueOf(ping));
                    binding.testProgressIndicator.setProgress(progress.addAndGet(100/MAX_PING_TIMES));
                });
            } catch (PingException e) {
                // Handle ping error gracefully, providing user-friendly messages
                Log.e("PingJitterTask", "Ping failed: " + e.getMessage());
                switch (e.getType()) {
                    case TIMEOUT:
                        binding.textHome.setText("Ping timed out");
                        break;
                    case INVALID_HOST:
                        binding.textHome.setText("Invalid host address");
                        break;
                    case UNKNOWN:
                        binding.textHome.setText("Ping error occurred");
                        break;
                }
            }
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


    private int measuringPing(String chosenHost) throws PingException {
        if (chosenHost == null || chosenHost.isEmpty()) {
            throw new PingException(PingExceptionType.INVALID_HOST, "Invalid host address");
        }

        int ping = 0;
        try {
            if(InetAddress.getByName(chosenHost).isReachable(TIMEOUT_MS)){
            try {
                Process process = new ProcessBuilder("ping", "-c", "1", "-w", String.valueOf(TIMEOUT_MS), chosenHost).start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("time=")) {
                        int startIndex = line.indexOf("time=") + 5;
                        int endIndex = line.indexOf(" ms", startIndex);
                        ping = (int) Float.parseFloat(line.substring(startIndex, endIndex));
                        break;
                    }
                }
                int exitCode = process.waitFor();
                reader.close();
                process.destroy();

                if (exitCode != 0 || ping == 0) {
                    throw new PingException(
                            exitCode == -1 ? PingExceptionType.TIMEOUT : PingExceptionType.UNKNOWN,
                            "Ping failed"
                    );
                } else {
                    return ping;
                }
            } catch (InterruptedException e) {
                throw new PingException(PingExceptionType.UNKNOWN, "Ping error: " + e.getMessage(), e);
            } catch (IOException e) {
                throw new PingException(PingExceptionType.UNKNOWN, "Error reading ping output", e);
            }
            }else {
                ping = ERROR_MEASURING_PING;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return ping;
    }


    private enum PingExceptionType {
        TIMEOUT,
        INVALID_HOST,
        UNKNOWN
    }

    private static class PingException extends Exception {
        private final PingExceptionType type;

        public PingException(PingExceptionType type, String message) {
            super(message);
            this.type = type;
        }

        public PingException(PingExceptionType type, String message, Throwable cause) {
            super(message, cause);
            this.type = type;
        }

        public PingExceptionType getType() {
            return type;
        }
    }

}
