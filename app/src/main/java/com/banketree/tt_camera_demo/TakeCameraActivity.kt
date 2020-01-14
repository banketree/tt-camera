package com.banketree.tt_camera_demo

import android.content.Intent
import com.thinkcore.storage.TFilePath
import com.thinkcore.storage.TStorageUtils
import com.ttm.camera_component.ui.CameraActivity
import com.ttm.camera_component.ui.PreviewActivity
import java.io.File

class TakeCameraActivity : CameraActivity() {
    private val tFilePath: TFilePath by lazy { TFilePath(this) }

    override fun getSavedDir(isVideo: Boolean): File {
        if (TStorageUtils.isExternalStoragePresent()) {
            return File(
                if (isVideo)
                    tFilePath.externalVideoDir else tFilePath.externalImageDir
            )
        }

        return File(
            if (isVideo)
                tFilePath.interVideoDir else tFilePath.interImageDir
        )
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