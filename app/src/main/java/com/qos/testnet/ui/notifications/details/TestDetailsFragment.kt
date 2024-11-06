package com.qos.testnet.ui.notifications.details

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.qos.testnet.databinding.FragmentTestDetailsBinding
import com.qos.testnet.data.local.TestData

class TestDetailsFragment : Fragment() {

    private var _binding: FragmentTestDetailsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TestDetailsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTestDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Obtener y establecer TestData desde los argumentos
        val testData: TestData? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getSerializable("testData", TestData::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getSerializable("testData") as? TestData
        }

        testData?.let {
            viewModel.setTestData(it)
        }

        // Observa los cambios del testData
        viewModel.testData.observe(viewLifecycleOwner) { data ->
            data?.let {
                binding.textViewDispositivo.text = "Dispositivo: ${it.dispositivo}"
                binding.textViewFecha.text = "Fecha: ${it.fecha}"
                binding.textViewIdVersionAndroid.text = "Versión Android: ${it.idVersionAndroid}"
                binding.textViewIntensidadDeSenal.text = "Intensidad de Señal: ${it.intensidadDeSenal}"
                binding.textViewJitter.text = "Jitter: ${it.jitter}"
                binding.textViewOperadorDeRed.text = "Operador de Red: ${it.operadorDeRed}"
                binding.textViewPing.text = "Ping: ${it.ping}"
                binding.textViewPingHost.text = "Ping Host: ${it.pingHost}"
                binding.textViewRedScore.text = "Red Score: ${it.redScore}"
                binding.textViewServidor.text = "Servidor: ${it.servidor}"
                binding.textViewTipoDeRed.text = "Tipo de Red: ${it.tipoDeRed}"
                binding.textViewUbicacion.text = "Ubicación: ${it.ubicacion.orEmpty()}"
                binding.textViewUserId.text = "User ID: ${it.userId}"
                binding.textViewVelocidadDeCarga.text = "Velocidad de Carga: ${it.velocidadDeCarga}"
                binding.textViewVelocidadDeDescarga.text = "Velocidad de Descarga: ${it.velocidadDeDescarga}"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}