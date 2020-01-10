package com.ttm.lib_camera.listener

import android.graphics.Bitmap

interface JCameraListener {

    fun captureSuccess(bitmap: Bitmap)

    fun recordSuccess(url: String, firstFrame: Bitmap)

    fun quit()

}
