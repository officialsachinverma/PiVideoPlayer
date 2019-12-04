package com.project100pi.library.player

import android.content.*
import android.media.AudioManager
import android.net.Uri
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
import com.project100pi.library.misc.CurrentMediaState
import com.project100pi.library.listeners.MediaSessionListener
import com.project100pi.library.listeners.PiPlayerEventListener
import com.project100pi.library.misc.CurrentMediaState.Playback.DEFAULT_FAST_FORWARD_TIME
import com.project100pi.library.misc.CurrentMediaState.Playback.DEFAULT_REWIND_TIME
import com.project100pi.library.model.VideoMetaData
import java.lang.NullPointerException


class PiVideoPlayer(private val context: Context): MediaSessionListener {

    private var player: SimpleExoPlayer? = null
    var playWhenReady: Boolean = true

    private var _videoList = MutableLiveData<ArrayList<VideoMetaData>>()
    val videoList: LiveData<ArrayList<VideoMetaData>>
        get() = _videoList

    private val audioAttributes = AudioAttributes.Builder()
                                                                .setUsage(C.USAGE_MEDIA)
                                                                .setContentType(C.CONTENT_TYPE_MUSIC)
                                                                .build()

    private val dataSourceFactory: DefaultDataSourceFactory

    // Noisy Intent
    private val intentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
    private var noisyIntentRegistered = false
    private val becomingNoisyReceiver = BecomingNoisyReceiver()

    // Media Session
    private var mediaSessionManager: MediaSessionManager
    private var transportControls: MediaControllerCompat.TransportControls

    //Currently Playing Queue
    private var _nowPlaying = MutableLiveData<VideoMetaData>()
    val nowPlaying: LiveData<VideoMetaData>
        get() = _nowPlaying

    private var playerListener: PiPlayerEventListener? = null

    val concatenatingMediaSource = ConcatenatingMediaSource()

    init {

        initExoPlayer()

        // Initialising Data Source Factory
        dataSourceFactory = DefaultDataSourceFactory(context, Util.getUserAgent(context, userAgent))

        // Initialising media session manager
        mediaSessionManager = MediaSessionManager(context.applicationContext, this)

        transportControls = mediaSessionManager.getTransportControls()
        // Getting already saved media session token (it is a soft save and will live only till the application is alive)
        ApplicationHelper.mediaSessionToken = mediaSessionManager.getMediaSessionToken()

        CurrentMediaState.MediaSession.avaiable = true
    }

    /**
     * Initialising exo player
     */
    private fun initExoPlayer(){
        // Initialising exo player with default renders, track selectors and load controllers
        if (player == null) {
            player = ExoPlayerFactory.newSimpleInstance(
                context,
                DefaultRenderersFactory(context),
                DefaultTrackSelector(),
                DefaultLoadControl())

            // Registering listeners, audio attributes and settings playWhenReady
            player?.let {
                // Setting player event callbacks
                it.addListener(EventListener())
                // Setting audio attributes and enabling audio focus
                it.setAudioAttributes(audioAttributes, true)
                // Settings play when ready so that player starts playing
                // as soon as it gets prepared with media source (default is true)
                // if it is true only then it will start playing after prepare otherwise not
                it.playWhenReady = this.playWhenReady
            }
        }
    }

    /**
     * This method takes the uri and returns the suitable media source
     *
     * @param uri Uri
     * @return MediaSource
     */
    private fun buildMediaSource(uri: Uri): MediaSource {
        return when (@C.ContentType val type = Util.inferContentType(uri)) {

            C.TYPE_DASH -> DashMediaSource.Factory(dataSourceFactory).createMediaSource(uri)

            C.TYPE_SS -> SsMediaSource.Factory(dataSourceFactory).createMediaSource(uri)

            C.TYPE_HLS -> HlsMediaSource.Factory(dataSourceFactory).createMediaSource(uri)

            C.TYPE_OTHER -> ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(uri)

            else -> throw IllegalStateException("Unsupported type: $type")
        }
    }

    // Public APIs starts

