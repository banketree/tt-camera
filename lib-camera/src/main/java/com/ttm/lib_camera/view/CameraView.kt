package com.ttm.lib_camera.view

import android.graphics.Bitmap

interface CameraView {
    fun resetState(type: Int)

    fun confirmState(type: Int)

    fun showPicture(bitmap: Bitmap, isVertical: Boolean)

    fun playVideo(firstFrame: Bitmap, url: String)

    fun stopVideo()

    fun setTip(tip: String)

    fun startPreviewCallback()

    fun handlerFoucs(x: Float, y: Float): Boolean
}
