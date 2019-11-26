package com.project100pi.pivideoplayer.ui.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.project100pi.pivideoplayer.R
import com.project100pi.pivideoplayer.utils.Constants
import com.project100pi.pivideoplayer.utils.PermissionsUtil

class SplashActivity : AppCompatActivity(), PermissionsUtil.ShowAlertCallback {

    private lateinit var permissionUtil: PermissionsUtil
    private val permission =  Manifest.permission.WRITE_EXTERNAL_STORAGE
    private var granted: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        permissionUtil = PermissionsUtil(this, this)
    }

    override fun onStart() {
        super.onStart()
        if (!granted)
            permissionUtil.checkorRequestPermission(permission)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            Constants.PERMISSION_REQUEST_CODE -> {
                permissionUtil.checkorRequestPermission(permission = permissions[0])
            }
        }
    }

    override fun showAlertToAskPermissionRationale() {
        requestPermission()
    }

    override fun showAlert(isPermissionsRationale: Boolean) {
        AlertDialog.Builder(this)
            .setMessage("Some permissions are needed for Pi Video Player to go further. Do you want to grant those permissions?")
            .setPositiveButton(android.R.string.yes) { _, _ ->
                if (isPermissionsRationale) {
                    requestPermission()
                } else {
                    if (!granted)
                        permissionUtil.checkorRequestPermission(permission)
                }
            }
            .setNegativeButton(android.R.string.no) { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    override fun permissionGranted() {
        Handler().postDelayed( {
            DirectoryListActivity.start(context = this)
            finish()
        }, 1500)
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(this,
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE),
            Constants.PERMISSION_REQUEST_CODE)
    }
}