    /**
     * This method prepared the player with media and subtitle
     * It takes [VideoMetaData] and prepares the player with that media source
     * this method is available as public api to the application and hence provided with option
     * for reset position and reset state, so that user can pass these values as required
     *
     * @param videoMetaData Video meta data
     * @param resetPosition Whether the playback position should be reset to the default position in
     *     the first window. If false, playback will start from the position defined
     *     by getCurrentWindowIndex() and getCurrentPosition().
     * @param resetState Whether the timeline, tracks and track selections should be reset.
     *     Should be true unless the player is being prepared to play the same media as it was playing
     *     previously.
     */
    fun prepare(videoMetaData: VideoMetaData, resetPosition: Boolean, resetState: Boolean) {

        _nowPlaying.value = videoMetaData

        val mediaSource = buildMediaSource(Uri.parse(videoMetaData.path))

        player?.prepare(mediaSource, resetPosition, resetState)

        context.registerReceiver(becomingNoisyReceiver, intentFilter)

        noisyIntentRegistered = true

        CurrentMediaState.Playback.playing = true
    }

    /**
     * This method prepared the player with media and subtitle
     * It takes [VideoMetaData] and path of subtitle files and create a merging media source
     * and prepares the player with that media source
     * this method is available as public api to the application and hence provided with option
     * for reset position and reset state, so that user can pass these values as required
     *
     * @param videoMetaData Video meta data
     * @param subtitlePath Path of subtitle file
     * @param resetPosition Whether the playback position should be reset to the default position in
     *     the first window. If false, playback will start from the position defined
     *     by getCurrentWindowIndex() and getCurrentPosition().
     * @param resetState Whether the timeline, tracks and track selections should be reset.
     *     Should be true unless the player is being prepared to play the same media as it was playing
     *     previously.
     */
    fun prepare(videoMetaData: VideoMetaData, subtitlePath: String, resetPosition: Boolean, resetState: Boolean) {

        _nowPlaying.value = videoMetaData

        val dataSourceFactory = DefaultDataSourceFactory(context, Util.getUserAgent(context, userAgent))

        val mediaSource = buildMediaSource(Uri.parse(videoMetaData.path))

        val textFormat = Format.createTextSampleFormat(null, MimeTypes.TEXT_VTT, NO_VALUE, "hi")

        val subtitleSource = SingleSampleMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.parse(subtitlePath), textFormat, C.TIME_UNSET)

        player?.prepare(MergingMediaSource(mediaSource, subtitleSource), resetPosition, resetState)

        context.registerReceiver(becomingNoisyReceiver, intentFilter)

        noisyIntentRegistered = true

