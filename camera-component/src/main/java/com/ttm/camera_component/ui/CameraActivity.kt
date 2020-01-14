package com.ttm.camera_component.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.*
import android.text.TextUtils
import android.util.Log
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import com.miracles.camera.CameraFunctions
import com.miracles.camera.CameraView
import com.miracles.codec.camera.AudioDevice
import com.miracles.codec.camera.CapturePictureHandler
import com.miracles.codec.camera.Mp4Muxer
import com.miracles.codec.camera.Mp4MuxerHandler
import com.ttm.camera_component.listener.CameraControlListener
import com.ttm.camera_component.R
import com.ttm.camera_component.widget.CameraControlView
import com.ttm.camera_component.widget.RatioView
import kotlinx.android.synthetic.main.camera_activity_camera.*
import java.io.File

/**
 * @author banketree
 * @time 2020/1/14 11:31
 * @description
 * 按比例拍照以及录制视频
 * 16:9 4:3 1:1
 */
@Suppress("DEPRECATION")
abstract class CameraActivity : BaseActivity() {
    companion object {
        const val REQUEST_PREVIEW_PHOTO_VIDEO: Int = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.camera_activity_camera)
        initView()
    }

    override fun onResume() {
        super.onResume()
        val success = cameraView.start()
        if (!success) {
//            finish()
        }
    }

    override fun onPause() {
        super.onPause()
        cameraView.stop()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PREVIEW_PHOTO_VIDEO && data != null) {
            val path = data.getStringExtra(PreviewActivity.PARAMS_PATH)
            val flagPicture = data.getBooleanExtra(PreviewActivity.PARAMS_FLAG_PICTURE, true)
            if (TextUtils.isEmpty(path) || !File(path).exists()) {
                return
            }
            if (resultCode == Activity.RESULT_OK) {
                finish()
            } else { //取消则删除
                File(path).deleteOnExit()
            }
        }
    }

    private fun initView() {
        initControlView()
        initPictrueCamera(cameraView, getSavedDir(false), cameraControlView)
        initMP4MuxerCamera(cameraView, getSavedDir(true), cameraControlView)
    }

    private fun initControlView() {
        cameraControlView.setCameraControlListener(object :
            CameraControlListener {
            override fun onTakePictures() {
                cameraView.takePicture()
            }

            override fun onStartRecordVideo() {
                if (cameraView.isRecordingFrame()) cameraView.stopRecord()
                cameraView.startRecord()
            }

            override fun onEndRecordVideo() {
                cameraView.stopRecord()
            }
        })
        cameraControlView.setDuration(getRecordVideoDuration())

        back_img.setOnClickListener {
            finish()
        }
        ratio_tv.setOnClickListener {
            when (getRatioType()) {
                RatioView.RATIO_16_9 -> setRatioTypeUI(RatioView.RATIO_4_3)
                RatioView.RATIO_4_3 -> setRatioTypeUI(RatioView.RATIO_1_1)
                RatioView.RATIO_1_1 -> setRatioTypeUI(RatioView.RATIO_16_9)
            }
        }
        flash_img.setOnClickListener {
            var flashing = cameraView.getFlashing()
            when (flashing) {
                CameraFunctions.FLASH_AUTO -> {
                    flashing = CameraFunctions.FLASH_ON
                }
                CameraFunctions.FLASH_ON -> {
                    flashing = CameraFunctions.FLASH_OFF
                }
                CameraFunctions.FLASH_OFF -> {
                    flashing = CameraFunctions.FLASH_AUTO
                }
            }

            cameraView.setFlashing(flashing)
            setCameraFlashUI(flashing)
        }
        switch_face_img.setOnClickListener {
            val facing = cameraView.getFacing()
            if (facing == CameraFunctions.FACING_BACK) {
                cameraView.setFacing(CameraFunctions.FACING_FRONT)
            } else {
                cameraView.setFacing(CameraFunctions.FACING_BACK)
            }
        }

        setCameraFlashUI(cameraView.getFlashing())
    }

    private fun setCameraFlashUI(flashing: Int) {
        when (flashing) {
            CameraFunctions.FLASH_AUTO -> {
                flash_img.setImageResource(R.drawable.camera_flash_auto)
            }
            CameraFunctions.FLASH_ON -> {
                flash_img.setImageResource(R.drawable.camera_flash_on)
            }
            CameraFunctions.FLASH_OFF -> {
                flash_img.setImageResource(R.drawable.camera_flash_off)
            }
        }
    }

    private fun getRatioType(): Int {
        return ratioView.getRadioType()
    }

    private fun setRatioTypeUI(radioType: Int) {
        when (radioType) {
            RatioView.RATIO_16_9 -> {
                ratioView.setRadioType(RatioView.RATIO_16_9)
                ratio_tv.text = "16:9"
            }
            RatioView.RATIO_4_3 -> {
                ratioView.setRadioType(RatioView.RATIO_4_3)
                ratio_tv.text = "4:3"
            }
            RatioView.RATIO_1_1 -> {
                ratioView.setRadioType(RatioView.RATIO_1_1)
                ratio_tv.text = "1:1"
            }
        }
        initMP4MuxerCamera(cameraView, getSavedDir(true), cameraControlView)
    }

    /**
     * @param
     * @return
     * @author banketree
     * @time 2020/1/14 14:47
     * @description
     * 录制视频 持续时间 单位秒
     */
    open fun getRecordVideoDuration(): Int = 10

    open fun getSavedDir(isVideo: Boolean): File {
        val dir = File(Environment.getExternalStorageDirectory(), "Camera")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    var dialog: AlertDialog? = null
    protected open fun showDialog(@StringRes title: Int, @StringRes message: Int): AlertDialog {
        return showDialog(getString(title), getString(message))
    }

    protected open fun showDialog(title: String, message: String): AlertDialog {
        dialog?.dismiss()
        dialog = AlertDialog.Builder(this@CameraActivity)
            .setTitle(title)
            .setMessage(message)
            .setNegativeButton(R.string.camera_confirm) { _, _ ->
            }
            .create()
        dialog?.show()
        return dialog!!
    }

    protected open fun startPreviewActivity(filePath: String, picFlag: Boolean) {
        val intent = Intent(this@CameraActivity, PreviewActivity::class.java)
        intent.putExtra(PreviewActivity.PARAMS_PATH, filePath)
        intent.putExtra(PreviewActivity.PARAMS_FLAG_PICTURE, picFlag)
        startActivityForResult(
            intent,
            CameraActivity.REQUEST_PREVIEW_PHOTO_VIDEO
        )
    }

    ////////////////////////////////////////////////////////////////初始化拍照处理/////////////////////////////////////////////////////////////////////////////////////
    //视频 mp4 合成处理
    private var mmP4MuxerHandler: MMP4MuxerHandler? = null

    private fun initPictrueCamera(
        cameraView: CameraView,
        saveDirFile: File,
        _cameraControlView: CameraControlView
    ) {
        //picture callback
        cameraView.addCallback(object :
            CapturePictureHandler(saveDirFile.absolutePath + File.separator) {
            override fun onPictureCapturedResult(
                cameraView: CameraView,
                path: String,
                ex: Throwable?
            ) {
                _cameraControlView.resetState()
                super.onPictureCapturedResult(cameraView, path, ex)
                if (ex != null) {
                    showDialog(R.string.camera_error, R.string.camera_take_photo_error)
                    Log.i("onPictureTaken failed.", ex.message)
                } else {
                    startPreviewActivity(path, true)
                }
            }

            override fun getCropRect(width: Int, height: Int): Rect {
                return ratioView.getRatioAreaRect(width, height)
            }
        })
    }

    ////////////////////////////////////////////////////////////////初始化视频处理/////////////////////////////////////////////////////////////////////////////////////
    private fun initMP4MuxerCamera(
        cameraView: CameraView,
        saveDirFile: File,
        _cameraControlView: CameraControlView
    ) {
        //record preview size
        //cameraView.setCameraSizeStrategy(CameraFunctions.STRATEGY_RECORD_PREVIEW_SIZE, getRecordStrategy())
        //mp4Callback
        mmP4MuxerHandler?.let {
            cameraView.removeCallback(it)
        }
        mmP4MuxerHandler = null
        mmP4MuxerHandler = object : MMP4MuxerHandler(this@CameraActivity, saveDirFile) {
            override fun createMp4Muxer(frameWidth: Int, frameHeight: Int): Mp4Muxer {
                var framingRect = getRatioAreaRect(frameWidth, frameHeight)
                return super.createMp4Muxer(framingRect.width(), framingRect.height())
            }

            override fun onStopRecordingFrame(cameraView: CameraView, timeStampInNs: Long) {
                super.onStopRecordingFrame(cameraView, timeStampInNs)
                _cameraControlView.resetState()
                if (timeStampInNs < 2000) {
                    showDialog(R.string.camera_tip, R.string.camera_record_video_short_time_error)
                    File(getMp4Path()).deleteOnExit()
                    return
                }

                startPreviewActivity(getMp4Path(), false)
            }

            private fun getRatioAreaRect(width: Int, height: Int): Rect {
                var framingRect = ratioView.getRatioAreaRect(width, height)  //内部有旋转竖屏
                if (ratioView.isOrientationPortrait()) {
                    framingRect = Rect(
                        framingRect.top,
                        framingRect.left,
                        framingRect.bottom,
                        framingRect.right
                    ) //倒过来
                }
                Log.i(
                    "",
                    "$framingRect +  height:${framingRect.height()}+  width:${framingRect.width()}"
                )
                return framingRect
            }

            override fun onFrameRecording(
                cameraView: CameraView,
                frameBytes: CameraView.FrameBytes,
                width: Int,
                height: Int,
                format: Int,
                orientation: Int,
                facing: Int,
                timeStampInNs: Long,
                cropRect: Rect
            ) {
                var framingRect = getRatioAreaRect(width, height)
                super.onFrameRecording(
                    cameraView,
                    frameBytes,
                    width,
                    height,
                    format,
                    orientation,
                    facing,
                    timeStampInNs,
                    framingRect
                )
            }
        }
        cameraView.addCallback(mmP4MuxerHandler!!)
    }

    /**
     * Mp4 record handler .path
     */
    protected open class MMP4MuxerHandler(val context: Context, val saveDirFile: File) :
        Mp4MuxerHandler() {
        override fun createMp4Muxer(frameWidth: Int, frameHeight: Int): Mp4Muxer {
            val path = File(saveDirFile, "${System.currentTimeMillis()}_me.mp4").absolutePath
            val mp4Param = Mp4Muxer.Params().apply {
                this.path = path
                this.width = frameHeight   //横屏-屏幕宽高
                this.height = frameWidth
                this.balanceTimestampGapInSeconds = 5
            }
            val audioParam = AudioDevice.Params()
            val audioDevice = AudioDevice.create(audioParam)
            return Mp4Muxer(context, mp4Param, audioDevice)
        }

        override fun onStartRecordingFrame(cameraView: CameraView, timeStampInNs: Long) {
            super.onStartRecordingFrame(cameraView, timeStampInNs)
            Log.i("MMP4MuxerHandler", "---onStartRecordingFrame----")
        }

        override fun onStopRecordingFrame(cameraView: CameraView, timeStampInNs: Long) {
            super.onStopRecordingFrame(cameraView, timeStampInNs)
            Log.i("MMP4MuxerHandler", "---onStopRecordingFrame----")
        }
    }
}