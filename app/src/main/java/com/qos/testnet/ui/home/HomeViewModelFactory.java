package com.qos.testnet.ui.home;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import android.content.Context;


public class HomeViewModelFactory implements ViewModelProvider.Factory {
    private Context context = null;

    public HomeViewModelFactory(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(Class<T> modelClass) {
        if (modelClass.isAssignableFrom(HomeViewModel.class) && modelClass == HomeViewModel.class) {
            return (T) new HomeViewModel(context);
        } else {
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }
}