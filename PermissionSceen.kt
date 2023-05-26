package com.PdfCreator.Editor.ui

import PdfCreator.Editor.R
import PdfCreator.Editor.databinding.ActivityPermissionSceenBinding
import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.PdfCreator.Editor.ads.AdInterstitialGoogle
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener

class PermissionSceen : AppCompatActivity(), View.OnClickListener {

    lateinit var viewBinding: ActivityPermissionSceenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewBinding = ActivityPermissionSceenBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        uiViews()
        loadLInterstitialAd()
    }

    private fun uiViews() {
        viewBinding.grantPermisison.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {

            R.id.grantPermisison -> {
                requestStoragePermission()
            }
        }
    }

    private fun requestStoragePermission() {
        Dexter.withActivity(this)
            .withPermissions(
                Manifest.permission.CAMERA
            )
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    // check if all permissions are granted
                    if (report.areAllPermissionsGranted()) {
                       /* Toast.makeText(
                            applicationContext,
                            "All permissions are granted!",
                            Toast.LENGTH_SHORT
                        ).show()*/
                        val intent = Intent(this@PermissionSceen, HomeScreen::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                        startActivity(intent)
                        AdInterstitialGoogle.getInstance().showInterstitialAdNew(this@PermissionSceen)
                        finish()

                    }

                    // check for permanent denial of any permission
                    if (report.isAnyPermissionPermanentlyDenied) {
                        // show alert dialog navigating to Settings
                        openSettingsDialog()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: List<PermissionRequest>,
                    token: PermissionToken
                ) {
                    token.continuePermissionRequest()
                }
            }).withErrorListener {
                Toast.makeText(
                    applicationContext,
                    "Error occurred! ",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .onSameThread()
            .check()
    }

    private fun openSettingsDialog() {
        val builder = AlertDialog.Builder(this, R.style.AlertDialogTheme)
        builder.setTitle("Required Permissions")
        builder.setMessage("This app need Camera and Gallery to use awesome features. Grant it in app settings.")
        builder.setPositiveButton("Take Me To SETTINGS") { dialog, which ->
            dialog.cancel()
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri: Uri = Uri.fromParts("package", packageName, null)
            intent.data = uri
            startActivityForResult(intent, 101)
        }
        builder.setNegativeButton("Cancel") { dialog, which -> dialog.cancel() }
        builder.show()
    }


    override fun onResume() {

        if (checkPermission()) {
            val intent = Intent(this, HomeScreen::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            startActivity(intent)
            finish()
        } else {

        }
        super.onResume() // checkPermission()

    }

    private fun checkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun loadLInterstitialAd() {
        AdInterstitialGoogle.getInstance().loadInterstitialAd(this)
    }

}