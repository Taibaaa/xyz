package com.PdfCreator.Editor.utils

import android.app.Activity
import android.net.Uri
import android.provider.MediaStore
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream


object FileExternalPath {
    fun Activity.copyUriToExternalFilesDir(uri: Uri, fileName: String) {

        try {
            val inputStream = contentResolver.openInputStream(uri)
            val tempDir = externalCacheDir.toString()
            if (inputStream != null) {
                val file = File("$tempDir/$fileName")
                val fos = FileOutputStream(file)
                val bis = BufferedInputStream(inputStream)
                val bos = BufferedOutputStream(fos)
                val byteArray = ByteArray(1024)
                var bytes = bis.read(byteArray)
                while (bytes > 0) {
                    bos.write(byteArray, 0, bytes)
                    bos.flush()
                    bytes = bis.read(byteArray)
                }
                bos.close()
                fos.close()
            }
        } catch (e: Exception) {
        }
    }

    fun Activity.getFileNameByUri(uri: Uri): String {
        var fileName = System.currentTimeMillis().toString()
        val cursor = contentResolver.query(uri, null, null, null, null)
        if (cursor != null && cursor.count > 0) {
            cursor.moveToFirst()
            fileName =
                cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME))
            cursor.close()

            //  val picturePath = cursor.getString(columnIndex)
        }
        return fileName
    }


    fun Activity.getRealPathFromURI(contentURI: Uri): String? {
        val result: String?
        val cursor = contentResolver.query(contentURI, null, null, null, null)
        // val cursor  = contentResolver.query(contentURI, null, null, null, null)
        if (cursor == null) { // Source is Dropbox or other similar local file path
            result = contentURI.path
        } else {
            cursor.moveToFirst()
            val idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
            result = cursor.getString(idx)
            cursor.close()
        }
        return result
    }


    fun Activity.copyUriToExternalFilesDir1(uri: Uri) {

        try {
            val inputStream = contentResolver.openInputStream(uri)
            val tempDir = externalCacheDir.toString()
            if (inputStream != null) {
                val file = File("$tempDir")
                val fos = FileOutputStream(file)
                val bis = BufferedInputStream(inputStream)
                val bos = BufferedOutputStream(fos)
                val byteArray = ByteArray(1024)
                var bytes = bis.read(byteArray)
                while (bytes > 0) {
                    bos.write(byteArray, 0, bytes)
                    bos.flush()
                    bytes = bis.read(byteArray)
                }
                bos.close()
                fos.close()
            }
        } catch (e: Exception) {
        }
    }
}