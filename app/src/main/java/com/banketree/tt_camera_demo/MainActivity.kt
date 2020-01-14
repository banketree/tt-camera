package com.banketree.tt_camera_demo

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import com.ttm.camera_component.ui.CameraActivity
import com.ttm.camera_component.ui.PreviewActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        test_tv.setOnClickListener {
            startActivityForResult(Intent(this, TakeCameraActivity::class.java), 200)
        }
        test_two.setOnClickListener {
        }
        test_three.setOnClickListener {
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 200 && data != null) {
            val path = data.getStringExtra(PreviewActivity.PARAMS_PATH)
            val flagPicture = data.getBooleanExtra(PreviewActivity.PARAMS_FLAG_PICTURE, true)
            if (TextUtils.isEmpty(path) || !File(path).exists()) {
                return
            }
            if (resultCode == Activity.RESULT_OK) {
                val intent = Intent()
                intent.putExtra(PreviewActivity.PARAMS_PATH, path)
                intent.putExtra(PreviewActivity.PARAMS_FLAG_PICTURE, flagPicture)
                Toast.makeText(this, path, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
