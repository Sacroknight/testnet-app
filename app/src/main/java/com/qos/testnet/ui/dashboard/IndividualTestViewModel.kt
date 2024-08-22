package com.qos.testnet.ui.dashboard

import com.qos.testnet.utils.iperf.Iperf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.qos.testnet.utils.networkInformation.IperfJNI
import java.io.File
import java.io.IOException

class IndividualTestViewModel : ViewModel() {
    private val iperfJNI = IperfJNI()
    private val _testResults = MutableLiveData<String>()
    private val mText = MutableLiveData<String>()
    private val iperf: Iperf = Iperf()

    init {
        mText.value = "This is dashboard fragment"
    }

    val text: LiveData<String>
        get() = mText
    fun iperfProbe(){
        try {
            // Copiar el archivo binario de iperf3 para la arquitectura específica (ejemplo: arm64-v8a)
            val architecture = "jniLibs/arm64-v8a" // Cambia esto según la arquitectura deseada
            //iperf.copyIperfBinary(architecture)

            // Verificar si el archivo iperf3 existe en /data/tmp/
            val iperfFile = File("/assets/iperf3")
            if (iperfFile.exists()) {
               mText.value= "El archivo iperf3  se ha copiado correctamente."
            }

            // Ejecutar el comando iperf3
            //val result = iperf.executeIperfCommand("/assets/iperf3 -c 169.150.228.129 ")
            //mText.setValue(result)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}