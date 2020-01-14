package com.banketree.tt_camera_demo.yuv

import android.app.Activity
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.text.TextUtils
import android.view.SurfaceHolder
import android.view.View
import com.banketree.tt_camera_demo.R
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.yuv_activity_preview.*
import java.io.File


/**
 * Created by lxw
 */
class PreviewActivity : BaseActivity() {

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

        setContentView(R.layout.yuv_activity_preview)
        initView(path, flagPicture)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.stop()
        mediaPlayer?.release()
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
            photoView.visibility = View.VISIBLE
            Glide.with(this).load(path).into(photoView)
        } else {
            preview_surfaceView.visibility = View.VISIBLE
            playVideo(path)
        }
    }

    private var mediaPlayer: MediaPlayer? = null
    fun playVideo(path: String) {
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
                    refreshPortraitScreen(width, height)
                }

                Thread(Runnable {
                    it.start()
                }).start()
            }

            /*
                向player中设置dispay，也就是SurfaceHolder。
                但此时有可能SurfaceView还没有创建成功，所以需要监听SurfaceView的创建事件
             */
            preview_surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
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
    private fun refreshPortraitScreen(videoWidth: Int, videoHeight: Int) {
        if (videoHeight == 0) return
        val scale = (1.00f * videoHeight) / preview_surfaceView.measuredHeight
        val lp = preview_surfaceView.layoutParams
        lp.height = (scale * preview_surfaceView.measuredHeight).toInt()
        preview_surfaceView.layoutParams = lp
    }
}