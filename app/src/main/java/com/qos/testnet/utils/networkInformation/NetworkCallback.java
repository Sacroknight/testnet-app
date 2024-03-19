package com.qos.testnet.utils.networkInformation;

public interface NetworkCallback {
    void onRequestSuccess(String response);
    void onRequestFailure(String error);
}
