package com.qos.testnet.tests;

import android.annotation.SuppressLint;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.qos.testnet.R;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * The type Http upload test.
 */
public class UploadSpeedStats implements InternetTest, TestCallback {
    private static final int BUFFER_SIZE = 1024 * 1024; // 1 MB
    private static final int THREAD_COUNT = 4;
    private static final int ITERATIONS = 25;
    /**
     * The Start time.
     */
    long startTime = 0;
    /**
     * The End time.
     */
    long endTime = 0;
    /**
     * The Upload elapsed time.
     */
    double uploadElapsedTime = 0;
    /**
     * The Uploaded bytes.
     */
    int uploadedBytes = 0;
    /**
     * The Final upload rate.
     */
    double finalUploadRate = 0.0;
    /**
     * The Finished.
     */
    boolean finished = false;
    /**
     * The Instant upload rate.
     */
    double instantUploadRate = 0.0;

    /**
     * The Http client.
     */
    private final OkHttpClient client;
    private MutableLiveData<String> instantaneousMeasurementsLiveData = new MutableLiveData<>();
    private MutableLiveData<String> finalUploadRateLiveData = new MutableLiveData<>();
    private MutableLiveData<Integer> instantUploadRateForUI = new MutableLiveData<>();

    /**
     * Set if test is finished.
     */
    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    /**
     * Is finished boolean.
     *
     * @return the test is finished.
     */
    public boolean isFinished() {
        return finished;
    }

    private double round(double value) {
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    private int roundInt(double value) {
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return bd.intValue();
    }

    /**
     * Gets instant upload rate.
     *
     * @return the instant upload rate
     */
    public double getInstantUploadRate() {
        return instantUploadRate;
    }

    /**
     * Sets instant upload rate.
     *
     * @param uploadedByte the uploaded byte
     * @param elapsedTime  the elapsed time
     */
    public void setInstantUploadRate(int uploadedByte, double elapsedTime) {
        if (uploadedByte >= 0) {
            this.instantUploadRate = round((uploadedByte * 8 * 1e-6) / elapsedTime);
        } else {
            this.instantUploadRate = 0.0;
        }
        setInstantMeasurement(String.valueOf(getInstantUploadRate() + (R.string.mega_bits_per_second)));
        setProgress(roundInt(getInstantUploadRate()));
    }

    /**
     * Gets final upload rate.
     *
     * @return the final upload rate
     */
    public double getFinalUploadRate() {
        return round(finalUploadRate);
    }

    /**
     * Sets final upload rate
     */
    private void setFinalUploadRate(double uploadRate) {
        this.finalUploadRate = uploadRate;
    }

    /**
     * Instantiates a new Http upload test.
     */
    public UploadSpeedStats() {
        setFinished(false);
        instantaneousMeasurementsLiveData = new MutableLiveData<>();
        finalUploadRateLiveData = new MutableLiveData<>();
        instantUploadRateForUI = new MutableLiveData<>();
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    @SuppressLint("DefaultLocale")
    public void runUploadSpeedTest(TestCallback testCallback, String url) {
        try {
            uploadedBytes = 0;
            OnTestStart();
            byte[] buffer = new byte[BUFFER_SIZE];
            ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
            startTime = System.currentTimeMillis();
            for (int i = 0; i < ITERATIONS; i++) {
                executor.execute(() -> {
                    try {
                        RequestBody requestBody = RequestBody.create(buffer, MediaType.get("application/octet-stream"));
                        Request request = new Request.Builder().url(url).post(requestBody).build();
                        try (Response response = client.newCall(request).execute()) {
                            isResponseSuccessful(response, testCallback);
                            uploadedBytes += buffer.length;
                            endTime = System.currentTimeMillis();
                            uploadElapsedTime = (double) (endTime - startTime) / 1000;
                            setInstantUploadRate(uploadedBytes, uploadElapsedTime);
                            testCallback.OnTestBackground(String.format("%.2f Mb/s", getInstantUploadRate()), roundInt(getInstantUploadRate()));
                        }
                    } catch (IOException ex) {
                        String errorMessage = "Error during upload speed test: " + ex.getMessage();
                        Log.e(this.getClass().getTypeName(), errorMessage, ex);
                    }
                });
            }
            executor.shutdown();
            if (!executor.awaitTermination(45, TimeUnit.SECONDS)) {
                String timeoutMessage = "Test exceeded the maximum duration of 45 seconds.";
                Log.e(this.getClass().getTypeName(), timeoutMessage);
                executor.shutdownNow(); // Attempt to stop all actively executing tasks
            }
            endTime = System.currentTimeMillis();
            uploadElapsedTime = (endTime - startTime) / 1e3;
            setFinalUploadRate((uploadedBytes * 8 * 1e-6) / uploadElapsedTime);
            setFinalMeasurement(finalUploadRate + " Mb/s");

        } catch (InterruptedException e) {
            String errorMessage = "Error during upload speed test: " + e.getMessage();
            Log.e(this.getClass().getTypeName(), errorMessage, e);
            Thread.currentThread().interrupt();
        } finally {
            finished = true;
            testCallback.OnTestSuccess(getFinalUploadRate() + " Mb/s");
        }
    }

    private void isResponseSuccessful(Response response, TestCallback testCallback) {
        if (!response.isSuccessful()) {
            String errorMessage = "Unexpected code " + response + ": " + response.message();
            Log.e(this.getClass().getTypeName(), errorMessage);
            testCallback.OnTestFailed(errorMessage);
        }
    }

    @Override
    public MutableLiveData<String> getInstantMeasurement() {
        return instantaneousMeasurementsLiveData;
    }

    @Override
    public void setInstantMeasurement(String currentMeasurement) {
        instantaneousMeasurementsLiveData.postValue(currentMeasurement);
    }

    @Override
    public MutableLiveData<String> getFinalMeasurement() {
        return finalUploadRateLiveData;
    }

    @Override
    public void setFinalMeasurement(String finalMeasurement) {
        finalUploadRateLiveData.postValue(finalMeasurement);
    }

    @Override
    public MutableLiveData<Integer> getProgress() {
        return instantUploadRateForUI;
    }

    @Override
    public void setProgress(int instantUploadRate) {
        instantUploadRateForUI.postValue(instantUploadRate);
    }

    @Override
    public void OnTestStart() {
        finished = false;
    }

    @Override
    public void OnTestSuccess(String finalUploadRate) {
        // TODO document why this method is empty
    }

    @Override
    public void OnTestBackground(String instantUploadRate, int instantUploadRateUi) {
        // TODO document why this method is empty
    }
}