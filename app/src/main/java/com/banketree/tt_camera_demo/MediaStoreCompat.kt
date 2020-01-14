@file:Suppress("DEPRECATION")

package com.banketree.tt_camera_demo


import android.content.Context
import android.content.pm.PackageManager
import android.os.Environment
import androidx.core.os.EnvironmentCompat
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MediaStoreCompat {

    @Throws(IOException::class)
    fun createTempFile(
        context: Context,
        isVideo: Boolean,
        isPublicDir: Boolean = false,
        directory: String? = null
    ): File? {
        // Create an image file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = if (isVideo) {
            String.format("JPEG_%s.jpg", timeStamp)
        } else {
            String.format("VIDEO_%s.mp4", timeStamp)
        }
        var storageDir: File?
        if (isPublicDir) {
            //公共目录
            storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES
            )
            if (!storageDir!!.exists()) {
                storageDir.mkdirs()
            }
        } else {
            //App目录
            storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        }

        if (directory != null) {
            storageDir = File(storageDir, directory)
            if (!storageDir.exists()) {
                storageDir.mkdirs()
            }
        }

        // Avoid joining path components manually
        val tempFile = File(storageDir, imageFileName)
        // Handle the situation that user's external storage is not ready
        return if (Environment.MEDIA_MOUNTED != EnvironmentCompat.getStorageState(tempFile)) {
            null
        } else tempFile
    }

    /**
     * Checks whether the device has a camera feature or not.
     *
     * @param context a context to check for camera feature.
     * @return true if the device has a camera feature. false otherwise.
     */
    fun hasCameraFeature(context: Context): Boolean {
        val pm = context.applicationContext.packageManager
        return pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)
    }
}
