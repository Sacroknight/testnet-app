package com.qos.testnet.tests;

import android.os.Handler;

import com.qos.myapplication.databinding.FragmentHomeBinding;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * The type Http upload test using OkHttp.
 */
public class UploadSpeedStats extends Thread {

    /**
     * The Uploaded k byte.
     */
    static int uploadedBytes = 0;
    /**
     * The File url.
     */
    public String fileURL;
    /**
     * The Upload elapsed time.
     */
    double uploadElapsedTime = 0;
    /**
     * The Finished.
     */
    boolean finished = false;
    /**
     * The Elapsed time.
     */
    double elapsedTime = 0;
    private final Handler handler;
    private final FragmentHomeBinding binding;
    private double instantUploadRate;

    public void setFinalUploadRate(double finalUploadRate) {
        this.finalUploadRate = finalUploadRate;
    }

    /**
     * The Final upload rate.
     */
    double finalUploadRate = 0.0;
    /**
     * The Start time.
     */
    long startTime;

    /**
     * Instantiates a new Http upload test.
     *
     * @param fragmentHomeBinding the fragment home binding
     * @param handler the fragment handler
     */
    public UploadSpeedStats(FragmentHomeBinding fragmentHomeBinding, Handler handler) {
        this.binding = fragmentHomeBinding;
        this.handler = handler;
    }

    private double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    /**
     * Is finished boolean.
     *
     * @return the boolean
     */
    public boolean isFinished() {
        return finished;
    }

    /**
     * Gets instant upload rate.
     *
     * @return the instant upload rate
     */
    public double getInstantUploadRate() {
        if (uploadedBytes >= 0) {
            long now = System.currentTimeMillis();
            elapsedTime = (now - startTime) / 1000.0;
            return round(((uploadedBytes / 1000.0) * 8) / elapsedTime, 2);
        } else {
            return 0.0;
        }
    }

    /**
     * Gets final upload rate.
     *
     * @return the final upload rate
     */
    public double getFinalUploadRate() {
        return round(finalUploadRate, 2);
    }

    @Override
    public void run() {
        try {
            OkHttpClient client = new OkHttpClient();
            uploadedBytes = 0;
            startTime = System.currentTimeMillis();
            ExecutorService executor = Executors.newFixedThreadPool(4);
            byte[] buffer = new byte[150 * 1024];
            int timeout = 10;

            for (int i = 0; i < 4; i++) {
                executor.execute(() -> {
                    while (true) {
                        try {
                            Request request = new Request.Builder()
                                    .url(fileURL)
                                    .post(RequestBody.create(MediaType.get("application/octet-stream"), buffer))
                                    .build();
                            try (Response response = client.newCall(request).execute()) {
                                uploadedBytes += buffer.length;
                                long endTime = System.currentTimeMillis();
                                double uploadElapsedTime = (endTime - startTime) / 1000.0;
                                setInstantUploadRate(uploadedBytes, uploadElapsedTime);
                                handler.post(this::run2);
                                if (uploadElapsedTime >= timeout) {
                                    break;
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }

            executor.shutdown();
            while (!executor.isTerminated()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                    Thread.currentThread().interrupt();
                }
            }
            long now = System.currentTimeMillis();
            uploadElapsedTime = (now - startTime) / 1000.0;
            finalUploadRate = ((uploadedBytes*8*1e-6) / uploadElapsedTime);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        finished = true;
    }

    private void setInstantUploadRate(int uploadedByte, double uploadElapsedTime) {
        if (uploadedByte >= 0) {
            this.instantUploadRate = round ((uploadedByte*8*1e-6) / elapsedTime, 2);
        } else {
            this.instantUploadRate = 0.0;
        }
    }

    public void getUploadSpeed(String fileURL){
        this.fileURL = fileURL;
        start();
    }
    private void run2() {
        binding.textHome.setText(String.valueOf(uploadedBytes));
        binding.testProgressIndicator.setMax((int) (instantUploadRate + 10));
        binding.testProgressIndicator.setProgress((int) instantUploadRate);
    }
}
