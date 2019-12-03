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

class MediaSessionManager(private val appContext: Context, private val mediaSessionListener: MediaSessionListener) : MediaSessionCompat.Callback() {

    private val mediaSessionCompat: MediaSessionCompat

    init {
        /*
         * Media Session and Media Session Connector automatically
         * handles a basic set of playback actions such as play, pause,
         * seek to, forward, rewind and stop.
         * For these basic actions putting listeners is not required.
         */

        val mediaButtonReceiver = ComponentName(appContext, MediaReceiver::class.java)

        val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON)

        mediaButtonIntent.`package` = appContext.packageName

        mediaButtonIntent.setClass(appContext, MediaReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(appContext, 0, mediaButtonIntent, 0)

        mediaSessionCompat = MediaSessionCompat(appContext, "MediaSessionManager", mediaButtonReceiver, pendingIntent)

        mediaSessionCompat.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
        // register media session callback
        mediaSessionCompat.setCallback(this)
        // On making the media session active android system knows that latest active media session
        // and hence system will provide all the callbacks and all to the latest media session
        mediaSessionCompat.isActive = true
        // Soft saving the session token (the saved token will remain live till the app is alive)
        storeTokenAndID()
    }

    /**
     * Gets a TransportControls instance for this session.
     *
     * @return A controls instance
     */
    fun getTransportControls(): MediaControllerCompat.TransportControls = mediaSessionCompat.controller.transportControls

    /**
     * Retrieves a token object that can be used for interacting with this session.
     * The owner of the session is responsible for deciding how to distribute these
     * tokens.
     *
     * On platform versions before
     * {@link android.os.Build.VERSION_CODES#LOLLIPOP} this token may only be
     * used within your app as there is no way to guarantee other apps are using
     * the same version of the support library.
     *
     * @return A token that can be used to create a media controller for this
     *         session.
     */
    fun getMediaSessionToken(): MediaSessionCompat.Token = mediaSessionCompat.sessionToken

    override fun onMediaButtonEvent(mediaButtonEvent: Intent): Boolean {
        Logger.i("onMediaButtonEvent() called with: mediaButtonEvent = [" + mediaButtonEvent.action + "]")

        MediaReceiver.handleMediaButtonIntent(appContext, mediaButtonEvent)

        return true
    }

    override fun onPause() {
        //player?.playWhenReady = false
        mediaSessionListener.onPause()
    }

    override fun onPlay() {
        //player?.playWhenReady = true
        mediaSessionListener.onPlay()
    }

    override fun onSkipToPrevious() {
        //player?.previous()
        mediaSessionListener.onSkipToPrevious()
    }

    override fun onSkipToNext() {
        //player?.next()
        mediaSessionListener.onSkipToNext()
    }

    /**
     * Stores media session token in application Helper class
     */
    private fun storeTokenAndID() {

        Logger.i("storeTokenAndID() :: storing Token and ID for further use")

        ApplicationHelper.mediaSessionToken = getMediaSessionToken()

        Logger.i("storeTokenAndID() :: successfully stored Token and ID")
    }

}