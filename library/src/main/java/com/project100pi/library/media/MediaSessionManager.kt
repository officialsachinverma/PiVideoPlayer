package com.project100pi.library.media

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import com.project100pi.library.misc.ApplicationHelper
import com.project100pi.library.misc.Logger
import com.project100pi.library.listeners.MediaSessionListener

class MediaSessionManager: MediaSessionCompat.Callback {

    private val TAG = "MediaSessionManager"

    private val appContext: Context
    private val mediaSessionCompat: MediaSessionCompat
    private val mediaSessionListener: MediaSessionListener

    constructor(appContext: Context, mediaSessionListener: MediaSessionListener) {
        /*
         * Media Session and Media Session Connector automatically
         * handles a basic set of playback actions such as play, pause,
         * seek to, forward, rewind and stop.
         * For these basic actions putting listeners is not required.
         */

        this.appContext = appContext
        this.mediaSessionListener = mediaSessionListener

        val mediaButtonReceiver = ComponentName(appContext, MediaReceiver::class.java)
        val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON)
        mediaButtonIntent.setClass(appContext, MediaReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(appContext, 0, mediaButtonIntent, 0)
        mediaSessionCompat = MediaSessionCompat(appContext, "MediaSessionManager", mediaButtonReceiver, pendingIntent)
        mediaSessionCompat.setFlags(
            MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        )
        mediaSessionCompat.setCallback(this)
        mediaSessionCompat.isActive = true
        storeTokenAndID()
    }

    fun getTransportControls(): MediaControllerCompat.TransportControls = mediaSessionCompat.controller.transportControls

    fun getMediaSessionToken(): MediaSessionCompat.Token = mediaSessionCompat.sessionToken

    override fun onMediaButtonEvent(mediaButtonEvent: Intent): Boolean {
        Logger.i("onMediaButtonEvent() called with: mediaButtonEvent = [" + mediaButtonEvent.action + "]")
        MediaReceiver.handleMediaButtonIntent(appContext, mediaButtonEvent)
        return true
    }

    override fun onPause() {
        //player?.playWhenReady = false
        mediaSessionListener.msPause()
    }

    override fun onPlay() {
        //player?.playWhenReady = true
        mediaSessionListener.msPlay()
    }

    override fun onSkipToPrevious() {
        //player?.previous()
        mediaSessionListener.msSkipToPrevious()
    }

    override fun onSkipToNext() {
        //player?.next()
        mediaSessionListener.msSkipToNext()
    }

    private fun storeTokenAndID() {

        Logger.i("storeTokenAndID() :: storing Token and ID for further use")

        ApplicationHelper.mediaSessionToken = getMediaSessionToken()
        //ApplicationHelper.setMediaPlayerAudioSessionId(playbackManager.getMediaPlayerAudioSessionId())

        Logger.i("storeTokenAndID() :: successfully stored Token and ID")
    }

}