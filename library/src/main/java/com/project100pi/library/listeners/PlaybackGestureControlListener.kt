package com.project100pi.library.listeners

/**
 * Created by Sachin Verma on 2019-11-25.
 */

interface PlaybackGestureControlListener {
    fun onVolumeUp()
    fun onVolumeDown()
    fun onBrightnessUp()
    fun onBrightnessDown()
    fun onFastForward()
    fun onRewind()
    fun onActionUp()
}