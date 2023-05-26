package com.PdfCreator.Editor.ui

import PdfCreator.Editor.databinding.ActivitySplashScreenBinding
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.PdfCreator.Editor.ads.AdInterstitialGoogle
import com.PdfCreator.Editor.ads.AdsNativeGoogle

class SplashScreen : AppCompatActivity() {

    private val SPLASH_DELAY: Long = 6500 //7 seconds
    private lateinit var Viewbinding: ActivitySplashScreenBinding
    private var nativeAd: AdsNativeGoogle? = null

    companion object{
        var ads:Boolean=false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Viewbinding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(Viewbinding.root)

        //for changing colors in dark mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        ads = false
        loadLNativeAd()


        Viewbinding.btnLetsGo.visibility = View.INVISIBLE
        Handler(Looper.getMainLooper()!!).postDelayed({

            if (Viewbinding.btnLetsGo.visibility == View.INVISIBLE) {
                Viewbinding.btnLetsGo.visibility = View.VISIBLE
            } else {
                Viewbinding.btnLetsGo.visibility = View.INVISIBLE
            }
            Viewbinding.progressBar.visibility = View.INVISIBLE
        }, SPLASH_DELAY)

        Viewbinding.btnLetsGo.setOnClickListener {
            if (checkPermission()) {
                val intent = Intent(this, HomeScreen::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                startActivity(intent)
                AdInterstitialGoogle.getInstance().showInterstitialAdNew(this)
                finish()
            } else {
                val intent = Intent(this, PermissionSceen::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                startActivity(intent)
                finish()
            }
        }

    }

    private fun loadLNativeAd() {
        AdInterstitialGoogle.getInstance().loadInterstitialAd(this)
        nativeAd = AdsNativeGoogle()
        nativeAd?.adLargeGoogle(this, Viewbinding.nativeAdContainerShimmer,Viewbinding.nativeAdContainer)
    }


    private fun checkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

}