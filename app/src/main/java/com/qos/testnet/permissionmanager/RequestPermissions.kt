package com.qos.testnet.permissionmanager

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.qos.myapplication.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RequestPermissions(private val context: Context) {
    private val permissionPreferences: PermissionPreferences by lazy { PermissionPreferences.getInstance() }

    fun hasLocationPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasReadPhonePermissions(): Boolean {
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

    fun hasAllNecessaryPermissions(): Boolean {
        return hasLocationPermissions() && hasReadPhonePermissions()
    }

    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(
            (context as Activity), arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ), REQUEST_LOCATION_PERMISSION
        )
    }

    private fun requestReadPhonePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                (context as Activity), arrayOf(
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.READ_BASIC_PHONE_STATE
                ), REQUEST_READ_PHONE_PERMISSION
            )
        } else {
            ActivityCompat.requestPermissions(
                (context as Activity),
                arrayOf(Manifest.permission.READ_PHONE_STATE),
                REQUEST_READ_PHONE_PERMISSION
            )
        }
    }

    fun requestLocationPermissionsDialog() {
        val toppings = arrayOf(context.getString(R.string.dont_Ask_Again))
        val checkedItems = 0
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.request_Location))
            .setMessage(context.getString(R.string.request_Location_Dialog))
            .setSingleChoiceItems(toppings, checkedItems) { _, which ->
                CoroutineScope(Dispatchers.IO).launch {
                    permissionPreferences.savePermissionPreference(
                        context,
                        PermissionPreferences.PermissionPreferencesKeys.DONT_ASK_AGAIN_LOCATION_PERMISSION,
                        which == 0
                    )
                }
            }
            .setPositiveButton(context.getString(R.string.grant_Permission)) { _, _ ->
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
                }
                .setNegativeButton(context.getString(R.string.continue_Without_Permission)) { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
    }
    fun showPermissionDeniedWarning() {
        val toppings = arrayOf(context.getString(R.string.dont_Ask_Again))
        val checkedItems = 0

        AlertDialog.Builder(context)
            .setTitle("Permission Warning")
            .setMessage(
                "Cell information retrieval requires location permission. " +
                        "Test results will have lower precision without it. " +
                        "The experiment can still be conducted."
            )
            .setSingleChoiceItems(toppings, checkedItems) { _, which ->
                CoroutineScope(Dispatchers.IO).launch {
                    val dontAskAgain = which == 0
                    permissionPreferences.savePermissionPreference(
                        context,
                        PermissionPreferences.PermissionPreferencesKeys.DONT_ASK_AGAIN_LOCATION_PERMISSION,
                        dontAskAgain
                    )
                    permissionPreferences.savePermissionPreference(
                        context,
                        PermissionPreferences.PermissionPreferencesKeys.DONT_ASK_AGAIN_PHONE_PERMISSION,
                        dontAskAgain
                    )
                }
            }
            .setPositiveButton("Continue") { dialog, _ ->
                dialog.dismiss()
            }
            .setNegativeButton("Grant Permissions") { dialog, _ ->
                dialog.dismiss()
                requestLocationPermissions()
                requestReadPhonePermissions()
            }
            .show()
    }

    companion object {
        const val REQUEST_LOCATION_PERMISSION: Int = 69
        const val REQUEST_READ_PHONE_PERMISSION: Int = 88
    }
}
