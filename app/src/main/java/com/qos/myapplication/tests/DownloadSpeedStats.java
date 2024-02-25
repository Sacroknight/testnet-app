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
    private final byte[] buffer = new byte[1024 * 1024];
    private static OkHttpClient client;
    private static String downloadSpeed;
    public DownloadSpeedStats(){
        client = new OkHttpClient.Builder().build();
        setDownloadSpeed(measureDownloadSpeed());
    }
    public String getDownloadSpeed() {
        return downloadSpeed;
    }
    public void setDownloadSpeed(String downloadSpeed) {
        this.downloadSpeed = downloadSpeed;
    }
    // Función para medir la velocidad de subida
    public static float measureUploadSpeed() throws IOException {
        Request request = new Request.Builder()
                .url("https://speedtest.openweb.com.ar/upload.php")
                .build();

        long startTime = System.nanoTime();
        try (Response response = client.newCall(request).execute()) {
            long endTime = System.nanoTime();
            long duration = endTime - startTime;
            float speed = (5242880 / (float) duration) * 1000000; // bytes per second (5MB)
            return speed / 1024 / 1024; // convert to Mbps
        }
    }

    // Función para medir la velocidad de bajada
    public static String measureDownloadSpeed(){
        long startTime = System.nanoTime();
        Request request = new Request.Builder().url(DOWNLOAD_URL).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                downloadSpeed = (String.valueOf(ERROR_MEASURING_DOWNLOAD_SPEED));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try (ResponseBody body = response.body()) {
                    if(body!=null) {
                        long endTime = System.nanoTime();
                        long duration = endTime - startTime;
                        long fileSize = body.contentLength();
                        double speed = calculateDownloadSpeed(fileSize,duration);
                        downloadSpeed = String.format("%.2f", (speed));
                                            }else{
                        downloadSpeed = String.valueOf(ERROR_MEASURING_DOWNLOAD_SPEED);
                    }
                }
            }
        });
        return downloadSpeed;
    }
         private static double calculateDownloadSpeed(Long downloadedBytes, Long downloadTime){
        double downloadSizeInMegabits = (downloadedBytes * 8) / 1e6; // Convertir a megabits
        double downloadTimeInSeconds = downloadTime / 1e9; // Convertir a segundos
        double downloadSpeedMbps = downloadSizeInMegabits / downloadTimeInSeconds; // Calcular la velocidad de descarga en Mbps
        return downloadSpeedMbps;
    }
}

