package com.banketree.tt_camera_demo.yuv

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.os.Bundle
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
        private const val PARAMS_PATH = "path"
        private const val PARAMS_FLAG_PICTURE = "flag.picture"
        fun start(context: Context, path: String, flagPicture: Boolean) {
            val intent = Intent(context, PreviewActivity::class.java)
            intent.putExtra(PARAMS_PATH, path)
            intent.putExtra(PARAMS_FLAG_PICTURE, flagPicture)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.yuv_activity_preview)
        val intent = intent
        val path = intent.getStringExtra(PARAMS_PATH)
        val flagPicture = intent.getBooleanExtra(PARAMS_FLAG_PICTURE, true)
        if (path == null || !File(path).exists()) {
            finish()
        } else {
            initView(path, flagPicture)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.stop()
        mediaPlayer?.release()
    }

    private fun initView(path: String, flagPicture: Boolean) {
        if (flagPicture) {
            photoView.visibility = View.VISIBLE
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(path, options)
            val sampleSize = 1080 / options.outWidth
            options.inJustDecodeBounds = false
            if (sampleSize > 1) {
                options.inSampleSize = sampleSize
            }
            Glide.with(this).load(path).into(photoView)
        } else {
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
            preview_surfaceView.visibility = View.VISIBLE
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