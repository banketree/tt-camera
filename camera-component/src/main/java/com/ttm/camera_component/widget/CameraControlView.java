package com.ttm.camera_component.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.CountDownTimer;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import com.ttm.camera_component.listener.CameraControlListener;

public class CameraControlView extends View {
    //空闲状态
    public static final int STATE_IDLE = 0x001;
    //按下状态
    public static final int STATE_PRESS = 0x002;
    //长按状态
    public static final int STATE_LONG_PRESS = 0x003;
    //录制状态
    public static final int STATE_RECORDERING = 0x004;
    //禁止状态
    public static final int STATE_BAN = 0x005;

    //进度条颜色
    private int progress_color = 0xEE16AE16;
    //外圆背景色
    private int outside_color = 0xEEDCDCDC;
    //内圆背景色
    private int inside_color = 0xFFFFFFFF;

    //只能拍照
    public static final int FUN_TYPE_ONLY_CAPTURE = 0x101;
    //只能录像
    public static final int FUN_TYPE_ONLY_RECORDER = 0x102;
    //两者都可以
    public static final int FUN_TYPE_BOTH = 0x103;

    //当前按钮状态
    private int state;
    //按钮可执行的功能状态（拍照,录制,两者）
    private int funType;

    private Paint mPaint;
    //进度条宽度
    private float strokeWidth;
    //长按外圆半径变大的Size
    private int outside_add_size;
    //长安内圆缩小的Size
    private int inside_reduce_size;

    //中心坐标
    private float center_X;
    private float center_Y;
    //按钮半径
    private float button_radius;
    //外圆半径
    private float button_outside_radius;
    //内圆半径
    private float button_inside_radius;
    //按钮大小
    private int button_size = 0;
    //录制视频的进度
    private float progress;
    //录制视频最大时间长度
    private int duration;

    private RectF rectF;
    //长按后处理的逻辑Runnable
    private LongPressRunnable longPressRunnable;
    //计时器
    private RecordCountDownTimer timer;

    //按钮回调接口
    private CameraControlListener cameraControlListener;

    public CameraControlView(Context context) {
        this(context, null);
        initView(context);
    }

