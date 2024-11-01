package com.qos.testnet.data.local

import java.text.DecimalFormat

data class TestData(
    var dispositivo: String = "",
    var fecha: String = "",
    var idVersionAndroid: Int = 0,
    var intensidadDeSenal: Int = 0,
    var jitter: Int = 0,
    var operadorDeRed: String = "",
    var ping: Int = 0,
    var redScore: Double = 0.0,
    var servidor: String = "",
    var tipoDeRed: String = "",
    var ubicacion: String? = "",
    var userId: String = "",
    var velocidadDeCarga: Double = 0.0,
    var velocidadDeDescarga: Double = 0.0
) {
    // Constructor sin argumentos
    constructor() : this(
        dispositivo = "",
        fecha = "",
        idVersionAndroid = 0,
        intensidadDeSenal = 0,
        jitter = 0,
        operadorDeRed = "",
        ping = 0,
        redScore = 0.0,
        servidor = "",
        tipoDeRed = "",
        ubicacion = "",
        userId = "",
        velocidadDeCarga = 0.0,
        velocidadDeDescarga = 0.0
    )

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

    private fun formatDouble(value: Double): String {
        val df = DecimalFormat("#.00")
        return df.format(value)
    }
}