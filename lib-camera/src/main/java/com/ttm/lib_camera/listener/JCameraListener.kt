package com.ttm.lib_camera.listener

import android.graphics.Bitmap

/**
 *  * @author 陈嘉桐
 * @description
 * 拍照 以及 录像 回调
 */
interface JCameraListener {

    fun captureSuccess(bitmap: Bitmap)

    fun recordSuccess(url: String, firstFrame: Bitmap)

    fun quit()

}
