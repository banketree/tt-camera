package com.banketree.tt_camera_demo.yuv

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.*
import android.text.TextUtils
import android.util.Log
import com.banketree.tt_camera_demo.R
import com.miracles.camera.*
import com.miracles.codec.camera.AudioDevice
import com.miracles.codec.camera.CapturePictureHandler
import com.miracles.codec.camera.Mp4Muxer
import com.miracles.codec.camera.Mp4MuxerHandler
import kotlinx.android.synthetic.main.yuv_activity_camera.*
import java.io.File

/**
 * Created by lxw
 */
@Suppress("DEPRECATION")
class CameraActivity : BaseActivity() {
    companion object {
        const val REQUEST_PREVIEW_PHOTO: Int = 101
    }

    private val baseActivity: BaseActivity by lazy { this }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.yuv_activity_camera)
        initView()
    }

    private fun getSavedDir(): File {
        val dir = File(Environment.getExternalStorageDirectory(), "camera&Test")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun initView() {
        initCameraView()
        initControlView()
    }

    private var mmP4MuxerHandler: MMP4MuxerHandler? = null
    private fun initVideoCamera() {
        //record preview size
        //cameraView.setCameraSizeStrategy(CameraFunctions.STRATEGY_RECORD_PREVIEW_SIZE, getRecordStrategy())
        //mp4Callback
        mmP4MuxerHandler?.let {
            cameraView.removeCallback(it)
        }
        mmP4MuxerHandler = null
        mmP4MuxerHandler = object : MMP4MuxerHandler(this@CameraActivity, getSavedDir()) {
            override fun createMp4Muxer(frameWidth: Int, frameHeight: Int): Mp4Muxer {
                var framingRect = getRatioAreaRect(frameWidth, frameHeight)
                return super.createMp4Muxer(framingRect.width(), framingRect.height())
            }

            override fun onStopRecordingFrame(cameraView: CameraView, timeStampInNs: Long) {
                super.onStopRecordingFrame(cameraView, timeStampInNs)
                cameraControlView.resetState()
                if (timeStampInNs < 2000) {
                    File(mMp4Path).deleteOnExit()
                    return
                }

                val intent = Intent(this@CameraActivity, PreviewActivity::class.java)
                intent.putExtra(PreviewActivity.PARAMS_PATH, mMp4Path)
                intent.putExtra(PreviewActivity.PARAMS_FLAG_PICTURE, false)
                startActivityForResult(intent, REQUEST_PREVIEW_PHOTO)
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
//                return Rect(160, 0,1120, height)
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

    private fun initCameraView() {
        initVideoCamera()
        //picture callback
        cameraView.addCallback(object :
            CapturePictureHandler(getSavedDir().absolutePath + File.separator) {
            override fun onPictureCapturedResult(
                cameraView: CameraView,
                path: String,
                ex: Throwable?
            ) {
                cameraControlView.resetState()
                super.onPictureCapturedResult(cameraView, path, ex)
                if (ex != null) {
                    logMEE("onPictureTaken failed.", ex)
                } else {
                    val intent = Intent(this@CameraActivity, PreviewActivity::class.java)
                    intent.putExtra(PreviewActivity.PARAMS_PATH, path)
                    intent.putExtra(PreviewActivity.PARAMS_FLAG_PICTURE, true)
                    startActivityForResult(intent, REQUEST_PREVIEW_PHOTO)
                }
            }

            override fun getCropRect(width: Int, height: Int): Rect {
                return ratioView.getRatioAreaRect(width, height)
            }
        })

        cameraControlView.setCameraControlListener(object : CameraControlListener {
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

            override fun onError(error: String) {
                cameraView.stopRecord()
            }
        })
    }

    private fun initControlView() {
        image_back.setOnClickListener {
            finish()
        }
        ratio_tv.setOnClickListener {
            when (getRatioType()) {
                RatioView.RATIO_16_9 -> setRatioTypeUI(RatioView.RATIO_4_3)
                RatioView.RATIO_4_3 -> setRatioTypeUI(RatioView.RATIO_1_1)
                RatioView.RATIO_1_1 -> setRatioTypeUI(RatioView.RATIO_16_9)
            }
        }
        image_flash.setOnClickListener {
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
        image_switch.setOnClickListener {
            val facing = cameraView.getFacing()
            if (facing == CameraFunctions.FACING_BACK) {
                cameraView.setFacing(CameraFunctions.FACING_FRONT)
            } else {
                cameraView.setFacing(CameraFunctions.FACING_BACK)
            }
        }

        setCameraFlashUI(cameraView.getFlashing())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PREVIEW_PHOTO && data != null) {
            val path = data.getStringExtra(PreviewActivity.PARAMS_PATH)
            val flagPicture = data.getBooleanExtra(PreviewActivity.PARAMS_FLAG_PICTURE, true)
            if (TextUtils.isEmpty(path) || !File(path).exists()) {
                return
            }
            if (resultCode == Activity.RESULT_OK) {
                finish()
            } else {
                File(path).deleteOnExit()
            }
        }
    }

    /**
     * Mp4 record handler .path
     */
    private open class MMP4MuxerHandler(val ctx: Context, val dir: File) : Mp4MuxerHandler() {
        override fun createMp4Muxer(frameWidth: Int, frameHeight: Int): Mp4Muxer {
            val path = File(dir, "${System.currentTimeMillis()}_me.mp4").absolutePath
            val mp4Param = Mp4Muxer.Params().apply {
                this.path = path
                this.width = frameHeight   //横屏-屏幕宽高
                this.height = frameWidth
                this.balanceTimestampGapInSeconds = 5
            }
            val audioParam = AudioDevice.Params()
            val audioDevice = AudioDevice.create(audioParam)
            return Mp4Muxer(ctx, mp4Param, audioDevice)
        }

        override fun onStartRecordingFrame(cameraView: CameraView, timeStampInNs: Long) {
            super.onStartRecordingFrame(cameraView, timeStampInNs)
            logMED("---onStartRecordingFrame----")
        }

        override fun onStopRecordingFrame(cameraView: CameraView, timeStampInNs: Long) {
            super.onStopRecordingFrame(cameraView, timeStampInNs)
            logMED("---onStopRecordingFrame----")
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
            cropRect: Rect //重写它 裁剪
        ) {
            super.onFrameRecording(
                cameraView,
                frameBytes,
                width,
                height,
                format,
                orientation,
                facing,
                timeStampInNs,
                cropRect
            )
        }
    }

    override fun onResume() {
        super.onResume()
        val success = cameraView.start()
        if (!success) {
            finish()
        }
    }

    override fun onPause() {
        super.onPause()
        cameraView.stop()
    }

    private fun setCameraFlashUI(flashing: Int) {
        when (flashing) {
            CameraFunctions.FLASH_AUTO -> {
                image_flash.setImageResource(R.drawable.yuv_flash_auto)
            }
            CameraFunctions.FLASH_ON -> {
                image_flash.setImageResource(R.drawable.yuv_flash_on)
            }
            CameraFunctions.FLASH_OFF -> {
                image_flash.setImageResource(R.drawable.yuv_flash_off)
            }
        }
    }

    private fun getRatioType(): Int {
        return ratioView.getRadioType()
    }

    private fun setRatioTypeUI(radioType: Int) {
        if (radioType == RatioView.RATIO_16_9) {
            ratioView.setRadioType(RatioView.RATIO_16_9)
            ratio_tv.text = "16:9"
        } else if (radioType == RatioView.RATIO_4_3) {
            ratioView.setRadioType(RatioView.RATIO_4_3)
            ratio_tv.text = "4:3"
        } else if (radioType == RatioView.RATIO_1_1) {
            ratioView.setRadioType(RatioView.RATIO_1_1)
            ratio_tv.text = "1:1"
        }
        initVideoCamera()
    }
}