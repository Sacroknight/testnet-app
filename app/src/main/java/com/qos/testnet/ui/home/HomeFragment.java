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
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.qos.myapplication.databinding.FragmentHomeBinding;

/**
 * The Home fragment.
 */
public class HomeFragment extends Fragment {

    public FragmentHomeBinding binding;
    public Handler handler;
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        handler = new Handler(Looper.getMainLooper());


        HomeViewModelFactory factory = new HomeViewModelFactory(requireContext());
        HomeViewModel homeViewModel = new ViewModelProvider(this, factory).get(HomeViewModel.class);
        View root = binding.getRoot();

        binding.startButton.setOnClickListener(view -> {
            binding.startButton.setEnabled(false);
            binding.testProgressIndicator.setVisibility(View.VISIBLE);
            homeViewModel.startTasks();
        });

        HomeViewModel.getInstantMeasurements().observe(getViewLifecycleOwner(), s -> binding.instantMeasurements.setText(s));


        // Observe the device info and update the UI accordingly
        HomeViewModel.getDeviceInfo().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(String deviceInfo) {
                binding.deviceInformation.setText(deviceInfo);
            }
        });

        // Observe the progress and update the UI accordingly
        HomeViewModel.getProgress().observe(getViewLifecycleOwner(), new Observer<Integer>() {
            @Override
            public void onChanged(Integer progress) {
                binding.testProgressIndicator.setProgress(progress);
            }
        });
        HomeViewModel.getJitterMeasurement().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(String s) {

            }
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        binding.testProgressIndicator.setProgress(0);
        super.onDestroyView();
        binding = null;
    }
}
