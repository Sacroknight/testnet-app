package com.qos.myapplication.ui.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.qos.myapplication.R;
import com.qos.myapplication.databinding.FragmentHomeBinding;
import com.qos.myapplication.permissionmanager.RequestPermissions;
import com.qos.myapplication.tests.DeviceInformation;
import com.qos.myapplication.tests.PingJitterStats;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private DeviceInformation deviceInformation;
    private PingJitterStats pingJitterStats;
    private String chosenHost;
    private Handler handler;
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        pingJitterStats = new PingJitterStats();
        handler = new Handler(Looper.getMainLooper());
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        Bundle args = getArguments();
        if(args != null && args.containsKey("device_info")){
            deviceInformation = (DeviceInformation) args.getSerializable("device_info");
        }
        if(deviceInformation ==null){
            deviceInformation = new DeviceInformation(requireContext());
        }

        homeViewModel.init(deviceInformation,pingJitterStats);

        Button start_Button = binding.startButton;
        start_Button.setOnClickListener(view -> {
            binding.deviceInformation.setText("");
            chosenHost = pingJitterStats.chooseHost();
            String deviceInfoText = homeViewModel.getDeviceInfoText(chosenHost);
            binding.deviceInformation.append(deviceInfoText);
            updateDeviceInfoText();
            start_Button.setEnabled(false);
        });

        return root;
    }


    private void updateDeviceInfoText() {
        if (deviceInformation != null) {
            deviceInformation.retrieveLocation();
            deviceInformation.retrieveSignalStrength();
            new PingJitterTask(pingJitterStats, chosenHost, binding.testProgressIndicator, binding.startButton, handler, binding).execute();
        } else {
            binding.textHome.setText(getContext().getString(R.string.not_deviceInfo));
        }
    }

    private class PingJitterTask extends AsyncTask<Void, Integer, Void> {

        private final PingJitterStats pingJitterStats;
        private final String chosenHost;
        private final ProgressBar progressBar;
        private final Button button;
        private final Handler handler;

        private final FragmentHomeBinding binding;

        public PingJitterTask(PingJitterStats pingJitterStats, String chosenHost, ProgressBar progressBar, Button button, Handler handler, FragmentHomeBinding binding) {
            this.pingJitterStats = pingJitterStats;
            this.chosenHost = chosenHost;
            this.progressBar = progressBar;
            this.button = button;
            this.handler = handler;
            this.binding =binding;
        }


        @Override
        protected Void doInBackground(Void... voids) {
            pingJitterStats.measuringPingJitter(chosenHost, handler, binding);
            getActivity().runOnUiThread(() -> progressBar.setProgress(100));
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            // Update UI after ping and jitter measurements are done
            String deviceInfoText = "Manufacturer: " + deviceInformation.getManufacturer() +
                    "\nModel: " + deviceInformation.getModel() +
                    "\nAndroid Version: " + deviceInformation.getAndroidVersion() +
                    "\nActual Location: " + deviceInformation.getLocation() +
                    "\nSignal Strenght: " + deviceInformation.getCarrier() +" "+ deviceInformation.getSignalStrength() +" dBm" +
                    "\nCurrent Host: " + chosenHost +
                    "\nPing: " + pingJitterStats.getPingMeasure() +
                    "\nJitter: " + pingJitterStats.getJitterMeasure();
            binding.deviceInformation.append(deviceInfoText);
            button.setEnabled(true);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        RequestPermissions requestPermissions = new RequestPermissions(requireContext());
        if (requestCode == requestPermissions.REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, update location
                LocationManager locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
                deviceInformation.gettingLocation(locationManager);
            } else {
                // Permission denied
                deviceInformation.setLocation(String.valueOf(deviceInformation.DENIED_PERMISSIONS));
            }
        }
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
