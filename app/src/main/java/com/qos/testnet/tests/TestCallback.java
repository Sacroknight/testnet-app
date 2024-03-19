package com.qos.testnet.tests;

import android.content.Context;
import android.widget.Toast;

import androidx.lifecycle.MutableLiveData;

public interface TestCallback {
    void OnTestStart();
    void OnTestSuccess(String message);
    void OnTestBackground(String currentBackgroundTestResult, int currentBackgroundProgress);
    default void OnTestFailure(){
        Toast.makeText((Context) this, "Test Failed", Toast.LENGTH_SHORT).show();
    };
    default void OnTestFailed(String message){
    };
    default void OnTestSkipped(String message){
    }
    default void OnTestTimeout(String message){

    }
}
