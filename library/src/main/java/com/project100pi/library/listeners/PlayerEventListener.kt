package com.project100pi.library.listeners

import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player

/**
 * Created by Sachin Verma on 2019-11-25.
 *
 * This interface provides three method
 * The caller of this interface should implement
 * [Player.EventListener]
 * The implementer of this interface will get know about
 * when the track has changed, when playback of current track has
 * been completed and if there occurs an exception while playback
 *
 * @since v1
 *
 * [onPlayerTrackCompleted]
 * [onTracksChanged]
 * [onPlayerError]
 */
interface PiPlayerEventListener {

    /**
     * Called when player has completed playback of selected track.
     */
    fun onPlayerTrackCompleted()

    /**
     * Called when the available or selected tracks change.
     */
    fun onTracksChanged()

    /**
     * Returns the error that caused playback to fail. This is the same error that will have been
     * reported via Player.EventListener method: onPlayerError(ExoPlaybackException) callback at the time of
     * failure. It can be inspected using this method until the player is re-prepared.
     *
     * @return The error, or null.
     */
    fun onPlayerError(error: ExoPlaybackException?)
}