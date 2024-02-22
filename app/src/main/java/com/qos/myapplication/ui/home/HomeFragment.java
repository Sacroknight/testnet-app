package com.qos.myapplication.ui.home;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.qos.myapplication.databinding.FragmentHomeBinding;
import com.qos.myapplication.tests.DeviceInformation;
import com.qos.myapplication.tests.PingJitterStats;

public class HomeFragment extends Fragment {

    public FragmentHomeBinding binding;
    DeviceInformation deviceInformation;
    public Handler handler;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        handler = new Handler(Looper.getMainLooper());
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);


        Bundle args = getArguments();

        if(args != null && args.containsKey("device_info")){
            deviceInformation = (DeviceInformation) args.getSerializable("device_info");
        }
        if(deviceInformation ==null){
            deviceInformation = new DeviceInformation(requireContext());
        }
        PingJitterStats pingJitterStats = new PingJitterStats(binding, handler);

        HomeViewModel homeViewModel =
                new HomeViewModel(deviceInformation, pingJitterStats);

        View root = binding.getRoot();

        Button start_Button = binding.startButton;

        final TextView textView = binding.deviceInformation;
        final TextView device_Info = binding.deviceInformation;
        start_Button.setOnClickListener(view -> {
            homeViewModel.startPingJitterMeasurement(start_Button);
            deviceInformation.updateDeviceLocationAndSignal(sharedPreferences.getBoolean("dontAskAgain", false)
                    ,sharedPreferences.getBoolean("dontAskAngainDenied",false));
            start_Button.setEnabled(false);
        });
        homeViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        homeViewModel.getDeviceInfo().observe(getViewLifecycleOwner(), device_Info::setText);

        return root;
    }

//     @Override
//   public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//         RequestPermissions requestPermissions = new RequestPermissions(requireContext());
//        if (requestCode == requestPermissions.REQUEST_LOCATION_PERMISSION) {
//            DeviceInformation deviceInformation = new DeviceInformation(requireContext());
//            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                // Permission granted, update location
//                deviceInformation.retrieveLocation(sharedPreferences.getBoolean("dontAskAngainDenied",false));
//            } else {
//                // Permission denied
//                deviceInformation.setLocation(String.valueOf(deviceInformation.DENIED_PERMISSIONS));
//            }
//        }
//    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
