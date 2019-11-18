package com.project100pi.pivideoplayer.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale

/**
 * Created by kartikeysrivastava on 2019-08-30
 */

/*
 * Class that checks whether runtime permission are required are not
 * U must implement the show alert callback in your activity where u want to check
 * 1- If runtime permission are not required or app has already granted permission
 *    the app must allow user to continue operations. permissionGranted() method of ShowAlertCallback will be called
 *     U can let the user continue his operations
 * 2- Say if app hasn't allowed permission then showAlert() of ShowAlertCallback will be called and their u should continue your permission request operation
 *
 */
class PermissionsUtil(private var activity: Activity, private val showAlertCallback: ShowAlertCallback?) {
    var count = 0
    private var beforeClickPermissionRat = shouldShowRequestPermissionRationale(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)

    private fun shouldAskPermission(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    }

    private fun shouldAskPermission(context: Context, permission: String): Boolean {
        if (shouldAskPermission()) {
            val permissionResult = ActivityCompat.checkSelfPermission(context, permission)
            if (permissionResult != PackageManager.PERMISSION_GRANTED) {
                return true
            }
        }
        return false
    }

    /*
     * Method to be called whenever user wants to check permission status
     * 1- When user has opened the particular activity or fragment where the check is required
     * 2- When user has updated permission status say requestpermission was invoked from the activity or fragment . When onRequestPermissionResult() is called again u can invoked
     *    checkorRequestPermission() again to know the new status
     */
    fun checkorRequestPermission(permission: String) {
        if(shouldAskPermission(activity,permission)) {
            //Check if count = 2 and permission is still not granted finish the app since app can't continue without storage permission
            if(count == 2){
                Toast.makeText(activity, "Since permission was not granted app wont be able to continue.", Toast.LENGTH_SHORT).show()
                activity.finish()
            }
            //If count = 1 two cases can occur
            //1 - Dont ask again was not clicked or its clicked this time -> In this case just finish the app
            //2 - Don't ask again was checked previous time itself when the app was opened and in this case direct the user to the settings page.

            if (count == 1) {
                if ((!shouldShowRequestPermissionRationale(activity,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)) && (!(beforeClickPermissionRat))) {
                    count = 2
                    Toast.makeText(activity, "Click on permission and check storage permission for app to continue.", Toast.LENGTH_SHORT).show()

                    activity.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + activity.packageName))
                    )
                } else {
                    activity.finish()
                }
            }
            //If count = 0 and app hasn't been granted permission yet the app must show an alert to inform user about the permission info.
            if (count == 0) {
                count = 1
                showAlertCallback?.showAlert()
            }

        }
        else{
            //If permission is not required that is either phone sdk version < Marshmallow or permission has been already granted in this case allow the user directly to enter the app
            showAlertCallback?.permissionGranted()
        }
    }

    /*
     * Interface to manage callbacks for permissions.
     * Implement this interface wherever u need to have a check for permission.
     * It is advised to have it in viewmodel so that data can be retained even after configurtion changes
     * Implement the callbacks and use the data as per your use case
     *
     */
    interface ShowAlertCallback{
        fun showAlert()
        fun permissionGranted()
    }

    /*
     * To update the activity instance since on configuration change the activity instance will be created as new
     */
    fun updateActivity(activity: Activity){
        this.activity = activity
    }

}