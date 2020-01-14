package com.banketree.tt_camera_demo

import android.view.SurfaceView
import android.widget.ImageView
import com.ttm.camera_component.ui.PreviewActivity

/**
 * @author banketree
 * @time 2020/1/14 14:42
 * @description
 * 预览图片
 */
class PreviewCameraActivity : PreviewActivity() {

    override fun previewPhoto(path: String, photoImg: ImageView) {
        super.previewPhoto(path, photoImg)
    }

    override fun playVideo(path: String, surfaceView: SurfaceView) {
        super.playVideo(path, surfaceView)
    }
}