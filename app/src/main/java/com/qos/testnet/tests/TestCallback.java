package com.qos.testnet.tests;

public interface TestCallback {
    void onTestStart();

    void onTestSuccess(String message);

    void onTestBackground(String currentBackgroundTestResult, int currentBackgroundProgress);

    void onTestFailure(String error);

    default void onTestFailed(String message) {
    }

    default void onTestSkipped(String message) {
    }

    default void onTestTimeout(String message) {

    }
}
