package com.qos.myapplication.tests;

import androidx.annotation.NonNull;

import com.qos.myapplication.databinding.FragmentHomeBinding;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class DownloadSpeedStats implements EventListener {
    private final int ERROR_MEASURING_DOWNLOAD_SPEED = -1;
    private final String DOWNLOAD_URL = "https://upload.wikimedia.org/wikipedia/commons/e/e7/Captive_Macaw_Colombia.jpg";
    private final String DOWNLOAD_URL1 = "https://download2301.mediafire.com/ey6w9zi4l1jgBi0lDaFnpnj-KgInIgt9OMU63Tx8GK1Wqfe7KcQ1XNiZ7E5lGIfrPyiGETQxqcK2msiR9x1Wqz7HSz5bwIlhxWiecn4MBUmbhUeGVdyh4re4AqOOCZq3pkL4PoysGHAoraeI9S6sMo3OXWhux2kn_z-wZwX2vVRN/dim8i1xwjv1gvfp/hero-c35bd03ceaa5f919e98b20c905044a3d.webm";
    private static OkHttpClient client = null;
    private int downloadedBytes = 0;
    private double downloadElapsedTime = 0;
    private final double TIME_OUT = 15*1e3;
    boolean timedOut = false;

    /**
     * The Start time.
     */
    long startTime = 0;
    /**
     * The End time.
     */
    long endTime = 0;

    private double instantDownloadSpeed;
    private double downloadSpeed;
    private String downloadSpeedText;
    private FragmentHomeBinding binding;

    public String getDownloadSpeedText() {
        return downloadSpeedText;
    }

    public void setDownloadSpeedText(String downloadSpeedText) {
        this.downloadSpeedText = downloadSpeedText;
    }

    public boolean finished = false;
    public DownloadSpeedStats(FragmentHomeBinding fragmentHomeBinding){
        client = new OkHttpClient();
        this.binding = fragmentHomeBinding;
    }
    public double getDownloadSpeed() {
        return round(downloadSpeed,2);
    }
    public void setDownloadSpeed(double downloadSpeed) {
        this.downloadSpeed = downloadSpeed;
    }

    public double getInstantDownloadSpeed(){
        return instantDownloadSpeed;
    }
    public void setInstantDownloadRate (int downloadedByte, double elapsedTime) {
        if (downloadedByte >= 0) {
            this.instantDownloadSpeed = round (((downloadedByte * 8) /1e6) / (elapsedTime), 2);
        } else {
            this.instantDownloadSpeed = ERROR_MEASURING_DOWNLOAD_SPEED;
        }
    }
    public boolean isFinished(){
        return finished;
    }

    private double round (double value, int places) {
        if (places < 0) throw new IllegalArgumentException ();
        BigDecimal bd = BigDecimal.valueOf (value);
        bd = bd.setScale (places, RoundingMode.HALF_UP);
        return bd.doubleValue ();
    }
    public void measureDownloadSpeed(){
        List<String> fileUrls = new ArrayList<>();
        fileUrls.add(DOWNLOAD_URL);
        fileUrls.add(DOWNLOAD_URL1);
        downloadedBytes = 0;
        startTime = System.currentTimeMillis();
        for (String link:fileUrls){
            Request request = new Request.Builder().url(link).build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    setDownloadSpeedText(e.getMessage());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) {
                    if(response.isSuccessful()){
                        int totalRead = 0;
                        try(ResponseBody body = response.body()){
                            if(body != null){
                                byte[] buffer = new byte[2048];
                                InputStream inputStream = body.byteStream();
                                while ((totalRead += inputStream.read(buffer)) != -1){
                                downloadedBytes += totalRead;
                                endTime = System.currentTimeMillis();
                                downloadElapsedTime = (endTime-startTime)/1e3; // Convert to seconds
                                    setInstantDownloadRate(totalRead, downloadElapsedTime);
                                    binding.textHome.setText(String.valueOf(getInstantDownloadSpeed()));

                                    if (downloadElapsedTime >= TIME_OUT) {
                                        timedOut = true;
                                        break;
                                    }
                                }
                                inputStream.close();
                            }else{
                                setDownloadSpeed(ERROR_MEASURING_DOWNLOAD_SPEED);
                            }
                        }catch (Exception ex) {
                            ex.printStackTrace ();
                        }
                    }else{
                        setDownloadSpeed(ERROR_MEASURING_DOWNLOAD_SPEED);
                    }
                }
            });
        }
        endTime = System.currentTimeMillis();
        downloadElapsedTime = (endTime - startTime)/1e3;
        downloadSpeed = calculateDownloadSpeed(downloadedBytes,downloadElapsedTime);
        binding.textHome.setText(String.valueOf(getDownloadSpeed()));
        finished = true;
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
        private static double calculateDownloadSpeed(int downloadedBytes, double downloadTime){
        double downloadSizeInMegabits = (downloadedBytes * 8) / 1e6; // Convertir a megabits
        double downloadSpeedMbps = downloadSizeInMegabits / downloadTime; // Calcular la velocidad de descarga en Mbps
        return downloadSpeedMbps;
    }
}

