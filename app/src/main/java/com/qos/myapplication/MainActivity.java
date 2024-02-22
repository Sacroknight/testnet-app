package com.qos.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.qos.myapplication.databinding.ActivityMainBinding;
import com.qos.myapplication.permissionmanager.RequestPermissions;
import com.qos.myapplication.tests.DeviceInformation;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private RequestPermissions requestPermissions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        requestPermissions = new RequestPermissions(this);
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        boolean permissionsRequested = sharedPreferences.getBoolean("permissionsRequested", false);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if(!permissionsRequested){
            requestPermissions.checkAndRequestPermission();
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("permissionsRequested", true);
            editor.apply();
        }

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);
    }

}