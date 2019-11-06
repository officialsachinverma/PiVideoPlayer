package com.project100pi.library.media

import android.content.Context
import android.os.RemoteException
import android.support.v4.media.session.MediaControllerCompat
import com.project100pi.library.misc.ApplicationHelper
import com.project100pi.library.misc.CurrentSettings

object MediaCommandHandlerUtil {

    fun handleNext(appContext: Context) {
        if (CurrentSettings.Playback.playing) {
            val token = ApplicationHelper.mediaSessionToken
            try {
                val mediaControllerCompat = MediaControllerCompat(appContext, token)
                mediaControllerCompat.transportControls.skipToNext()
            } catch (e: RemoteException) {
                e.printStackTrace()
            }

        }
    }

    fun handlePrevious(appContext: Context) {
        if (CurrentSettings.Playback.playing) {
            val token = ApplicationHelper.mediaSessionToken
            try {
                val mediaControllerCompat = MediaControllerCompat(appContext, token)
                mediaControllerCompat.transportControls.skipToPrevious()
            } catch (e: RemoteException) {
                e.printStackTrace()
            }

        }
    }

    fun handlePlay(appContext: Context) {
        if (!CurrentSettings.Playback.playing) {
            val token = ApplicationHelper.mediaSessionToken
            try {
                val mediaControllerCompat = MediaControllerCompat(appContext, token)
                mediaControllerCompat.transportControls.play()
            } catch (e: RemoteException) {
                e.printStackTrace()
            }

        }
    }

    fun handlePause(appContext: Context) {
        if (CurrentSettings.Playback.playing) {
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