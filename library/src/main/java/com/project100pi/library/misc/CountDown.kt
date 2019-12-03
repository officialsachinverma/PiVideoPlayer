package com.project100pi.library.misc

import android.os.CountDownTimer
import android.view.View

class CountDown(millisInFuture: Long, countDownInterval: Long, val view: View) : CountDownTimer(millisInFuture, countDownInterval) {

    init {
        start()
    }

    override fun onFinish() {
        view.visibility = View.GONE
    }

    override fun onTick(p0: Long) {

    }

}