package com.miracles.camera

import android.annotation.SuppressLint
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.hardware.Camera
import android.os.Build
import android.util.SparseIntArray
import android.view.SurfaceHolder
import androidx.collection.SparseArrayCompat
import java.io.IOException
import java.util.concurrent.TimeUnit


/**
 * Created by lxw
 */
@Suppress("DEPRECATION")
class Camera1(preview: CameraPreview, callback: CameraFunctions.Callback) : CameraDevice(preview, callback) {
    companion object {
        private const val INVALID_CAMERA_ID = -1
        private val FLASH_MODES = SparseArrayCompat<String>()
        private val INTERNAL_FACINGS = SparseIntArray()

        init {
            FLASH_MODES.put(CameraFunctions.FLASH_OFF, Camera.Parameters.FLASH_MODE_OFF)
            FLASH_MODES.put(CameraFunctions.FLASH_ON, Camera.Parameters.FLASH_MODE_ON)
            FLASH_MODES.put(CameraFunctions.FLASH_TORCH, Camera.Parameters.FLASH_MODE_TORCH)
            FLASH_MODES.put(CameraFunctions.FLASH_AUTO, Camera.Parameters.FLASH_MODE_AUTO)
            FLASH_MODES.put(CameraFunctions.FLASH_RED_EYE, Camera.Parameters.FLASH_MODE_RED_EYE)

            INTERNAL_FACINGS.put(CameraFunctions.FACING_FRONT, Camera.CameraInfo.CAMERA_FACING_FRONT)
            INTERNAL_FACINGS.put(CameraFunctions.FACING_BACK, Camera.CameraInfo.CAMERA_FACING_BACK)
        }
    }

    private var mCamera: Camera? = null
    private val mCameraInfo = Camera.CameraInfo()
    private var mCameraId = INVALID_CAMERA_ID
    private var mCameraParameters: Camera.Parameters? = null
    private val mPreviewSizes = arrayListOf<Size>()
    private val mPictureSizes = arrayListOf<Size>()
    private var mShowingPreview = false

    init {
        preview.callback = object : CameraPreview.PreviewCallback {
            override fun onSurfaceChanged() {
                setUpPreview()
                adjustCameraParameters()
            }
        }
    }

    override fun open(): Boolean {
        chooseCamera()
        openCamera()
        if (preview.isReady()) {
            setUpPreview()
        }
        mCamera?.startPreview()
        mShowingPreview = true
        //what if recording frame and then  switch cameras ,back to record state.
        if (isRecordingFrame()) {
            previewFrameWithBuffers()
        }
        return true
    }

    override fun close() {
        stopPreview()
        releaseCamera()
    }

    override fun setAutoFocus(autoFocus: Boolean) {
        if (setAutoFocusInternal(autoFocus)) {
            mCamera?.parameters = mCameraParameters
        }
    }

