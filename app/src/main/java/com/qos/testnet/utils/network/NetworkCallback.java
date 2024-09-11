package com.qos.testnet.utils.network;

public interface NetworkCallback {
    void onRequestSuccess(String response);
    void onRequestFailure(String error);
}
