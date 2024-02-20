package com.qos.myapplication.ui.notifications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.qos.myapplication.tests.PingJitterStats;
import com.qos.myapplication.databinding.FragmentTestResultsBinding;

import com.qos.myapplication.tests.DeviceInformation;

public class TestResultsFragment extends Fragment{

    private static FragmentTestResultsBinding binding;
    private static DeviceInformation deviceInformation;
    private static PingJitterStats pingJitterStats;
    static String chosenHost;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        TestResultsViewModel testResultsViewModel =
                new ViewModelProvider(this).get(TestResultsViewModel.class);
        pingJitterStats = new PingJitterStats();
        chosenHost = pingJitterStats.chooseHost();

        binding = FragmentTestResultsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        // Se obtiene la información del dispositivo desde los argumentos del fragmento
       /* Bundle args = getArguments();
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
        }*/
          return root;
        }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /*@Override
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
    }*/

   /* private static class PingJitterTask extends AsyncTask<Void, Void, Void> {
        private final PingJitterStats pingJitterStats;
        private final String chosenHost;

        public PingJitterTask(PingJitterStats pingJitterStats, String chosenHost) {
            this.pingJitterStats = pingJitterStats;
            this.chosenHost = chosenHost;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            pingJitterStats.measuringPingJitter(chosenHost);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            // Update UI after ping and jitter measurements are done
            updateDeviceInfoText();
        }
    }
*/

    /*private static void updateDeviceInfoText() {

        deviceInformation.retrieveSignalStrength(); // Use callback for updates
        deviceInformation.retrieveLocation();
        new PingJitterTask(pingJitterStats, chosenHost).execute();

        // Wait for location update
            if (deviceInformation != null) {
               // pingJitterStats.measuringPingJitter(chosenHost); // Start background task
                new Handler().postDelayed(() -> {
                    // Update text based on device information
                    String deviceInfoText = "Marca: " + deviceInformation.getManufacturer() +
                            "\nModelo: " + deviceInformation.getModel() +
                            "\nVersión de Android: " + deviceInformation.getAndroidVersion() +
                            "\nIntensidad de señal: " + deviceInformation.getSignalStrength() +
                            "\nUbicación: " + deviceInformation.getLocation() +
                            "\nPing: " + pingJitterStats.getPingMeasure() +
                            "\nJitter: " + pingJitterStats.getJitterMeasure();
                    Objects.requireNonNull(binding.textNotifications).setText(deviceInfoText);
                }, (1000)); // Wait for 1 second before updating text
            }else {
                binding.textNotifications.setText("No se pudo obtener la información del dispositivo.");
            }
        }*/
    }