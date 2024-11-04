package com.qos.testnet.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.qos.testnet.data.local.TestData
import com.qos.testnet.permissionmanager.PermissionPreferences
import kotlinx.coroutines.tasks.await

class RepositoryCRUD(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val context: Context
) {
    private val permissionPreferences: PermissionPreferences by lazy { PermissionPreferences.getInstance() }


    suspend fun fetchData(userId: String): List<TestData> {
        if (userId.isEmpty() || userId.contentEquals("-1")) {
            return emptyList()
        }
        val collectionRef = firestore.collection("test_results")
        val querySnapshot = collectionRef.whereEqualTo("userId", userId).get().await()
        return querySnapshot.documents.mapNotNull { document ->
            document.toObject(TestData::class.java)
        }
    }

    suspend fun getUserId(): String {
        val userLocal = permissionPreferences.getUserId(
            context,
            PermissionPreferences.PermissionPreferencesKeys.USER_ID
        )
        if (userLocal.isNotEmpty() || !userLocal.contentEquals("-1")) {
            return userLocal
        }
        return getUserIdFromFirebase()
    }

    private suspend fun getUserIdFromFirebase(): String {
        firebaseAuth.currentUser?.let {
            return it.uid
        }
        val authResult = firebaseAuth.signInAnonymously().await()
        return authResult.user?.uid ?: throw IllegalStateException("Failed to create anonymous user")
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
}