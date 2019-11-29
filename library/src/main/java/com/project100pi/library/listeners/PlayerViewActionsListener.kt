package com.project100pi.library.listeners

/**
 * Created by Sachin Verma on 2019-11-20.
 *
 * This interface provides three methods
 * The implementer of this interface will
 * get control on user's action on [PiVideoPlayerView]
 * buttons of features like screen rotate, back button
 * and show current playlist/queue
 *
 * @since v1
 *
 * [onPlayerBackButtonPressed]
 * [onPlayerCurrentQueuePressed]
 * [onScreenRotatePressed]
 */

interface PlayerViewActionsListener {

    /**
     * Called when user presses home up button
     * in player view toolbar
     */
    fun onPlayerBackButtonPressed()

    /**
     * Called when user presses home up button
     * in player view toolbar
     */
    fun onPlayerCurrentQueuePressed()

    /**
     * Called when user presses screen rotate icon
     * on playerview
     */
    fun onScreenRotatePressed()
}