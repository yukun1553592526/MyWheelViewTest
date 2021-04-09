package com.example.mywheelviewtest.timer

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.widget.Switch
import com.example.mywheelviewtest.WheelView

class MessageHandler : Handler {

    companion object {
        const val WHAT_INVALIDATE_LOOP_VIEW = 1000
        const val WHAT_SMOOTH_SCROLL = 2000
        const val WHAT_ITEM_SELECTED = 3000
    }

    private val mWheelView:  WheelView

    constructor(wheelView: WheelView) : super(Looper.getMainLooper()) {
        this.mWheelView = wheelView
    }

    override fun handleMessage(msg: Message) {
        when (msg.what) {
            WHAT_INVALIDATE_LOOP_VIEW -> mWheelView.invalidate()
            WHAT_SMOOTH_SCROLL -> mWheelView.smoothScroll(WheelView.MotionAction.FLIING)
            WHAT_ITEM_SELECTED -> mWheelView.onItemSelected()
        }
    }
}