package com.qos.testnet.data.local

import java.io.Serializable
import java.text.DecimalFormat

data class TestData(
    var dispositivo: String = "",
    var fecha: String = "",
    var idVersionAndroid: Int = 0,
    var intensidadDeSenal: Int = 0,
    var jitter: Int = 0,
    var operadorDeRed: String = "",
    var ping: Int = 0,
    var pingHost: String = "",
    var redScore: Double = 0.0,
    var servidor: String = "",
    var tipoDeRed: String = "",
    var ubicacion: String? = "",
    var userId: String = "",
    var velocidadDeCarga: Double = 0.0,
    var velocidadDeDescarga: Double = 0.0
) : Serializable {

    // Funciones para obtener los valores formateados
    fun getFormattedRedScore(): String {
        return formatDouble(redScore)
    }

    fun getFormattedVelocidadDeCarga(): String {
        return formatDouble(velocidadDeCarga)
    }

    fun getFormattedVelocidadDeDescarga(): String {
        return formatDouble(velocidadDeDescarga)
    }

    private fun formatDouble(value: Number): String {
        val df = DecimalFormat("#.00")
        return df.format(value)
    }
}