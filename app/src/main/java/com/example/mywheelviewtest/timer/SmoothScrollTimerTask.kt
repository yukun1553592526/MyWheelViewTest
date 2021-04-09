package com.example.mywheelviewtest.timer

import com.example.mywheelviewtest.WheelView
import java.util.*

class SmoothScrollTimerTask : TimerTask {

    private val wheelView: WheelView
    private var realTotalOffset = 0
    private var realOffset = 0
    private var offset = 0

    constructor(wheelView: WheelView, offset: Int) {
        this.wheelView = wheelView
        this.offset = offset
        realTotalOffset = Int.MAX_VALUE
    }

    override fun run() {
        if (realTotalOffset == Integer.MAX_VALUE) {
            realTotalOffset = offset
        }
        /**
         * divide the scrolling area into 10 units and redraw it in 10 units
         */
        realOffset = (realTotalOffset * 0.1f).toInt()

        if (realOffset == 0) {
            if (realTotalOffset < 0) {
                realOffset = -1
            } else {
                realOffset = 1
            }
        }

        if (Math.abs(realTotalOffset) <= 1) {
            wheelView.cancelFuture()
            wheelView.handler.sendEmptyMessage(MessageHandler.WHAT_ITEM_SELECTED)
        } else {
            wheelView.totalScrollY = wheelView.totalScrollY + realOffset
            /**
             * Carefully, if it isn't loop, it needs to callback that clicking the empty
             * area, otherwise, it would appear that the selected item was located in the
             * the position of -1 index
             */
            if (!wheelView.isLoop) {
                val itemHeight = wheelView.itemHeight
                val top = ((-wheelView.initPosition) * itemHeight).toFloat()
                val bottom = ((wheelView.itemsCount - 1 - wheelView.initPosition) * itemHeight)
                if (wheelView.totalScrollY < top || wheelView.totalScrollY >= bottom) {
                    wheelView.totalScrollY = wheelView.totalScrollY - realOffset
                    wheelView.cancelFuture()
                    wheelView.handler.sendEmptyMessage(MessageHandler.WHAT_ITEM_SELECTED)
                    return
                }
            }
            wheelView.handler.sendEmptyMessage(MessageHandler.WHAT_INVALIDATE_LOOP_VIEW)
            realTotalOffset -= realOffset
        }
    }

}