package com.PdfCreator.Editor.ui

import PdfCreator.Editor.R
import PdfCreator.Editor.databinding.ActivityAddSignatureBinding
import PdfCreator.Editor.databinding.ActivityDisplayClickedImageBinding
import android.Manifest
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.app.Dialog
import android.app.appsearch.AppSearchResult.RESULT_OK
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import com.PdfCreator.Editor.ads.AdInterstitialGoogle
import com.PdfCreator.Editor.history.HistoryModel
import com.itextpdf.text.Document
import com.itextpdf.text.Image
import com.itextpdf.text.Rectangle
import com.itextpdf.text.pdf.PdfWriter
import com.jaredrummler.android.colorpicker.ColorPickerDialog
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class AddSignature : AppCompatActivity(), ColorPickerDialogListener {

    private var selectedColor: Int? = null
    lateinit var viewBinding: ActivityAddSignatureBinding
    private var folderName: String? = null
    private var seekbarPenSize: Int? = null
    var sharedPreferences: SharedPreferences? = null
    var editor: SharedPreferences.Editor? = null
    var sentUri: String? = null
    var mFlag2: Boolean = false
    var imageId1: String? = null
    var backPressedId1: String? = null
    var byteArray: ByteArray? = null
    var getEditing: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityAddSignatureBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        multipleOperations()
        loadLInterstitialAd()
        loadSeekbarProgress()

        viewBinding.myToolbar.ivBack.setOnClickListener {
            onBackPressed()

        }


        viewBinding.tabs.constCancel.setOnClickListener {
            viewBinding.tabs.ivColor.setBackgroundResource(0)
            viewBinding.tabs.ivUndo.setBackgroundResource(0)
            viewBinding.tabs.ivRedo.setBackgroundResource(0)
            viewBinding.tabs.ivSave.setBackgroundResource(0)
            viewBinding.tabs.ivHome.setBackgroundResource(R.drawable.selection_sign)

            if (viewBinding.drawView.isDrawing() > 0) {
                deleteConfirmationDialog()
            } else {
                Toast.makeText(this, "There is nothing to clear", Toast.LENGTH_SHORT).show()
            }
        }


        viewBinding.tabs.consSave.setOnClickListener {

            viewBinding.tabs.ivColor.setBackgroundResource(0)
            viewBinding.tabs.ivUndo.setBackgroundResource(0)
            viewBinding.tabs.ivRedo.setBackgroundResource(0)
            viewBinding.tabs.ivHome.setBackgroundResource(0)
            viewBinding.tabs.ivSave.setBackgroundResource(R.drawable.selection_sign)
            getEditing = true
            getByteArray()

            val data = Intent(this, DisplayClickedImageActivity::class.java)
            data.putExtra("SignBitmap", byteArray)
            data.putExtra("clickedImageUri", sentUri)
            data.putExtra("flag2", mFlag2)
            data.putExtra("id_image", imageId1!!.toInt())
            data.putExtra("foldersId", backPressedId1)
            data.putExtra("folderName", folderName)
            data.putExtra("editing", getEditing)

            Log.e("TAG", "onCreate: putSign $getEditing")

            startActivityForResult(data, 333)
            //  AdInterstitialGoogle.getInstance().showInterstitialAdNew(this)

        }

        viewBinding.tabs.layoutColor.setOnClickListener {
            viewBinding.tabs.ivColor.setBackgroundResource(R.drawable.selection_sign)
            viewBinding.tabs.ivUndo.setBackgroundResource(0)
            viewBinding.tabs.ivRedo.setBackgroundResource(0)
            viewBinding.tabs.ivSave.setBackgroundResource(0)
            viewBinding.tabs.ivHome.setBackgroundResource(0)

            getEditing = true
            ColorPickerDialog.newBuilder()
                .setColor(selectedColor!!)
                .show(this)
        }

        viewBinding.tabs.layoutUndo.setOnClickListener {
            viewBinding.tabs.ivColor.setBackgroundResource(0)
            viewBinding.tabs.ivRedo.setBackgroundResource(0)
            viewBinding.tabs.ivUndo.setBackgroundResource(R.drawable.selection_sign)
            viewBinding.tabs.ivSave.setBackgroundResource(0)
            viewBinding.tabs.ivHome.setBackgroundResource(0)


            getEditing = true
            viewBinding.drawView.undo()
        }


        viewBinding.tabs.layoutRedo.setOnClickListener {
            viewBinding.tabs.ivColor.setBackgroundResource(0)
            viewBinding.tabs.ivUndo.setBackgroundResource(0)
            viewBinding.tabs.ivRedo.setBackgroundResource(R.drawable.selection_sign)
            viewBinding.tabs.ivSave.setBackgroundResource(0)
            viewBinding.tabs.ivHome.setBackgroundResource(0)


            getEditing = true
            viewBinding.drawView.redo()
        }

        viewBinding.tabs.sbPenSize.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                getEditing = true
                viewBinding.drawView.setStrokeWidth(progress.toFloat())
                //  saveData()
                val sharedPreferences = getSharedPreferences("my_preference", MODE_PRIVATE)
                val editor = sharedPreferences.edit()
                editor.putInt("seekbarpenSize", progress)

                editor.apply()

            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

    }

    private fun getByteArray() {

        viewBinding.drawView.buildDrawingCache()
        val signBitmap: Bitmap = viewBinding.drawView.getDrawingCache()
        val bStream = ByteArrayOutputStream()
        signBitmap.compress(Bitmap.CompressFormat.PNG, 100, bStream)
        byteArray = bStream.toByteArray()
    }

    private fun multipleOperations() {


        imageId1 = intent.getStringExtra("id_image")
        backPressedId1 = intent.getStringExtra("foldersId")
        Log.e("TAG", "onCreate: signoncreate $backPressedId1")

        getEditing = intent.getBooleanExtra("editing", false)
        Log.e("TAG", "oncreate: get $getEditing")

        sharedPreferences = getSharedPreferences("DrawPref", MODE_PRIVATE)
        editor = sharedPreferences?.edit()
        selectedColor = sharedPreferences?.getInt("Color", Color.RED)
        viewBinding.drawView.setColor(selectedColor!!)
        var toolbarHeading: TextView = findViewById(R.id.toolbar_title)

        folderName = intent.getStringExtra("folderName")
        toolbarHeading.setText(folderName)
        sentUri = intent.getStringExtra("clickedImageUri")
        mFlag2 = intent.getBooleanExtra("flag2", false)
    }

    fun loadSeekbarProgress() {
        val sharedPreferences = getSharedPreferences("my_preference", MODE_PRIVATE)
        seekbarPenSize = sharedPreferences.getInt("seekbarpenSize", 50)
        viewBinding.tabs.sbPenSize.setProgress(seekbarPenSize!!)
        viewBinding.drawView.setStrokeWidth(seekbarPenSize!!.toFloat())
    }


    override fun onColorSelected(dialogId: Int, color: Int) {
        editor?.putInt("Color", color)?.commit()
        selectedColor = color
        viewBinding.drawView.setColor(selectedColor!!)

    }

    override fun onDialogDismissed(dialogId: Int) {

    }

    override fun startActivityForResult(intent: Intent?, requestCode: Int, options: Bundle?) {
        super.startActivityForResult(intent, requestCode, options)
        if (requestCode == 333) {
            //  Toast.makeText(this, "hello 333", Toast.LENGTH_SHORT).show()
        }
    }

    fun saveData() {
        val sharedPreferences = getSharedPreferences("my_preference", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt("seekbarpenSize", viewBinding.tabs.sbPenSize.getProgress())

        editor.apply()
    }

    private fun loadLInterstitialAd() {
        AdInterstitialGoogle.getInstance().loadInterstitialAd(this)
    }

    override fun onBackPressed() {

        val intent = Intent(this, DisplayClickedImageActivity::class.java)
        getByteArray()
        intent.putExtra("SignBitmap", byteArray)
        intent.putExtra("clickedImageUri", sentUri)
        intent.putExtra("flag2", mFlag2)
        intent.putExtra("id_image", imageId1!!.toInt())
        intent.putExtra("foldersId", backPressedId1)
        intent.putExtra("folderName", folderName)
        Log.e("TAG", "onCreate: backpressed $backPressedId1")
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        startActivity(intent)

        super.onBackPressed()
    }

    private fun deleteConfirmationDialog() {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setView(R.layout.dialog_discard_changes)
        val alertDialog = dialogBuilder.create()
        alertDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        alertDialog.show()
        alertDialog.setCancelable(false)

        val cancelBtn = alertDialog.findViewById(R.id.noBtn) as TextView
        val okBtn = alertDialog.findViewById(R.id.yesBtn) as TextView

        cancelBtn.setOnClickListener {
            alertDialog.dismiss()
        }

        okBtn.setOnClickListener {
            viewBinding.drawView.clearCanvas()
            alertDialog.dismiss()
        }

    }

}


private val Int.toPx: Float
    get() = (this * Resources.getSystem().displayMetrics.density)
