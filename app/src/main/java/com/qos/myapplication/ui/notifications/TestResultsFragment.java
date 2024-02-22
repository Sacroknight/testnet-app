package com.qos.myapplication.ui.notifications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.qos.myapplication.databinding.FragmentTestResultsBinding;

public class TestResultsFragment extends Fragment{

    private  FragmentTestResultsBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        TestResultsViewModel testResultsViewModel =
                new ViewModelProvider(this).get(TestResultsViewModel.class);

        binding = FragmentTestResultsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textNotifications;
        testResultsViewModel.getText().observe(getViewLifecycleOwner(),textView::setText);
          return root;
        }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}