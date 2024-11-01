package com.qos.testnet

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.qos.testnet.databinding.ActivityMainBinding
import com.qos.testnet.permissionmanager.PermissionPreferences
import com.qos.testnet.permissionmanager.RequestPermissions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private var binding: ActivityMainBinding? = null
    private var requestPermissions: RequestPermissions? = null
    private val permissionPreferences: PermissionPreferences by lazy { PermissionPreferences.getInstance() }
    private lateinit var mAuth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        mAuth = FirebaseAuth.getInstance()
        requestPermissions = RequestPermissions(this)

        CoroutineScope(Dispatchers.Main).launch {
            val permissionsRequested = permissionPreferences.getPermissionPreference(
                this@MainActivity,
                PermissionPreferences.PermissionPreferencesKeys.ASK_OPEN_PERMISSION
            )

            if (!permissionsRequested) {
                requestPermissions?.requestAllPermissionsDialog()

                permissionPreferences.savePermissionPreference(
                    this@MainActivity,
                    PermissionPreferences.PermissionPreferencesKeys.ASK_OPEN_PERMISSION, true
                )
            }
        }

        if(!requestPermissions?.hasWriteStoragePermissions()!! || !requestPermissions?.hasReadStoragePermissions()!!) {
            requestPermissions!!.requestWriteStoragePermissions()
            requestPermissions!!.requestReadStoragePermissions()
        }

        if (mAuth.currentUser == null) {
            singInAnonymously()
        } else {
            val user: FirebaseUser? = mAuth.currentUser
            user?.let {
                Log.d("MainActivity", "User already signed in, userId: ${it.uid}")
            }
        }

        lifecycleScope.launch {
            val usrId = permissionPreferences.getUserId(
                this@MainActivity,
                PermissionPreferences.PermissionPreferencesKeys.USER_ID
            )
            Log.d("PreferencesStored", "Collected user Id: $usrId")
        }

        val navView = findViewById<BottomNavigationView>(R.id.nav_view)
        val navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main)

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home,
                R.id.navigation_dashboard,
                R.id.navigation_notifications
            )
        )

        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)
        NavigationUI.setupWithNavController(navView, navController)
    }

    private fun singInAnonymously() {
        mAuth.signInAnonymously()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user: FirebaseUser? = mAuth.currentUser
                    user?.let {
                        lifecycleScope.launch {
                            permissionPreferences.saveUserId(
                                this@MainActivity,
                                PermissionPreferences.PermissionPreferencesKeys.USER_ID, it.uid
                            )
                            Log.d("MainActivity", "SingInAnonymously:success, user ID:  ${it.uid}")
                        }

                    }
                } else {
                    Log.w("MainActivity", "SingInAnonymously:failure", task.exception)
                    Toast.makeText(this@MainActivity, "Authentication failed", Toast.LENGTH_LONG)
                        .show()
                }
            }
    }
}
