package com.ttm.lib_camera;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;


import androidx.annotation.RequiresApi;

import com.ttm.lib_camera.listener.CaptureListener;
import com.ttm.lib_camera.listener.ErrorListener;
import com.ttm.lib_camera.listener.JCameraListener;
import com.ttm.lib_camera.listener.TypeListener;
import com.ttm.lib_camera.state.CameraMachine;
import com.ttm.lib_camera.util.FileUtil;
import com.ttm.lib_camera.util.LogUtil;
import com.ttm.lib_camera.util.ScreenUtils;
import com.ttm.lib_camera.view.CameraView;

import java.io.IOException;


/**
 * =====================================
 * 作    者: 陈嘉桐
 * 版    本：1.0.4
 * 创建日期：2017/4/25
 * 描    述：
 * =====================================
 */
public class JCameraView extends FrameLayout implements CameraInterface.CameraOpenOverCallback, SurfaceHolder
        .Callback, CameraView {
    //Camera状态机
    private CameraMachine machine;

    //闪关灯状态
    private static final int TYPE_FLASH_AUTO = 0x021;
    private static final int TYPE_FLASH_ON = 0x022;
    private static final int TYPE_FLASH_OFF = 0x023;
    private int type_flash = TYPE_FLASH_OFF;

    //拍照浏览时候的类型
    public static final int TYPE_PICTURE = 0x001;
    public static final int TYPE_VIDEO = 0x002;
    public static final int TYPE_SHORT = 0x003;
    public static final int TYPE_DEFAULT = 0x004;

    //录制视频比特率
    public static final int MEDIA_QUALITY_HIGH = 20 * 100000;
    public static final int MEDIA_QUALITY_MIDDLE = 16 * 100000;
    public static final int MEDIA_QUALITY_LOW = 12 * 100000;
    public static final int MEDIA_QUALITY_POOR = 8 * 100000;
    public static final int MEDIA_QUALITY_FUNNY = 4 * 100000;
    public static final int MEDIA_QUALITY_DESPAIR = 2 * 100000;
    public static final int MEDIA_QUALITY_SORRY = 1 * 80000;

    //只能拍照
    public static final int BUTTON_STATE_ONLY_CAPTURE = 0x101;
    //只能录像
    public static final int BUTTON_STATE_ONLY_RECORDER = 0x102;
    //两者都可以
    public static final int BUTTON_STATE_BOTH = 0x103;

    //回调监听
    private JCameraListener jCameraLisenter;

    private Context mContext;
    private VideoView mVideoView;
    private ImageView mBackImg;
    private TextView mRatioTv; //比例 16：9 ||   4：3  ||  1：1   ||   9：16
    private ImageView mPhoto;
    private ImageView mSwitchCamera;
    private ImageView mFlashLamp;
    private RatioVideoView mRatioView;
    private CaptureLayout mCaptureLayout;
    private FoucsView mFoucsView;
    private MediaPlayer mMediaPlayer;

    private int layout_width;
    private float screenProp = 0f;

    //捕获的图片
    private Bitmap captureBitmap;
    //第一帧图片
    private Bitmap firstFrame;
    //视频URL
    private String videoUrl;


    //切换摄像头按钮的参数
    //录制时间
    private int duration = 0;

    //缩放梯度
    private int zoomGradient = 0;

    private boolean firstTouch = true;
    private float firstTouchLength = 0;

    public JCameraView(Context context) {
        this(context, null);
    }

    public JCameraView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public JCameraView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        //没设置默认为60s
        duration = 60 * 1000;
        initData();
        initView();
    }

    private void initData() {
        layout_width = ScreenUtils.getScreenWidth(mContext);
        //缩放梯度
        zoomGradient = (int) (layout_width / 16f);
        LogUtil.i("zoom = " + zoomGradient);
        machine = new CameraMachine(getContext(), this, this);
    }

    private void initView() {
        setWillNotDraw(false);
        View view = LayoutInflater.from(mContext).inflate(R.layout.camera_view, this);
        mVideoView = view.findViewById(R.id.video_preview);
        mBackImg = view.findViewById(R.id.image_back);
        mRatioTv = view.findViewById(R.id.ratio_tv);
        mPhoto = view.findViewById(R.id.image_photo);
        mSwitchCamera = view.findViewById(R.id.image_switch);
        mFlashLamp = view.findViewById(R.id.image_flash);
        mRatioView = view.findViewById(R.id.ratio_view);
        setFlashRes();
        mFlashLamp.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                type_flash++;
                if (type_flash > 0x023) {
                    type_flash = TYPE_FLASH_AUTO;
                }
                setFlashRes();
            }
        });
        mCaptureLayout = (CaptureLayout) view.findViewById(R.id.capture_layout);
        mCaptureLayout.setDuration(duration);
        mFoucsView = (FoucsView) view.findViewById(R.id.fouce_view);
        mVideoView.getHolder().addCallback(this); //插入到视频中
        //切换摄像头
        mSwitchCamera.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                machine.swtich(mVideoView.getHolder(), screenProp);
            }
        });
        //拍照 录像
        mCaptureLayout.setCaptureLisenter(new CaptureListener() {
            @Override
            public void takePictures() {
                mSwitchCamera.setVisibility(INVISIBLE);
                mFlashLamp.setVisibility(INVISIBLE);
                mBackImg.setVisibility(INVISIBLE);
                mRatioTv.setVisibility(INVISIBLE);

                machine.capture();
            }

            @Override
            public void recordStart() {
                mSwitchCamera.setVisibility(INVISIBLE);
                mFlashLamp.setVisibility(INVISIBLE);
                mBackImg.setVisibility(INVISIBLE);
                mRatioTv.setVisibility(INVISIBLE);
                machine.record(mVideoView.getHolder().getSurface(), screenProp);
            }

            @Override
            public void recordShort(final long time) {
                mCaptureLayout.setTextWithAnimation("录制时间过短");
                mSwitchCamera.setVisibility(VISIBLE);
                mFlashLamp.setVisibility(VISIBLE);
                mBackImg.setVisibility(VISIBLE);
                mRatioTv.setVisibility(VISIBLE);
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        machine.stopRecord(true, time);
                    }
                }, 1500 - time);
            }

            @Override
            public void recordEnd(long time) {
                machine.stopRecord(false, time);
            }

            @Override
            public void recordZoom(float zoom) {
                LogUtil.i("recordZoom");
                machine.zoom(zoom, CameraInterface.TYPE_RECORDER);
            }

            @Override
            public void recordError() {
                if (errorLisenter != null) {
                    errorLisenter.AudioPermissionError();
                }
            }
        });
        //确认 取消
        mCaptureLayout.setTypeLisenter(new TypeListener() {
            @Override
            public void cancel() {
                machine.cancle(mVideoView.getHolder(), screenProp);
            }

            @Override
            public void confirm() {
                machine.confirm();
            }
        });

        mBackImg.setOnClickListener(v -> {
            if (jCameraLisenter != null) jCameraLisenter.quit();
        });

        mRatioTv.setOnClickListener(v -> {
            int radioType = getRatioType();
            if (radioType == RatioVideoView.RATIO_16_9) {
                mRatioView.setRadioType(RatioVideoView.RATIO_4_3);
                mRatioTv.setText("4:3");
            } else if (radioType == RatioVideoView.RATIO_4_3) {
                mRatioView.setRadioType(RatioVideoView.RATIO_1_1);
                mRatioTv.setText("1:1");
            } else if (radioType == RatioVideoView.RATIO_1_1) {
                mRatioView.setRadioType(RatioVideoView.RATIO_16_9);
                mRatioTv.setText("16:9");
            }
        });
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        float widthSize = mVideoView.getMeasuredWidth();
        float heightSize = mVideoView.getMeasuredHeight();
        if (screenProp == 0) {
            screenProp = heightSize / widthSize;
        }
    }

    @Override
    public void cameraHasOpened() {
        CameraInterface.getInstance().doStartPreview(mVideoView.getHolder(), screenProp);
    }

    //生命周期onResume
    public void onResume() {
        LogUtil.i("JCameraView onResume");
        resetState(TYPE_DEFAULT); //重置状态
        CameraInterface.getInstance().registerSensorManager(mContext);
        CameraInterface.getInstance().setSwitchView(mSwitchCamera, mFlashLamp);
        machine.start(mVideoView.getHolder(), screenProp);
    }

    //生命周期onPause
    public void onPause() {
        LogUtil.i("JCameraView onPause");
        stopVideo();
        resetState(TYPE_PICTURE);
        CameraInterface.getInstance().isPreview(false);
        CameraInterface.getInstance().unregisterSensorManager(mContext);
    }

    //SurfaceView生命周期
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        LogUtil.i("JCameraView SurfaceCreated");
        new Thread() {
            @Override
            public void run() {
                CameraInterface.getInstance().doOpenCamera(JCameraView.this);
            }
        }.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        LogUtil.i("JCameraView SurfaceDestroyed");
        CameraInterface.getInstance().doDestroyCamera();
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (event.getPointerCount() == 1) {
                    //显示对焦指示器
                    setFocusViewWidthAnimation(event.getX(), event.getY());
                }
                if (event.getPointerCount() == 2) {
                    Log.i("CJT", "ACTION_DOWN = " + 2);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (event.getPointerCount() == 1) {
                    firstTouch = true;
                }
                if (event.getPointerCount() == 2) {
                    //第一个点
                    float point_1_X = event.getX(0);
                    float point_1_Y = event.getY(0);
                    //第二个点
                    float point_2_X = event.getX(1);
                    float point_2_Y = event.getY(1);

                    float result = (float) Math.sqrt(Math.pow(point_1_X - point_2_X, 2) + Math.pow(point_1_Y -
                            point_2_Y, 2));

                    if (firstTouch) {
                        firstTouchLength = result;
                        firstTouch = false;
                    }
                    if ((int) (result - firstTouchLength) / zoomGradient != 0) {
                        firstTouch = true;
                        machine.zoom(result - firstTouchLength, CameraInterface.TYPE_CAPTURE);
                    }
//                    Log.i("CJT", "result = " + (result - firstTouchLength));
                }
                break;
            case MotionEvent.ACTION_UP:
                firstTouch = true;
                break;
        }
        return true;
    }

    //对焦框指示器动画
    private void setFocusViewWidthAnimation(float x, float y) {
        machine.foucs(x, y, new CameraInterface.FocusCallback() {
            @Override
            public void focusSuccess() {
                mFoucsView.setVisibility(INVISIBLE);
            }
        });
    }

    private void updateVideoViewSize(float videoWidth, float videoHeight) {
        if (videoWidth > videoHeight) {
            LayoutParams videoViewParam;
            int height = (int) ((videoHeight / videoWidth) * getWidth());
            videoViewParam = new LayoutParams(LayoutParams.MATCH_PARENT, height);
            videoViewParam.gravity = Gravity.CENTER;
            mVideoView.setLayoutParams(videoViewParam);
        }
    }

    /**************************************************
     * 对外提供的API                     *
     **************************************************/

    public void setSaveVideoPath(String path) {
        CameraInterface.getInstance().setSaveVideoPath(path);
    }


    public void setJCameraLisenter(JCameraListener jCameraLisenter) {
        this.jCameraLisenter = jCameraLisenter;
    }


    private ErrorListener errorLisenter;

    /**
     * 启动Camera错误回调
     */
    public void setErrorLisenter(ErrorListener errorLisenter) {
        this.errorLisenter = errorLisenter;
        CameraInterface.getInstance().setErrorLinsenter(errorLisenter);
    }

    /**
     * 设置CaptureButton功能（拍照和录像）
     */
    public void setFeatures(int state) {
        this.mCaptureLayout.setButtonFeatures(state);
    }

    /**
     * 设置录制质量
     */
    public void setMediaQuality(int quality) {
        CameraInterface.getInstance().setMediaQuality(quality);
    }

    @Override
    public void resetState(int type) {
        switch (type) {
            case TYPE_VIDEO: {
                stopVideo();    //停止播放
                //初始化VideoView
                FileUtil.deleteFile(videoUrl);
                mVideoView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
                machine.start(mVideoView.getHolder(), screenProp);
            }
            break;
            case TYPE_PICTURE: {
                mPhoto.setVisibility(INVISIBLE);
            }
            break;
            case TYPE_SHORT: {
            }
            break;
            case TYPE_DEFAULT: {
                mVideoView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            }
            break;
            default:
                break;
        }
        mSwitchCamera.setVisibility(VISIBLE);
        mFlashLamp.setVisibility(VISIBLE);
        mBackImg.setVisibility(VISIBLE);
        mRatioTv.setVisibility(VISIBLE);
        mCaptureLayout.resetCaptureLayout();
    }

    @Override
    public void confirmState(int type) {
        switch (type) {
            case TYPE_VIDEO: {
                stopVideo();    //停止播放
                mVideoView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
                machine.start(mVideoView.getHolder(), screenProp);
                if (jCameraLisenter != null) {
                    jCameraLisenter.recordSuccess(videoUrl, firstFrame);
                }
            }
            break;
            case TYPE_PICTURE: {
                mPhoto.setVisibility(INVISIBLE);
                if (jCameraLisenter != null) {
                    Rect frameRect = mRatioView.getRatioAreaRect(CameraInterface.getInstance().getPreviewWidth(), CameraInterface.getInstance().getPreviewHeight());
                    if (frameRect != null) {
                        captureBitmap = Bitmap.createBitmap(captureBitmap, frameRect.left, frameRect.top, frameRect.width(), frameRect.height());
                    }
                    jCameraLisenter.captureSuccess(captureBitmap);
                }
            }
            break;
            case TYPE_SHORT:
                break;
            case TYPE_DEFAULT:
                break;
            default:
                break;
        }
        mCaptureLayout.resetCaptureLayout();
    }

    @Override
    public void showPicture(Bitmap bitmap, boolean isVertical) {
        if (isVertical) {
            mPhoto.setScaleType(ImageView.ScaleType.FIT_XY);
        } else {
            mPhoto.setScaleType(ImageView.ScaleType.FIT_CENTER);
        }
        captureBitmap = bitmap;
        mPhoto.setImageBitmap(bitmap);
        mPhoto.setVisibility(VISIBLE);
        mCaptureLayout.startAlphaAnimation();
        mCaptureLayout.startTypeBtnAnimator();
    }

    @Override
    public void playVideo(Bitmap firstFrame, final String url) {
        videoUrl = url;
        JCameraView.this.firstFrame = firstFrame;
        new Thread(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void run() {
                try {
                    if (mMediaPlayer == null) {
                        mMediaPlayer = new MediaPlayer();
                    } else {
                        mMediaPlayer.reset();
                    }
                    mMediaPlayer.setDataSource(url);
                    mMediaPlayer.setSurface(mVideoView.getHolder().getSurface());
                    mMediaPlayer.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT);
                    mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    mMediaPlayer.setOnVideoSizeChangedListener(new MediaPlayer
                            .OnVideoSizeChangedListener() {
                        @Override
                        public void
                        onVideoSizeChanged(MediaPlayer mp, int width, int height) {
                            updateVideoViewSize(mMediaPlayer.getVideoWidth(), mMediaPlayer
                                    .getVideoHeight());
                        }
                    });
                    mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            mMediaPlayer.start();
                        }
                    });
                    mMediaPlayer.setLooping(true);
                    mMediaPlayer.prepare();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    public void stopVideo() {
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    @Override
    public void setTip(String tip) {
        mCaptureLayout.setTip(tip);
    }

    @Override
    public void startPreviewCallback() {
        LogUtil.i("startPreviewCallback");
        handlerFoucs(mFoucsView.getWidth() / 2, mFoucsView.getHeight() / 2);
    }

    @Override
    public boolean handlerFoucs(float x, float y) {
        if (y > mCaptureLayout.getTop()) {
            return false;
        }
        mFoucsView.setVisibility(VISIBLE);
        if (x < mFoucsView.getWidth() / 2) {
            x = mFoucsView.getWidth() / 2;
        }
        if (x > layout_width - mFoucsView.getWidth() / 2) {
            x = layout_width - mFoucsView.getWidth() / 2;
        }
        if (y < mFoucsView.getWidth() / 2) {
            y = mFoucsView.getWidth() / 2;
        }
        if (y > mCaptureLayout.getTop() - mFoucsView.getWidth() / 2) {
            y = mCaptureLayout.getTop() - mFoucsView.getWidth() / 2;
        }
        mFoucsView.setX(x - mFoucsView.getWidth() / 2);
        mFoucsView.setY(y - mFoucsView.getHeight() / 2);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(mFoucsView, "scaleX", 1, 0.6f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(mFoucsView, "scaleY", 1, 0.6f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(mFoucsView, "alpha", 1f, 0.4f, 1f, 0.4f, 1f, 0.4f, 1f);
        AnimatorSet animSet = new AnimatorSet();
        animSet.play(scaleX).with(scaleY).before(alpha);
        animSet.setDuration(400);
        animSet.start();
        return true;
    }

    private void setFlashRes() {
        switch (type_flash) {
            case TYPE_FLASH_AUTO: {
                mFlashLamp.setImageResource(R.drawable.camera_ic_flash_auto);
                machine.flash(Camera.Parameters.FLASH_MODE_AUTO);
            }
            break;
            case TYPE_FLASH_ON: {
                mFlashLamp.setImageResource(R.drawable.camera_ic_flash_on);
                machine.flash(Camera.Parameters.FLASH_MODE_ON);
            }
            break;
            case TYPE_FLASH_OFF: {
                mFlashLamp.setImageResource(R.drawable.camera_ic_flash_off);
                machine.flash(Camera.Parameters.FLASH_MODE_OFF);
            }
            break;
        }
    }

    public int getRatioType() {
        return mRatioView.getRadioType();
    }
}