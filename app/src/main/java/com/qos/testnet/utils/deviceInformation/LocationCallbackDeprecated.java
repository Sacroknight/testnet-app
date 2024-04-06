package com.qos.testnet.utils.deviceInformation;

public interface LocationCallbackDeprecated {
    void onLocationSuccess(String location);

    void onApproxLocationSuccess(String location);

    void onLocationFailed(String error);

    void onApproxLocationFailed(String error);

    void onLocationRetrievalException(String e);
}
