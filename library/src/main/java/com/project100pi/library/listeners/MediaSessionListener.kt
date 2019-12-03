package com.project100pi.library.listeners

import android.support.v4.media.session.MediaSessionCompat

/**
 * This interface provides two methods
 * The caller of this interface methods
 * should implement [MediaSessionCompat.Callback]
 * The implementer of of this interface will get
 * event requests based on media button clicks.
 *
 * @since v1
 *
 * [onPlay]
 * [onPause]
 * [onSkipToNext]
 * [onSkipToPrevious]
 */
interface MediaSessionListener{

    /**
     * Called when requests to begin playback.
     */
    fun onPlay()

    /**
     * Called when requests to pause playback.
     */
    fun onPause()

    /**
     * Called when requests to skip to the next media item.
     */
    fun onSkipToNext()

    /**
     * Called when requests to skip to the previous media item.
     */
    fun onSkipToPrevious()
}