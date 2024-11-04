package com.qos.testnet.ui.notifications.details

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.qos.testnet.data.local.TestData

class TestDetailsViewModel : ViewModel() {

    private val _testData = MutableLiveData<TestData>()
    // Exposición de LiveData de solo lectura
    val testData: LiveData<TestData> get() = _testData

    // Método para establecer el TestData en el ViewModel
    fun setTestData(data: TestData) {
        _testData.value = data
    }
}