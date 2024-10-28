package com.qos.testnet.ui.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

@Suppress("UNCHECKED_CAST")
class IndividualTestViewModelFactory(context: Context?) : ViewModelProvider.Factory {
    private var context: Context? = null

    init {
        this.context = context
    }
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(IndividualTestViewModel::class.java) && modelClass == IndividualTestViewModel::class.java) {
            return IndividualTestViewModel(context!!) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}