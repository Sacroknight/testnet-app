package com.qos.testnet.utils.network;


import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class GetBetterHost implements NetworkCallback {
    private OkHttpClient client;
    private String isp;
    private static final String URL = "https://www.speedtest.net/api/js/servers?engine=js&limit=10&https_functional=true";

    /**
     * The Self lat.
     */
    private double selfLat = 0.0;
    /**
     * The Self lon.
     */
    private double selfLon = 0.0;
    /**
     * url of the best server
     */
    private String urlAddress;
    private String urlUploadAddress;
    /**
     * The Finished.
     */
    private boolean finished = false;
    private static final double SEMI_MAJOR_AXIS_MT = 6378137;
    private static final double SEMI_MINOR_AXIS_MT = 6356752.314245;
    private static final double FLATTENING = 1 / 298.257223563;
    private static final double ERROR_TOLERANCE = 1e-12;

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
    private void setUrlAddress(String urlAddress) {
        this.urlAddress = urlAddress;
    }

    /**
     * Return url of the best server available
     *
     * @return urlAddress of the server
     */
    public String getUrlAddress() {
        return urlAddress;
    }

    /**
     * Set url of the best server for upload speed testing
     */
    private void setUrlUploadAddress(String url) {
        this.urlUploadAddress = url;
    }

    /**
     * Return url of the best server available
     *
     * @return urlAddress of the server
     */
    public String getUrlUploadAddress() {
        return urlUploadAddress;
    }

    /**
     * Get if is the process is finished.
     *
     * @return the boolean
     */
    public boolean getFinished() {
        return finished;
    }

    public void retrieveBestHost(NetworkCallback networkCallback) {
        try {
            // Initialize the HTTP client
            client = new OkHttpClient();
            // Build the request
            Request request = new Request.Builder()
                    .url(URL)
                    .addHeader("Accept", "application/json")
                    .build();
            // Retrieve the best host by distance
            retrieveBestHostByDistance(request, networkCallback);
            // Notify success if no exceptions occur
            networkCallback.onRequestSuccess(getUrlAddress());
        } catch (IOException | JSONException e) {
            Log.e(this.getClass().getName(), "An error occurred while retrieving the best host: " + e.getMessage());
            networkCallback.onRequestFailure("An error occurred: " + e.getMessage());
        } finally {
            finished = true;
        }
    }

    private void retrieveBestHostByDistance(Request request, NetworkCallback networkCallback) throws IOException, JSONException {
        double serverLon;
        double serverLat;
        // Make the request
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                networkCallback.onRequestFailure("Failed to get response from server: " + response);
                Log.e(this.getClass().getName(), "Failed to get response from server:" + response);
                return; // Early return to reduce nesting
            }

            // Obtain the body of the response as JSON
            assert response.body() != null;
            String responseData = response.body().string();
            JSONArray jsonArray = new JSONArray(responseData);

            double minDistance = Double.MAX_VALUE;
            String bestHostUrl = "";

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                serverLat = jsonObject.getDouble("lat");
                serverLon = jsonObject.getDouble("lon");
                String serverSponsor = jsonObject.getString("sponsor");
                double distance = vicentyDistance(selfLat, selfLon, serverLat, serverLon);

                boolean isPreferredSponsor = serverSponsor.trim().equalsIgnoreCase(isp.trim());
                if ((isPreferredSponsor && distance <= 1000) || distance < minDistance) {
                    minDistance = distance;
                    bestHostUrl = jsonObject.getString("url");
                    if (isPreferredSponsor && minDistance <= 1000) {
                        break; // Exit early if preferred sponsor is close enough
                    }
                }
            }

            setUrlUploadAddress(bestHostUrl);

            String[] urlParts = bestHostUrl.split("/");
            String uploadAddress = urlParts[urlParts.length - 1];
            String urls = bestHostUrl.replace(uploadAddress, "");
            setUrlAddress(urls);
        } catch (IOException | JSONException e) {
            networkCallback.onRequestFailure("An error occurred: " + e.getMessage());
            Log.e(this.getClass().getName(), "An error occurred while processing the response", e);
        }
    }

    public void getBestHost(String usrLocation, String isp, NetworkCallback networkCallback) {
        this.isp = isp;
        String[] locationParts = usrLocation.split(",");
        if (locationParts.length != 2) {
            throw new IllegalArgumentException("Invalid user location format");
        }
        selfLat = Double.parseDouble(locationParts[0].trim());
        selfLon = Double.parseDouble(locationParts[1].trim());
        retrieveBestHost(networkCallback);
    }

    double vicentyDistance(double latitude1, double longitude1, double latitude2, double longitude2) {
        double u1 = Math.atan((1 - FLATTENING) * Math.tan(Math.toRadians(latitude1)));
        double u2 = Math.atan((1 - FLATTENING) * Math.tan(Math.toRadians(latitude2)));

        double sinU1 = Math.sin(u1);
        double cosU1 = Math.cos(u1);
        double sinU2 = Math.sin(u2);
        double cosU2 = Math.cos(u2);

        double longitudeDifference = Math.toRadians(longitude2 - longitude1);
        double previousLongitudeDifference;

        double sinSigma;
        double cosSigma;
        double sigma;
        double sinAlpha;
        double cosSqAlpha;
        double cos2SigmaM;

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
            double c = FLATTENING / 16 * cosSqAlpha * (4 + FLATTENING * (4 - 3 * cosSqAlpha));
            longitudeDifference = Math.toRadians(longitude2 - longitude1) + (1 - c) * FLATTENING * sinAlpha *
                    (sigma + c * sinSigma * (cos2SigmaM + c * cosSigma * (-1 + 2 * Math.pow(cos2SigmaM, 2))));
        } while (Math.abs(longitudeDifference - previousLongitudeDifference) > ERROR_TOLERANCE);

        double uSq = cosSqAlpha * (Math.pow(SEMI_MAJOR_AXIS_MT, 2) - Math.pow(SEMI_MINOR_AXIS_MT, 2)) / Math.pow(SEMI_MINOR_AXIS_MT, 2);

        double a = 1 + uSq / 16384 * (4096 + uSq * (-768 + uSq * (320 - 175 * uSq)));
        double b = uSq / 1024 * (256 + uSq * (-128 + uSq * (74 - 47 * uSq)));

        double deltaSigma = b * sinSigma * (cos2SigmaM + b / 4 * (cosSigma * (-1 + 2 * Math.pow(cos2SigmaM, 2))
                - b / 6 * cos2SigmaM * (-3 + 4 * Math.pow(sinSigma, 2)) * (-3 + 4 * Math.pow(cos2SigmaM, 2))));

        double distanceMt = SEMI_MINOR_AXIS_MT * a * (sigma - deltaSigma);
        return distanceMt / 1000;
    }

    @Override
    public void onRequestSuccess(String urlAddress) {
        // TODO document why this method is empty
    }

    @Override
    public void onRequestFailure(String error) {
        // TODO document why this method is empty
    }
}
