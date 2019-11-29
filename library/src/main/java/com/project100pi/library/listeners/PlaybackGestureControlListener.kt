package com.project100pi.library.listeners

import android.view.*
import com.google.android.exoplayer2.ui.spherical.SingleTapListener

/**
 * Created by Sachin Verma on 2019-11-25.
 *
 * This interface provides seven method
 * The caller of this interface should implement
 * [GestureDetector.SimpleOnGestureListener] and [SingleTapListener]
 *
 * @since v1
 *
 * [onVolumeUp]
 * [onVolumeDown]
 * [onBrightnessUp]
 * [onBrightnessDown]
 * [onFastForward]
 * [onRewind]
 * [onActionUp]
 *
 */
interface PlaybackGestureControlListener {

    /**
     * Called when user has started scrolling from BOTTOM to TOP on
     * the RIGHT side of the RIGHT HALF portion of screen
     */
    fun onVolumeUp()

    /**
     * Called when user has started scrolling from TOP to BOTTOM on
     * the RIGHT side of the RIGHT HALF portion of screen
     */
    fun onVolumeDown()

    /**
     * Called when user has started scrolling from BOTTOM to TOP on
     * the LEFT side of the LEFT HALF portion of screen
     */
    fun onBrightnessUp()

    /**
     * Called when user has started scrolling from TOP to BOTTOM on
     * the LEFT side of the LEFT HALF portion of screen
     */
    fun onBrightnessDown()

    /**
     * Called when user has started scrolling from LEFT to RIGHT on
     * the BOTTOM side of the BOTTOM HALF portion of screen
     */
    fun onFastForward()

    /**
     * Called when user has started scrolling from RIGHT to LEFT on
     * the BOTTOM side of the BOTTOM HALF portion of screen
     */
    fun onRewind()

    /**
     * Called when user releases the touch on screen
     * Or user has stopped touching the screen
     */
    fun onActionUp()
}