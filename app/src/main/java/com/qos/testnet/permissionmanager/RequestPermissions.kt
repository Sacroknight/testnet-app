package com.qos.testnet.permissionmanager

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.qos.testnet.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RequestPermissions(private val context: Context) {
    private val permissionPreferences: PermissionPreferences by lazy { PermissionPreferences.getInstance() }

    fun hasLocationPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasReadPhonePermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_BASIC_PHONE_STATE
                    ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasNetworkStatePermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_NETWORK_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasWriteStoragePermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasReadStoragePermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun requestWriteStoragePermissions() {
        ActivityCompat.requestPermissions(
            context as Activity,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            REQUEST_WRITE_STORAGE_PERMISSION
        )
    }

    fun requestReadStoragePermissions() {
        ActivityCompat.requestPermissions(
            context as Activity,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            REQUEST_READ_STORAGE_PERMISSION
        )
    }

    fun hasAllNecessaryPermissions(): Boolean {
        return hasLocationPermissions() &&
                hasReadPhonePermissions() &&
                hasReadStoragePermissions() &&
                hasWriteStoragePermissions() &&
                hasNetworkStatePermissions()
    }

    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(
            context as Activity, arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ), REQUEST_LOCATION_PERMISSION
        )
    }

    fun requestReadPhonePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                context as Activity, arrayOf(
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.READ_BASIC_PHONE_STATE
                ), REQUEST_READ_PHONE_PERMISSION
            )
        } else {
            ActivityCompat.requestPermissions(
                context as Activity, arrayOf(
                    Manifest.permission.READ_PHONE_STATE
                ), REQUEST_READ_PHONE_PERMISSION
            )
        }
    }

    fun requestAccessNetworkStatePermission(){
        ActivityCompat.requestPermissions(
            context as Activity,
            arrayOf(Manifest.permission.ACCESS_NETWORK_STATE),
            REQUEST_ACCESS_NETWORK_STATE
        )
    }
    fun requestLocationPermissionsDialog() {
        val dontAskAgain = booleanArrayOf(false)

        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.request_Location))
            .setMessage(context.getString(R.string.request_Location_Dialog))
            .setMultiChoiceItems(
                arrayOf(context.getString(R.string.dont_Ask_Again)),
                dontAskAgain
            ) { _, _, isChecked ->
                dontAskAgain[0] = isChecked
            }
            .setPositiveButton(context.getString(R.string.grant_Permission)) { _, _ ->
                CoroutineScope(Dispatchers.IO).launch {
                    permissionPreferences.savePermissionPreference(
                        context,
                        PermissionPreferences.PermissionPreferencesKeys.DONT_ASK_AGAIN_LOCATION_PERMISSION,
                        dontAskAgain[0]
                    )
                }
                requestLocationPermissions()
            }
            .setNegativeButton(context.getString(R.string.continue_Without_Permission)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    fun requestAllPermissionsDialog() {
        AlertDialog.Builder(context).setTitle(context.getString(R.string.permissions_Needed))
            .setMessage(context.getString(R.string.permissions_Needed_Dialog))
            .setPositiveButton(context.getString(R.string.grant_Permission)) { _, _ ->
                requestLocationPermissions()
                requestReadPhonePermissions()
                requestAccessNetworkStatePermission()
                requestReadStoragePermissions()
                requestWriteStoragePermissions()
            }
            .setNegativeButton(context.getString(R.string.continue_Without_Permission)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    fun showPermissionDeniedWarning() {
        val dontAskAgain = booleanArrayOf(false)

        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.permissions_Needed))
            .setMessage(context.getString(R.string.permissions_Needed_Dialog))
            .setMultiChoiceItems(
                arrayOf(context.getString(R.string.dont_Ask_Again)),
                dontAskAgain
            ) { _, _, isChecked ->
                dontAskAgain[0] = isChecked
            }
            .setPositiveButton(context.getString(R.string.continue_Without_Permission)) { dialog, _ ->
                CoroutineScope(Dispatchers.IO).launch {
                    permissionPreferences.savePermissionPreference(
                        context,
                        PermissionPreferences.PermissionPreferencesKeys.DONT_ASK_AGAIN_LOCATION_PERMISSION,
                        dontAskAgain[0]
                    )
                    permissionPreferences.savePermissionPreference(
                        context,
                        PermissionPreferences.PermissionPreferencesKeys.DONT_ASK_AGAIN_PHONE_PERMISSION,
                        dontAskAgain[0]
                    )
                }
                dialog.dismiss()
            }
            .setNegativeButton(context.getString(R.string.grant_Permission)) { dialog, _ ->
                dialog.dismiss()
                requestLocationPermissions()
                requestReadPhonePermissions()
            }
            .show()
    }

    companion object {
        const val REQUEST_LOCATION_PERMISSION: Int = 69
        const val REQUEST_READ_PHONE_PERMISSION: Int = 88
        const val REQUEST_ACCESS_NETWORK_STATE: Int = 89
        const val REQUEST_WRITE_STORAGE_PERMISSION: Int = 90
        const val REQUEST_READ_STORAGE_PERMISSION: Int = 91
    }
}