    public CameraControlView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
        initView(context);
    }

    public CameraControlView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    void initView(Context context) {
        button_size = 150;
        button_radius = button_size / 2.0f;

        button_outside_radius = button_radius;
        button_inside_radius = button_radius * 0.75f;

        strokeWidth = button_size / 15;
        outside_add_size = button_size / 5;
        inside_reduce_size = button_size / 8;

        center_X = (button_size + outside_add_size * 2) / 2;
        center_Y = (button_size + outside_add_size * 2) / 2;

        rectF = new RectF(
                center_X - (button_radius + outside_add_size - strokeWidth / 2),
                center_Y - (button_radius + outside_add_size - strokeWidth / 2),
                center_X + (button_radius + outside_add_size - strokeWidth / 2),
                center_Y + (button_radius + outside_add_size - strokeWidth / 2));

        timer = new RecordCountDownTimer(duration, duration / 360);    //录制定时器
        postInvalidate();

        mPaint = new Paint();
        mPaint.setAntiAlias(true);

        progress = 0;
        longPressRunnable = new LongPressRunnable();

        //初始化为空闲状态
        state = STATE_IDLE;
        //初始化按钮为可录制可拍照
        funType = FUN_TYPE_BOTH;
        //默认最长录制时间为10s
        duration = 10 * 1000;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(button_size + outside_add_size * 2, button_size + outside_add_size * 2);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mPaint.setStyle(Paint.Style.FILL);
        //外圆（半透明灰色）
        mPaint.setColor(outside_color);
        canvas.drawCircle(center_X, center_Y, button_outside_radius, mPaint);
        //内圆（白色）
        mPaint.setColor(inside_color);
        canvas.drawCircle(center_X, center_Y, button_inside_radius, mPaint);

        //如果状态为录制状态，则绘制录制进度条
        if (state == STATE_RECORDERING) {
            mPaint.setColor(progress_color);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(strokeWidth);
            canvas.drawArc(rectF, -90, progress, false, mPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                if (event.getPointerCount() > 1 || state != STATE_IDLE) {
                    break;
                }

                //修改当前状态为点击按下
                state = STATE_PRESS;

                //判断按钮状态是否为可录制状态
                if ((funType == FUN_TYPE_ONLY_RECORDER || funType == FUN_TYPE_BOTH)) {
                    //同时延长250启动长按后处理的逻辑Runnable
                    postDelayed(longPressRunnable, 250);
                }
            }
            break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                //根据当前按钮的状态进行相应的处理
                handlerUnpressByState();
            }
            break;
            default:
                break;
        }
        return true;
    }

    //当手指松开按钮时候处理的逻辑
    private void handlerUnpressByState() {
        //移除长按逻辑的Runnable
        removeCallbacks(longPressRunnable);
        //根据当前状态处理
        switch (state) {
            //当前是点击按下
            case STATE_PRESS: {
                if (cameraControlListener != null
                        && (funType == FUN_TYPE_ONLY_CAPTURE
                        || funType == FUN_TYPE_BOTH)) {
                    startCaptureAnimation(button_inside_radius);
                } else {
                    state = STATE_IDLE;
                }
            }
            break;
            //当前是长按状态
            case STATE_RECORDERING: {
                timer.cancel(); //停止计时器
                recordEnd();    //录制结束
            }
            break;
            default:
                break;
        }
    }

    //录制结束
    private void recordEnd() {
        if (cameraControlListener != null) {
            //回调录制结束  如果录制时间过短 则是业务逻辑处理
            cameraControlListener.onEndRecordVideo();
        }
        resetRecordAnim();  //重制按钮状态
    }

    //重制状态
    private void resetRecordAnim() {
        state = STATE_BAN;
        //重制进度
        progress = 0;
        invalidate();
        //还原按钮初始状态动画
        startRecordAnimation(
                button_outside_radius,
                button_radius,
                button_inside_radius,
                button_radius * 0.75f
        );
    }

    //内圆动画
    private void startCaptureAnimation(float inside_start) {
        ValueAnimator inside_anim = ValueAnimator.ofFloat(inside_start, inside_start * 0.5f, inside_start);
        inside_anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                button_inside_radius = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        inside_anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                //回调拍照接口
                if (cameraControlListener != null) cameraControlListener.onTakePictures();
                state = STATE_BAN;
            }
        });
        inside_anim.setDuration(100);
        inside_anim.start();
    }

    //内外圆动画
    private void startRecordAnimation(float outside_start, float outside_end, float inside_start, float inside_end) {
        ValueAnimator outside_anim = ValueAnimator.ofFloat(outside_start, outside_end);
        ValueAnimator inside_anim = ValueAnimator.ofFloat(inside_start, inside_end);
        //外圆动画监听
        outside_anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                button_outside_radius = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        //内圆动画监听
        inside_anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                button_inside_radius = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        AnimatorSet set = new AnimatorSet();
        //当动画结束后启动录像Runnable并且回调录像开始接口
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                //设置为录制状态
                if (state == STATE_LONG_PRESS) {
                    if (cameraControlListener != null) cameraControlListener.onStartRecordVideo();
                    state = STATE_RECORDERING;
                    timer.start();
                }
            }
        });
        set.playTogether(outside_anim, inside_anim);
        set.setDuration(100);
        set.start();
    }


    //更新进度条
    private void updateProgress(long millisUntilFinished) {
        progress = 360f - millisUntilFinished / (float) duration * 360f;
        invalidate();
    }

    //录制视频计时器
    private class RecordCountDownTimer extends CountDownTimer {
        RecordCountDownTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onTick(long millisUntilFinished) {
            updateProgress(millisUntilFinished);
        }

        @Override
        public void onFinish() {
            updateProgress(0);
            recordEnd();
        }
    }

    //长按线程
    private class LongPressRunnable implements Runnable {
        @Override
        public void run() {
            state = STATE_LONG_PRESS;   //如果按下后经过500毫秒则会修改当前状态为长按状态
            //启动按钮动画，外圆变大，内圆缩小
            startRecordAnimation(
                    button_outside_radius,
                    button_outside_radius + outside_add_size,
                    button_inside_radius,
                    button_inside_radius - inside_reduce_size
            );
        }
    }

    /**************************************************
     * 对外提供的API                     *
     **************************************************/
    /**
     * 设置最长录制时间
     */
    public void setDuration(int time) {
        this.duration = time * 1000;
        //录制定时器
        timer = new RecordCountDownTimer(duration, duration / 360);
    }

    /**
     * 设置回调接口
     */
    public void setCameraControlListener(CameraControlListener cameraControlListener) {
        this.cameraControlListener = cameraControlListener;
    }

    /**
     * 设置按钮功能（拍照和录像）
     */
    public void setFunType(int type) {
        this.funType = type;
    }

    /**
     * 是否空闲状态
     */
    public boolean isIdle() {
        return state == STATE_IDLE ? true : false;
    }

    /**
     * 设置状态
     */
    public void resetState() {
        state = STATE_IDLE;
    }
}
