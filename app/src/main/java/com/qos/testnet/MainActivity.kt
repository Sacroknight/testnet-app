package com.qos.testnet

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.Navigation
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.qos.myapplication.R
import com.qos.myapplication.databinding.ActivityMainBinding
import com.qos.testnet.permissionmanager.PermissionPreferences
import com.qos.testnet.permissionmanager.RequestPermissions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private var binding: ActivityMainBinding? = null
    private var requestPermissions: RequestPermissions? = null
    private val permissionPreferences: PermissionPreferences by lazy { PermissionPreferences.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

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
}
