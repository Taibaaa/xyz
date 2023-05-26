package com.PdfCreator.Editor.ui


import PdfCreator.Editor.R
import PdfCreator.Editor.databinding.ActivityDisplayClickedImageBinding
import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.MediaStore.Images
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.PdfCreator.Editor.ads.AdInterstitialGoogle
import com.PdfCreator.Editor.ads.AdsNativeGoogle
import com.PdfCreator.Editor.roomdbfiles.FilesViewModel
import com.PdfCreator.Editor.utils.FileExternalPath.copyUriToExternalFilesDir
import com.PdfCreator.Editor.utils.FileExternalPath.getFileNameByUri
import com.xiaopo.flying.sticker.DrawableSticker
import com.xiaopo.flying.sticker.Sticker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream


class DisplayClickedImageActivity : AppCompatActivity() {

    lateinit var viewBinding: ActivityDisplayClickedImageBinding
    var sentUri: String? = null
    var imageId1: Int? = null
    var imageId2: String? = null
    var backPressedId1: String? = null
    var folderName: String? = null
    var orgFileName: String? = null
    lateinit var viewModel: FilesViewModel
    var flag1: Boolean = false
    var flag2: Boolean = false
    var isEditing: Boolean = false
    private var nativeAd: AdsNativeGoogle? = null


