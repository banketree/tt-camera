package com.banketree.tt_camera_demo.yuv


import android.annotation.TargetApi
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.Build
import android.util.AttributeSet
import android.view.View


class CameraButton : View {
    private var button_type: Int = 0

    private var center_X: Float = 0f
    private var center_Y: Float = 0f
    private var button_radius: Float = 0f

    private lateinit var mPaint: Paint
    private lateinit var path: Path
    private var strokeWidth: Float = 0f

    private var index: Float = 0f
    private lateinit var rectF: RectF

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init()
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(
        context,
        attrs,
        defStyleAttr,
        defStyleRes
    ) {
        init()
    }

    private fun init() {
        val button_size = measuredWidth
        button_radius = button_size / 2.0f
        center_X = button_size / 2.0f
        center_Y = button_size / 2.0f

        mPaint = Paint()
        path = Path()
        strokeWidth = button_size / 50f
        index = button_size / 12f
        rectF = RectF(center_X, center_Y - index, center_X + index * 2, center_Y + index)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    fun setType(type: Int) {
        button_type = type
        init()
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        when (button_type) {
            TYPE_CONFIRM -> {
                //如果类型为确认，则绘制绿色勾
                mPaint.isAntiAlias = true
                mPaint.color = -0x1
                mPaint.style = Paint.Style.FILL
                canvas.drawCircle(center_X, center_Y, button_radius, mPaint)
                mPaint.isAntiAlias = true
                mPaint.style = Paint.Style.STROKE
                mPaint.color = -0xff3400
                mPaint.strokeWidth = strokeWidth

                path.moveTo(center_X - measuredWidth / 6f, center_Y)
                path.lineTo(center_X - measuredWidth / 21.2f, center_Y + measuredWidth / 7.7f)
                path.lineTo(center_X + measuredWidth / 4.0f, center_Y - measuredWidth / 8.5f)
                path.lineTo(center_X - measuredWidth / 21.2f, center_Y + measuredWidth / 9.4f)
                path.close()
                canvas.drawPath(path, mPaint)
            }
            TYPE_CANCEL -> {
                //如果类型为取消，则绘制内部为返回箭头
                mPaint.isAntiAlias = true
                mPaint.color = -0x11232324
                mPaint.style = Paint.Style.FILL
                canvas.drawCircle(center_X, center_Y, button_radius, mPaint)

                mPaint.color = Color.BLACK
                mPaint.style = Paint.Style.STROKE
                mPaint.strokeWidth = strokeWidth

                path.moveTo(center_X - index / 7, center_Y + index)
                path.lineTo(center_X + index, center_Y + index)

                path.arcTo(rectF, 90f, -180f)
                path.lineTo(center_X - index, center_Y - index)
                canvas.drawPath(path, mPaint)
                mPaint.style = Paint.Style.FILL
                path.reset()
                path.moveTo(center_X - index, (center_Y - index * 1.5).toFloat())
                path.lineTo(center_X - index, (center_Y - index / 2.3).toFloat())
                path.lineTo((center_X - index * 1.6).toFloat(), center_Y - index)
                path.close()
                canvas.drawPath(path, mPaint)
            }
        }
    }

    companion object {
        //确认
        const val TYPE_CANCEL = 0x001
        //取消
        const val TYPE_CONFIRM = 0x002
    }
}
