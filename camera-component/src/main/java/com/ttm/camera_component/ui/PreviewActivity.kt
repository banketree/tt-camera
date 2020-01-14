package com.ttm.camera_component.ui

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.text.TextUtils
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.ImageView
import com.ttm.camera_component.widget.CameraButton
import com.ttm.camera_component.R
import kotlinx.android.synthetic.main.camera_activity_preview.*
import java.io.File


/**
 * @author banketree
 * @time 2020/1/14 13:50
 * @description
 * 预览拍照 以及 录制的视频
 * 自适应位置
 */
open class PreviewActivity : BaseActivity() {
    companion object {
        const val PARAMS_PATH = "path"
        const val PARAMS_FLAG_PICTURE = "flag.picture"
    }

    private var path: String = ""
    private var flagPicture: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        path = intent.getStringExtra(PARAMS_PATH)
        flagPicture = intent.getBooleanExtra(PARAMS_FLAG_PICTURE, true)
        if (TextUtils.isEmpty(path) || !File(path).exists()) {
            finish()
            return
        }

        setContentView(R.layout.camera_activity_preview)
        initView(path, flagPicture)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        releaseImageViewResouce(preview_photo_img)
    }

    private fun getResultIntent(): Intent {
        val intent = Intent()
        intent.putExtra(PARAMS_PATH, path)
        intent.putExtra(PARAMS_FLAG_PICTURE, flagPicture)
        return intent
    }

    private fun initView(path: String, flagPicture: Boolean) {
        camera_cancel_btn.postDelayed({
            camera_cancel_btn.setType(CameraButton.TYPE_CANCEL)
            camera_comfirm_btn.setType(CameraButton.TYPE_CONFIRM)
        }, 250)

        //取消
        camera_cancel_btn.setOnClickListener {
            setResult(Activity.RESULT_CANCELED, getResultIntent())
            finish()
        }
        //确认
        camera_comfirm_btn.setOnClickListener {
            setResult(Activity.RESULT_OK, getResultIntent())
            finish()
        }

        if (flagPicture) {
            preview_photo_img.visibility = View.VISIBLE
            previewPhoto(path, preview_photo_img)
        } else {
            preview_surfaceView.visibility = View.VISIBLE
            playVideo(path, preview_surfaceView)
        }
    }

    open fun previewPhoto(path: String, photoImg: ImageView) {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        val bitmap = BitmapFactory.decodeFile(path, options)
        bitmap?.recycle()
        val sampleSize = 1080 / options.outWidth
        options.inJustDecodeBounds = false
        if (sampleSize > 1) {
            options.inSampleSize = sampleSize
        }
        photoImg.setImageBitmap(BitmapFactory.decodeFile(path, options))
    }

    private fun releaseImageViewResouce(imageView: ImageView) {
        val drawable = imageView.drawable
        if (drawable != null && drawable is BitmapDrawable) {
            drawable.bitmap.let {
                if (!it.isRecycled)
                    it.recycle()
            }
        }
    }

    private var mediaPlayer: MediaPlayer? = null
    open fun playVideo(path: String, surfaceView: SurfaceView) {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        //下面开始实例化MediaPlayer对象
        mediaPlayer = MediaPlayer()

        mediaPlayer?.let {
            //设置数据数据源，也就播放文件地址，可以是网络地址
            try {
                it.setDataSource(path)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            it.isLooping = true
            //只有当播放器准备好了之后才能够播放，所以播放的出发只能在触发了prepare之后
            it.setOnPreparedListener { _ ->
                it.setOnVideoSizeChangedListener { _, width, height ->
                    refreshPortraitScreen(width, height, surfaceView)
                }

                Thread(Runnable {
                    it.start()
                }).start()
            }

            /*
                向player中设置dispay，也就是SurfaceHolder。
                但此时有可能SurfaceView还没有创建成功，所以需要监听SurfaceView的创建事件
             */
            surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    //将播放器和SurfaceView关联起来
                    it.setDisplay(holder)
                    //异步缓冲当前视频文件，也有一个同步接口
                    it.prepareAsync()
                }

                override fun surfaceChanged(
                    holder: SurfaceHolder,
                    format: Int,
                    width: Int,
                    height: Int
                ) {
                }

                override fun surfaceDestroyed(holder: SurfaceHolder) {
                }
            })
        }
    }

    //重新刷新 竖屏显示的大小  树屏显示以宽度为准
    private fun refreshPortraitScreen(videoWidth: Int, videoHeight: Int, surfaceView: SurfaceView) {
        if (videoHeight == 0) return
        val scale = (1.00f * videoHeight) / surfaceView.measuredHeight
        val lp = surfaceView.layoutParams
        lp.height = (scale * surfaceView.measuredHeight).toInt()
        surfaceView.layoutParams = lp
    }
}