package com.qos.testnet.tests;

import android.os.Handler;

import com.qos.myapplication.databinding.FragmentHomeBinding;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * The type Http download test.
 */
public class DownloadSpeedStats extends Thread {
    /**
     * The Timeout.
     */
    private final int TIME_OUT = 15;
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
    private final FragmentHomeBinding binding;
    private final Handler handler;
    AtomicInteger progress = new AtomicInteger(0);


    /**
     * Instantiates a new Http download test.
     *
     */
    public DownloadSpeedStats(FragmentHomeBinding fragmentHomeBinding, Handler handler) {
        this.binding = fragmentHomeBinding;
        this.handler = handler;
        client = new OkHttpClient();
    }
    private double round (double value, int places) {
        if (places < 0) throw new IllegalArgumentException ();
        BigDecimal bd = BigDecimal.valueOf (value);
        bd = bd.setScale (places, RoundingMode.HALF_UP);
        return bd.doubleValue ();
    }

    /**
     * Gets instant download rate.
     *
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
            this.instantDownloadRate = round ((downloadedByte *8*1e-6) / elapsedTime, 2);
        } else {
            this.instantDownloadRate = 0.0;
        }
    }
    /**
     * Gets final download rate.
     *
     * @return the final download rate
     */
    public double getFinalDownloadRate () {
        return round (finalDownloadRate, 2);
    }
    /**
     * Sets final download rate
     */
    public void setFinalDownloadRate (double finalDownloadRate) {
        this.finalDownloadRate= finalDownloadRate;
    }

    /**
     * Is finished boolean.
     *
     * @return the boolean
     */
    public boolean isFinished () {
        return finished;
    }
      @Override
    public void run () {
        downloadedBytes = 0;
        List<String> fileUrls = new ArrayList<>();
        //fileUrls.add(urlServer + "random2000x2000.jpg");
        //fileUrls.add(urlServer + "random3000x3000.jpg");
        fileUrls.add(urlServer + "random5000x5000.jpg");
        fileUrls.add(urlServer + "random6000x6000.jpg");
        startTime = System.currentTimeMillis();
        outer:
        for (String link : fileUrls) {
            try {
                Request request = new Request.
                        Builder().url(link).build();
                Response response = client.newCall(request).execute();
                if (!response.isSuccessful()){
                    throw new IOException("Unexpected code " + response);
                }
                byte[] buffer = new byte[31250];
                assert response.body() != null;
                InputStream inputStream = response.body().byteStream();
                int len;
                while((len = inputStream.read(buffer)) != -1){
                    downloadedBytes += len;
                    endTime = System.currentTimeMillis();
                    downloadElapsedTime = (double) (endTime - startTime) /1000;
                    setInstantDownloadRate(downloadedBytes, downloadElapsedTime);
                    handler.post(this::run2);
                    if (downloadElapsedTime >= TIME_OUT){
                        break outer;
                    }
                }
                inputStream.close();
                response.close();
            } catch (Exception ex) {
                ex.printStackTrace ();
            }
        }
        endTime = System.currentTimeMillis ();
        downloadElapsedTime = (endTime - startTime)/1e3;
        finalDownloadRate = (downloadedBytes*8*1e6) / downloadElapsedTime;
        finished = true;
    }
    public void getDownloadSpeed(String urlServer){
        this.urlServer = urlServer;
        start();
    }

    private void run2() {
        binding.textHome.setText(String.valueOf(instantDownloadRate));
        binding.testProgressIndicator.setMax((int) (instantDownloadRate + 10));
        binding.testProgressIndicator.setProgress((int) instantDownloadRate);
    }
}
