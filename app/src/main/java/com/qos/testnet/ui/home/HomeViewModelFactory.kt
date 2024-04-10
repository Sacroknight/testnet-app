package com.qos.testnet.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.qos.testnet.ui.home.HomeViewModel

@Suppress("UNCHECKED_CAST")
class HomeViewModelFactory(context: Context?) : ViewModelProvider.Factory {
    private var context: Context? = null

    init {
        this.context = context
    }

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java) && modelClass == HomeViewModel::class.java) {
            return HomeViewModel(context!!) as T
        } else {
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}