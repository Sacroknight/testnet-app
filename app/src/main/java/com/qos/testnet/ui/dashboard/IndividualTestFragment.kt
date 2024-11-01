package com.qos.testnet.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.qos.testnet.databinding.FragmentIndividualTestBinding
import com.qos.testnet.utils.AdapterResults
import kotlinx.coroutines.launch

class IndividualTestFragment : Fragment() {

    private var _binding: FragmentIndividualTestBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: AdapterResults

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentIndividualTestBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val factory = IndividualTestViewModelFactory(requireContext())
        val individualTestViewModel =
            ViewModelProvider(this, factory)[IndividualTestViewModel::class.java]

        // Configuración de las vistas y observadores
        binding.textDashboard.text = individualTestViewModel.text.value

        adapter = AdapterResults()
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(context)

        individualTestViewModel.fetchedData.observe(viewLifecycleOwner) { dataList ->
            adapter.submitList(dataList)
        }

        binding.buttonSend.setOnClickListener {
//            val newTestData = TestData(
//                dispositivo = "Poco X3",
//                fecha = "2024-10-11T15:00:00Z",
//                idVersionAndroid = 13,
//                intensidadDeSenal = -65,
//                jitter = 9,
//                operadorDeRed = "TIGO",
//                ping = 69,
//                redScore = 6,
//                servidor = "speedtest.tigo.com:8080/",
//                tipoDeRed = "3.5G",
//                ubicacion = "3.37673093,-76.55031147",
//                userId = "test_Id",
//                velocidadDeCarga = 20.66,
//                velocidadDeDescarga = 10.22
//            )
//            individualTestViewModel.sendData(newTestData)
        }

        binding.buttonFetch.setOnClickListener {
            lifecycleScope.launch {
                individualTestViewModel.fetchData()
            }
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}