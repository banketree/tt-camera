package com.banketree.tt_camera_demo.yuv

import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.os.Environment
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import com.banketree.tt_camera_demo.R
import com.miracles.camera.*
import com.miracles.codec.camera.AudioDevice
import com.miracles.codec.camera.CapturePictureHandler
import com.miracles.codec.camera.Mp4Muxer
import com.miracles.codec.camera.Mp4MuxerHandler
import com.ttm.lib_camera.RatioVideoView
import kotlinx.android.synthetic.main.yuv_activity_camera.*
import java.io.File

/**
 * Created by lxw
 */
class CameraActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.yuv_activity_camera)
        initView()
    }

    private fun getSavedDir(): File {
        val dir = File(Environment.getExternalStorageDirectory(), "camera&muxer")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun initView() {
        initCameraView()
        initControlView()
    }

    private fun initCameraView() {
        //record preview size
        //cameraView.setCameraSizeStrategy(CameraFunctions.STRATEGY_RECORD_PREVIEW_SIZE, getRecordStrategy())
        //mp4Callback
        cameraView.addCallback(object : MMP4MuxerHandler(this@CameraActivity, getSavedDir()) {
            override fun onStopRecordingFrame(cameraView: CameraView, timeStampInNs: Long) {
                super.onStopRecordingFrame(cameraView, timeStampInNs)
                PreviewActivity.start(this@CameraActivity, mMp4Path, false)
                cameraControlView.resetState()
            }
        })
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
                    PreviewActivity.start(this@CameraActivity, path, true)
                }
            }

            override fun getCropRect(width: Int, height: Int): Rect {
                val rect = ratioView.getRatioAreaRect(width, height)  //默认竖屏
                return rect
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
            }
        })
    }

    private fun initControlView() {
        image_back.setOnClickListener {
            finish()
        }
        ratio_tv.setOnClickListener {
            when (getRatioType()) {
                RatioVideoView.RATIO_16_9 -> setRatioTypeUI(RatioVideoView.RATIO_4_3)
                RatioVideoView.RATIO_4_3 -> setRatioTypeUI(RatioVideoView.RATIO_1_1)
                RatioVideoView.RATIO_1_1 -> setRatioTypeUI(RatioVideoView.RATIO_16_9)
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


    private fun getRecordStrategy(): ChooseSizeStrategy {
        val display = resources.displayMetrics
        logMED("display width=${display.widthPixels} ,height=${display.heightPixels}")
        val aspectRatio = display.heightPixels.toFloat() / display.widthPixels
        return MChooseStrategy(16 / 9f)
    }

    private class MChooseStrategy(val aspectRatio: Float) : ChooseSizeStrategy {
        private val mFacingBackChooseStrategy: ChooseSizeStrategy
        private val mFacingFrontChooseStrategy: ChooseSizeStrategy

        init {
            mFacingFrontChooseStrategy = ChooseSizeStrategy.AspectRatioStrategy(
                aspectRatio,
                (480 * aspectRatio).toInt(),
                480
            )
            mFacingBackChooseStrategy = ChooseSizeStrategy.AspectRatioStrategy(
                aspectRatio,
                (1080 * aspectRatio).toInt(),
                1080
            )
        }

        constructor(parcel: Parcel) : this(parcel.readFloat()) {
        }

        override fun writeToParcel(dest: Parcel?, flags: Int) {
            dest?.writeFloat(aspectRatio)
        }

        override fun describeContents() = 0

        override fun chooseSize(
            preview: CameraPreview,
            displayOrientation: Int,
            cameraSensorOrientation: Int,
            facing: Int,
            sizes: List<Size>
        ): Size {
            return if (facing == CameraFunctions.FACING_FRONT) {
                mFacingFrontChooseStrategy.chooseSize(
                    preview,
                    displayOrientation,
                    cameraSensorOrientation,
                    facing,
                    sizes
                )
            } else {
                mFacingBackChooseStrategy.chooseSize(
                    preview,
                    displayOrientation,
                    cameraSensorOrientation,
                    facing,
                    sizes
                )
            }
        }


        companion object CREATOR : Parcelable.Creator<MChooseStrategy> {
            override fun createFromParcel(parcel: Parcel): MChooseStrategy {
                return MChooseStrategy(parcel)
            }

            override fun newArray(size: Int): Array<MChooseStrategy?> {
                return arrayOfNulls(size)
            }
        }
    }

    /**
     * Mp4 record handler .path
     */
    private open class MMP4MuxerHandler(val ctx: Context, val dir: File) : Mp4MuxerHandler() {
        override fun createMp4Muxer(frameWidth: Int, frameHeight: Int): Mp4Muxer {
            val path = File(dir, "me.mp4").absolutePath
            val mp4Param = Mp4Muxer.Params().apply {
                this.path = path
                this.width = frameHeight /// 2
                this.height = frameWidth /// 2
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
                image_flash.setImageResource(com.ttm.lib_camera.R.drawable.camera_ic_flash_auto)
            }
            CameraFunctions.FLASH_ON -> {
                image_flash.setImageResource(com.ttm.lib_camera.R.drawable.camera_ic_flash_on)
            }
            CameraFunctions.FLASH_OFF -> {
                image_flash.setImageResource(com.ttm.lib_camera.R.drawable.camera_ic_flash_off)
            }
        }
    }

    private fun getRatioType(): Int {
        return ratioView.getRadioType()
    }

    private fun setRatioTypeUI(radioType: Int) {
        if (radioType == RatioVideoView.RATIO_16_9) {
            ratioView.setRadioType(RatioVideoView.RATIO_16_9)
            ratio_tv.text = "16:9"
        } else if (radioType == RatioVideoView.RATIO_4_3) {
            ratioView.setRadioType(RatioVideoView.RATIO_4_3)
            ratio_tv.text = "4:3"
        } else if (radioType == RatioVideoView.RATIO_1_1) {
            ratioView.setRadioType(RatioVideoView.RATIO_1_1)
            ratio_tv.text = "1:1"
        }
    }
}