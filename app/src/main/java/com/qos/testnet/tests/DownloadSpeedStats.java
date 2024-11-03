package com.qos.testnet.tests;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.qos.testnet.R;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * The type Http download test.
 */
public class DownloadSpeedStats implements InternetTest, TestCallback {
    /**
     * The Start time.
     */
    long startTime = 0;
    /**
     * The End time.
     */
    long endTime = 0;
    /**
     * The Download elapsed time.
     */
    double downloadElapsedTime = 0;
    /**
     * The Downloaded bytes.
     */
    int downloadedBytes = 0;
    /**
     * The Final download rate.
     */
    double finalDownloadRate = 0.0;
    /**
     * The Finished.
     */
    boolean finished = false;
    /**
     * The Instant download rate.
     */
    double instantDownloadRate = 0;

    /**
     * The Http conn.
     */
    private final OkHttpClient client;
    private final MutableLiveData<String> instantaneousMeasurementsLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> finalDownloadRateLiveData = new MutableLiveData<>();
    private final MutableLiveData<Integer> instantDownloadRateForUI = new MutableLiveData<>();
    private final Context context;

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
     * Gets instant download rate.
     *
     * @return the instant download rate
     */
    public double getInstantDownloadRate() {
        return instantDownloadRate;
    }

    /**
     * Sets instant download rate.
     *
     * @param downloadedByte the downloaded byte
     * @param elapsedTime    the elapsed time
     */
    public void setInstantDownloadRate(int downloadedByte, double elapsedTime) {
        if (downloadedByte >= 0) {
            this.instantDownloadRate = round((downloadedByte * 8 * 1e-6) / elapsedTime);
        } else {
            this.instantDownloadRate = 0.0;
        }
        setInstantMeasurement(String.valueOf(getInstantDownloadRate() + (R.string.mega_bits_per_second)));
        setProgress(roundInt(getInstantDownloadRate()));
    }

    /**
     * Gets final download rate.
     *
     * @return the final download rate
     */
    public double getFinalDownloadRate() {
        return round(finalDownloadRate);
    }

    /**
     * Sets final download rate
     */
    private void setFinalDownloadRate(double downloadRate) {
        this.finalDownloadRate = downloadRate;
    }

    /**
     * Instantiates a new Http download test.
     */
    public DownloadSpeedStats(Context context) {
        this.context = context;
        setFinished(false);
        client = new OkHttpClient();
    }

    public void runDownloadSpeedTest(TestCallback testCallback, String url) {
        try {
            OnTestStart();
            downloadedBytes = 0;
            List<String> fileUrls = new ArrayList<>();
            fileUrls.add(url + "random2000x2000.jpg");
            fileUrls.add(url + "random3000x3000.jpg");
            fileUrls.add(url + "random5000x5000.jpg");
            fileUrls.add(url + "random6000x6000.jpg");
            startTime = System.currentTimeMillis();
            for (String link : fileUrls) {
                Response response = null;
                InputStream inputStream = null;
                try {
                    Request request = new Request.
                            Builder().url(link).build();
                    response = client.newCall(request).execute();
                    isResponseSuccessful(response, testCallback);
                    byte[] buffer = new byte[31250];
                    assert response.body() != null;
                    inputStream = response.body().byteStream();
                    int len;
                    while ((len = inputStream.read(buffer)) != -1) {
                        downloadedBytes += len;
                        endTime = System.currentTimeMillis();
                        downloadElapsedTime = (double) (endTime - startTime) / 1000;
                        setInstantDownloadRate(downloadedBytes, downloadElapsedTime);
                        testCallback.OnTestBackground(String.format("%.2f" + context.getString(R.string.mega_bits_per_second), getInstantDownloadRate()), roundInt(getInstantDownloadRate()));

                        /*
                         * The Timeout.
                         */
                        if (downloadElapsedTime >= 45) {
                            Log.i(this.getClass().getTypeName(), "Test exceeded the maximum duration of 30 seconds.");
                            break;
                        }
                    }
                } catch (Exception ex) {
                    String errorMessage = "Error during download speed test: " + ex.getMessage();
                    Log.e(this.getClass().getTypeName(), errorMessage, ex);
                    testCallback.OnTestFailed(errorMessage);
                } finally {
                    if (inputStream != null) {
                        try {
                            inputStream.close(); // Aseguramos que el flujo de entrada se cierre.
                        } catch (IOException e) {
                            Log.e(this.getClass().getTypeName(), "Error Closing inputStream", e);
                        }
                    }
                    if (response != null) {
                        response.close(); // Aseguramos que el cuerpo de la respuesta se cierre.
                    }
                }
            }
            endTime = System.currentTimeMillis();
            downloadElapsedTime = (endTime - startTime) / 1e3;
            setFinalDownloadRate((downloadedBytes * 8 * 1e-6) / downloadElapsedTime);
            setFinalMeasurement(finalDownloadRate + " Mb/s");

        } finally {
            finished = true;
            testCallback.OnTestSuccess(getFinalDownloadRate() + " Mb/s");
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
        return finalDownloadRateLiveData;
    }

    @Override
    public void setFinalMeasurement(String finalMeasurement) {
        finalDownloadRateLiveData.postValue(finalMeasurement);
    }

    @Override
    public MutableLiveData<Integer> getProgress() {
        return instantDownloadRateForUI;
    }

    @Override
    public void setProgress(int instantDownloadRate) {
        instantDownloadRateForUI.postValue(instantDownloadRate);
    }

    @Override
    public void OnTestStart() {
        finished = false;
    }

    @Override
    public void OnTestSuccess(String finalDownloadRate) {
        // TODO document why this method is empty
    }

    public void OnTestBackground(String instantDownloadRate, int instantDownloadRateUi) {
        // TODO document why this method is empty
    }

}