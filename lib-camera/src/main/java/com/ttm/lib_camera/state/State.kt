package com.ttm.lib_camera.state

import android.view.Surface
import android.view.SurfaceHolder

import com.ttm.lib_camera.CameraInterface


interface State {

    fun start(holder: SurfaceHolder, screenProp: Float)

    fun stop()

    fun foucs(x: Float, y: Float, callback: CameraInterface.FocusCallback)

    fun swtich(holder: SurfaceHolder, screenProp: Float)

    fun restart()

    fun capture()

    fun record(surface: Surface, screenProp: Float)

    fun stopRecord(isShort: Boolean, time: Long)

    fun cancle(holder: SurfaceHolder, screenProp: Float)

    fun confirm()

    fun zoom(zoom: Float, type: Int)

    fun flash(mode: String)
}
