package com.banketree.tt_camera_demo.yuv

import android.annotation.TargetApi
import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.View
import android.view.WindowManager

class RatioView : View {
    companion object {
        const val RATIO_16_9 = 0 //16:9
        const val RATIO_4_3 = 1  //4:3
        const val RATIO_1_1 = 2  //1:1
    }

    private var framingRect: Rect? = null //显示的部分
    private var paint: Paint = Paint()
    var maskColor: Int = Color.BLACK
    private var radioType: Int = RATIO_16_9

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context)
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(
        context,
        attrs,
        defStyleAttr,
        defStyleRes
    ) {
        init(context)
    }

    private fun init(context: Context) {
        paint.isAntiAlias = true
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        // 画遮罩层
        drawMask(canvas)
    }

    /**
     * 画遮罩层
     *
     * @param canvas
     */
    private fun drawMask(canvas: Canvas?) {
        canvas ?: return
        val width = canvas.width
        val height = canvas.height

        framingRect?.let {
            if (maskColor != Color.TRANSPARENT) {
                paint.style = Paint.Style.FILL
                paint.color = maskColor
                canvas.drawRect(0f, 0f, width.toFloat(), it.top.toFloat(), paint)

                canvas.drawRect(
                    0f,
                    it.top.toFloat(),
                    it.left.toFloat(),
                    (it.bottom).toFloat(),// + 1
                    paint
                )
                canvas.drawRect(
                    (it.right).toFloat(),// + 1
                    it.top.toFloat(),
                    width.toFloat(),
                    (it.bottom).toFloat(),// + 1
                    paint
                )
                canvas.drawRect(
                    0f,
                    (it.bottom).toFloat(),// + 1
                    width.toFloat(),
                    height.toFloat(),
                    paint
                )
            }
        }
    }

//    /**
//     * 画边框线
//     *
//     * @param canvas
//     */
//    private fun drawBorderLine(canvas: Canvas) {
//        if (mBorderSize > 0) {
//            mPaint.setStyle(Paint.Style.STROKE)
//            mPaint.setColor(mBorderColor)
//            mPaint.setStrokeWidth(mBorderSize.toFloat())
//            canvas.drawRect(framingRect, mPaint)
//        }
//    }

    fun getRadioType(): Int {
        return radioType
    }

    //默认 竖屏
    fun setRadioType(radioType: Int) {
        this.radioType = radioType
        //16/9 == measuredHeight/measuredWidth
        // 宽 固定填充 只需要计算高
        when (radioType) {
            RATIO_16_9 -> { // 默认全屏
                framingRect = Rect(0, 0, measuredWidth, measuredHeight)
            }
            RATIO_4_3 -> {
                val resultHeight: Int = (4.00f / 3.00f * measuredWidth).toInt()
                val top = (measuredHeight - resultHeight) / 2  //需要居中 所以求起点Y
                framingRect = Rect(0, top, measuredWidth, top + resultHeight)
            }
            RATIO_1_1 -> {
                val resultHeight: Int = measuredWidth   //(1.00f / 1.00f * measuredWidth).toInt()
                val top = (measuredHeight - resultHeight) / 2  //需要居中 所以求起点Y
                framingRect = Rect(0, top, measuredWidth, top + resultHeight)
            }
        }
        postInvalidate()
    }

    private fun getRatioAreaRect(previewHeight: Int): Rect {
        val rect = Rect(framingRect)
        var ratio = 1.0f * previewHeight / measuredHeight
        if (ratio > 1) ratio = 1.0f
        rect.left = (rect.left * ratio).toInt()
        rect.right = (rect.right * ratio).toInt()
        rect.top = (rect.top * ratio).toInt()
        rect.bottom = (rect.bottom * ratio).toInt()
        return rect
    }

    fun getRatioAreaRect(preViewWidth: Int, preViewHeight: Int): Rect {
        var width = preViewWidth
        var height = preViewHeight

        val manager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val outMetrics = DisplayMetrics()
        manager.defaultDisplay.getMetrics(outMetrics)
        if (this.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) { //竖屏
            width = preViewHeight
            height = preViewWidth
        }

        if (framingRect == null) { //如果为空 则取16:9 全部
            return Rect(0, 0, width, height)
        }

        return getRatioAreaRect(height)
    }

}