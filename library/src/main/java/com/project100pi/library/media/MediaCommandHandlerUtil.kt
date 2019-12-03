package com.project100pi.library.media

import android.content.Context
import android.os.RemoteException
import android.support.v4.media.session.MediaControllerCompat
import com.project100pi.library.misc.ApplicationHelper
import com.project100pi.library.misc.CurrentMediaState

object MediaCommandHandlerUtil {

    /**
     * Skips to the next item.
     *
     * @param appContext Context
     */
    fun handleNext(appContext: Context) {
        if (CurrentMediaState.Playback.playing) {
            val token = ApplicationHelper.mediaSessionToken
            try {
                val mediaControllerCompat = MediaControllerCompat(appContext, token)
                mediaControllerCompat.transportControls.skipToNext()
            } catch (e: RemoteException) {
                e.printStackTrace()
            }

        }
    }

    /**
     * Skips to the previous item.
     *
     * @param appContext Context
     */
    fun handlePrevious(appContext: Context) {
        if (CurrentMediaState.Playback.playing) {
            val token = ApplicationHelper.mediaSessionToken
            try {
                val mediaControllerCompat = MediaControllerCompat(appContext, token)
                mediaControllerCompat.transportControls.skipToPrevious()
            } catch (e: RemoteException) {
                e.printStackTrace()
            }

        }
    }

    /**
     * Request that the player start its playback at its current position.
     *
     * @param appContext Context
     */
    fun handlePlay(appContext: Context) {
        if (!CurrentMediaState.Playback.playing) {
            val token = ApplicationHelper.mediaSessionToken
            try {
                val mediaControllerCompat = MediaControllerCompat(appContext, token)
                mediaControllerCompat.transportControls.play()
            } catch (e: RemoteException) {
                e.printStackTrace()
            }

        }
    }

    /**
     * Request that the player pause its playback and stay at its current
     * position.
     *
     * @param appContext Context
     */
    fun handlePause(appContext: Context) {
        if (CurrentMediaState.Playback.playing) {
            val token = ApplicationHelper.mediaSessionToken
            try {
                val mediaControllerCompat = MediaControllerCompat(appContext, token)
                mediaControllerCompat.transportControls.pause()
            } catch (e: RemoteException) {
                e.printStackTrace()
            }

        }
    }

}