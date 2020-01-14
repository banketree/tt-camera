# tt-camera
Camera1 Camera2 系列
根据业务自定义拍照（测试功能）

拍照、录制视频 尺寸剪接（画中画 16:9/4:3/1:1）

功能引用依赖库

    implementation 'com.github.banketree.tt-camera:camera-component:v0.0.2'


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


其他 分支用于 功能测试

![Image text](https://github.com/banketree/tt-camera/blob/master/screenShot/2.jpg)

![Image text](https://github.com/banketree/tt-camera/blob/master/screenShot/3.jpg)

![Image text](https://github.com/banketree/tt-camera/blob/master/screenShot/4.jpg)

![Image text](https://github.com/banketree/tt-camera/blob/master/screenShot/5.jpg)

![Image text](https://github.com/banketree/tt-camera/blob/master/screenShot/1.jpg)


感谢开源：

    https://github.com/CJT2325/CameraView
    https://github.com/banketree/Camera-Muxer
