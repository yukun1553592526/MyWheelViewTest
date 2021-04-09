package com.example.mywheelviewtest.listener

import android.view.GestureDetector
import android.view.MotionEvent
import com.example.mywheelviewtest.WheelView

class LoopViewGestureListener(private val wheelView: WheelView) : GestureDetector.SimpleOnGestureListener() {

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent?,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        return super.onFling(e1, e2, velocityX, velocityY)
        wheelView.scrollBy(velocityY)
    }
}