    @SuppressLint("WrongThread")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewBinding = ActivityDisplayClickedImageBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)


        loadLNativeAd()
        loadLInterstitialAd()
        toolbarFunctions()
        loadSticker()


        viewBinding.stickerView.setOnClickListener {
            viewBinding.stickerView.setLocked(!viewBinding.stickerView.isLocked())
        }

        viewBinding.myToolbar.ivBack.setOnClickListener {
            val intent = Intent(this, FolderFiles::class.java)
            intent.putExtra("folderName", folderName)
            intent.putExtra("folderId", backPressedId1)
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            startActivity(intent)
        }

        viewBinding.tabs.constEdit.setOnClickListener {
            flag1 = true
            isEditing = true
            sentUri = intent.getStringExtra("clickedImageUri")
            val intent1 = Intent(this, ImageEditActivity::class.java)
            intent1.putExtra("image_uri", sentUri.toString())
            intent1.putExtra("id_image", imageId2)
            intent1.putExtra("orgFileName", orgFileName)
            intent1.putExtra("clickedImageUri", sentUri)
            intent1.putExtra("flag1", flag1)
            intent1.putExtra("folderName", folderName)
            intent1.putExtra("foldersId", backPressedId1)
            intent1.putExtra("editing", isEditing)

            Log.e("TAG", "onCreate: myedit $backPressedId1")

            startActivity(intent1)
        }

        viewBinding.tabs.layoutSave.setOnClickListener {
            viewBinding.stickerView.setLocked(true)
            viewBinding.stickerView.buildDrawingCache()
            val editedImage: Bitmap = viewBinding.stickerView.getDrawingCache()

            val fileName = System.currentTimeMillis().toString()
            val myfile = bitmapToFile(editedImage, "$fileName.jpg")
            //addImageToGallery(myfile.toString(), this)
            MediaStore.Images.Media.insertImage(
                getContentResolver(),
                editedImage,
                fileName,
                "myImage"
            )
            addImageToGallery(myfile.toString(), this)

            Toast.makeText(this, "Image Saved to Gallery", Toast.LENGTH_SHORT).show()
        }


        viewBinding.tabs.layoutSign.setOnClickListener {
            flag1 = true
            isEditing = true
            sentUri = intent.getStringExtra("clickedImageUri")
            val intent1 = Intent(this, AddSignature::class.java)

            intent1.putExtra("clickedImageUri", sentUri)
            intent1.putExtra("flag2", flag2)
            intent1.putExtra("id_image", imageId2)
            intent1.putExtra("folderName", folderName)
            intent1.putExtra("foldersId", backPressedId1)
            intent1.putExtra("editing", isEditing)

            Log.e("TAG", "onCreate: sign $backPressedId1")
            startActivityForResult(intent1, 222)
        }

    }


    private fun toolbarFunctions() {
        val savePdfBtn: ImageView = findViewById(R.id.createPdf)
        val shareImageBtn: ImageView = findViewById(R.id.shareImage)
        val tvShareBtn: TextView = findViewById(R.id.tvShareImage)
        val btnSaveChanges: ImageView = findViewById(R.id.saveChanges)
        val toolbarHeading: TextView = findViewById(R.id.folderTitleName)

        folderName = intent.getStringExtra("folderName")
        toolbarHeading.setText(folderName)
        savePdfBtn.visibility = View.GONE
        shareImageBtn.visibility = View.GONE
        tvShareBtn.visibility = View.GONE

        isEditing = intent.getBooleanExtra("editing", false)
        Log.e("TAG", "loadSticker: $isEditing")
        if (isEditing) {
            //    Toast.makeText(this, "true", Toast.LENGTH_SHORT).show()
            btnSaveChanges.visibility = View.VISIBLE
        } else {
            // Toast.makeText(this, "false", Toast.LENGTH_SHORT).show()
            btnSaveChanges.visibility = View.GONE
        }

        btnSaveChanges.setOnClickListener {

            saveEditedImage()

        }
    }

    private fun loadSticker() {

        imageId1 = intent.getIntExtra("id_image", 0)
        backPressedId1 = intent.getStringExtra("foldersId")
        Log.e("TAG", "onCreate: displayClicked $backPressedId1")
        orgFileName = intent.getStringExtra("orgFileName")

        imageId2 = imageId1.toString()

        val mFlag3 = intent.getBooleanExtra("flag3", false)
        val mFlag1 = intent.getBooleanExtra("flag1", false)
        val mFlag2 = intent.getBooleanExtra("flag2", false)

        val byteArray = intent.getByteArrayExtra("SignBitmap")


        if (mFlag3) {
            //  Toast.makeText(this, "flag3" + mFlag3, Toast.LENGTH_SHORT).show()
            sentUri = intent.getStringExtra("clickedImageUri")
            sentUri?.let {
            orgFileName = File(Uri.parse(it).path).name
            Log.e("TAG", "showImage: $orgFileName")

            //decoding the uri to Bitmap

                val bitmap: Bitmap = BitmapFactory.decodeFile(it)
                viewBinding.showSelectedImage.setImageBitmap(bitmap)
            }



        } else if (mFlag1) {
            // Toast.makeText(this, "flag 1" + flag1, Toast.LENGTH_SHORT).show()

            val byteArray = intent.getByteArrayExtra("EditedImage")

            //decoding ByteArray to a Bitmap
            val bmp = BitmapFactory.decodeByteArray(byteArray, 0, byteArray!!.size)

            val outputStream = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.PNG, 100, outputStream)

            // Toast.makeText(this, "out"+outputStream, Toast.LENGTH_SHORT).show()

            viewBinding.showSelectedImage.setImageBitmap(bmp)
        } else {
            //   Toast.makeText(this, "flag 2" + mFlag2, Toast.LENGTH_SHORT).show()
            //decoding ByteArray to a Bitmap
            val bmp = BitmapFactory.decodeByteArray(byteArray, 0, byteArray!!.size)

            val outputStream = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            // Toast.makeText(this, "out"+outputStream, Toast.LENGTH_SHORT).show()

            /*  Viewbinding.stickerView.setImageBitmap(bitmap)*/

            sentUri = intent.getStringExtra("clickedImageUri")
            val bitmap: Bitmap = BitmapFactory.decodeFile(sentUri)
            viewBinding.showSelectedImage.setImageBitmap(bitmap)

            val drawable: Drawable = BitmapDrawable(resources, bmp)
            val drawable1: Drawable? = drawable
            viewBinding.stickerView.addSticker(
                DrawableSticker(drawable1),
                Sticker.Position.BOTTOM or Sticker.Position.RIGHT
            )
            /*var fileName = System.currentTimeMillis().toString()
            var myfile = bitmapToFile(bitmap, "$fileName.jpg")*/
        }
    }


    private fun saveEditedImage() {
        //initializing Viewmodel
        viewModel = ViewModelProvider(
            this, ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(FilesViewModel::class.java)

        viewBinding.stickerView.buildDrawingCache()
        val editedImage: Bitmap = viewBinding.stickerView.getDrawingCache()
        val fileName = System.currentTimeMillis().toString()
        val myfile = bitmapToFile(editedImage, "$fileName.jpg")

      /*  val uri = Uri.fromFile(File(myfile!!.toURI().toString()))
        val filename: String = getFileNameByUri(uri)
        if (myfile!!.name != null) {
            copyUriToExternalFilesDir(uri, myfile!!.name)
        }
*/
        CoroutineScope(Dispatchers.IO).launch {
            viewModel.repository.updateUri(
                externalCacheDir.toString() + "/${myfile!!.name}",
                imageId1!!
            )
        }
        val intent = Intent(this, FolderFiles::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        intent.putExtra("folderId", backPressedId1)
        intent.putExtra("folderName", folderName)
        startActivity(intent)
       // Toast.makeText(this, "Image Saved in Folder", Toast.LENGTH_SHORT).show()
        AdInterstitialGoogle.getInstance().showInterstitialAdNew(this)
    }


    fun bitmapToFile(bitmap: Bitmap, fileNameToSave: String): File? { // File name like "image.png"
        //create a file to write bitmap data
        var file: File? = null
        return try {
            file = File( externalCacheDir.toString() + File.separator + fileNameToSave)
            file.createNewFile()
            //Convert bitmap to byte array
            val bos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 0, bos) // YOU can also save it in JPEG
            val bitmapdata = bos.toByteArray()
            //write the bytes in file
            val fos = FileOutputStream(file)
            fos.write(bitmapdata)
            fos.flush()
            fos.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            file // it will return null
        }
    }

    fun bitmapToFileSave(
        bitmap: Bitmap,
        fileNameToSave: String
    ): File? { // File name like "image.png"
        //create a file to write bitmap data
        var file: File? = null
        return try {
            file = File(
                Environment.getExternalStorageDirectory()
                    .toString() + File.separator + fileNameToSave
            )
            file.createNewFile()

            //Convert bitmap to byte array
            val bos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 0, bos) // YOU can also save it in JPEG
            val bitmapdata = bos.toByteArray()

            //write the bytes in file
            val fos = FileOutputStream(file)
            fos.write(bitmapdata)
            fos.flush()
            fos.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            file // it will return null
        }
    }

    fun addImageToGallery(filePath: String?, context: Context) {
        val values = ContentValues()
        values.put(Images.Media.DATE_TAKEN, System.currentTimeMillis())
        values.put(Images.Media.MIME_TYPE, "image/jpeg")
        values.put(MediaStore.MediaColumns.DATA, filePath)
        context.getContentResolver().insert(Images.Media.EXTERNAL_CONTENT_URI, values)
    }

    override fun startActivityForResult(intent: Intent?, requestCode: Int, options: Bundle?) {
        super.startActivityForResult(intent, requestCode, options)
        if (requestCode == 222) {
            // Toast.makeText(this, "hello 222", Toast.LENGTH_SHORT).show()
        }
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

        val intent = Intent(this, FolderFiles::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        intent.putExtra("folderName", folderName)
        intent.putExtra("folderId", backPressedId1)
        startActivity(intent)

        super.onBackPressed()
    }

    override fun onResume() {

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