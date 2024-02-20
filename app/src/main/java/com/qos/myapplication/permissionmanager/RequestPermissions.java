package com.qos.myapplication.permissionmanager;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.qos.myapplication.R;

public class RequestPermissions{
    private final Context context;
    public final int REQUEST_LOCATION_PERMISSION = 0001;
    private final int REQUEST_READ_PHONE_PERMISSION = 0010;
    public final int REQUEST_ALL_PERMISSION = 0011;


    public RequestPermissions(Context context){
        this.context = context;
    }
    public boolean hasLocationPermissions(){
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
    public boolean hasReadPhonePermissions(){
        boolean result;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            result = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_BASIC_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
        }else{
            result = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
        }
        return result;
    }
    public boolean hasAllNecessaryPermissions(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return  hasLocationPermissions() && hasReadPhonePermissions();
        }else{
            return hasLocationPermissions() &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
        }
    }
    private void requestLocationPermissions(int requestCode){
        ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION}, requestCode);
    }
    private void requestLocationPermissionsDialog(){
        String[] toppings = {context.getString(R.string.dont_Ask_Again)};
        boolean[] checkedItems = {false};
        new AlertDialog.Builder(context).setTitle(context.getString(R.string.request_Location))
                .setMessage(context.getString(R.string.request_Location_Dialog))
                .setMultiChoiceItems(toppings, checkedItems, (dialog, which, isChecked) -> {
                    if (isChecked) {
                        SharedPreferences sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putBoolean("dontAskAgain", true);
                        editor.apply();
                    }
                })
                .setPositiveButton(context.getString(R.string.grant_Permission), (dialog, which) ->{
                    requestLocationPermissions(REQUEST_LOCATION_PERMISSION);
                }).setNegativeButton(context.getString(R.string.continue_Without_Permission), (dialog, which) -> dialog.dismiss()).
                show();
    }
    private void requestReadPhonePermissions(int requestCode){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.READ_BASIC_PHONE_STATE}, requestCode);
        }else {
            ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.READ_PHONE_STATE}, requestCode);
        }
    }

    private void requestReadPhonePermissionsDialog(int requestCode){
        String[] toppings = {context.getString(R.string.dont_Ask_Again)};
        boolean[] checkedItems = {false};
        new AlertDialog.Builder(context).setTitle(context.getString(R.string.request_Phone_Permission))
                .setMessage(context.getString(R.string.request_Phone_Dialog))
                .setMultiChoiceItems(toppings, checkedItems, (dialog, which, isChecked) -> {
                    if (isChecked) {
                        SharedPreferences sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putBoolean("dontAskAgain", true);
                        editor.apply();
                    }
                })
                .setPositiveButton(context.getString(R.string.grant_Permission), (dialog, which) ->{
                    requestReadPhonePermissions(REQUEST_READ_PHONE_PERMISSION);})
                .setNegativeButton(context.getString(R.string.continue_Without_Permission), (dialog, which) -> dialog.dismiss()).
                show();
    }
    private void requestAllPermissions(){
        new AlertDialog.Builder(context).setTitle(context.getString(R.string.permissions_Needed))
                .setMessage(context.getString(R.string.permissions_Needed_Dialog))
                .setPositiveButton(context.getString(R.string.grant_Permission), (dialog, which) ->{
                    requestLocationPermissions(REQUEST_LOCATION_PERMISSION);
                    requestReadPhonePermissions(REQUEST_LOCATION_PERMISSION);
                    })
                .setNegativeButton(context.getString(R.string.continue_Without_Permission), (dialog, which) -> dialog.dismiss()).
                show();
    }
    public void checkAndRequestPermission(){
        if(!hasAllNecessaryPermissions()){
            requestAllPermissions();
        }
    }
    public void showPermissionDeniedWarning() {
        String[] toppings = {context.getString(R.string.dont_Ask_Again)};
        boolean[] checkedItems = {false};
        new AlertDialog.Builder(context)
                .setTitle("Permission Warning")
                .setMessage("Cell information retrieval requires location permission. " +
                        "Test results will have lower precision without it. " +
                        "The experiment can still be conducted.").setMultiChoiceItems(toppings, checkedItems, (dialog, which, isChecked) -> {
                    if (isChecked) {
                        SharedPreferences sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putBoolean("dontAskAgainDenied", true);
                        editor.apply();
                    }
                })
                .setPositiveButton("Continue", (dialog, which) -> dialog.dismiss())
                .setNegativeButton("Grant Permissions", ((dialog, which) -> {
                    dialog.dismiss();
                    if(!hasAllNecessaryPermissions()){
                    requestLocationPermissions(REQUEST_LOCATION_PERMISSION);
                    requestReadPhonePermissions(REQUEST_READ_PHONE_PERMISSION);
                    }else if(!hasLocationPermissions()){
                        requestLocationPermissions(REQUEST_LOCATION_PERMISSION);
                    }else if(!hasReadPhonePermissions()){
                        requestReadPhonePermissions(REQUEST_READ_PHONE_PERMISSION);
                    }
                }))// Optionally, close app if user wants
                .show();
    }


}