    private fun setAutoFocusInternal(autoFocus: Boolean): Boolean {
        mAutoFocus = autoFocus
        val parameter = mCameraParameters ?: return false
        return if (isCameraOpened()) {
            val modes = parameter.supportedFocusModes
            val focusMode = if (autoFocus) {
                when {
                    modes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE) -> Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
                    modes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO) -> Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO
                    modes.contains(Camera.Parameters.FOCUS_MODE_INFINITY) -> Camera.Parameters.FOCUS_MODE_INFINITY
                    else -> modes[0]
                }
            } else {
                when {
                    modes.contains(Camera.Parameters.FOCUS_MODE_AUTO) -> Camera.Parameters.FOCUS_MODE_AUTO
                    modes.contains(Camera.Parameters.FOCUS_MODE_MACRO) -> Camera.Parameters.FOCUS_MODE_MACRO
                    else -> modes[0]
                }
            }
            parameter.focusMode = focusMode
            true
        } else {
            false
        }
    }

    override fun setFlash(flash: Int) {
        if (setFlashInternal(flash)) {
            mCamera?.parameters = mCameraParameters
        }
    }

    private fun setFlashInternal(flash: Int): Boolean {
        val parameters = mCameraParameters ?: return false
        if (isCameraOpened()) {
            val modes = parameters.supportedFlashModes
            val mode = FLASH_MODES.get(flash)
            if (modes != null && modes.contains(mode)) {
                parameters.flashMode = mode
                mFlash = flash
                return true
            }
            if (modes == null || !modes.contains(mode)) {
                parameters.flashMode = FLASH_MODES.get(CameraFunctions.FLASH_OFF)
                mFlash = CameraFunctions.FLASH_OFF
                return true
            }
            return false
        } else {
            mFlash = flash
            return false
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun setUpPreview() {
        try {
            val camera = mCamera ?: return
            if (preview.getOutputClass() === SurfaceHolder::class.java) {
                val needsToStopPreview = mShowingPreview && Build.VERSION.SDK_INT < 14
                if (needsToStopPreview) {
                    camera.stopPreview()
                }
                camera.setPreviewDisplay(preview.getSurfaceHolder())
                if (needsToStopPreview) {
                    camera.startPreview()
                }
            } else {
                camera.setPreviewTexture(preview.getSurfaceTexture())
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    private fun stopPreview() {
        if (!mShowingPreview) return
        mCamera?.run {
            setPreviewCallbackWithBuffer(null)
            stopPreview()
        }
        mShowingPreview = false
    }

    private fun chooseCamera() {
        val internal = INTERNAL_FACINGS.get(facing)
        for (index in 0 until Camera.getNumberOfCameras()) {
            Camera.getCameraInfo(index, mCameraInfo)
            if (mCameraInfo.facing == internal) {
                mCameraId = index
                return
            }
        }
    }

    private fun openCamera() {
        releaseCamera()
        mCamera = Camera.open(mCameraId)
        val camera = mCamera ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            camera.enableShutterSound(false)
        }
        mCameraParameters = camera.parameters
        mCameraParameters?.previewFormat
        mCameraParameters?.pictureFormat
        mPreviewSizes.clear()
        for (size in mCameraParameters?.supportedPreviewSizes ?: arrayListOf()) {
            mPreviewSizes.add(Size(size.width, size.height))
        }
        mPictureSizes.clear()
        for (size in mCameraParameters?.supportedPictureSizes ?: arrayListOf()) {
            mPictureSizes.add(Size(size.width, size.height))
        }
        adjustCameraParameters()
        camera.setDisplayOrientation(calcDisplayOrientation(displayOrientation))
        callback.onCameraOpened()
    }

    private fun adjustCameraParameters() {
        val parameters = mCameraParameters ?: return
        val previewSize = getCameraSizeStrategy(STRATEGY_PREVIEW_SIZE).chooseSize(preview, displayOrientation, mCameraInfo.orientation, facing, mPreviewSizes)
        cacheCameraSize(SIZE_PREVIEW, previewSize)
        cacheCameraSize(SIZE_RECORD, previewSize)
        val pictureSize = getCameraSizeStrategy(STRATEGY_PICTURE_SIZE).chooseSize(preview, displayOrientation, mCameraInfo.orientation, facing, mPictureSizes)
        cacheCameraSize(SIZE_PICTURE, pictureSize)
        if (mShowingPreview) {
            mCamera?.stopPreview()
        }
        parameters.setPreviewSize(previewSize.width, previewSize.height)
        logMED("previewSize is $previewSize")
        parameters.setPictureSize(pictureSize.width, pictureSize.height)
        logMED("pictureSize is $pictureSize")
        parameters.setRotation(calcCameraRotation(displayOrientation))
        parameters.setRecordingHint(true)
        setAutoFocusInternal(mAutoFocus)
        setFlashInternal(mFlash)
        mCamera?.parameters = parameters
        if (mShowingPreview) {
            mCamera?.startPreview()
        }
    }

    override fun setCameraSizeStrategy(kind: Int, strategy: ChooseSizeStrategy) {
        if (kind == STRATEGY_RECORD_PREVIEW_SIZE) {
            super.setCameraSizeStrategy(STRATEGY_PREVIEW_SIZE, strategy)
        }
        super.setCameraSizeStrategy(kind, strategy)
    }

    @SuppressLint("ObsoleteSdkInt")
    override fun updateDisplayOrientation(displayOrientation: Int) {
        val camera = mCamera ?: return
        mCameraParameters?.setRotation(calcCameraRotation(displayOrientation))
        camera.parameters = mCameraParameters
        val needsToStopPreview = mShowingPreview && Build.VERSION.SDK_INT < 14
        if (needsToStopPreview) {
            camera.stopPreview()
        }
        camera.setDisplayOrientation(calcDisplayOrientation(displayOrientation))
        if (needsToStopPreview) {
            camera.startPreview()
        }
    }

    override fun takePicture() {
        if (!isCameraOpened()) {
            throw IllegalStateException("Camera is not ready. Call start() before takePicture().")
        }
        startTakePictureInternal()
    }

    private fun startTakePictureInternal() {
        if (mPictureCaptureInProgress.getAndSet(true)) return
        callback.onStartCapturePicture()
        previewFrameWithBuffers()
    }

    private fun stopTakePictureInternal() {
        if (!mPictureCaptureInProgress.get()) return
        mCamera?.setPreviewCallbackWithBuffer(null)
        mPreviewBytesPool.clear()
        mPictureCaptureInProgress.set(false)
    }

    override fun startRecord() {
        if (mRecordingFrameInProgress.getAndSet(true)) return
        callback.onStartRecordingFrame(timeStampInNs())
        if (!getAutoFocus()) {
            setAutoFocus(true)
        }
        previewFrameWithBuffers()
    }

    private fun recordDiscardGapInNano(): Long {
        val cam = mCamera ?: return 0
        val previewFpsRange = IntArray(2)
        cam.parameters.getPreviewFpsRange(previewFpsRange)
        if (previewFpsRange.size != 2) return 0
        return (1e9 / (previewFpsRange[0] + previewFpsRange[1]) * 1000 * 2).toLong()
    }

    /**
     * set preview buffer 2 camera to get data from callback
     */
    private fun previewFrameWithBuffers() {
        val cam = mCamera ?: return
        val previewSize = cam.parameters.previewSize
        val discardGapInNano = recordDiscardGapInNano()
        val sizeInByte = previewSize.width * previewSize.height * 3 / 2
        val maxFactor = getCameraSizeStrategy(STRATEGY_PREVIEW_SIZE).bytesPoolSize(com.miracles.camera.Size(previewSize.width, previewSize.height))
        if (mPreviewBytesPool.perSize != sizeInByte) {
            mPreviewBytesPool.clear()
            mPreviewBytesPool = ByteArrayPool(maxFactor, sizeInByte)
        }
        for (x in 0..1) {
            cam.addCallbackBuffer(mPreviewBytesPool.getBytes())
        }
        val format = cam.parameters.previewFormat
        logMED("discardGapInNano is $discardGapInNano")
        cam.setPreviewCallbackWithBuffer { data, _ ->
            val dataValidate = data?.size ?: 0 > 0
            when {
                mPictureCaptureInProgress.get() && dataValidate -> {
                    callback.onPictureCaptured(data, data.size, previewSize.width, previewSize.height, format,
                            calcCameraRotation(displayOrientation), facing, timeStampInNs())
                    stopTakePictureInternal()
                    return@setPreviewCallbackWithBuffer
                }
                mRecordingFrameInProgress.get() && dataValidate -> {
                    val reused = mPreviewBytesPool.getBytes(discardGapInNano, TimeUnit.NANOSECONDS)
                            ?: let {
                                cam.addCallbackBuffer(data)
                                return@setPreviewCallbackWithBuffer
                            }
                    callback.onFrameRecording(data, data.size, mPreviewBytesPool, previewSize.width, previewSize.height, format,
                            calcCameraRotation(displayOrientation), facing, timeStampInNs())
                    cam.addCallbackBuffer(reused)
                }
                else -> cam.addCallbackBuffer(data)
            }
        }
    }

    override fun stopRecord() {
        if (!mRecordingFrameInProgress.get()) return
        mCamera?.setPreviewCallbackWithBuffer(null)
        callback.onStopRecordingFrame(timeStampInNs())
        mPreviewBytesPool.clear()
        mRecordingFrameInProgress.set(false)
    }

    override fun isCameraOpened() = mCamera != null

    private fun releaseCamera() {
        mCamera?.apply {
            release()
        }
        mCamera = null
        mCameraParameters = null
        callback.onCameraClosed()
    }

    //Note:setZoom must be after setPreviewSize.
    override fun setZoom(zoom: Int) {
        val params = mCameraParameters ?: return
        val supported = params.isZoomSupported
        if (!supported) return
        var z = zoom
        if (z <= ZOOM_MIN) {
            z = ZOOM_MIN
        } else if (z >= ZOOM_MAX) {
            z = ZOOM_MAX
        }
        params.zoom = constraintZoom(z, ZOOM_MAX, params.maxZoom)
        mCamera?.parameters = mCameraParameters
    }

    override fun getZoom(): Int {
        val params = mCamera?.parameters ?: return ZOOM_MIN
        val supported = params.isZoomSupported
        if (!supported) return ZOOM_MIN
        return if (!supported) {
            ZOOM_MIN
        } else {
            constraintZoom(params.zoom, params.maxZoom, ZOOM_MAX)
        }
    }

    override fun focus(focusRect: Rect?, meteringRect: Rect?, cb: ((Boolean) -> Unit)?) {
        val cam = mCamera
        val params = mCameraParameters
        if (cam == null || params == null) {
            cb?.invoke(false)
            return
        }
        if (focusRect != null && focusRect.width() > 0 && focusRect.height() > 0) {
            if (params.maxNumFocusAreas > 0) {
                val focusArea = getFocusArea(focusRect)
                if (focusArea.width() > 0 && focusArea.height() > 0) {
                    params.focusAreas = arrayListOf(Camera.Area(focusArea, 1000))
                }
            }
        }
        if (meteringRect != null && meteringRect.width() > 0 && meteringRect.height() > 0) {
            if (params.maxNumMeteringAreas > 0) {
                val meteringArea = getFocusArea(meteringRect)
                if (meteringArea.width() > 0 && meteringArea.height() > 0) {
                    params.meteringAreas = arrayListOf(Camera.Area(meteringArea, 1000))
                }
            }
        }
        cam.cancelAutoFocus()
        if (getAutoFocus()) {
            setAutoFocus(false)
        }
        cam.parameters = params
        cam.autoFocus { success, _ ->
            cb?.invoke(success)
            cam.cancelAutoFocus()
        }
    }

    private fun getFocusArea(rect: Rect): Rect {
        val previewSize = getSize(SIZE_PREVIEW)
        if (previewSize.height < 0 || previewSize.width < 0 || !preview.isReady()) {
            return Rect()
        }
        val matrix = Matrix()
        val prf = RectF(0f, 0f, preview.getWidth().toFloat(), preview.getHeight().toFloat())
        val sensorOrientation = calcCameraRotation(0)
        val swap = sensorOrientation == 90 || sensorOrientation == 270
        val pzrf = RectF(0f, 0f, if (swap) previewSize.height.toFloat() else previewSize.width.toFloat(),
                if (swap) previewSize.width.toFloat() else previewSize.height.toFloat())
        matrix.postRotate(-displayOrientation.toFloat())
        matrix.setRectToRect(prf, pzrf, Matrix.ScaleToFit.FILL)
        matrix.postRotate(-sensorOrientation.toFloat())
        val maxCameraAreaRectF = RectF(-1000f, -1000f, 1000f, 1000f)
        matrix.setRectToRect(pzrf, maxCameraAreaRectF, Matrix.ScaleToFit.FILL)
        val dst = RectF(rect.left.toFloat(), rect.top.toFloat(), rect.right.toFloat(), rect.bottom.toFloat())
        matrix.mapRect(dst)
        val clampLeft = maxCameraAreaRectF.left.toInt()
        val clampBottom = maxCameraAreaRectF.bottom.toInt()
        val clampFun = clamp(clampLeft, clampBottom)
        return Rect(clampFun(dst.left.toInt()), clampFun(dst.top.toInt()), clampFun(dst.right.toInt()), clampFun(dst.bottom.toInt()))
    }

    private fun clamp(min: Int, max: Int): (Int) -> Int {
        return fun(value: Int): Int {
            return if (value < min) min else if (value > max) max else value
        }
    }

    /**
     * Calculate camera display orientation
     */
    private fun calcDisplayOrientation(screenOrientationDegrees: Int): Int {
        return if (mCameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            (360 - (mCameraInfo.orientation + screenOrientationDegrees) % 360) % 360
        } else {  // back-facing
            (mCameraInfo.orientation - screenOrientationDegrees + 360) % 360
        }
    }

    /**
     * Calculate camera rotation( pictures's orientation clockwise )
     */
    private fun calcCameraRotation(screenOrientationDegrees: Int): Int {
        return if (mCameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            (mCameraInfo.orientation + screenOrientationDegrees) % 360
        } else {  // back-facing
            val landscapeFlip = if (screenOrientationDegrees == 90 || screenOrientationDegrees == 270) 180 else 0
            (mCameraInfo.orientation + screenOrientationDegrees + landscapeFlip) % 360
        }
    }
}