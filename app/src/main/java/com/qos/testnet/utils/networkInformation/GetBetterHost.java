package com.qos.testnet.utils.networkInformation;



import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.auth.AuthenticationException;
import com.qos.testnet.utils.deviceInformation.LocationCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class GetBetterHost implements NetworkCallback {
    OkHttpClient client;
    String ISP;
    private JSONArray jsonArray;
    private JSONObject locationInfo;
    String apiUrl = "https://api.ip2location.io/?key=30ABFB42A85F6E2C877172679CC6DD48&format=json";
    String url = "https://www.speedtest.net/api/js/servers?engine=js&limit=10&https_functional=true";


    /**
     * The Self lat.
     */
    private double selfLat = 0.0;
    /**
     * The Self lon.
     */
    private double selfLon = 0.0;
    /**
     * The server lat.
     */
    private double serverLat = 0.0;
    /**
     * The server lon.
     */
    private double serverLon = 0.0;
    /**
     * url of the best server
     */
    public String urlAddress;
    public String urlUploadAddress;
    /**
     * The Finished.
     */
    public boolean finished = false;
    double SEMI_MAJOR_AXIS_MT = 6378137;
    double SEMI_MINOR_AXIS_MT = 6356752.314245;
    double FLATTENING = 1 / 298.257223563;
    double ERROR_TOLERANCE = 1e-12;
    /**
     * Gets self lat.
     *
     * @return the self lat
     */
    public double getSelfLat() {
        return selfLat;
    }

    /**
     * Gets self lon.
     *
     * @return the self lon
     */
    public double getSelfLon() {
        return selfLon;
    }

    /**
     * Set url of the best server
     */
    private void setUrlAddress(String urlAddress){
        this.urlAddress = urlAddress;
    }
    /**
     * Return url of the best server available
     * @return urlAddress of the server
     */
    public String getUrlAddress() {
        return urlAddress;
    }
    /**
     * Set url of the best server for upload speed testing
     */
    private void setUrlUploadAdress(String url) {
        this.urlUploadAddress = url;
    }
    /**
     * Return url of the best server available
     * @return urlAddress of the server
     */
    public String getUrlUploadAddress() {
        return urlUploadAddress;
    }

    /**
     * Get if is the process is finished.
     * @return the boolean
     */
    public boolean getFinished() {
        return finished;
    }

    public void retrieveBestHost(NetworkCallback networkCallback) {
        try {
            client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Accept", "application/json")
                    .build();
            try{
                //Make the request
                Response response = client.newCall(request).execute();
                if(!response.isSuccessful()){
                    networkCallback.onRequestFailure("Failed to get response from server: " + response);
                }else {
                    double minDistance = Double.MAX_VALUE;
                    //Obtain the body of the response on a JSON
                    assert response.body() != null;
                    String responseData = response.body().string();
                    jsonArray = new JSONArray(responseData);
                    String bestHostUrl = "";
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        serverLat = jsonObject.getDouble("lat");
                        serverLon = jsonObject.getDouble("lon");
                        String serverSponsor = jsonObject.getString("sponsor");
                        double distance = vicentyDistance(selfLat, selfLon, serverLat, serverLon);
                        if (serverSponsor.trim().equalsIgnoreCase(ISP.trim())) {
                                minDistance = distance;
                                bestHostUrl = jsonObject.getString("url");
                        } else {
                            if (distance < minDistance) {
                                minDistance = distance;
                                bestHostUrl = jsonObject.getString("url");
                            }
                        }
                    }
                    setUrlUploadAdress(bestHostUrl);
                    String uploadAddress= bestHostUrl.split ("/")[ bestHostUrl.split ("/").length - 1 ];
                    String urls = bestHostUrl.replace (uploadAddress, "");
                    setUrlAddress(urls);
                }
            } catch (IOException | JSONException e) {
                throw new RuntimeException(e);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }
        networkCallback.onRequestSuccess(getUrlAddress());
        finished = true;
    }



    public void getBestHost(String usrLocation, String ISP, NetworkCallback networkCallback){
        this.ISP = ISP;
        String[] locationParts = usrLocation.split(",");
        if (locationParts.length != 2) {
            throw new IllegalArgumentException("Invalid user location format");
        }
        selfLat = Double.parseDouble(locationParts[0].trim());
        selfLon = Double.parseDouble(locationParts[1].trim());
        retrieveBestHost(networkCallback);
    }
    public void getBestHost(String ISP, NetworkCallback networkCallback){
        getLocation();
        this.ISP = ISP;
        retrieveBestHost(networkCallback);
    }

    public void getLocation(){
        client = new OkHttpClient();
        Request request = new Request.Builder().url(apiUrl).build();
        try{
            String responseData;
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
                if (response.code() == 401)
                    throw new AuthenticationException("Unauthorized API request");
                assert response.body() != null;
                responseData = response.body().string();
            }
            locationInfo = new JSONObject(responseData);
            selfLon = locationInfo.getDouble("longitude");
            selfLat = locationInfo.getDouble("latitude");
        } catch (JSONException | IOException | AuthenticationException e) {
            throw new RuntimeException(String.valueOf(e));
        }
    }

    double vicentyDistance(double latitude1, double longitude1, double latitude2, double longitude2) {
        double U1 = Math.atan((1 - FLATTENING) * Math.tan(Math.toRadians(latitude1)));
        double U2 = Math.atan((1 - FLATTENING) * Math.tan(Math.toRadians(latitude2)));

        double sinU1 = Math.sin(U1);
        double cosU1 = Math.cos(U1);
        double sinU2 = Math.sin(U2);
        double cosU2 = Math.cos(U2);

        double longitudeDifference = Math.toRadians(longitude2 - longitude1);
        double previousLongitudeDifference;

        double sinSigma, cosSigma, sigma, sinAlpha, cosSqAlpha, cos2SigmaM;

        do {
            sinSigma = Math.sqrt(Math.pow(cosU2 * Math.sin(longitudeDifference), 2) +
                    Math.pow(cosU1 * sinU2 - sinU1 * cosU2 * Math.cos(longitudeDifference), 2));
            cosSigma = sinU1 * sinU2 + cosU1 * cosU2 * Math.cos(longitudeDifference);
            sigma = Math.atan2(sinSigma, cosSigma);
            sinAlpha = cosU1 * cosU2 * Math.sin(longitudeDifference) / sinSigma;
            cosSqAlpha = 1 - Math.pow(sinAlpha, 2);
            cos2SigmaM = cosSigma - 2 * sinU1 * sinU2 / cosSqAlpha;
            if (Double.isNaN(cos2SigmaM)) {
                cos2SigmaM = 0;
            }
            previousLongitudeDifference = longitudeDifference;
            double C = FLATTENING / 16 * cosSqAlpha * (4 + FLATTENING * (4 - 3 * cosSqAlpha));
            longitudeDifference = Math.toRadians(longitude2 - longitude1) + (1 - C) * FLATTENING * sinAlpha *
                    (sigma + C * sinSigma * (cos2SigmaM + C * cosSigma * (-1 + 2 * Math.pow(cos2SigmaM, 2))));
        } while (Math.abs(longitudeDifference - previousLongitudeDifference) > ERROR_TOLERANCE);

        double uSq = cosSqAlpha * (Math.pow(SEMI_MAJOR_AXIS_MT, 2) - Math.pow(SEMI_MINOR_AXIS_MT, 2)) / Math.pow(SEMI_MINOR_AXIS_MT, 2);

        double A = 1 + uSq / 16384 * (4096 + uSq * (-768 + uSq * (320 - 175 * uSq)));
        double B = uSq / 1024 * (256 + uSq * (-128 + uSq * (74 - 47 * uSq)));

        double deltaSigma = B * sinSigma * (cos2SigmaM + B / 4 * (cosSigma * (-1 + 2 * Math.pow(cos2SigmaM, 2))
                - B / 6 * cos2SigmaM * (-3 + 4 * Math.pow(sinSigma, 2)) * (-3 + 4 * Math.pow(cos2SigmaM, 2))));

        double distanceMt = SEMI_MINOR_AXIS_MT * A * (sigma - deltaSigma);
        return distanceMt / 1000;
    }

    @Override
    public void onRequestSuccess(String urlAddress) {

    }

    @Override
    public void onRequestFailure(String error) {

    }
}
