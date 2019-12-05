package com.project100pi.pivideoplayer.ui.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.project100pi.pivideoplayer.R
import com.project100pi.pivideoplayer.utils.Constants

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        handlePermissionRequest()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            Constants.Permission.WRITE_PERMISSION_REQUEST_CODE -> {

                // Checking in results in case user has denied the permission
                for (res in grantResults) {
                    if (res == PackageManager.PERMISSION_DENIED) {
                        if (!shouldShowRationaleForStoragePermission()) {
                            // If user has denied the rationale permission
                            // then we have to navigate user to settings page.
                            showAlertToGrantPermissionViaSettings()
                        } else {
                            // if denied then ask for permission again.
                            showRequestPermissionRationaleDialog()
                        }
                        return
                    }
                }

                // Control will come here only when none permission is denied
                for ((i, _) in permissions.withIndex()) {
                    // checking if WRITE_EXTERNAL_STORAGE permission is granted or not
                    // if yes the navigate user to DirectoryListActivity activity.
                    if (permissions[i] == Manifest.permission.WRITE_EXTERNAL_STORAGE && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        launchDirectoryListActivity()
                    }
                }
            }
        }
    }

    /**
     * This method checks whether permissions is required or not
     * if it is required then checks in case user has already denied the permission
     * if user has already denied the permission the it will show a dialog
     */
    private fun handlePermissionRequest() {
        if (!isStoragePermissionGiven()) {
            if (shouldShowRationaleForStoragePermission()) {
                showRequestPermissionRationaleDialog()
            } else {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    Constants.Permission.WRITE_PERMISSION_REQUEST_CODE)
            }
        } else {
            launchDirectoryListActivity()
        }
    }

    /**
     * This method starts DirectoryListActivity with a delay of 1.5 secs
     */

    private fun launchDirectoryListActivity() {
        Handler().postDelayed(
            {
                DirectoryListActivity.start(this)
                finish()
            },
            1500)
    }

    /**
     * This method tells whether the user has denied the rationale permission
     * and now permission can be enabled from settings page only.
     * @return Boolean
     */

    private fun shouldShowRationaleForStoragePermission(): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    /**
     * This method shows an alert dialog which tells user that
     * why permissions is required for this app.
     * Alert dialog records two user responses.
     * YES -> Ask for permission
     * NO  -> Show a toast msg and finish the activity
     */

    private fun showRequestPermissionRationaleDialog() {
        AlertDialog.Builder(this)
            .setMessage(R.string.grant_storage_permission)
            .setPositiveButton(android.R.string.yes) { _, _ ->
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    Constants.Permission.WRITE_PERMISSION_REQUEST_CODE)
            }
            .setNegativeButton(android.R.string.no) { _, _ ->
                Toast.makeText(
                    this,
                    R.string.grant_permission_toast,
                    Toast.LENGTH_SHORT
                )
                    .show()
                finish()
            }
            .setCancelable(false)
            .show()
    }

    /**
     * This method shows an alert dialog which tells user that now user has to
     * go to settings page to enable the permission.
     * Alert dialog records two user responses.
     * YES -> Navigate to settings page
     * NO  -> Show a toast msg and finish the activity
     */

    private fun showAlertToGrantPermissionViaSettings() {
        AlertDialog.Builder(this)
            .setMessage(getString(R.string.permission_settings_msg))
            .setPositiveButton(android.R.string.yes) { _, _->
                navigateToAppSettings()
            }
            .setNegativeButton(android.R.string.no) { _, _->
                Toast.makeText(
                    this,
                    R.string.grant_permission_toast,
                    Toast.LENGTH_SHORT
                )
                    .show()
                finish()
            }
            .create()
            .show()
    }

    /**
     * This method navigates to application settings page.
     * when user has denied permission permanently then user has to
     * go to settings page to enable the permissions.
     */

    private fun navigateToAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        Toast.makeText(this, R.string.go_into_permissions, Toast.LENGTH_LONG)
            .show()
        finish()
    }

    /**
     * This method check whether permission is already given or not.
     * if permission is granted then it will return true otherwise false.
     * It also checks if android version is below Marshmallow then
     * there no need to ask for permission and hence return true.
     * @return Boolean
     */

    private fun isStoragePermissionGiven(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }
}
