package com.project100pi.library.listeners

/**
 * Created by Sachin Verma on 2019-12-03.
 *
 * this interface provides two methods
 *
 * @since v1
 *
 * [showSystemUI]
 * [hideSystemUI]
 */
interface PlaybackControllerVisibilityListener {

    /**
     * This method will be called when user taps on screen
     * in intention to show the controller. At that time
     * we have to show the status bar of the system.
     */
    fun showSystemUI()

    /**
     * This method will be called when user taps on screen
     * or hide controller timeout happens to hide the controller.
     * we have to hide the status bar of the system.
     */
    fun hideSystemUI()
}