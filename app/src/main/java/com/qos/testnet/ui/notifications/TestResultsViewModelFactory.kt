package com.qos.testnet.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.qos.testnet.data.repository.RepositoryCRUD
import com.qos.testnet.viewmodel.TestResultsViewModel

class TestResultsViewModelFactory(private val repository: RepositoryCRUD) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TestResultsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TestResultsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}