package com.qos.testnet.ui.home;


import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.qos.myapplication.databinding.FragmentHomeBinding;
import com.qos.testnet.tests.DownloadSpeedStats;
import com.qos.testnet.tests.PingAndJitterMeasurements;
import com.qos.testnet.tests.UploadSpeedStats;
import com.qos.testnet.utils.networkInformation.GetBetterHost;
import com.qos.testnet.utils.deviceInformation.DeviceInformation;
import com.qos.testnet.utils.deviceInformation.LocationInfo;

/**
 * The Home fragment.
 */
public class HomeFragment extends Fragment {

    public FragmentHomeBinding binding;
    public Handler handler;
    DeviceInformation deviceInformation;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        Bundle args = getArguments();
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        handler = new Handler(Looper.getMainLooper());
        GetBetterHost getBetterHost = new GetBetterHost();
        LocationInfo locationInfo = new LocationInfo(requireContext());
        DownloadSpeedStats downloadSpeedStats = new DownloadSpeedStats(binding, handler);
        UploadSpeedStats uploadSpeedStats = new UploadSpeedStats(binding, handler);
        if (args != null && args.containsKey("device_info")) {
            deviceInformation = (DeviceInformation) args.getSerializable("device_info");
        } else if (deviceInformation == null) {
            deviceInformation = new DeviceInformation(requireContext());
        }
        PingAndJitterMeasurements pingAndJitterMeasurements = new PingAndJitterMeasurements(binding, handler);

        final TextView textView = binding.deviceInformation;
        final TextView device_Info = binding.deviceInformation;
        final TextView downloadSpeed = binding.downloadSpeed;
        final Button start_Button = binding.startButton;
        final ProgressBar progressBar = binding.testProgressIndicator;

        HomeViewModel homeViewModel = new HomeViewModel(deviceInformation, pingAndJitterMeasurements,
                downloadSpeedStats, uploadSpeedStats, getBetterHost, locationInfo, requireContext(), handler, start_Button, progressBar);
        View root = binding.getRoot();

        start_Button.setOnClickListener(view -> {
            progressBar.setVisibility(View.VISIBLE);
            homeViewModel.startTasks();
        });
        homeViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        homeViewModel.getDeviceInfo().observe(getViewLifecycleOwner(), device_Info::setText);
        homeViewModel.getDownloadSpeed().observe(getViewLifecycleOwner(), downloadSpeed::setText);

        return root;
    }

    @Override
    public void onDestroyView() {
        binding.testProgressIndicator.setProgress(0);
        super.onDestroyView();
        binding = null;
    }
}
