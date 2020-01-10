package com.ttm.lib_camera.listener

/**
 *  * @author 陈嘉桐
 * @description
 * 拍照回调
 */
interface CaptureListener {
    fun takePictures()

    fun recordShort(time: Long)

    fun recordStart()

    fun recordEnd(time: Long)

    fun recordZoom(zoom: Float)

    fun recordError()
}
