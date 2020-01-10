package com.banketree.tt_camera_demo

import android.Manifest
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Bitmap
import android.os.Environment
import com.ttm.lib_camera.listener.JCameraListener
import java.io.File
import android.os.Build
import android.view.View
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_camera.*


/**
 * @author banketree
 * @time 2020/1/10 14:00
 * @description
 * 获取视频 或 图片
 */
class TakeCameraActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //全屏显示
        initFullScreen()
        setContentView(R.layout.activity_camera)
        initView()
    }

    /**
     * 全屏显示
     */
    fun initFullScreen() {
        if (Build.VERSION.SDK_INT >= 19) {
            val decorView = window.decorView
            decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        } else {
            val decorView = window.decorView
            val option = View.SYSTEM_UI_FLAG_FULLSCREEN
            decorView.systemUiVisibility = option
        }
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onResume() {
        super.onResume()
        if (!isPermissionsOK()) { //请确认权限是否开启
            Toast.makeText(this, "权限失败", Toast.LENGTH_SHORT)
                .show()
            return
        }
        jcameraview.onResume()
    }

    override fun onPause() {
        super.onPause()
        jcameraview.onPause()
    }

    fun initView() {
        //设置视频保存路径
        jcameraview.setSaveVideoPath(Environment.getExternalStorageDirectory().path + File.separator + "Camera")
        //JCameraView监听
        jcameraview.setJCameraLisenter(object : JCameraListener {
            override fun captureSuccess(bitmap: Bitmap) {
                //获取图片bitmap
//                Log.i("JCameraView", "bitmap = " + bitmap.width)
            }

            override fun recordSuccess(url: String, firstFrame: Bitmap) {
            }

            //退出按钮
            override fun quit() {
                finish()
            }
        })
    }

    /**
     * 权限判断
     */
    private fun isPermissionsOK(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                //具有权限
                return true
            }
        } else { //低于6.0 则自动获取
            return true
        }

        return false
    }
}