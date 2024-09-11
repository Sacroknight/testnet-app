package com.qos.testnet.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.qos.testnet.utils.networkInformation.IperfJNI

class IndividualTestViewModel : ViewModel() {
    private val iperfJNI = IperfJNI()
    private val _testResults = MutableLiveData<String>()
    private val mText = MutableLiveData<String>()

    init {
        mText.value = "This is dashboard fragment"
    }

    val text: LiveData<String>
        get() = mText
}