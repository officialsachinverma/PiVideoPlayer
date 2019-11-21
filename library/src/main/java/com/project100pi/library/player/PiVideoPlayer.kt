package com.project100pi.library.player

import android.content.*
import android.media.AudioManager
import android.net.Uri
import android.os.Looper
import android.support.v4.media.session.MediaControllerCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.source.*
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.google.android.exoplayer2.video.VideoListener
import com.project100pi.library.misc.Util.userAgent
import com.google.android.exoplayer2.Format.NO_VALUE
import com.google.android.exoplayer2.util.MimeTypes
import com.project100pi.library.misc.Logger
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource
import com.project100pi.library.media.MediaSessionManager
import com.project100pi.library.misc.ApplicationHelper
import com.project100pi.library.misc.CurrentSettings
import com.project100pi.library.listeners.MediaSessionListener
import com.project100pi.library.listeners.PiPlayerEventListener
import com.project100pi.library.model.VideoMetaData


class PiVideoPlayer(private val context: Context): MediaSessionListener {

    private val TAG: String = "PiVideoPlayer"

    val DEFAULT_REWIND_TIME = 3000 // 3 secs
    val DEFAULT_FAST_FORWARD_TIME = 3000 // 3 secs

    private var player: SimpleExoPlayer? = null
    var playWhenReady: Boolean = true

    private var videoList = MutableLiveData<ArrayList<VideoMetaData>>()
    val videoListExposed: LiveData<ArrayList<VideoMetaData>>
        get() = videoList

