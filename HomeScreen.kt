package com.PdfCreator.Editor.ui

import PdfCreator.Editor.R
import PdfCreator.Editor.databinding.ActivityHomeScreenBinding
import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.PdfCreator.Editor.adapter.ContactClickInterface
import com.PdfCreator.Editor.adapter.ContactLongClickInterface
import com.PdfCreator.Editor.adapter.FoldersAdapter
import com.PdfCreator.Editor.ads.AdInterstitialGoogle
import com.PdfCreator.Editor.ads.AdsNativeGoogle
import com.PdfCreator.Editor.roomdbfiles.FilesModel
import com.PdfCreator.Editor.roomdbfiles.FilesViewModel
import com.PdfCreator.Editor.roomdbfolders.FoldersModel
import com.PdfCreator.Editor.roomdbfolders.FoldersViewModel
import com.PdfCreator.Editor.ui.SplashScreen.Companion.ads
import com.PdfCreator.Editor.utils.FileExternalPath.copyUriToExternalFilesDir
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.pixplicity.easyprefs.library.Prefs
import kotlinx.android.synthetic.main.toolbar_main.*
import kotlinx.coroutines.launch
import pl.aprilapps.easyphotopicker.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class HomeScreen : AppCompatActivity(), View.OnClickListener, ContactClickInterface,
    ContactLongClickInterface {

    private lateinit var viewBinding: ActivityHomeScreenBinding
    lateinit var viewModel: FoldersViewModel
    lateinit var viewModelFiles: FilesViewModel
    var folderAdapter: FoldersAdapter? = null
    private var easyImage: EasyImage? = null
    var folderType: String? = null
    var folderName: String? = null
    private var selectedList = ArrayList<FoldersModel>()
    var count1 = 0
    var count2 = -1
    var folderCount = -1
    var folderCountAd = 0
    var deleteCountAd = 0
    var adCount = 0
    var clickCountAd = 0
    private var nativeAd: AdsNativeGoogle? = null
    var imageId: String? = null
    var flagBack: Boolean? = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewBinding = ActivityHomeScreenBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        ads = true
        folderCount = Prefs.getInt("countFolder", -1)
        loadLInterstitialAd()
        loadLNativeAd()
        uiViews()
        LoadChat()
        initializeImagePickerLib()

    }

    private fun uiViews() {
        viewBinding.openOption.setOnClickListener(this)
        viewBinding.createFolder.setOnClickListener(this)
        viewBinding.camera.setOnClickListener(this)
        viewBinding.gallery.setOnClickListener(this)
        viewBinding.tabs.layoutHome.setOnClickListener(this)
        viewBinding.tabs.layoutHistory.setOnClickListener(this)
        viewBinding.myShimmer.toolbarIconSearch.setOnClickListener(this)
        viewBinding.myShimmer.toolbarIconMenu.setOnClickListener(this)
        viewBinding.search.setOnClickListener(this)

    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.openOption -> {
                if (viewBinding.camera.visibility == View.VISIBLE && viewBinding.gallery.visibility == View.VISIBLE) {

                    viewBinding.camera.visibility = View.GONE
                    viewBinding.gallery.visibility = View.GONE
                    viewBinding.linearGallery.visibility = View.GONE
                    viewBinding.linearCamera.visibility = View.GONE

                    val hideAnim = AnimationUtils.loadAnimation(this, R.anim.hide_button)
                    val hideLayout = AnimationUtils.loadAnimation(this, R.anim.hide_layout)
                    viewBinding.openOption.startAnimation(hideAnim)
                    viewBinding.camera.startAnimation(hideLayout)
                    viewBinding.gallery.startAnimation(hideLayout)
                    viewBinding.linearGallery.startAnimation(hideLayout)
                    viewBinding.linearCamera.startAnimation(hideLayout)
                } else {
                    viewBinding.camera.visibility = View.VISIBLE
                    viewBinding.gallery.visibility = View.VISIBLE
                    viewBinding.linearGallery.visibility = View.VISIBLE
                    viewBinding.linearCamera.visibility = View.VISIBLE
                    val showAnim = AnimationUtils.loadAnimation(this, R.anim.show_button)
                    val showLayout = AnimationUtils.loadAnimation(this, R.anim.show_layout)
                    viewBinding.openOption.startAnimation(showAnim)
                    viewBinding.camera.startAnimation(showLayout)
                    viewBinding.gallery.startAnimation(showLayout)
                    viewBinding.linearGallery.startAnimation(showLayout)
                    viewBinding.linearCamera.startAnimation(showLayout)
                }

            }
            R.id.createFolder -> {

                createFolderDialog()
            }
            R.id.search -> {
                viewBinding.search.setIconifiedByDefault(false)
            }
            R.id.camera -> {
                /*  Toast.makeText(this, "cam", Toast.LENGTH_SHORT).show()*/
                openCamera()
            }
            R.id.gallery -> {
                /*   Toast.makeText(this, "gal", Toast.LENGTH_SHORT).show()*/
                openGallery()
            }
            R.id.layoutHome -> {
                viewBinding.tabs.layoutHome.setEnabled(false)
                Handler().postDelayed(Runnable { // This method will be executed once the timer is over
                    viewBinding.tabs.layoutHome.setEnabled(true)

                    val intent = Intent(this, HomeScreen::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    //   multipleControl(viewBinding.tabs.layoutHome)
                    startActivity(intent)

                }, 1000) // set time as per your requirement


            }
            R.id.layoutHistory -> {

                flagBack=true
                viewBinding.tabs.layoutHome.setEnabled(false)
                Handler().postDelayed(Runnable { // This method will be executed once the timer is over
                    viewBinding.tabs.layoutHome.setEnabled(true)
                    val intent = Intent(this, PdfFilesHistory::class.java)

                    intent.putExtra("backflag", flagBack)
                    intent.putExtra("folderName", folderName)
                    Log.e("TAG33", "onClick: $imageId", )
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    multipleControl(viewBinding.tabs.layoutHistory)
                    startActivity(intent)


                }, 1000) // set time as per your requirement

            }

            R.id.toolbarIconSearch -> {
                val rateIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + this.getPackageName())
                )
                startActivity(rateIntent)

            }

            R.id.toolbarIconMenu -> {
                val popupMenu = PopupMenu(this, toolbarIconMenu)

                popupMenu.setOnMenuItemClickListener { item ->

                    /*  itemToHide= toolbarIconMenu.findViewById(R.id.rename) as MenuItem
                      itemToHide!!.setVisible(false)
  */
                    when (item.itemId) {
                        R.id.deleteFolder -> {

                            if (Prefs.contains("selectedList")) {
                                getSelectedList("selectedList")
                            } else {
                                Toast.makeText(
                                    this,
                                    "Select Item to delete",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            true
                        }
                        R.id.rename -> {

                            folderType = "Edit"
                            createFolderDialog()

                            true
                        }
                        else -> false
                    }

                }

                popupMenu.inflate(R.menu.folders_menu)

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
                   // Toast.makeText(this, "icons not shown" + e, Toast.LENGTH_SHORT).show()
                    Log.e("TAG", "onClick: $e")
                } finally {
                    popupMenu.show()
                }

            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun createFolderDialog() {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setView(R.layout.folder_name_dialog)
        val alertDialog = dialogBuilder.create()
        alertDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        alertDialog.show()
        alertDialog.setCancelable(false)

        val cancelBtn = alertDialog.findViewById(R.id.btnNo) as TextView
        val okBtn = alertDialog.findViewById(R.id.btn_Yes) as TextView
        val tvFolderName = alertDialog.findViewById(R.id.etFolderName) as EditText

        cancelBtn.setOnClickListener {
            alertDialog.dismiss()
        }

        okBtn.setOnClickListener {
            folderName = tvFolderName.text.toString().trim()

            //for editing case
            if (folderType.equals("Edit")) {
                if (folderName!!.isNotEmpty()) {
                    renameFolders("selectedList")
                    alertDialog.dismiss()
                } else {
                    tvFolderName.setError("Folder Name Required")
                }
            } else {
                //for Inserting New Contact
                folderCountAd++
                if (folderName!!.isNotEmpty()) {

                    val currentDate =
                        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

                    lifecycleScope.launch {
                        viewModel.addFolder(
                            FoldersModel(
                                null,
                                folderName!!,
                                currentDate,
                                "0",
                                false
                            )
                        )
                    }
                    if (folderCountAd % 2 != 0) {
                        AdInterstitialGoogle.getInstance().showInterstitialAdNew(this@HomeScreen)

                    } else {
                        Log.e("TAG", "createFolderDialog: $folderCount")
                    }
                    alertDialog.dismiss()
                } else {
                    tvFolderName.setError("Folder Name Required")
                }
            }
            /* val intent = Intent(this, HomeScreen::class.java)
             startActivity(intent) // start same activity
             finish() // destroy older activity
             overridePendingTransition(0, 0) // this is important for seamless transition*/
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        super.onActivityResult(requestCode, resultCode, data)

        adCount++

        easyImage!!.handleActivityResult(
            requestCode,
            resultCode,
            data,
            this,
            object : DefaultCallback() {

                override fun onImagePickerError(
                    @NonNull error: Throwable,
                    @NonNull source: MediaSource
                ) {
                    //Some error handling
                    error.printStackTrace()
                }

                override fun onMediaFilesPicked(imageFiles: Array<MediaFile>, source: MediaSource) {
                    folderCount++
                    Prefs.putInt("countFolder", folderCount)
                    var size = imageFiles.size
                    val currentDate =
                        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

                    if (folderCount == 0) {
                        lifecycleScope.launch {
                            viewModel.addFolder(
                                FoldersModel(
                                    null,
                                    "New Folder",
                                    currentDate,
                                    size.toString(),
                                    false
                                )
                            )
                        }
                    } else {
                        lifecycleScope.launch {
                            viewModel.addFolder(
                                FoldersModel(
                                    null,
                                    "New Folder " + folderCount,
                                    currentDate,
                                    size.toString(),
                                    false
                                )
                            )
                        }
                    }

                    imageFiles.forEach {
                        /* Log.d("EasyImage", "Image file returned: " + it.file.path.toUri())*/
                        var uri = Uri.fromFile(File(it.file.path))
                        Log.e("TAG", "Selected uri: $uri")
                        //val filename: String? = getFileNameByUri(uri)
                        Log.e("TAG", "Selected it.file.name: ${it.file.name}")
                        //Log.e("TAG", "Selected filename: $filename")
                        if (it.file.name != null) {
                            copyUriToExternalFilesDir(uri, it.file.name)
                        }
                        // val imageUri = data!!.data
                        Log.e("TAG", "onActivityResult: $it.file.name")
                        lifecycleScope.launch {
                            viewModelFiles.addFiles(
                                FilesModel(
                                    null,
                                    externalCacheDir.toString() + "/${it.file.name}",
                                    viewModel.insertedId!!
                                )
                            )
                            Log.i("INSERT_ID", "Inserted ID is: ${viewModel.insertedId}")
                        }
                    }
                    ads = false
                    if (adCount % 2 != 0) {

                        AdInterstitialGoogle.getInstance().showInterstitialAdNew(this@HomeScreen)
                    } else {
                        Log.e("TAG", "else: $adCount")
                    }

                }

                override fun onCanceled(@NonNull source: MediaSource) {
                   /* Toast.makeText(this@HomeScreen, "You haven't picked Image", Toast.LENGTH_SHORT)
                        .show()*/
                    Log.e("TAG", "onCanceled: $source")
                }
            })
    }

    private fun LoadChat() {

        viewBinding.tabs.ivHome.setImageResource(R.drawable.home)
        viewBinding.tabs.tvHome.setTextColor(Color.parseColor("#165598"))
        val backIcon: ImageView = findViewById(R.id.ivBack)
        backIcon.visibility = View.GONE


        //initializing Viewmodel
        viewModel = ViewModelProvider(
            this, ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(FoldersViewModel::class.java)

        //initializing Viewmodel
        viewModelFiles = ViewModelProvider(
            this, ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(FilesViewModel::class.java)


        val foldersRV: RecyclerView = findViewById(R.id.rvGridFolders)
        foldersRV.layoutManager = GridLayoutManager(this, 2)
        //implementation of interface
        val foldersAdapter = this?.let { FoldersAdapter(it, this, this) }

        //setting the adapter to the recycler view
        foldersRV.adapter = foldersAdapter
        viewModel = ViewModelProvider(this).get(FoldersViewModel::class.java)

        viewModel.allFolders.observe(this, Observer { list ->
            list?.let {
                Log.e("TAG", "getItemCount: ${it.size}")
                if (list.isEmpty()) {
                    //Toast.makeText(this, "empty", Toast.LENGTH_SHORT).show()
                    viewBinding.noDataAnimation.visibility = View.VISIBLE
                    viewBinding.tvNoData.visibility = View.VISIBLE

                    //foldersAdapter.notifyDataSetChanged()

                } else {
                    viewBinding.noDataAnimation.visibility = View.GONE
                    viewBinding.tvNoData.visibility = View.GONE
                    // foldersAdapter?.updateList(it)

                }
                foldersAdapter?.updateList(it)
                foldersAdapter.notifyDataSetChanged()

            }

        })

        val searchView: SearchView = findViewById(R.id.search)

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                Log.e("Hello", newText!!)
                foldersAdapter.filter.filter(newText)
                return true
            }

        })

    }

    override fun onContactClick(foldersModel: FoldersModel) {
        clickCountAd++

        val intent = Intent(this, FolderFiles::class.java)
        Log.d("idHome", "HomeClicked" + foldersModel.id)

        intent.putExtra("folderId", foldersModel.id.toString())
        intent.putExtra("folderName", foldersModel.folderName)

        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        if (clickCountAd % 2 == 0) {
            startActivity(intent)
            AdInterstitialGoogle.getInstance().showInterstitialAdNew(this@HomeScreen)
        } else {
            startActivity(intent)
        }
    }

    override fun onContactLongClick(foldersModel: FoldersModel, count: Int) {

    }

    private fun openCamera() {

        easyImage!!.openCameraForImage(this)
    }

    private fun openGallery() {
        easyImage!!.openDocuments(this)
    }

    private fun initializeImagePickerLib() {
        //checkPermission()

        easyImage = EasyImage.Builder(this)
            .setChooserTitle("Pick media")
            .setCopyImagesToPublicGalleryFolder(true)
            .setChooserType(ChooserType.CAMERA_AND_GALLERY)
            .setFolderName("EasyImage sample")
            .allowMultiple(true)
            .build()
    }


    fun getSelectedList(key: String?): List<FoldersModel>? {
        deleteCountAd++
        if (key != null) {
            val string: String = Prefs.getString("selectedList", null)
            val type = object : TypeToken<ArrayList<FoldersModel?>?>() {}.type
            selectedList = Gson().fromJson(string, type) as ArrayList<FoldersModel>
            Log.e("TAG", "getSelectedList: ${selectedList.size}")
            selectedList!!.forEach {
                if (it.selected1) {
                    viewModel.deleteSelectedFolders(listOf(it.id))
                    Prefs.clear()
                    LoadChat()
                    if(deleteCountAd %2 != 0){
                        AdInterstitialGoogle.getInstance().showInterstitialAdNew(this@HomeScreen)
                    }
                    else{
                        Log.e("TAG", "getSelectedList: $deleteCountAd")
                    }
                }
            }
            val intent = Intent(this, HomeScreen::class.java)
            startActivity(intent) // start same activity
            finish() // destroy older activity
            overridePendingTransition(0, 0) // this is important for seamless transition

        }
        return null
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun renameFolders(key: String?): List<FoldersModel>? {
        if (key != null && Prefs.contains("selectedList")) {
            val string: String = Prefs.getString("selectedList", null)
            val type = object : TypeToken<ArrayList<FoldersModel?>?>() {}.type
            val selectedList = Gson().fromJson(string, type) as ArrayList<FoldersModel>

            selectedList.forEach {
                if (it.selected1) {
                    count1++
                    count2 = it.id
                }
            }
            if (count1 == 1) {
                viewModel.renameFolder(folderName!!, count2)
                Prefs.clear()
                count1 = 0
                LoadChat()
                /*Toast.makeText(this, "Folder Name Updated..", Toast.LENGTH_SHORT).show()*/
            } else {
                Toast.makeText(
                    this,
                    "Cannot rename more than 1 Folder",
                    Toast.LENGTH_SHORT
                ).show()
                count1 = 0
                LoadChat()
            }
         /*   val intent = Intent(this, HomeScreen::class.java)
            startActivity(intent) // start same activity
            finish() // destroy older activity
            overridePendingTransition(0, 0) // this is important for seamless transition
*/

        } else {
            Toast.makeText(this, "Select Folder to rename", Toast.LENGTH_SHORT).show()
        }
        val intent = Intent(this, HomeScreen::class.java)
        startActivity(intent) // start same activity
        finish() // destroy older activity
        overridePendingTransition(0, 0) // this is important for seamless transition
        return null
    }

    private fun multipleControl(view: View) {
        view.setEnabled(false)
        Handler().postDelayed(Runnable { // This method will be executed once the timer is over
            view.setEnabled(true)
        }, 1500) // set time as per your requirement
    }


    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onBackPressed() {

        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setView(R.layout.rating_dialog)
        val alertDialog = dialogBuilder.create()
        alertDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        alertDialog.show()
        alertDialog.setCancelable(true)
        /*val window: Window = alertDialog.getWindow()!!
        window.setLayout(ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT)
*/
        val exitBtn = alertDialog.findViewById(R.id.btnExit) as AppCompatButton
        val btnFeedback = alertDialog.findViewById(R.id.btn_Feedback) as AppCompatButton
        val btnRate = alertDialog.findViewById(R.id.ratingBar) as RatingBar
        val rateApp = alertDialog.findViewById(R.id.btnRateApp) as AppCompatButton
        val rateComments = alertDialog.findViewById(R.id.ratingText) as TextView

        btnRate.setOnRatingBarChangeListener(RatingBar.OnRatingBarChangeListener { ratingBar, rating, fromUser ->

            /*rateApp.visibility = View.VISIBLE
            btnFeedback.visibility = View.GONE*/
            rateComments.visibility = View.VISIBLE

            var message: String? = null

            when (rating) {
                0f -> {
                    rateComments.visibility = View.GONE
                }
                1f -> {
                    message = " Not Good "
                }
                2f -> {
                    message = " Quite Ok! "
                }
                3f -> {
                    message = " Very Good! "
                }
                4f -> {
                    message = " Excellent!! "
                }
                5f -> {
                    message = " Awesome! Thank You! :) "
                }
            }
            rateComments.setText(message)

        })

        rateApp.setOnClickListener {
            val rateIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("market://details?id=" + this.getPackageName())
            )
            startActivity(rateIntent)
            alertDialog.dismiss()
        }

        exitBtn.setOnClickListener {
            finishAffinity()
            alertDialog.dismiss()
        }

        btnFeedback.setOnClickListener {

            val intent = Intent(Intent.ACTION_SEND)
            val recipients = arrayOf("noshershahid22@gmail.com")
            intent.putExtra(Intent.EXTRA_EMAIL, recipients)
            intent.putExtra(Intent.EXTRA_SUBJECT, "Feedback Document Scanner App")
            intent.putExtra(Intent.EXTRA_TEXT, "Feedback")
            intent.type = "text/html"
            startActivity(Intent.createChooser(intent, "Send mail"))
            alertDialog.dismiss()
        }

    }

    private fun loadLNativeAd() {
        nativeAd = AdsNativeGoogle()
        nativeAd?.adSmallGoogle(
            this,
            viewBinding.nativeAdContainerShimmerSmall,
            viewBinding.nativeAdContainerSmall
        )
    }

    private fun loadLInterstitialAd() {
        AdInterstitialGoogle.getInstance().loadInterstitialAd(this)
    }

    override fun onResume() {
        LoadChat()

        if (!checkPermission()) {
            val intent = Intent(this, PermissionSceen::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            startActivity(intent)
            finish()
        } else {

        }
        super.onResume()
    }

    private fun checkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
}