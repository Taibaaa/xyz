package com.PdfCreator.Editor.ui

import PdfCreator.Editor.R
import PdfCreator.Editor.databinding.ActivityPdfImagesBinding
import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.PdfCreator.Editor.adapter.PdfImagesAdapter
import com.PdfCreator.Editor.ads.AdInterstitialGoogle
import com.PdfCreator.Editor.history.HistoryModel
import com.PdfCreator.Editor.history.HistoryViewModel
import com.PdfCreator.Editor.roomdbfiles.FilesModel
import com.PdfCreator.Editor.roomdbfiles.FilesViewModel
import com.PdfCreator.Editor.utils.FileExternalPath.copyUriToExternalFilesDir
import com.PdfCreator.Editor.utils.FileExternalPath.copyUriToExternalFilesDir1
import com.itextpdf.text.Document
import com.itextpdf.text.Image
import com.itextpdf.text.Rectangle
import com.itextpdf.text.pdf.PdfWriter
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*


class PdfImages : AppCompatActivity(), PdfImagesAdapter.ItemClickInterface {

    lateinit var viewBinding: ActivityPdfImagesBinding
    lateinit var viewModel: FilesViewModel
    lateinit var viewModelHistory: HistoryViewModel
    private var pdfAdapter: PdfImagesAdapter? = null
    var imageId: String? = null
    var pdfFilename: String? = null
    var folderName: String? = null
    var folderId: Long? = null
    var myImagesUris = ArrayList<String>()
    private var mTempArry: String? = null

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewBinding = ActivityPdfImagesBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        loadLInterstitialAd()
        loadImagesFromDatabase()
        toolbarFunctions()

        viewBinding.myTopBar.ivBack.setOnClickListener {
           onBackPressed()
        }

    }



    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun toolbarFunctions() {
        val savePdfBtn: ImageView = findViewById(R.id.createPdf)
        val shareImageBtn: ImageView = findViewById(R.id.shareImage)
        val tvShareBtn: TextView = findViewById(R.id.tvShareImage)
        savePdfBtn.visibility = View.VISIBLE
        shareImageBtn.visibility = View.GONE
        tvShareBtn.visibility = View.GONE

        savePdfBtn.setOnClickListener {
            createPdfDialog()
        }
    }


    private fun loadImagesFromDatabase() {
        //initializing Viewmodel Files
        viewModel = ViewModelProvider(
            this, ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(FilesViewModel::class.java)

        //initializing Viewmodel History
        viewModelHistory = ViewModelProvider(
            this, ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(HistoryViewModel::class.java)


        val folderTextName: TextView = findViewById(R.id.folderTitleName)
        folderName = intent.getStringExtra("folderName")
        folderTextName.setText(folderName)

        val ChatRV: RecyclerView = findViewById(R.id.rvShowPdfImages)
        ChatRV.layoutManager = LinearLayoutManager(this)
        pdfAdapter = PdfImagesAdapter(this, this)

        imageId = intent.getStringExtra("folderId")
    /*    folderId = imageId!!.toInt()*/
        folderId = imageId!!.toLong()
        ChatRV.adapter = pdfAdapter

        viewModel.repository.getAllImages(folderId).observe(this, Observer { list ->
            list?.let {
                if (list.isEmpty()) {
                    //Toast.makeText(this, "empty", Toast.LENGTH_SHORT).show()
                    viewBinding.noDataAnimation.visibility = View.VISIBLE
                    viewBinding.tvNoData.visibility = View.VISIBLE


                } else {
                    viewBinding.noDataAnimation.visibility = View.GONE
                    viewBinding.tvNoData.visibility = View.GONE
                }
                pdfAdapter!!.updateList(it)
                pdfAdapter?.notifyDataSetChanged()

                it.forEach {
                    mTempArry = it.selectedImagesUri
                    myImagesUris.add(mTempArry!!)
                    // Toast.makeText(this, "empty " + mTempArry +"  my images uri  "+myImagesUris, Toast.LENGTH_SHORT).show()
                }

            }

        })

    }

    override fun onSingleClick(filesModel: FilesModel, position: Int) {

    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun createPdfDialog() {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setView(R.layout.dialog_pdf_filename)
        val alertDialog = dialogBuilder.create()
        alertDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        alertDialog.show()
        alertDialog.setCancelable(false)

        val cancelBtn = alertDialog.findViewById(R.id.noBtn) as TextView
        val okBtn = alertDialog.findViewById(R.id.yesBtn) as TextView
        val tvFolderName = alertDialog.findViewById(R.id.etFileName) as EditText

        cancelBtn.setOnClickListener {
            alertDialog.dismiss()
        }

        okBtn.setOnClickListener {
            pdfFilename = tvFolderName.text.toString().trim()

            val sd = cacheDir
            if (pdfFilename!!.isNotEmpty()) {
                var img: Image = Image.getInstance(myImagesUris.get(0))
                val document = Document(img)

                val dir = File(sd,"/DocumentScannerApp/")
                dir.mkdirs()

                val file = File(dir, pdfFilename + ".pdf")
                var fileTosave = Uri.fromFile(file)
               // fileTosave.path

                copyUriToExternalFilesDir1(fileTosave)

                if (!file.exists()) {
                    val fOut = FileOutputStream(file)
                    val currentDate =
                        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

                    lifecycleScope.launch {
                        viewModelHistory.addPdfFiles(
                            HistoryModel(
                                currentDate,
                                file.path,
                                pdfFilename!!
                            )
                        )
                        Log.i("INSERT_ID", "Inserted ID is: ${viewModel.insertedId}")

                    }

                    PdfWriter.getInstance(document, fOut)
                    document.open()
                    for (image in myImagesUris) {
                        img = Image.getInstance(image)
                        /*img.setBorder(Rectangle.BOX)
                        img.setBorderWidth(5f)*/
                        val documentRect = document.pageSize
                        img.setAbsolutePosition(
                            (documentRect.getWidth() - img.getScaledWidth()) / 5,
                            (documentRect.getHeight() - img.getScaledHeight()) / 5
                        )
                        document.add(img)
                        document.newPage()
                    }
                    document.close()

                    /* Toast.makeText(this, "Pdf Created. Open from Documents", Toast.LENGTH_SHORT)
                         .show()*/
                    alertDialog.dismiss()
                    val intent = Intent(this, PdfFilesHistory::class.java)
                    intent.putExtra("folderId",imageId)
                    intent.putExtra("folderName",folderName)

                    Log.e("TAG33", "createPdfDialog: $imageId")
                    Log.e("TAG33", "createPdfDialog: $folderName")

                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    startActivity(intent)
                    AdInterstitialGoogle.getInstance().showInterstitialAdNew(this)
                } else {
                    Toast.makeText(this, "Filename Exists, Change Filename", Toast.LENGTH_SHORT)
                        .show()
                }

            } else {
                tvFolderName.setError("Filename Required")
            }
        }

    }

    private fun loadLInterstitialAd() {
        AdInterstitialGoogle.getInstance().loadInterstitialAd(this)
    }

    override fun onBackPressed() {
       /* if(imageId ==null)
        {
            val intent = Intent(this, HomeScreen::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            startActivity(intent)
        }*/
//        else{
            val intent = Intent(this, FolderFiles::class.java)
            intent.putExtra("folderId", imageId)
            intent.putExtra("folderName", folderName)
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            startActivity(intent)
       // }

        super.onBackPressed()
    }

}