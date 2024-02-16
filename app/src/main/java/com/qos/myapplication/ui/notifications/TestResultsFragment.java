package com.qos.myapplication.ui.notifications;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.location.LocationResult;
import com.qos.myapplication.databinding.FragmentTestResultsBinding;
import static com.qos.myapplication.tests.DeviceInformation.*;

import com.qos.myapplication.tests.DeviceInformation;
import com.qos.myapplication.tests.PermissionCallback;

import java.util.Objects;

public class TestResultsFragment extends Fragment implements PermissionCallback {

    private FragmentTestResultsBinding binding;
    private DeviceInformation deviceInformation;

        public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        TestResultsViewModel testResultsViewModel =
                new ViewModelProvider(this).get(TestResultsViewModel.class);

        binding = FragmentTestResultsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        // Se obtiene la información del dispositivo desde los argumentos del fragmento
        Bundle args = getArguments();
        if(args != null && args.containsKey("device_info")){
            deviceInformation = (DeviceInformation) args.getSerializable("device_info");
        }
        if(deviceInformation ==null){
            deviceInformation = new DeviceInformation(requireContext());
        }
        if(hasLocationPermissions()){
            updateDeviceInfoText();
        }else{
            deviceInformation.requestLocationPermission(REQUEST_LOCATION_PERMISSION);
        }
            return root;
        }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onPermissionGranted() {
        updateDeviceInfoText();
    }
    @Override
    public void onPermissionDenied() {
        // Handle denied permission:
        // Show message, disable features, etc.
    }
    private boolean hasLocationPermissions() {
        return ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void updateDeviceInfoText() {

            deviceInformation.retrieveSignalStrength(); // Use callback for updates
            deviceInformation.retrieveLocation();
            // Wait for location update
            if (deviceInformation != null) {

                new Handler().postDelayed(() -> {
                    // Update text based on device information
                    String deviceInfoText = "Marca: " + deviceInformation.getManufacturer() +
                            "\nModelo: " + deviceInformation.getModel() +
                            "\nVersión de Android: " + deviceInformation.getAndroidVersion() +
                            "\nIntensidad de señal: " + deviceInformation.getSignalStrength() +
                            "\nUbicación: " + deviceInformation.getLocation();
                    Objects.requireNonNull(binding.textNotifications).setText(deviceInfoText);
                }, 1000); // Wait for 1 second before updating text
            }else {
                binding.textNotifications.setText("No se pudo obtener la información del dispositivo.");
            }
        }
    }