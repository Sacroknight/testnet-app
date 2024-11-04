package com.qos.testnet.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qos.testnet.data.local.TestData
import com.qos.testnet.data.repository.RepositoryCRUD
import kotlinx.coroutines.launch

class TestResultsViewModel(private val repository: RepositoryCRUD) : ViewModel() {

    private val _fetchedData = MutableLiveData<List<TestData>>()
    val fetchedData: LiveData<List<TestData>> get() = _fetchedData

    fun fetchData() {
        viewModelScope.launch {
            try {
                val userId = repository.getUserId()
                val dataList = repository.fetchData(userId)
                _fetchedData.value = dataList.sortedByDescending { it.fecha }
            } catch (e: Exception) {
                Log.e(this.javaClass.typeName, e.message, e)
            }
        }
    }
}