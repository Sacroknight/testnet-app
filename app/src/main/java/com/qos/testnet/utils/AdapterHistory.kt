package com.qos.testnet.utils

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.qos.testnet.R
import com.qos.testnet.data.local.TestData
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class AdapterHistory(private val onItemClick: (TestData) -> Unit) :
    ListAdapter<TestData, AdapterHistory.TestDataViewHolder>(TestDataDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TestDataViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.cardview_history, parent, false)
        return TestDataViewHolder(view)
    }

    override fun onBindViewHolder(holder: TestDataViewHolder, position: Int) {
        val testData = getItem(position)
        holder.bind(testData, onItemClick)
    }

    class TestDataViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewFecha: TextView = itemView.findViewById(R.id.textViewFecha)
        private val textViewScore: TextView = itemView.findViewById(R.id.textViewScore)
        private val textViewRed: TextView = itemView.findViewById(R.id.textViewRed)

        @SuppressLint("SetTextI18n", "DefaultLocale")
        fun bind(testData: TestData, onItemClick: (TestData) -> Unit) {
            // Formato de la fecha en origen siguiendo el guardado (ej: Bogotá)
            val originalFormat = SimpleDateFormat("dd-MM-yyyy'T'HH:mm:ss.SSSXXX", Locale("es", "CO"))
            originalFormat.timeZone = TimeZone.getTimeZone("America/Bogota")  // Coincide con el guardado

            // Formato deseado para mostrar
            val targetFormat = SimpleDateFormat("d 'de' MMMM 'de' yyyy", Locale("es", "ES"))

            try {
                // Analiza la fecha desde el formato original
                val date: Date = originalFormat.parse(testData.fecha)
                // Reformatea la fecha al formato objetivo
                val formattedDate: String = targetFormat.format(date)
                textViewFecha.text = "Fecha: $formattedDate"
            } catch (e: ParseException) {
                e.printStackTrace()
                textViewFecha.text = "Fecha: Desconocida"
            }
            val scoreAux = testData.redScore
            val formatedScore = String.format("%.2f", scoreAux)
            textViewScore.text = "Score: ${formatedScore}/100"
            textViewRed.text = "Tipo de Red: ${testData.tipoDeRed}"

            itemView.setOnClickListener {
                onItemClick(testData)
            }
        }
    }
    class TestDataDiffCallback : DiffUtil.ItemCallback<TestData>() {
        override fun areItemsTheSame(oldItem: TestData, newItem: TestData): Boolean {
            return oldItem.userId == newItem.userId // Ajusta según el identificador único
        }

        override fun areContentsTheSame(oldItem: TestData, newItem: TestData): Boolean {
            return oldItem == newItem
        }
    }
}