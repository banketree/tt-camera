package com.banketree.tt_camera_demo

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.miracles.camera.CameraFunctions
import com.miracles.camera.CameraView
import kotlinx.android.synthetic.main.view_camera_control.view.*

/**
 * Created by lxw
 */
class CameraControls : FrameLayout {
    private var mCameraView: CameraView? = null
    private var mPendingVideoCapture = false
    private var mCapturingVideo = false

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initView()
    }

    @RequiresApi(21)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        initView()
    }

    private fun initView() {
        LayoutInflater.from(context).inflate(R.layout.view_camera_control, this, true)
        setBackgroundColor(Color.TRANSPARENT)
        ivCapture.setOnTouchListener(this::capturePictureOrRecordVideo)
        setOnClickListener(object : DoubleTap() {
            override fun onDoubleClicked(view: View) {
                val cameraView = mCameraView ?: return
                val facing = cameraView.getFacing()
                if (facing == CameraFunctions.FACING_BACK) {
                    cameraView.setFacing(CameraFunctions.FACING_FRONT)
                } else {
                    cameraView.setFacing(CameraFunctions.FACING_BACK)
                }

            }

//            override fun onClicked(view: View) {
//                val cameraView = mCameraView ?: return
//                val zoom = cameraView.getZoom()
//                cameraView.setZoom(zoom+1)
//            }
        })
//        setOnTouchListener { v, event ->
//            val cameraView = mCameraView ?: return@setOnTouchListener false
//            if (event.action == MotionEvent.ACTION_DOWN) {
//                cameraView.focus(Rect((event.x - 100).toInt(), (event.y - 100).toInt(),
//                        (event.x + 100).toInt(), (event.y + 100).toInt()), {
//                    Toast.makeText(cameraView.context, "ManuallyFocused!", Toast.LENGTH_SHORT).show()
//                })
//            }
//            return@setOnTouchListener false
//        }
    }

    private open class DoubleTap() : OnClickListener {
        private var mHint = 0

        override fun onClick(view: View) {
            if (++mHint == 1) {
                view.postDelayed({
                    if (mHint == 1) {
                        onClicked(view)
                    }
                    mHint = 0
                }, 250)
            } else {
                onDoubleClicked(view)
            }
        }

        open fun onClicked(view: View) {}
        open fun onDoubleClicked(view: View) {}

    }

    fun bindCameraView(cameraView: CameraView) {
        this.mCameraView = cameraView
    }

    private fun capturePictureOrRecordVideo(v: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchDownAnimation(v)
                mPendingVideoCapture = true
                postDelayed({
                    if (mPendingVideoCapture) {
                        mCapturingVideo = true
                        mCameraView?.startRecord()
                        Toast.makeText(context, "正在录制...", Toast.LENGTH_SHORT).show()
                    }
                }, 250)
            }
            MotionEvent.ACTION_UP -> {
                touchUpAnimation(v)
                mPendingVideoCapture = false
                if (mCapturingVideo) {
                    mCapturingVideo = false
                    mCameraView?.stopRecord()
                } else {
                    mCameraView?.takePicture()
                }
            }
        }
        return true
    }

    private fun touchDownAnimation(view: View) {
        view.animate()
                .scaleX(0.88f)
                .scaleY(0.88f)
                .setDuration(300)
                .setInterpolator(OvershootInterpolator())
                .start()
    }

    private fun touchUpAnimation(view: View) {
        view.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .setInterpolator(OvershootInterpolator())
                .start()
    }
}