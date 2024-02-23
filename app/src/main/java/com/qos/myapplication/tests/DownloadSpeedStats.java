package com.qos.myapplication.tests;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.EventListener;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class DownloadSpeedStats implements EventListener {
    private static final int ERROR_MEASURING_DOWNLOAD_SPEED = -1;
    private static final String DOWNLOAD_URL = "https://upload.wikimedia.org/wikipedia/commons/e/e7/Captive_Macaw_Colombia.jpg";

    private OkHttpClient client;
    private long downloadStartTime;
    private long downloadedBytes;
    private String downloadSpeed;

    public String getDownloadSpeed() {
        return downloadSpeed;
    }
    public DownloadSpeedStats(){
        client = new OkHttpClient.Builder().build();
    }
    public void setDownloadSpeed(String downloadSpeed) {
        this.downloadSpeed = downloadSpeed;
    }
    private String measureDownloadSpeed() {
        Request request = new Request.Builder().url(DOWNLOAD_URL).build();
        downloadStartTime = System.currentTimeMillis();
        downloadedBytes = 0;

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                downloadSpeed = String.valueOf(ERROR_MEASURING_DOWNLOAD_SPEED);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                ResponseBody body =response.body();
                if(body != null ){
                    long downloadTime = System.currentTimeMillis() - downloadStartTime;
                    double downloadSpeedMbps = (downloadedBytes * 8.0) / (downloadTime * 1e6);
                    downloadSpeed = String.valueOf(downloadSpeedMbps);
                }else {
                    downloadSpeed = String.valueOf(ERROR_MEASURING_DOWNLOAD_SPEED);
                }
            }
        });
        return downloadSpeed;
    }
    public void onResponseStart(Call call){
        long downloadStartTime = System.currentTimeMillis();
        double downloadedBytes = 0;
    }
    public void onResponseProgress(Response response, long bytesRead, long contentLength, boolean done) {
        // Track downloaded bytes and update speed as progress happens
        downloadedBytes += bytesRead;
        if (done) {
            long downloadTime = System.currentTimeMillis() - downloadStartTime;
            double downloadSpeedMbps = (downloadedBytes * 8.0) / (downloadTime * 1e6);
            // Log or display the calculated download speed
            setDownloadSpeed(String.valueOf(downloadSpeedMbps));
        }
    }

}

