package com.project100pi.library.misc

import android.os.CountDownTimer
import android.view.View

class CountDown: CountDownTimer {

    val view: View

    constructor(millisInFuture: Long, countDownInterval: Long, view: View): super(millisInFuture, countDownInterval) {
        this.view = view
        start()
    }

    override fun onFinish() {
        view.visibility = View.GONE
    }

    override fun onTick(p0: Long) {

    }

}