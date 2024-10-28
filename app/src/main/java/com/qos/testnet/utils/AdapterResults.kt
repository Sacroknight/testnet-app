package com.qos.testnet.utils

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.qos.testnet.R
import com.qos.testnet.data.local.TestData

class AdapterResults : ListAdapter<TestData, AdapterResults.TestDataViewHolder>(TestDataDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TestDataViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_test_data, parent, false)
        return TestDataViewHolder(view)
    }

    override fun onBindViewHolder(holder: TestDataViewHolder, position: Int) {
        val testData = getItem(position)
        holder.bind(testData)
    }

    class TestDataViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewDispositivo: TextView = itemView.findViewById(R.id.textViewDispositivo)
        private val textViewFecha: TextView = itemView.findViewById(R.id.textViewFecha)
        private val textViewIdVersionAndroid: TextView = itemView.findViewById(R.id.textViewIdVersionAndroid)
        private val textViewIntensidadDeSenal: TextView = itemView.findViewById(R.id.textViewIntensidadDeSenal)
        private val textViewJitter: TextView = itemView.findViewById(R.id.textViewJitter)
        private val textViewOperadorDeRed: TextView = itemView.findViewById(R.id.textViewOperadorDeRed)
        private val textViewPing: TextView = itemView.findViewById(R.id.textViewPing)
        private val textViewRedScore: TextView = itemView.findViewById(R.id.textViewRedScore)
        private val textViewServidor: TextView = itemView.findViewById(R.id.textViewServidor)
        private val textViewTipoDeRed: TextView = itemView.findViewById(R.id.textViewTipoDeRed)
        private val textViewUbicacion: TextView = itemView.findViewById(R.id.textViewUbicacion)
        private val textViewUserId: TextView = itemView.findViewById(R.id.textViewUserId)
        private val textViewVelocidadDeCarga: TextView = itemView.findViewById(R.id.textViewVelocidadDeCarga)
        private val textViewVelocidadDeDescarga: TextView = itemView.findViewById(R.id.textViewVelocidadDeDescarga)

        fun bind(testData: TestData) {
            textViewDispositivo.text = "Dispositivo: ${testData.dispositivo}"
            textViewFecha.text = "Fecha: ${testData.fecha}"
            textViewIdVersionAndroid.text = "Version Android: ${testData.idVersionAndroid}"
            textViewIntensidadDeSenal.text = "Intensidad de Señal: ${testData.intensidadDeSenal} dBm"
            textViewJitter.text = "Jitter: ${testData.jitter} ms"
            textViewOperadorDeRed.text = "Operador de Red: ${testData.operadorDeRed}"
            textViewPing.text = "Ping: ${testData.ping} ms"
            textViewRedScore.text = "Red Score: ${testData.redScore}"
            textViewServidor.text = "Servidor: ${testData.servidor}"
            textViewTipoDeRed.text = "Tipo de Red: ${testData.tipoDeRed}"
            textViewUbicacion.text = "Ubicación: ${testData.ubicacion}"
            textViewUserId.text = "User ID: ${testData.userId}"
            textViewVelocidadDeCarga.text = "Velocidad de Carga: ${testData.velocidadDeCarga} Mbps"
            textViewVelocidadDeDescarga.text = "Velocidad de Descarga: ${testData.velocidadDeDescarga} Mbps"
        }
    }

    class TestDataDiffCallback : DiffUtil.ItemCallback<TestData>() {
        override fun areItemsTheSame(oldItem: TestData, newItem: TestData): Boolean {
            return oldItem.userId == newItem.userId // Ajusta según un identificador único si es necesario
        }

        override fun areContentsTheSame(oldItem: TestData, newItem: TestData): Boolean {
            return oldItem == newItem
        }
    }
}