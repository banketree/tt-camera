package com.banketree.tt_camera_demo

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.banketree.tt_camera_demo.yuv.CameraActivity
import com.banketree.tt_camera_demo.yuv.CameraButton
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        test_tv.setOnClickListener {
            startActivity(Intent(this, TakeCameraActivity::class.java))
        }
        test_two.setOnClickListener {
            startActivity(Intent(this, CameraActivity::class.java))
        }
        test_three.setOnClickListener {
        }
    }
}
