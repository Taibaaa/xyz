package com.PdfCreator.Editor.ui

import PdfCreator.Editor.R
import PdfCreator.Editor.databinding.ActivityPdfFilesHistoryBinding
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.*
import android.os.StrictMode.VmPolicy
import android.util.Log
import android.view.View
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.PdfCreator.Editor.adapter.HistoryAdapter
import com.PdfCreator.Editor.adapter.PdfClickInterface
import com.PdfCreator.Editor.adapter.PdfLongClickInterface
import com.PdfCreator.Editor.ads.AdInterstitialGoogle
import com.PdfCreator.Editor.ads.AdsNativeGoogle
import com.PdfCreator.Editor.history.HistoryModel
import com.PdfCreator.Editor.history.HistoryViewModel
import com.PdfCreator.Editor.ui.SplashScreen.Companion.ads
import com.PdfCreator.Editor.utils.FileExternalPath.copyUriToExternalFilesDir
import com.airbnb.lottie.LottieAnimationView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.itextpdf.text.pdf.PdfImage
import com.pixplicity.easyprefs.library.Prefs
import kotlinx.android.synthetic.main.toolbar_main.*
import java.io.File


class PdfFilesHistory : AppCompatActivity(), View.OnClickListener, PdfClickInterface,
    PdfLongClickInterface {

    lateinit var viewBinding: ActivityPdfFilesHistoryBinding
    lateinit var viewModel: HistoryViewModel
    var pdfAdapter: HistoryAdapter? = null
    var fileList: MutableList<String?>? = ArrayList()
    var deleteCount = 0
    private var nativeAd: AdsNativeGoogle? = null
    var folderName: String? = null
    var folderId: Long? = null
    var imageId: String? = null
    var flagOfBack: Boolean? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewBinding = ActivityPdfFilesHistoryBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        ads = true
        loadLInterstitialAd()
        loadLNativeAd()
        LoadPdfFiles()
        uiViews()

    }

    private fun uiViews() {
        viewBinding.tabs.layoutHome.setOnClickListener(this)
        viewBinding.tabs.layoutHistory.setOnClickListener(this)
        viewBinding.myTopBar.toolbarIconMenu.setOnClickListener(this)
        viewBinding.myTopBar.ivBack.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.layoutHome -> {

                val intent = Intent(this, HomeScreen::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                startActivity(intent)

            }
            R.id.ivBack -> {
                onBackPressed()
            }
            R.id.layoutHistory -> {
                val intent = Intent(this, PdfFilesHistory::class.java)
                intent.putExtra("folderId", imageId)
                intent.putExtra("folderName", folderName)
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                startActivity(intent)
            }
            R.id.toolbarIconMenu -> {
                multipleControl(viewBinding.myTopBar.toolbarIconMenu)
                val popupMenu = PopupMenu(this, toolbarIconMenu)
                popupMenu.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.deletePdfs -> {
                            if (Prefs.contains("checkedList")) {
                                getSelectedList("checkedList")
                            } else {
                                Toast.makeText(
                                    this,
                                    "Select Item to Delete",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            true
                        }

                        R.id.sharePdfs -> {
                            if (Prefs.contains("checkedList")) {
                                val string: String = Prefs.getString("checkedList", null)
                                val type = object : TypeToken<ArrayList<HistoryModel?>?>() {}.type
                                val selectedList =
                                    Gson().fromJson(string, type) as ArrayList<HistoryModel>

                                //   fileList!!.clear()
                                selectedList.forEach {
                                    if (it.selected) {
                                        // val file = File(it.filePath)
                                        fileList!!.add(it.filePath)

                                        Prefs.clear()

                                    }

                                }
                                share(this, fileList)
                                fileList!!.clear()
                            } else {
                                Toast.makeText(this, "Select Files to share", Toast.LENGTH_SHORT)
                                    .show()
                            }

                            //  shareMultiple(fileList!!, this)
                            true
                        }
                        else -> false
                    }

                }
                /*  val gson = Gson()
                  Type type = new TypeToken<List<ActivitiesModel>>() { }.getType();
                  String json = spreferenceManager.jsonString;
                  List<ActivitiesModel> arrPackageData = gson.fromJson(json, type);

                  val gson = Gson()
                  val json: String = Prefs.getString("checkedList", "0")
                  val obj: MyObject = gson.fromJson(json, MyObject::class.java)
  */

                popupMenu.inflate(R.menu.pdf_menu)

                try {
                    val fieldMPopup = PopupMenu::class.java.getDeclaredField("mPopup")
                    fieldMPopup.isAccessible = true
                    val mPopup = fieldMPopup.get(popupMenu)
                    mPopup.javaClass.getDeclaredMethod("setForceShowIcon", Boolean::class.java)
                        .invoke(mPopup, true)


                    // Change PopupMenu editChat subMenu title color
                    /* val itemSetAs = popupMenu.menu
                     val s = itemSetAs.findItem(R.id.editChat).subMenu
                     val headerTitle = SpannableString(itemSetAs.findItem(R.id.editChat).title)
                     headerTitle.setSpan(ForegroundColorSpan(Color.BLACK), 0, headerTitle.length, 0)
                     s.setHeaderTitle(headerTitle)*/

                } catch (e: Exception) {
                    Toast.makeText(this, "icons not shown" + e, Toast.LENGTH_SHORT).show()
                    Log.e("TAG", "onClick: $e")
                } finally {
                    popupMenu.show()
                }

            }
        }

    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private fun LoadPdfFiles() {

        viewBinding.tabs.ivHistory.setImageResource(R.drawable.document)
        viewBinding.tabs.tvHistory.setTextColor(Color.parseColor("#165598"))
        val builder = VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())
        builder.detectFileUriExposure()

        val animationView: LottieAnimationView = findViewById(R.id.toolbarIconSearch)
        val toolbarHeading: TextView = findViewById(R.id.toolbar_title)

        folderName = intent.getStringExtra("folderName")
        flagOfBack = intent.getBooleanExtra("backflag", false)

        if(flagOfBack == true){

        }
        else{
            imageId = intent.getStringExtra("folderId")
            folderId = imageId!!.toLong()
        }

        Log.e("TAG33", "LoadPdfFiles: $folderName")
        Log.e("TAG33", "LoadPdfFiles: $folderId")

        toolbarHeading.setText("My Pdf")
        animationView.visibility = View.GONE

        //initializing Viewmodel
        viewModel = ViewModelProvider(
            this, ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(HistoryViewModel::class.java)


        val foldersRV: RecyclerView = findViewById(R.id.rvHistory)
        foldersRV.layoutManager = GridLayoutManager(this, 2)
        //implementation of interface
        pdfAdapter = this?.let { HistoryAdapter(it, this, this) }

        //setting the adapter to the recycler view
        foldersRV.adapter = pdfAdapter
        viewModel = ViewModelProvider(this).get(HistoryViewModel::class.java)

        viewModel.repository.getAllPdfFiles().observe(this, Observer { list ->
            list?.let {

                if (list.isEmpty()) {
                    //Toast.makeText(this, "empty", Toast.LENGTH_SHORT).show()
                    viewBinding.noDataAnimation.visibility = View.VISIBLE
                    viewBinding.tvNoData.visibility = View.VISIBLE
                    pdfAdapter?.updateList(it)

                } else {
                    viewBinding.noDataAnimation.visibility = View.GONE
                    viewBinding.tvNoData.visibility = View.GONE
                    pdfAdapter?.updateList(it)
                }

            }

        })

    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun onPdfClick(historyModel: HistoryModel) {


        val pdfFilename = historyModel.fileName
        val sd = cacheDir
        val file = File(
            "/data/user/0/com.image.document.scanner/cache/",
            "/DocumentScannerApp/" + pdfFilename + ".pdf"
        )
        val contentUri =
            FileProvider.getUriForFile(this, "com.image.document.scanner.fileprovider", file)
        Log.e("TAG", "onPdfClick: $contentUri")
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(contentUri, "application/pdf")
        intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        var builder = StrictMode.VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())
        startActivity(Intent.createChooser(intent, "View PDF"))


    }


    fun share(context: AppCompatActivity, paths: MutableList<String?>?) {
        if (paths == null || paths.size == 0) {
            return
        }
        Log.e("TAG", "share: $paths")
        val uris = ArrayList<Uri>()
        val intent = Intent()
        intent.action = Intent.ACTION_SEND_MULTIPLE
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.type = "*/*"
        for (path in paths) {
            val file = File(path)
            val contentUri = FileProvider.getUriForFile(context, "$packageName.fileprovider", file)
            //intent.data = contentUri
            uris.add(contentUri)

        }

        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        intent.putExtra("folderId", imageId)
        context.startActivity(intent)

        /*val intent1 = Intent(this, PdfFilesHistory::class.java)
        startActivity(intent1) // start same activity
        finish() // destroy older activity
        overridePendingTransition(0, 0) // this is important for seamless transition
*/

    }

    override fun onPdfLongClick(historyModel: HistoryModel, position: Int) {
    }

    fun getSelectedList(key: String?): List<HistoryModel>? {
        deleteCount++
        if (key != null) {
            val string: String = Prefs.getString("checkedList", null)
            val type = object : TypeToken<ArrayList<HistoryModel?>?>() {}.type
            val selectedList = Gson().fromJson(string, type) as ArrayList<HistoryModel>

            selectedList.forEach {
                if (it.selected) {
                    viewModel.deletePdfFiles(listOf(it.mId))
                    Prefs.clear()
                    LoadPdfFiles()

                    if (deleteCount % 2 != 0) {
                        AdInterstitialGoogle.getInstance().showInterstitialAdNew(this)

                    } else {
                        Log.e("TAG", "getSelectedList: $deleteCount")
                    }
                }
            }
        }
       /* val intent = Intent(this, PdfFilesHistory::class.java)
        intent.putExtra("folderId", imageId)
        intent.putExtra("folderName", folderName)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        startActivity(intent) */// start same activity
    // destroy older activity
        overridePendingTransition(0, 0) // this is important for seamless transition

        return null
    }

    private fun multipleControl(view: View) {
        view.setEnabled(false)
        Handler().postDelayed(Runnable { // This method will be executed once the timer is over
            view.setEnabled(true)
        }, 1000) // set time as per your requirement
    }

    override fun onResume() {
        LoadPdfFiles()
        super.onResume()
    }

    private fun loadLInterstitialAd() {
        AdInterstitialGoogle.getInstance().loadInterstitialAd(this)
    }

    private fun loadLNativeAd() {
        nativeAd = AdsNativeGoogle()
        nativeAd?.adSmallGoogle(
            this,
            viewBinding.nativeAdContainerShimmerSmall,
            viewBinding.nativeAdContainerSmall
        )
    }

    override fun onBackPressed() {

        if (flagOfBack == true) {
            val intent = Intent(this, HomeScreen::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            startActivity(intent)
            finish()
        } else {
            val intent = Intent(this, PdfImages::class.java)
            intent.putExtra("folderId", imageId)
            intent.putExtra("folderName", folderName)

            Log.e("TAG33", "onBackPressed: $imageId")
            Log.e("TAG33", "onBackPressed: $folderName")

            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            startActivity(intent)
            finish()
        }

        super.onBackPressed()
    }

}