    private var currentWindow = 0
    private var playbackPosition: Long = 0

    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.CONTENT_TYPE_MUSIC)
        .build()

    private val dataSourceFactory: DefaultDataSourceFactory

    private var srtPath = ""

    // Noisy Intent
    private val intentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
    private var noisyIntentRegistered = false
    private val becomingNoisyReceiver = BecomingNoisyReceiver()

    // Media Session
    private var mediaSessionManager: MediaSessionManager
    private var transportControls: MediaControllerCompat.TransportControls

    //Currently Playing Queue
    private var nowPlaying = MutableLiveData<VideoMetaData>()
    val nowPlayingExposed: LiveData<VideoMetaData>
        get() = nowPlaying

    private var playerListener: PiPlayerEventListener? = null

    val concatenatingMediaSource = ConcatenatingMediaSource()

    init {

        if (player == null) {
            player = ExoPlayerFactory.newSimpleInstance( context,
                DefaultRenderersFactory(context),
                DefaultTrackSelector(),
                DefaultLoadControl()
            )

        }

        dataSourceFactory = DefaultDataSourceFactory(
        context, Util.getUserAgent(context, userAgent))

        mediaSessionManager =
            MediaSessionManager(context.applicationContext, this)
        transportControls = mediaSessionManager.getTransportControls()
        ApplicationHelper.mediaSessionToken = mediaSessionManager.getMediaSessionToken()
        CurrentSettings.MediaSession.avaiable = true

        player?.let {
            it.addListener(EventListener())
            it.setAudioAttributes(audioAttributes, true)
            it.playWhenReady = this.playWhenReady
            it.seekTo(currentWindow, playbackPosition)
        }
    }

    private fun buildMediaSource(uri: Uri): MediaSource {
        return when (@C.ContentType val type = Util.inferContentType(uri)) {
            C.TYPE_DASH -> DashMediaSource.Factory(dataSourceFactory).createMediaSource(uri)
            C.TYPE_SS -> SsMediaSource.Factory(dataSourceFactory).createMediaSource(uri)
            C.TYPE_HLS -> HlsMediaSource.Factory(dataSourceFactory).createMediaSource(uri)
            C.TYPE_OTHER -> ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(uri)
            else -> throw IllegalStateException("Unsupported type: $type")
        }
    }

    // Public APIs starts

    fun prepare(videoMetaData: VideoMetaData) {
        nowPlaying.value = videoMetaData
        val mediaSource = buildMediaSource(Uri.parse(videoMetaData.path))
        player?.prepare(mediaSource, true, false)
        context.registerReceiver(becomingNoisyReceiver, intentFilter)
        noisyIntentRegistered = true
        CurrentSettings.Playback.playing = true
    }

    fun prepare(videoMetaData: VideoMetaData, subtitlePath: String, resetPosition: Boolean, resetState: Boolean) {
        nowPlaying.value = videoMetaData
        val dataSourceFactory = DefaultDataSourceFactory(
            context, Util.getUserAgent(context, userAgent))
        val mediaSource = buildMediaSource(Uri.parse(videoMetaData.path))
        val textFormat = Format.createTextSampleFormat(
            null, MimeTypes.TEXT_VTT,
            NO_VALUE, "hi"
        )
        val subtitleSource = SingleSampleMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.parse(subtitlePath), textFormat, C.TIME_UNSET)
        player?.prepare(MergingMediaSource(mediaSource, subtitleSource), resetPosition, resetState)
        context.registerReceiver(becomingNoisyReceiver, intentFilter)
        noisyIntentRegistered = true
        CurrentSettings.Playback.playing = true
    }

    fun prepare(videoMetaDataList: ArrayList<VideoMetaData>, resetPosition: Boolean, resetState: Boolean) {
        videoList.value = videoMetaDataList
        for (videoMetaData in videoMetaDataList) {
            val mediaSource = buildMediaSource(Uri.parse(videoMetaData.path))
            concatenatingMediaSource.addMediaSource(mediaSource)
        }
        player?.prepare(concatenatingMediaSource, resetPosition, resetState)
        context.registerReceiver(becomingNoisyReceiver, intentFilter)
        noisyIntentRegistered = true
        CurrentSettings.Playback.playing = true
    }

    fun release(){
        player?.release()
        player = null
        if (noisyIntentRegistered)
            context.unregisterReceiver(becomingNoisyReceiver)
        CurrentSettings.Playback.playing = false
    }

    fun play(){
        player?.playWhenReady = true
        CurrentSettings.Playback.playing = true
    }

    fun pause(){
        player?.playWhenReady = false
        CurrentSettings.Playback.playing = false
    }

    fun seekTo(positionMs: Long){
        player?.seekTo(positionMs)
    }

    fun seekTo(windowIndex: Int, positionMs: Long){
        player?.seekTo(windowIndex, positionMs)
        videoList.value?.let {
            nowPlaying.value = videoList.value!![windowIndex]
        }

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

    fun next(){
        player?.next()
    }

    fun previous() {
        player?.previous()
    }

    fun getCurrentPosition() = player!!.currentPosition

    fun getCurrentWindowIndex() = player!!.currentWindowIndex

    fun getPlaybackState() = player!!.playbackState

    fun getPlaybackError() = player!!.playbackError!!

   /* fun getPlayWhenReady(): Boolean {
        return playWhenReady
    }*/

    fun addListener(listener: VideoListener) {
        player?.addVideoListener(listener)
    }

    fun removeListener(listener: VideoListener) {
        player?.removeVideoListener(listener)
    }

    fun getCurrentTrackGroups(): TrackGroupArray = player!!.currentTrackGroups

    fun setPlayerEventListener(playerListener: PiPlayerEventListener) {
        this.playerListener = playerListener
    }

    fun hasNext() = player?.hasNext() ?: false

    fun hasPrevious() = player?.hasPrevious() ?: false

    fun repeatOff() {
        player?.repeatMode = SimpleExoPlayer.REPEAT_MODE_OFF
    }

    fun repeatOne() {
        player?.repeatMode = SimpleExoPlayer.REPEAT_MODE_ONE
    }

    fun repeatAll() {
        player?.repeatMode = SimpleExoPlayer.REPEAT_MODE_ALL
    }

    fun shuffle(enable: Boolean){
        player?.shuffleModeEnabled = enable
    }

    // Public APIs ends

    // Module APIs starts

    internal fun getExoPlayer() = player

    internal fun getApplicationLooper(): Looper = player!!.applicationLooper

    internal fun getVideoComponent() = player!!.videoComponent

    internal fun getTextComponent() = player!!.textComponent

    internal fun setVideoScalingMode(videoScalingMode: Int){
        player!!.videoScalingMode = videoScalingMode
    }

    internal fun addSubtitle(absolutePath: String) {
        this.srtPath = absolutePath
        val playbackPosition = getCurrentPosition()
        val currentWindow = getCurrentWindowIndex()
        when {
            videoList.value!!.size > 0 -> {
                prepare(videoList.value!![getCurrentWindowIndex()], absolutePath, resetPosition = true, resetState = true)
            }
            nowPlaying.value != null -> {
                prepare(nowPlaying.value!!, absolutePath, resetPosition = true, resetState = true)
            }
        }
        seekTo(currentWindow, playbackPosition)
    }

    //internal fun getCurrentPlayingVideo() = nowPlaying.value

    // Module APIs ends

    // Media Session Listeners Starts

    override fun msPlay() {
        play()
    }

    override fun msPause() {
        pause()
    }

    override fun msSkipToNext() {
        next()
    }

    override fun msSkipToPrevious() {
        previous()
    }

    // Media Session Listener Ends

    inner class EventListener: Player.EventListener {

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            Logger.i("onPlayerStateChanged")
            when(playbackState) {
                Player.STATE_IDLE -> {
                    Logger.i("STATE_IDLE")
                }
                Player.STATE_BUFFERING -> {
                    Logger.i("STATE_BUFFERING")
                }
                Player.STATE_READY -> {
                    Logger.i("STATE_READY")
                }
                Player.STATE_ENDED -> {
                    Logger.i("STATE_ENDED")
                    playerListener?.onPlayerTrackCompleted()
                }
            }
        }

        override fun onSeekProcessed() {
            Logger.i("onSeekProcessed")
        }

        override fun onLoadingChanged(isLoading: Boolean) {
            Logger.i("onLoadingChanged")
        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {
            Logger.i("onPlaybackParametersChanged")
        }

        override fun onPlayerError(error: ExoPlaybackException?) {
            Logger.i("onPlayerError")
            if (noisyIntentRegistered)
                context.unregisterReceiver(becomingNoisyReceiver)
            playerListener?.onPlayerError(error)
        }

        override fun onPositionDiscontinuity(reason: Int) {
            Logger.i("onPositionDiscontinuity")
            playerListener?.onTracksChanged()
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            Logger.i("onRepeatModeChanged")
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            Logger.i("onShuffleModeEnabledChanged")
        }

        override fun onTimelineChanged(timeline: Timeline?, manifest: Any?, reason: Int) {
            Logger.i("onTimelineChanged")
        }

        override fun onTracksChanged(
            trackGroups: TrackGroupArray?,
            trackSelections: TrackSelectionArray?
        ) {
            Logger.i("onTracksChanged")
            videoList.value?.let {
                nowPlaying.value = it[getCurrentWindowIndex()]
            }
            playerListener?.onTracksChanged()
        }
    }

    inner class BecomingNoisyReceiver: BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                // Pause the playback
                player!!.playWhenReady = false
            }
        }
    }

}