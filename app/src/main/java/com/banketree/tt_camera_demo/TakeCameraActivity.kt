package com.banketree.tt_camera_demo

import android.content.Intent
import com.ttm.camera_component.ui.CameraActivity
import com.ttm.camera_component.ui.PreviewActivity
import java.io.File

class TakeCameraActivity : CameraActivity() {

    override fun getSavedDir(): File {
        return super.getSavedDir()
    }

    override fun startPreviewActivity(filePath: String, picFlag: Boolean) {
        val intent = Intent(this, PreviewCameraActivity::class.java)
        intent.putExtra(PreviewActivity.PARAMS_PATH, filePath)
        intent.putExtra(PreviewActivity.PARAMS_FLAG_PICTURE, picFlag)
        startActivityForResult(
            intent,
            CameraActivity.REQUEST_PREVIEW_PHOTO_VIDEO
        )
    }


    //录制视频 持续时间 单位秒
    override fun getRecordVideoDuration(): Int = 60
}