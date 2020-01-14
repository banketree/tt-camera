package com.ttm.camera_component.listener

/**
 * @author banketree
 * @time 2020/1/13 10:59
 * @description
 * 拍照 视频 回调
 */
interface CameraControlListener {
    /**
     * 拍照
     * */
    fun onTakePictures()

    /**
     * 开始录像
     * */
    fun onStartRecordVideo()

    /**
     * 结束录像
     * */
    fun onEndRecordVideo()
}