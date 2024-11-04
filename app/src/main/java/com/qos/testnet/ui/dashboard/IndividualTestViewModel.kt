package com.qos.testnet.ui.dashboard

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.qos.testnet.data.local.TestData
import com.qos.testnet.data.repository.RepositoryCRUD
import kotlinx.coroutines.launch

class IndividualTestViewModel(individualContext: Context) : ViewModel() {

    private val _testResults = MutableLiveData<String>()
    private val context by lazy { individualContext }
    private val mText = MutableLiveData<String>()
    private var _fetchedData = MutableLiveData<List<TestData>>()
    private val firebaseDatabase = FirebaseDatabase.getInstance()

    private val database: DatabaseReference = firebaseDatabase.getReference("test_results")
    private val firestore = FirebaseFirestore.getInstance()
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val userRepository: RepositoryCRUD = RepositoryCRUD(firestore, firebaseAuth, context)

    init {
        mText.value = "This is dashboard fragment"
    }

    fun sendData(data: TestData) {
        firestore.collection("test_results").add(data)
            .addOnSuccessListener { documentReference ->
                Log.d("sendData", "DocumentSnapshot added with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.e("sendData", "Error adding document", e)
            }
    }

    fun fetchData() {
        viewModelScope.launch {
            try {
                val userId = userRepository.getUserId()
                val dataList = userRepository.fetchData(userId)
                _fetchedData.value = dataList
            } catch (e: Exception) {
                Log.e(this.javaClass.simpleName, "Error al recuperar los datos", e)
            }
        }
    }

    fun sendDataRealtimeDataBase(data: TestData) {
        database.push().setValue(data).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("sendData", "Datos enviados correctamente")
            } else {
                Log.e("sendData", "Error al enviar los datos: ${task.exception?.message}")
            }
        }
    }

    fun fetchDataRealtimeDataStore(userId: String) {
        val query = database.orderByChild("userId").equalTo(userId)

        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val dataList = mutableListOf<TestData>()
                for (dataSnapshot in snapshot.children) {
                    val data = dataSnapshot.getValue(TestData::class.java)
                    if (data != null) {
                        dataList.add(data)
                    }
                }
                _fetchedData.value = dataList
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("fetchData", "Error al recuperar los datos: ${error.message}")
            }
        })
    }

    val fetchedData: LiveData<List<TestData>>
        get() = _fetchedData

    val text: LiveData<String>
        get() = mText
}