        CurrentMediaState.Playback.playing = true
    }

    /**
     * This method prepared the player with a list of videos
     * It takes list of [VideoMetaData] and prepares the media source
     * this method is available as public api to the application and hence provided with option
     * for reset position and reset state, so that user can pass these values as required
     *
     * @param videoMetaDataList A playlist of videos
     * @param resetPosition Whether the playback position should be reset to the default position in
     *     the first window. If false, playback will start from the position defined
     *     by getCurrentWindowIndex() and getCurrentPosition().
     * @param resetState Whether the timeline, tracks and track selections should be reset.
     *     Should be true unless the player is being prepared to play the same media as it was playing
     *     previously.
     */
    fun prepare(videoMetaDataList: ArrayList<VideoMetaData>, resetPosition: Boolean, resetState: Boolean) {

        _videoList.value = videoMetaDataList

        for (videoMetaData in videoMetaDataList) {
            val mediaSource = buildMediaSource(Uri.parse(videoMetaData.path))
            concatenatingMediaSource.addMediaSource(mediaSource)
        }

        player?.prepare(concatenatingMediaSource, resetPosition, resetState)

        context.registerReceiver(becomingNoisyReceiver, intentFilter)

        noisyIntentRegistered = true

        CurrentMediaState.Playback.playing = true
    }

    /**
     * Releases the player. This method must be called when the player is no longer required. The
     * player must not be used after calling this method.
     */
    fun release(){

        player?.release()

        player = null

        if (noisyIntentRegistered)
            context.unregisterReceiver(becomingNoisyReceiver)

        CurrentMediaState.Playback.playing = false
    }

    /**
     * Sets playback state to play when ready.
     *
     */

    fun play(){
        player?.playWhenReady = true
        CurrentMediaState.Playback.playing = true
    }

    /**
     * Sets playback state to do not play.
     *
     */

    fun pause(){
        player?.playWhenReady = false
        CurrentMediaState.Playback.playing = false
    }

    /**
     * Seeks to a position specified in milliseconds in the current window.
     *
     * @param positionMs The seek position in the current window to seek to
     *     the window's default position.
     */

    fun seekTo(positionMs: Long){
        player?.seekTo(positionMs)
    }

    /**
     * Seeks to a position specified in milliseconds in the specified window.
     *
     * @param windowIndex The index of the window.
     * @param positionMs The seek position in the specified window, or {@link C#TIME_UNSET} to seek to
     *     the window's default position.
     * @throws IllegalSeekPositionException If the player has a non-empty timeline and the provided
     *     {@code windowIndex} is not within the bounds of the current timeline.
     */
    fun seekTo(windowIndex: Int, positionMs: Long){
        player?.seekTo(windowIndex, positionMs)
        _videoList.value?.let {
            _nowPlaying.value = _videoList.value!![windowIndex]
        }

    }

    /**
     * Rewind the playback with an specified rewind time
     *
     * We basically subtract a specified time in the current play back position
     * then seek to the new playback position (after subtracted the specified time duration)
     */
    fun rewind(){
        val currentPosition = player?.currentPosition ?: 0
        val seekPos = if((currentPosition - DEFAULT_REWIND_TIME) < 0) 0 else (currentPosition - DEFAULT_REWIND_TIME)
        player?.seekTo(seekPos)
    }

    /**
     * Fast forwards the playback with an specified fast forward time
     *
     * We basically add a specified time in the current play back position
     * then seek to the new playback position (after adding the specified time duration)
     */
    fun fFwd(){
        val currentPosition = player?.currentPosition ?: 0
        // do operation on playback time
        val seekPos = if((currentPosition + DEFAULT_FAST_FORWARD_TIME) > 100) 0 else (currentPosition + DEFAULT_FAST_FORWARD_TIME)
        player?.seekTo(seekPos)
    }

    /**
     * Seeks to the default position of the next window in the timeline, which may depend on the
     * current repeat mode and whether shuffle mode is enabled.
     * Does nothing if hasNext() is false.
     */
    fun next(){
        player?.next()
    }

    /**
     * Seeks to the default position of the previous window in the timeline, which may depend on the
     * current repeat mode and whether shuffle mode is enabled.
     * Does nothing if hasPrevious() is false.
     */
    fun previous() {
        player?.previous()
    }

    /**
     * Returns the playback position in the current content window, in milliseconds.
     */
    fun getCurrentPosition() = player!!.currentPosition

    /**
     * Returns the index of the window currently being played.
     */
    fun getCurrentWindowIndex() = player!!.currentWindowIndex

    /**
     * Returns the index of the previous timeline window to be played, which may depend on the current
     * repeat mode and whether shuffle mode is enabled.
     */
    fun getPreviousWindowIndex() = player!!.previousWindowIndex

    /**
     * Returns the index of the next timeline window to be played, which may depend on the current
     * repeat mode and whether shuffle mode is enabled.
     */
    fun getNextWindowIndex() = player!!.nextWindowIndex

    /**
     * Adds a listener to receive video events.
     *
     * @param listener The listener to register.
     */
    fun addListener(listener: VideoListener) {
        player?.addVideoListener(listener)
    }

    /**
     * Removes a listener of video events.
     *
     * @param listener The listener to unregister.
     */
    fun removeListener(listener: VideoListener) {
        player?.removeVideoListener(listener)
    }

    /**
     * Adds a listener to receive Pi Player Events.
     *
     * @param playerListener The listener to register.
     */
    fun addPlayerEventListener(playerListener: PiPlayerEventListener) {
        this.playerListener = playerListener
    }

    /**
     * Returns whether a next window exists, which may depend on the current repeat mode and whether
     * shuffle mode is enabled.
     * @returns false, if exo player is null.
     */
    fun hasNext() = player?.hasNext() ?: false

    /**
     * Returns whether a previous window exists, which may depend on the current repeat mode and
     * whether shuffle mode is enabled.
     * @returns false, if exo player is null.
     */
    fun hasPrevious() = player?.hasPrevious() ?: false

    /**
     * Sets the RepeatMode to be used for playback to REPEAT_MODE_OFF
     */
    fun repeatOff() {
        player?.repeatMode = SimpleExoPlayer.REPEAT_MODE_OFF
    }

    /**
     * Sets the RepeatMode to be used for playback to REPEAT_MODE_ONE
     */
    fun repeatOne() {
        player?.repeatMode = SimpleExoPlayer.REPEAT_MODE_ONE
    }

    /**
     * Sets the RepeatMode to be used for playback to REPEAT_MODE_ALL
     */
    fun repeatAll() {
        player?.repeatMode = SimpleExoPlayer.REPEAT_MODE_ALL
    }

    /**
     * Sets whether shuffling of windows is enabled.
     *
     * @param shuffleModeEnabled Whether shuffling is enabled.
     */
    fun shuffle(shuffleModeEnabled: Boolean){
        player?.shuffleModeEnabled = shuffleModeEnabled
    }

    /**
     * Returns the duration of the current content window in milliseconds
     */
    fun getDuration() = player?.duration ?: 0

    // Public APIs ends

    // Module APIs starts

    /**
     * Returns instance of Simple Exo Player
     *
     * @return SimpleExoPlayer?
     */
    internal fun getExoPlayer() = player

    /**
     * Sets the Video Scaling Mode.
     *
     * @param videoScalingMode
     */
    internal fun setVideoScalingMode(videoScalingMode: Int){
        player!!.videoScalingMode = videoScalingMode
    }

    /**
     * Prepare player with ConcatenatingMediaSource or MergingMediaSource
     * based on the availability of playlist. If play is available then
     * ConcatenatingMediaSource, otherwise MergingMediaSource
     *
     * @param absolutePath the path of subtitle file
     */
    internal fun addSubtitle(absolutePath: String) {
        try {
            when {
                _videoList.value!!.size > 0 -> {
                    // This solution does not retain playlist and prepares player with single video with subtitle
//                prepare(videoList.value!![getCurrentWindowIndex()], absolutePath, resetPosition = true, resetState = true)

                    // We are saving current playback position and windows index
                    // Getting media source of that window and putting subtitle and that
                    // media source in merging media source
                    // then removing the old media source (the one WITHOUT subtitle)
                    // and adding new merging media source (the one WITH subtitle) in it's window index
                    // And again preparing the player with modified ConcatenatingMediaSource
                    // This is the only known solution (as of now) to add subtitle to a video of a playlist in exo player
                    // Adding subtitle to a single video is easy by simply using merging media source
                    // problem comes when there is a playlist

                    val index = getCurrentWindowIndex()
                    val position = getCurrentPosition()

                    val mediaSource = concatenatingMediaSource.getMediaSource(getCurrentWindowIndex())

                    val textFormat = Format.createTextSampleFormat(null, MimeTypes.TEXT_VTT, NO_VALUE, "hi")

                    val subtitleSource = SingleSampleMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.parse(absolutePath), textFormat, C.TIME_UNSET)

                    concatenatingMediaSource.addMediaSource(index+1, MergingMediaSource(mediaSource, subtitleSource))

//                seekTo(index+1, position)

                    concatenatingMediaSource.removeMediaSource(index)

                    player?.prepare(concatenatingMediaSource)

                    seekTo(index, position)
                }
                _nowPlaying.value != null -> {
                    prepare(_nowPlaying.value!!, absolutePath, resetPosition = true, resetState = true)
                }
            }
        } catch (e: NullPointerException) {
            e.printStackTrace()
            Logger.e(e.message.toString())
        }
    }

    //internal fun getCurrentPlayingVideo() = nowPlaying.value

    // Module APIs ends

    // Media Session Listeners Starts

    override fun onPlay() {
        play()
    }

    override fun onPause() {
        pause()
    }

    override fun onSkipToNext() {
        next()
    }

    override fun onSkipToPrevious() {
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
            _videoList.value?.let {
                _nowPlaying.value = it[getCurrentWindowIndex()]
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