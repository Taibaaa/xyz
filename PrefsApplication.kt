package com.PdfCreator.Editor

import android.app.Application
import android.content.ContextWrapper
import com.PdfCreator.Editor.ads.OpenApp_ads
import com.google.android.gms.ads.MobileAds
import com.pixplicity.easyprefs.library.Prefs

open class PrefsApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize the Prefs class
        Prefs.Builder()
            .setContext(this)
            .setMode(ContextWrapper.MODE_PRIVATE)
            .setPrefsName(packageName)
            .setUseDefaultSharedPreference(true)
            .build()

        //for open App ads
       /*  MobileAds.initialize(this)
         OpenApp_ads(this)*/

        //for Applovin ads
        /*AudienceNetworkAds.initialize(this)
        AppLovinSdk.getInstance( this ).setMediationProvider( "max" )

        AppLovinSdk.getInstance( this ).initializeSdk({ configuration: AppLovinSdkConfiguration ->
            // AppLovin SDK is initialized, start loading ads
        })
        *//*11 usama device*//*
        //  AppLovinSdk.getInstance(this).settings.testDeviceAdvertisingIds = arrayListOf("3150311b-06c8-41c1-adc2-21082038149e")

        *//*    Sir 12*//*
        //  AppLovinSdk.getInstance(this).settings.testDeviceAdvertisingIds = arrayListOf("358db0e7-ef27-4ab8-b02e-692cc8b193ca")

        *//* Tayyaba device 11 infinix*//*
             AppLovinSdk.getInstance(this).settings.testDeviceAdvertisingIds = arrayListOf("b203a681-869b-4699-bf00-98c39808612e")*/
    }

}