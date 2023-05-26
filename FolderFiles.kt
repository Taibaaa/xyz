package com.PdfCreator.Editor.ui

import PdfCreator.Editor.R
import PdfCreator.Editor.databinding.ActivityFolderFilesBinding
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.PdfCreator.Editor.adapter.ShowImagesAdapter
import com.PdfCreator.Editor.ads.AdInterstitialGoogle
import com.PdfCreator.Editor.ads.AdsNativeGoogle
import com.PdfCreator.Editor.roomdbfiles.FilesModel
import com.PdfCreator.Editor.roomdbfiles.FilesViewModel
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


class FolderFiles : AppCompatActivity(), View.OnClickListener,
    ShowImagesAdapter.ItemClickInterface, ShowImagesAdapter.ContactLongClickInterface {

    private lateinit var viewBinding: ActivityFolderFilesBinding
    lateinit var viewModel: FilesViewModel
    lateinit var viewModelFolders: FoldersViewModel
    private var easyImage: EasyImage? = null
    private var galleryAdapter: ShowImagesAdapter? = null
    var selectedList: ArrayList<FilesModel>? = null
    var imageId: String? = null
    var folderName: String? = null
    var folderId: Long? = null
    var flag3: Boolean = false
    var size: Int? = null
    var adImageCount = 0
    var deleteCount = 0
    private var nativeAd: AdsNativeGoogle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewBinding = ActivityFolderFilesBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        uiViews()
        loadLNativeAd()
        loadLInterstitialAd()
        filesDbOperations()
        initializeImagePickerLib()

        viewBinding.myTopBar.showPdfIcon.setOnClickListener {
            /*   Toast.makeText(this, "show Pdf", Toast.LENGTH_SHORT).show()*/
            if (size == 0) {
                Toast.makeText(this, "Select Images to make Pdf", Toast.LENGTH_SHORT).show()
            } else {

                Log.e("TAG2", "onCreate: $size")
                imageId = intent.getStringExtra("folderId")
                val intent = Intent(this, PdfImages::class.java)
                intent.putExtra("folderId", imageId)
                intent.putExtra("folderName", folderName)

                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                startActivity(intent)
            }


        }

        viewBinding.myTopBar.ivBack.setOnClickListener {
            onBackPressed()
        }

    }

    private fun uiViews() {
        viewBinding.openOption.setOnClickListener(this)
        viewBinding.camera.setOnClickListener(this)
        viewBinding.gallery.setOnClickListener(this)
        viewBinding.myTopBar.toolbarIconMenu.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.openOption -> {
                if (viewBinding.camera.visibility == View.VISIBLE && viewBinding.gallery.visibility == View.VISIBLE) {

                    viewBinding.camera.visibility = View.GONE
                    viewBinding.gallery.visibility = View.GONE
                    val hideAnim = AnimationUtils.loadAnimation(this, R.anim.hide_button)
                    val hideLayout = AnimationUtils.loadAnimation(this, R.anim.hide_layout)
                    viewBinding.openOption.startAnimation(hideAnim)
                    viewBinding.camera.startAnimation(hideLayout)
                    viewBinding.gallery.startAnimation(hideLayout)
                } else {
                    viewBinding.camera.visibility = View.VISIBLE
                    viewBinding.gallery.visibility = View.VISIBLE
                    val showAnim = AnimationUtils.loadAnimation(this, R.anim.show_button)
                    val showLayout = AnimationUtils.loadAnimation(this, R.anim.show_layout)
                    viewBinding.openOption.startAnimation(showAnim)
                    viewBinding.camera.startAnimation(showLayout)
                    viewBinding.gallery.startAnimation(showLayout)
                }
            }

            R.id.gallery -> {

                pickImagesFromGallery()

            }
            R.id.camera -> {

                openCamera()
            }

            R.id.toolbarIconMenu -> {
                val popupMenu = PopupMenu(this, toolbarIconMenu)

                popupMenu.setOnMenuItemClickListener { item ->

                    /*  itemToHide= toolbarIconMenu.findViewById(R.id.rename) as MenuItem
                      itemToHide!!.setVisible(false)
  */
                    when (item.itemId) {
                        R.id.deleteFiles -> {
                            if (Prefs.contains("selectedList")) {
                                getSelectedList("selectedList")
                            } else {
                                Toast.makeText(
                                    this,
                                    "Select Items to delete",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                           /* Toast.makeText(this, "Files Deleted Successfully", Toast.LENGTH_SHORT)
                                .show()*/
                            true
                        }
                        else -> false
                    }

                }

                popupMenu.inflate(R.menu.files_menu)

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
                   /* Toast.makeText(this, "icons not shown" + e, Toast.LENGTH_SHORT).show()*/
                    Log.e("TAG", "onClick: $e", )
                } finally {
                    popupMenu.show()
                }

            }
        }
    }


    private fun openCamera() {
        easyImage!!.openCameraForImage(this)
    }

    private fun pickImagesFromGallery() {
        easyImage!!.openDocuments(this)
    }


    fun filesDbOperations() {

        //initializing Viewmodel
        viewModel = ViewModelProvider(
            this, ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(FilesViewModel::class.java)

        //initializing Viewmodel
        viewModelFolders = ViewModelProvider(
            this, ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(FoldersViewModel::class.java)


        val ChatRV: RecyclerView = findViewById(R.id.rvGridFolders)
        val toolbarHeading: TextView = findViewById(R.id.toolbar_title)

        ChatRV.layoutManager = GridLayoutManager(this, 2)
        galleryAdapter = ShowImagesAdapter(this, this, this)

        imageId = intent.getStringExtra("folderId")
        folderName = intent.getStringExtra("folderName")
        toolbarHeading.setText(folderName)

        folderId = imageId!!.toLong()
        /* ChatRV.setAdapter(galleryAdapter)*/
        ChatRV.adapter = galleryAdapter

        viewModel.repository.getAllImages(folderId).observe(this, Observer { list ->
            list?.let {
                Log.e("TAG", "filesDbOperations: $it")

                size = list.size
                viewModelFolders.addFilesCount(size.toString(), folderId)

                if (list.isEmpty()) {
                    viewBinding.noDataAnimation.visibility = View.VISIBLE
                    viewBinding.tvNoData.visibility = View.VISIBLE

                } else {
                    viewBinding.noDataAnimation.visibility = View.GONE
                    viewBinding.tvNoData.visibility = View.GONE

                }
                galleryAdapter!!.updateList(it)
                galleryAdapter?.notifyDataSetChanged()

                /*val intent = Intent(this, FolderFiles::class.java)
                startActivity(intent) // start same activity
                finish() // destroy older activity
                overridePendingTransition(0, 0) // this is important for seamless transition*/
            }

        })

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        super.onActivityResult(requestCode, resultCode, data)
        adImageCount++
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
                    Log.d("TAG", "source: ${source.name}")
                    Log.d("TAG", "error: ${error.message}")
                    error.printStackTrace()
                }

                override fun onMediaFilesPicked(
                    imageFiles: Array<MediaFile>,
                    source: MediaSource
                ) {
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
                            viewModel.addFiles(
                                FilesModel(
                                    null,
                                    externalCacheDir.toString() + "/${it.file.name}",
                                    folderId
                                )
                            )
                            Log.i("INSERT_ID", "Inserted ID is: ${viewModel.insertedId}")
                        }
                    }
                    ads = false
                    if (adImageCount % 2 != 0) {

                        AdInterstitialGoogle.getInstance().showInterstitialAdNew(this@FolderFiles)

                    } else {
                        /*Toast.makeText(
                            this@FolderFiles,
                            "Images added Successfully",
                            Toast.LENGTH_SHORT
                        ).show()*/
                    }
                }


                override fun onCanceled(@NonNull source: MediaSource) {
                   /* Toast.makeText(
                        this@FolderFiles,
                        "You haven't picked Image",
                        Toast.LENGTH_SHORT
                    )
                        .show()*/
                    Log.e("TAG", "onCanceled: $source", )
                }
            })
    }


    private fun initializeImagePickerLib() {
        //checkPermission()

        easyImage = EasyImage.Builder(this)
            .setChooserTitle("Pick media")
            .setCopyImagesToPublicGalleryFolder(true) // THIS requires granting WRITE_EXTERNAL_STORAGE permission for devices running Android 9 or lower
            //                .setChooserType(ChooserType.CAMERA_AND_DOCUMENTS)
            .setChooserType(ChooserType.CAMERA_AND_DOCUMENTS)
            .setFolderName("EasyImage sample")
            .allowMultiple(true)
            .build()
    }

    override fun onSingleClick(filesModel: FilesModel, position: Int) {

        val data = Intent(this, DisplayClickedImageActivity::class.java)

        flag3 = true
        val idImage = filesModel.id

        val imageUri = filesModel.selectedImagesUri
        //  val intent = Intent(this, DisplayClickedImageActivity::class.java)
        data.putExtra("clickedImageUri", imageUri)
        data.putExtra("flag3", flag3)
        data.putExtra("id_image", idImage)
        data.putExtra("folderName", folderName)
        data.putExtra("foldersId", folderId.toString())

        startActivityForResult(data, 111)

        // setResult(Activity.RESULT_OK, data)
        // finish()

    }


    override fun startActivityForResult(intent: Intent?, requestCode: Int, options: Bundle?) {
        super.startActivityForResult(intent, requestCode, options)
        if (requestCode == 111) {
            // Toast.makeText(this, "hello 111", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onContactLongClick(filesModel: FilesModel) {

    }


    fun getSelectedList(key: String?): List<FilesModel>? {
        deleteCount++
        if (key != null) {
            val string: String = Prefs.getString("selectedList", null)
            val type = object : TypeToken<ArrayList<FilesModel?>?>() {}.type
            selectedList = Gson().fromJson(string, type) as ArrayList<FilesModel>

            selectedList!!.forEach {
                if (it.selected2) {
                    viewModel.deleteSelectedFiles(listOf(it.id))
                    Prefs.clear()
                    filesDbOperations()
                      if (deleteCount % 2 != 0) {
                          AdInterstitialGoogle.getInstance().showInterstitialAdNew(this)
                      }
                    else{
                          Log.e("TAG", "getSelectedList: $deleteCount", )
                      }
                }
            }

        }

       /* val intent = Intent(this, FolderFiles::class.java)
        startActivity(intent) // start same activity
        finish() // destroy older activity
        overridePendingTransition(0, 0) // this is important for seamless transition*/
        return null

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

    private fun checkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }


    override fun onResume() {
        super.onResume()
        filesDbOperations()
        if (!checkPermission()) {
            val intent = Intent(this, PermissionSceen::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            startActivity(intent)
            finish()
        } else {

        }
    }


    override fun onBackPressed() {
        val intent = Intent(this, HomeScreen::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        startActivity(intent)
        super.onBackPressed()
    }

}

