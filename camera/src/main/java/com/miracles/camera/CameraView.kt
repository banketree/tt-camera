package com.miracles.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import androidx.core.view.ViewCompat
import com.miracles.camera.CameraFunctions.Companion.SIZE_PREVIEW
import com.miracles.camera.CameraFunctions.Companion.STRATEGY_PICTURE_SIZE
import com.miracles.camera.CameraFunctions.Companion.STRATEGY_PREVIEW_SIZE
import com.miracles.camera.CameraFunctions.Companion.STRATEGY_RECORD_PREVIEW_SIZE
import java.util.*

/**
 * Created by lxw
 */
class CameraView : FrameLayout {
    private lateinit var mDeviceImpl: CameraDevice
    private lateinit var mCallbacks: CameraCallbackBridge
    private lateinit var mDisplayOrientationDetector: DisplayOrientationDetector
    var autoChangeOrientation = false
    private lateinit var previewImpl: CameraPreview
    private var mAdjustBounds = false
    private val mRequestLayoutCallback = object : CameraView.Callback {
        override fun onCameraOpened(cameraView: CameraView) {
            cameraView.requestLayout()
            mCallbacks.removeCallback(this)
        }
    }

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, @AttrRes defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initView(context, attrs)
    }

    @SuppressLint("NewApi")
    private fun initView(ctx: Context, attrs: AttributeSet?) {
        if (isInEditMode) return
        //Attrs
        val a = context.obtainStyledAttributes(attrs, R.styleable.CameraView)
        mAdjustBounds = a.getBoolean(R.styleable.CameraView_android_adjustViewBounds, false)
        val enableCamera2 = a.getBoolean(R.styleable.CameraView_enableCamera2, false)
        a.recycle()
        previewImpl = createCameraPreview()
        mCallbacks = CameraCallbackBridge(this)
        mDeviceImpl = if (enableCamera2) {
            when {
                Build.VERSION.SDK_INT < 21 -> Camera1(previewImpl, mCallbacks)
                Build.VERSION.SDK_INT < 23 -> Camera2(previewImpl, ctx, mCallbacks)
                else -> Camera2Api23(previewImpl, ctx, mCallbacks)
            }
        } else {
            Camera1(previewImpl, mCallbacks)
        }
        mDisplayOrientationDetector = object : DisplayOrientationDetector(ctx) {
            override fun onDisplayOrientationChanged(displayOrientation: Int) {
                //if is recording or taking picture do not rotate...
                if (autoChangeOrientation && !isPictureCapturing() && !isRecordingFrame()) {
                    mDeviceImpl.displayOrientation = displayOrientation
                }
            }
        }

    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (isInEditMode || !mAdjustBounds) return
        if (!mDeviceImpl.isCameraOpened()) {
            addCallback(mRequestLayoutCallback)
            return
        }
        val measuredWidth = MeasureSpec.getSize(widthMeasureSpec)
        val measuredHeight = MeasureSpec.getSize(heightMeasureSpec)
        val size = getSize(SIZE_PREVIEW)
        if (size.width > 0 && size.height > 0) {
            val ration = if (mDeviceImpl.displayOrientation % 90 == 0) {
                size.width.toFloat() / size.height
            } else {
                size.height.toFloat() / size.width
            }
            if (measuredWidth < measuredHeight * ration) {
                setMeasuredDimension(measuredWidth, (measuredHeight * ration).toInt())
            } else {
                setMeasuredDimension((measuredHeight * ration).toInt(), measuredHeight)
            }
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun createCameraPreview(): CameraPreview {
        return if (Build.VERSION.SDK_INT < 14) {
            SurfaceViewPreview(this)
        } else {
            TextureViewPreview(this)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isInEditMode) {
            mDisplayOrientationDetector.enable(ViewCompat.getDisplay(this)!!)
        }
    }

    override fun onDetachedFromWindow() {
        if (!isInEditMode) {
            mDisplayOrientationDetector.disable()
        }
        super.onDetachedFromWindow()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
    }

    /**
     * start camera and ready 2 preview.
     */
    fun start(): Boolean {
        mCallbacks.startBackgroundThread()
        try {
            if (!mDeviceImpl.open()) {
                val saveState = onSaveInstanceState()
                // Camera2 uses legacy hardware layer; fall back to Camera1
                mDeviceImpl = Camera1(previewImpl, mCallbacks)
                onRestoreInstanceState(saveState)
                mDeviceImpl.open()
            }
        } catch (ex: Throwable) {
            logMEE("start camera error.", ex)
            return false
        }
        logMED("start success!!!")
        return true
    }

    /**
     * stop and close camera
     */
    fun stop(): Boolean {
        try {
            if (isRecordingFrame()) {
                mDeviceImpl.stopRecord()
            }
            mDeviceImpl.close()
        } catch (ex: Throwable) {
            logMEE("stop camera failed", ex)
            return false
        } finally {
            mCallbacks.stopBackgroundThread()
        }
        logMED("stop camera success")
        return true
    }

    /**
     * Note:picture raw data will result from callback you had added to callbackBridge
     */
    fun takePicture() {
        mDeviceImpl.takePicture()
    }

    /**
     * start 2 record camera uncompressed data.
     */
    fun startRecord() {
        mDeviceImpl.startRecord()
    }

    /**
     * stop recording
     */
    fun stopRecord() {
        mDeviceImpl.stopRecord()
    }

    /**
     * set camera facing details
     *
     * @see CameraFunctions.FACING_BACK
     * @see CameraFunctions.FACING_FRONT
     */
    fun setFacing(facing: Int) {
        mDeviceImpl.facing = facing
    }

    /**
     * facing from current display info
     *
     * @see CameraFunctions.FACING_*
     */
    fun getFacing() = mDeviceImpl.facing

    /**
     * Auto focus.
     */
    fun setAutoFocus(autoFocus: Boolean) = mDeviceImpl.setAutoFocus(autoFocus)

    fun getAutoFocus() = mDeviceImpl.getAutoFocus()
    /**
     * FlashingMode
     *
     * @see CameraFunctions.FLASH_*
     */
    fun setFlashing(flashing: Int) = mDeviceImpl.setFlash(flashing)

    fun getFlashing() = mDeviceImpl.getFlash()
    /**
     * set camera's choose preview or picture size strategy.
     *
     * @see STRATEGY_PREVIEW_SIZE
     * @see STRATEGY_PICTURE_SIZE
     * @see STRATEGY_RECORD_PREVIEW_SIZE
     */
    fun setCameraSizeStrategy(kind: Int, strategy: ChooseSizeStrategy) {
        mDeviceImpl.setCameraSizeStrategy(kind, strategy)
    }

    fun getCameraSizeStrategy(kind: Int) = mDeviceImpl.getCameraSizeStrategy(kind)
    /**
     * get size of final preview or picture
     *
     * @see CameraFunctions.SIZE_PICTURE
     * @see CameraFunctions.SIZE_PICTURE
     * @see CameraFunctions.SIZE_RECORD
     */
    fun getSize(kind: Int) = mDeviceImpl.getSize(kind)


    /**
     * is capturing pic or  not .
     */
    fun isPictureCapturing() = mDeviceImpl.isCapturingPicture()

    /**
     * is recording in progress.
     */
    fun isRecordingFrame() = mDeviceImpl.isRecordingFrame()

    /**
     * Note: run cb on callbackBridge thread.do not blocking too long .
     *
     * add callback for cameraFunctions progress.
     *
     */
    fun addCallback(cb: Callback) {
        mCallbacks.addCallback(cb)
    }

    /**
     * remove callback added to callbacks
     */
    fun removeCallback(cb: Callback) {
        mCallbacks.removeCallback(cb)
    }

    /**
     * Zoom camera's  preview and output.
     *
     * @see CameraFunctions.ZOOM_MIN
     * @see CameraFunctions.ZOOM_MAX
     */
    fun setZoom(zoom: Int) {
        mDeviceImpl.setZoom(zoom)
    }

    fun getZoom() = mDeviceImpl.getZoom()

    fun focus(rect: Rect?, cb: ((Boolean) -> Unit)?) = mDeviceImpl.focus(rect, null, cb)
    /**
     * Note:it will set autoFocus to false.
     *
     * try 2  focus with focusRect&meteringRect and callback
     */
    fun focus(focusRect: Rect?, meteringRect: Rect?, cb: ((Boolean) -> Unit)?) =
        mDeviceImpl.focus(focusRect, meteringRect, cb)

    override fun onSaveInstanceState(): Parcelable {
        val state = SavedState(super.onSaveInstanceState())
        state.facing = getFacing()
        state.autoFocus = getAutoFocus()
        state.flash = getFlashing()
        state.previewSizeStrategy = getCameraSizeStrategy(CameraFunctions.STRATEGY_PREVIEW_SIZE)
        state.pictureSizeStrategy = getCameraSizeStrategy(CameraFunctions.STRATEGY_PICTURE_SIZE)
        state.recordPreviewSizeStrategy =
            getCameraSizeStrategy(CameraFunctions.STRATEGY_RECORD_PREVIEW_SIZE)
        return state
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state == null) return
        if (state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }
        super.onRestoreInstanceState(state.superState)
        setFacing(state.facing)
        setAutoFocus(state.autoFocus)
        setFlashing(state.flash)
        state.previewSizeStrategy?.run {
            setCameraSizeStrategy(CameraFunctions.STRATEGY_PREVIEW_SIZE, this)
        }
        state.pictureSizeStrategy?.run {
            setCameraSizeStrategy(CameraFunctions.STRATEGY_PICTURE_SIZE, this)
        }
        state.recordPreviewSizeStrategy?.run {
            setCameraSizeStrategy(CameraFunctions.STRATEGY_RECORD_PREVIEW_SIZE, this)
        }
    }

    protected class SavedState : BaseSavedState {
        internal var facing: Int = 0
        internal var autoFocus: Boolean = true
        internal var flash: Int = 0
        internal var previewSizeStrategy: ChooseSizeStrategy? = null
        internal var pictureSizeStrategy: ChooseSizeStrategy? = null
        internal var recordPreviewSizeStrategy: ChooseSizeStrategy? = null

        constructor(source: Parcelable?) : super(source)

        constructor(source: Parcel?, loader: ClassLoader?) : super(source) {
            if (source == null || loader == null) return
            facing = source.readInt()
            autoFocus = source.readByte().toInt() != 0
            flash = source.readInt()
            previewSizeStrategy = source.readParcelable(loader)
            pictureSizeStrategy = source.readParcelable(loader)
            recordPreviewSizeStrategy = source.readParcelable(loader)
        }

        override fun writeToParcel(out: Parcel?, flags: Int) {
            super.writeToParcel(out, flags)
            out?.writeInt(facing)
            out?.writeByte(if (autoFocus) 1 else 0)
            out?.writeInt(flash)
            out?.writeParcelable(previewSizeStrategy, 0)
            out?.writeParcelable(pictureSizeStrategy, 0)
            out?.writeParcelable(recordPreviewSizeStrategy, 0)
        }

        companion object CREATOR : Parcelable.ClassLoaderCreator<SavedState> {
            override fun createFromParcel(source: Parcel?): SavedState {
                return SavedState(source, CameraView.SavedState.CREATOR::class.java.classLoader)
            }

            override fun createFromParcel(`in`: Parcel?, loader: ClassLoader?): SavedState {
                return SavedState(`in`, loader)
            }

            override fun newArray(size: Int): Array<SavedState?> {
                return arrayOfNulls(size)
            }

        }
    }

    interface Callback {
        fun onCameraOpened(cameraView: CameraView) {}

        fun onCameraClosed(cameraView: CameraView) {}
        /**
         * result uncompressed yuv data
         */
        fun onStartCapturePicture(cameraView: CameraView) {}

        fun onPictureCaptured(
            cameraView: CameraView, data: ByteArray, len: Int, width: Int,
            height: Int, format: Int, orientation: Int, facing: Int, timeStampInNs: Long
        ) {

        }

        /**
         * result uncompressed yuv data.
         */
        fun onStartRecordingFrame(cameraView: CameraView, timeStampInNs: Long) {}

        fun onFrameRecording(
            cameraView: CameraView,
            frameBytes: FrameBytes,
            width: Int,
            height: Int,
            format: Int,
            orientation: Int,
            facing: Int,
            timeStampInNs: Long,
            cropRect: Rect = Rect(0, 0, width, height)
        ) {
        }

        fun onStopRecordingFrame(cameraView: CameraView, timeStampInNs: Long) {}
    }

    data class FrameBytes(
        val datas: ByteArray,
        val len: Int,
        val bytesPool: ByteArrayPool,
        var consumed: Boolean
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is FrameBytes) return false

            if (!Arrays.equals(datas, other.datas)) return false
            if (len != other.len) return false
            if (bytesPool != other.bytesPool) return false
            if (consumed != other.consumed) return false

            return true
        }

        override fun hashCode(): Int {
            var result = Arrays.hashCode(datas)
            result = 31 * result + len
            result = 31 * result + bytesPool.hashCode()
            result = 31 * result + consumed.hashCode()
            return result
        }
    }
}