package com.project100pi.library.player

import android.content.Context
import android.net.Uri
import android.os.Looper
import android.util.Log
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.google.android.exoplayer2.video.VideoListener
import com.project100pi.library.misc.Util.userAgent

class PiVideoPlayer {

    private val TAG: String = "PiVideoPlayer"

    private val DEFAULT_REWIND_TIME = 10000 // 10 secs
    private val DEFAULT_FAST_FORWARD_TIME = 10000 // 10 secs

    private var context: Context
    private var player: SimpleExoPlayer? = null
    var path: String = ""
    var playWhenReady: Boolean = true
    private var currentWindow = 0
    private var playbackPosition: Long = 0


    private var audioAttributes = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.CONTENT_TYPE_MUSIC)
        .build()

    constructor(context: Context) {
        this.context = context
        if (player == null) {
            player = ExoPlayerFactory.newSimpleInstance( context,
                DefaultRenderersFactory(context),
                DefaultTrackSelector(),
                DefaultLoadControl()
            )
            player?.addListener(EventListener())
            player?.setAudioAttributes(audioAttributes, true)
            player?.playWhenReady = this.playWhenReady
            player?.seekTo(currentWindow, playbackPosition)
        }

    }

    private fun buildMediaSource(uri: Uri): MediaSource {

        val dataSourceFactory = DefaultDataSourceFactory(
            context, Util.getUserAgent(context, userAgent))

        return ExtractorMediaSource.Factory(dataSourceFactory).createMediaSource(uri)
    }

    // Public APIs starts

    fun prepare(path: String) {
        this.path = path
        val mediaSource = buildMediaSource(Uri.parse(path))
        player?.prepare(mediaSource, true, false)
    }

    fun prepare(paths: ArrayList<String?>?, resetPosition: Boolean, resetState: Boolean) {

        val concatenatingMediaSource = ConcatenatingMediaSource()
        for (path in paths!!) {
            val mediaSource = buildMediaSource(Uri.parse(path))
            concatenatingMediaSource.addMediaSource(mediaSource)
        }

        player?.prepare(concatenatingMediaSource, resetPosition, resetState)
    }

    fun release(){
        player?.release()
        player = null
    }

    fun play(){
        player?.playWhenReady = true
    }

    fun pause(){
        player?.playWhenReady = false
    }

    fun seekTo(positionMs: Long){
        player?.seekTo(positionMs)
    }

    fun seekTo(windowIndex: Int, positionMs: Long){
        player?.seekTo(windowIndex, positionMs)
    }

    fun rewind(){
        val currentPosition = player?.currentPosition ?: 0
        val seekPos = if((currentPosition - DEFAULT_REWIND_TIME) < 0) 0 else (currentPosition - DEFAULT_REWIND_TIME)
        player?.seekTo(seekPos)
    }

    fun fFwd(){
        val currentPosition = player?.currentPosition ?: 0
        // do operation on playback time
        val seekPos = if((currentPosition + DEFAULT_FAST_FORWARD_TIME) > 100) 0 else (currentPosition + DEFAULT_FAST_FORWARD_TIME)
        player?.seekTo(seekPos)
    }

    fun getCurrentPosition() = player!!.currentPosition

    fun getCurrentWindowIndex() = player!!.currentWindowIndex

    fun getPlaybackState() = player!!.playbackState

    fun getPlaybackError() = player!!.playbackError!!

   /* fun getPlayWhenReady(): Boolean {
        return playWhenReady
    }*/

    fun getApplicationLooper(): Looper = player!!.applicationLooper

    fun addListener(listener: VideoListener) {
        player?.addVideoListener(listener)
    }

    fun removeListener(listener: VideoListener) {
        player?.removeVideoListener(listener)
    }

    fun getCurrentTrackGroups(): TrackGroupArray = player!!.currentTrackGroups

    fun getVideoComponent() = player!!.videoComponent!!

    // Public APIs ends

    // Module APIs starts

    internal fun getPlayer(): SimpleExoPlayer? {
        return player
    }

    // Module APIs ends

    inner class EventListener: Player.EventListener {

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            Log.i(TAG, "onPlayerStateChanged")
            when(playbackState) {
                Player.STATE_IDLE -> {Log.i(TAG, "STATE_IDLE")}
                Player.STATE_BUFFERING -> {Log.i(TAG, "STATE_BUFFERING")}
                Player.STATE_READY -> {Log.i(TAG,"STATE_READY")}
                Player.STATE_ENDED -> {Log.i(TAG,"STATE_ENDED")}
            }
        }

        override fun onSeekProcessed() {
            Log.i(TAG, "onSeekProcessed")

        }

        override fun onLoadingChanged(isLoading: Boolean) {
            Log.i(TAG,"onLoadingChanged")
        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {
            Log.i(TAG,"onPlaybackParametersChanged")
        }

        override fun onPlayerError(error: ExoPlaybackException?) {
            Log.i(TAG,"onPlayerError")
        }

        override fun onPositionDiscontinuity(reason: Int) {
            Log.i(TAG,"onPositionDiscontinuity")
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            Log.i(TAG,"onRepeatModeChanged")
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            Log.i(TAG,"onShuffleModeEnabledChanged")
        }

        override fun onTimelineChanged(timeline: Timeline?, manifest: Any?, reason: Int) {
            Log.i(TAG,"onTimelineChanged")
        }

        override fun onTracksChanged(
            trackGroups: TrackGroupArray?,
            trackSelections: TrackSelectionArray?
        ) {
            Log.i(TAG,"onTracksChanged")
        }
    }


}