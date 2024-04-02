package com.qos.testnet.tests;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;

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
    public static final double ERROR_MEASURING_DOWNLOADING_RATE = -404;
    /**
     * The server url.
     */
    public String urlServer = "";
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
    private MutableLiveData<String> instantaneousMeasurementsLiveData = null;
    private MutableLiveData<String> finalDownloadRateLiveData = null;
    private MutableLiveData<Integer> instantDownloadRateForUI = null;

    /**
     * Set if test is finished.
     */
    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    /**
     * Is finished boolean.
     * @return the test is finished.
     */
    public boolean isFinished () {
        return finished;
    }
    private double round (double value) {
        BigDecimal bd = BigDecimal.valueOf (value);
        bd = bd.setScale (2, RoundingMode.HALF_UP);
        return bd.doubleValue ();
    }
    private int roundInt (double value) {
        BigDecimal bd = BigDecimal.valueOf (value);
        bd = bd.setScale (2, RoundingMode.HALF_UP);
        return bd.intValue ();
    }

    /**
     * Gets instant download rate.
     * @return the instant download rate
     */
    public double getInstantDownloadRate () {
        return instantDownloadRate;
    }

    /**
     * Sets instant download rate.
     *
     * @param downloadedByte the downloaded byte
     * @param elapsedTime    the elapsed time
     */
    public void setInstantDownloadRate (int downloadedByte, double elapsedTime) {
        if (downloadedByte >= 0) {
            this.instantDownloadRate = round ((downloadedByte *8*1e-6) / elapsedTime);
        } else {
            this.instantDownloadRate = 0.0;
        }
        setInstantMeasurement(getInstantDownloadRate()+ " Mb/s");
        setProgress(roundInt(getInstantDownloadRate()));
    }

    /**
     * Gets final download rate.
     * @return the final download rate
     */
    public double getFinalDownloadRate () {
        return round (finalDownloadRate);
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
    public DownloadSpeedStats() {
        setFinished(false);
        instantaneousMeasurementsLiveData = new MutableLiveData<>();
        finalDownloadRateLiveData = new MutableLiveData<>();
        instantDownloadRateForUI = new MutableLiveData<>();
        client = new OkHttpClient();
    }
    public void runDownloadSpeedTest(TestCallback testCallback,String url) {
        try {
            OnTestStart();
            urlServer = url;
            downloadedBytes = 0;
            List<String> fileUrls = new ArrayList<>();
            fileUrls.add(urlServer + "random2000x2000.jpg");
            fileUrls.add(urlServer + "random3000x3000.jpg");
            fileUrls.add(urlServer + "random5000x5000.jpg");
            fileUrls.add(urlServer + "random6000x6000.jpg");
            startTime = System.currentTimeMillis();
            outer:
            for (String link : fileUrls) {
                try {
                    Request request = new Request.
                            Builder().url(link).build();
                    Response response = client.newCall(request).execute();
                    if (!response.isSuccessful()) {
                        String errorMessage = "Unexpected code " + response + ": " + response.message();
                        Log.e("DownloadSpeedStats", errorMessage);
                        testCallback.OnTestFailed(errorMessage);
                    }
                    byte[] buffer = new byte[31250];
                    assert response.body() != null;
                    InputStream inputStream = response.body().byteStream();
                    int len;
                    while ((len = inputStream.read(buffer)) != -1) {
                        downloadedBytes += len;
                        endTime = System.currentTimeMillis();
                        downloadElapsedTime = (double) (endTime - startTime) / 1000;
                        setInstantDownloadRate(downloadedBytes, downloadElapsedTime);
                        testCallback.OnTestBackground(getInstantDownloadRate() + " Mb/s", roundInt(getInstantDownloadRate()));
                        /*
                         * The Timeout.
                         */
                        int TIME_OUT = 30;
                        if (downloadElapsedTime >= TIME_OUT) {
                            break outer;
                        }
                    }
                    inputStream.close();
                    response.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            endTime = System.currentTimeMillis();
            downloadElapsedTime = (endTime - startTime) / 1e3;
            setFinalDownloadRate((downloadedBytes *8*1e-6) / downloadElapsedTime);
            setFinalMeasurement(finalDownloadRate + " Mb/s");

        }finally{
            finished = true;
            testCallback.OnTestSuccess(getFinalDownloadRate() + " Mb/s");
        }
    }




    @Override
    public MutableLiveData<String> getInstantMeasurement(){
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
    public void OnTestStart(){
        finished = false;
    }
    @Override
    public void OnTestSuccess(String finalDownloadRate){
    }
    public void OnTestBackground(String instantDownloadRate, int instantDownloadRateUi){
    }

}