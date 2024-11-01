package com.qos.testnet.tests;


import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;


public class PingAndJitterStats implements InternetTest, TestCallback {
    public static final int MAX_PING_TIMES = 40;
    private static final int TIMEOUT_MS = 5000;
    private static final int ERROR_MEASURING_PING = -1;
    private int failedPings = 0;

    private MutableLiveData<String> instantPing = new MutableLiveData<>();
    private MutableLiveData<String> finalPing = new MutableLiveData<>();
    private MutableLiveData<String> jitterLiveData = new MutableLiveData<>();
    private MutableLiveData<Integer> progress = new MutableLiveData<>();
    Map<String, Integer> host;
    private int pingMeasure;
    private int jitterMeasure;
    private boolean finished;

    /**
     * Return if the ping and jitter test is finished.
     *
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
     *
     * @return Measured ping
     */
    public int getPingMeasured() {
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
     *
     * @return jitter measured
     */
    public int getJitterMeasured() {
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
     *
     * @return jitter measured
     */
    public MutableLiveData<String> getJitterLivedata() {
        return jitterLiveData;
    }

    /**
     * Set the jitter measured for the ui updates.
     */
    public void setJitterLivedata(String jitterMeasured) {
        jitterLiveData.postValue(jitterMeasured);
    }

    public PingAndJitterStats() {
        getMostVisitedWebsites();
        progress = new MutableLiveData<>();
        instantPing = new MutableLiveData<>();
        finalPing = new MutableLiveData<>();
        jitterLiveData = new MutableLiveData<>();
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

    public void measuringPingJitter(final String chosenHost, final TestCallback testCallback) {
        new Thread(() -> {
            List<Integer> pingList = new ArrayList<>();
            for (int i = 0; i < MAX_PING_TIMES; i++) {
                int ping = measuringPing(chosenHost, testCallback);
                pingList.add(ping);

                // Contar pings fallidos o que tardan más de 5 segundos
                if (ping == ERROR_MEASURING_PING || ping > TIMEOUT_MS) {
                    failedPings++;
                }

                // Actualiza la interfaz de usuario en el hilo principal
                int pingProgress = i * (100 / MAX_PING_TIMES);
                String pingResult = ping + " ms";
                runOnUiThread(() -> {
                    setInstantMeasurement(pingResult);
                    setProgress(pingProgress);
                    testCallback.OnTestBackground(pingResult, pingProgress);
                });
            }
//            // Verificar si más del 20% de los pings fallaron
//            if (failedPings > MAX_PING_TIMES * 0.2) {
//                runOnUiThread(() -> testCallback.OnTestFailed("Más del 20% de los pings fallaron"));
//            } else {
//                // Calcular y establecer estadísticas después de completar las mediciones
            calculateAndSetStatistics(pingList, testCallback);
            Log.d(this.getClass().getName(), "Finished measuring ping and jitter, the number of packets lost is: " + failedPings);
//           }
        }).start();
    }

    /**
     * Método auxiliar para ejecutar acciones en el hilo principal
     */
    private void runOnUiThread(Runnable action) {
        new Handler(Looper.getMainLooper()).post(action);
    }


    private void calculateAndSetStatistics(List<Integer> pingList, TestCallback testCallback) {
        try {
            List<Integer> depuredPingList = pingList.stream().filter(ping -> ping != 0).collect(Collectors.toList());
            double averagePing = depuredPingList.stream().mapToInt(Integer::intValue).average().orElse(0);
            double variance = pingList.stream().mapToDouble(ping -> Math.pow(ping - averagePing, 2)).average().orElse(0);
            int jitter = (int) Math.sqrt(variance);
            setFinalPing((int) averagePing);
            setJitterMeasure(jitter);
            setFinished(true);
        } finally {
            setJitterLivedata(getJitterMeasured() + " ms");
            setFinalMeasurement(getPingMeasured() + " ms");
            testCallback.OnTestSuccess(getJitterMeasured() + " ms");
        }
    }

    private int measuringPing(String chosenHost, TestCallback testCallback) {
        int ping = 0;
        if (chosenHost == null || chosenHost.isEmpty()) {
            OnTestFailed(chosenHost);
            return ping;
        }

        Process process = null;
        try {
            process = new ProcessBuilder("ping", "-c", "1", "-W", String.valueOf(TIMEOUT_MS / 1000), chosenHost).start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("time=")) {
                        int startIndex = line.indexOf("time=") + 5;
                        int endIndex = line.indexOf(" ms", startIndex);
                        ping = (int) Float.parseFloat(line.substring(startIndex, endIndex));
                        break;
                    }
                }
            }

            // Espera a que el proceso termine
            int exitValue = process.waitFor();
            if (exitValue != 0) {
                ping = ERROR_MEASURING_PING;
            }
        } catch (IOException e) {
            Log.e(this.getClass().getName(), "Error al ejecutar el comando ping", e);
            testCallback.OnTestFailed(e.getMessage());
        } catch (InterruptedException e) {
            // Volver a interrumpir el hilo actual
            Thread.currentThread().interrupt();
            Log.e(this.getClass().getName(), "El hilo fue interrumpido", e);
            testCallback.OnTestFailed("El hilo fue interrumpido: " + e.getMessage());
        } catch (RuntimeException e) {
            // Captura cualquier otro error que pueda ocurrir
            Log.e(this.getClass().getName(), "Error inesperado al ejecutar el comando ping", e);
            testCallback.OnTestFailed("Error inesperado: " + e.getMessage());
        } finally {
            if (process != null) {
                process.destroy();
            }
        }

        return ping;
    }

    /**
     * Get the instant ping measured.
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
     *
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
     *
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
        // TODO document why this method is empty
    }

    @Override
    public void OnTestSuccess(String jitter) {
        // TODO document why this method is empty
    }

    @Override
    public void OnTestBackground(String currentPing, int currentProgress) {
        // TODO document why this method is empty
    }
}