package com.project100pi.pivideoplayer.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import butterknife.BindView
import butterknife.ButterKnife
import com.project100pi.library.factory.PiPlayerFactory
import com.project100pi.library.player.PiVideoPlayer
import com.project100pi.pivideoplayer.R
import com.project100pi.pivideoplayer.utils.Constants
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.view.View
import android.view.WindowManager
import android.widget.ProgressBar
import com.project100pi.library.listeners.PlaybackGestureControlListener
import com.project100pi.library.listeners.PlayerViewActionsListener
import com.project100pi.library.misc.CurrentMediaState
import com.project100pi.library.misc.Logger
import com.project100pi.library.model.VideoMetaData
import com.project100pi.library.ui.PiVideoPlayerView


class PlayerActivity : AppCompatActivity(),
    PlayerViewActionsListener,
    PlaybackGestureControlListener {

    private var videoList = arrayListOf<VideoMetaData>()

    @BindView(R.id.pv_player)
    lateinit var playerView: PiVideoPlayerView
    @BindView(R.id.pb_brightness)
    lateinit var progressBrightness: ProgressBar
    @BindView(R.id.pb_volume)
    lateinit var progressVolume: ProgressBar

    private var videoPlayer: PiVideoPlayer? = null

    private var currentWindow = 0
    private var playbackPosition: Long = 0

    private var layoutParams: WindowManager.LayoutParams? = null
    private var audioManager: AudioManager? = null
    private var maxSystemVolume: Int = 0
    private var minSystemVolume: Int = 0
    private var currentVolume = 0
    private var currentBrightness = 0f
    private var currentVolumeProgress = 0
    private var currentBrightnessProgress = 0
    private var volumeLevel = 0

    companion object {

        // starter pattern is more strict approach to starting an activity.
        // Main purpose is to improve more readability, while at the same time
        // decrease code complexity, maintenance costs, and coupling of your components.

        // Read more: https://blog.mindorks.com/learn-to-write-good-code-in-android-starter-pattern
        // https://www.programming-books.io/essential/android/starter-pattern-d2db17d348ca46ce8979c8af6504f018

        // Using starter pattern to start this activity
        fun start(context: Context, videoList: ArrayList<VideoMetaData>, currentWindow: Int) {
            val playerIntent = Intent(context, PlayerActivity::class.java)
            playerIntent.putParcelableArrayListExtra(Constants.Playback.PLAYBACK_QUEUE, videoList)
            playerIntent.putExtra(Constants.Playback.WINDOW, currentWindow)
            context.startActivity(playerIntent)
        }

        // Using starter pattern to start this activity
        fun start(context: Context, videoList: ArrayList<VideoMetaData>) {
            val playerIntent = Intent(context, PlayerActivity::class.java)
            playerIntent.putParcelableArrayListExtra(Constants.Playback.PLAYBACK_QUEUE, videoList)
            context.startActivity(playerIntent)
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
        ButterKnife.bind(this)

        layoutParams = window.attributes

        getDataFromIntent()

        // rotating the screen based on video orientation
        rotateScreenBasedOnVideoOrientation()

        setAudioParamForGestureControl()

        setBrightnessParamForGestureControl()
    }

    /**
     * Fetches dat from intent
     */
    private fun getDataFromIntent() {
        this.intent?.let {
            if (it.hasExtra(Constants.Playback.PLAYBACK_QUEUE))
                this.videoList = it.getParcelableArrayListExtra(Constants.Playback.PLAYBACK_QUEUE) ?: arrayListOf()
            if (it.hasExtra(Constants.Playback.WINDOW))
                this.currentWindow = it.getIntExtra(Constants.Playback.WINDOW, 0)
        }
    }

    /**
     * This method sets currentBrightness and
     * based on that decides what should be the
     * current progress on brightness progress bar
     */
    private fun setBrightnessParamForGestureControl() {
        // getting current activity brightness
        currentBrightness = window.attributes.screenBrightness
        // activity max brightness is 1.0 min is 0.1
        // getting the current brightness and calculating corresponding progress on bar
        // Eg.: if current brightness is 0.7 so the progress will be 0.7*100=70
        currentBrightnessProgress = currentBrightness.toInt() * 100
        // setting the current brightness on progress bar
        progressBrightness.progress = currentBrightnessProgress
        // settings max progress as 100
        progressBrightness.max = 100
    }

    /**
     * This method initialises audio manager
     * It sets maxSystemVolume and minSystemVolume,
     * based on that decides what should be the
     * current progress on volume progress bar
     */
    private fun setAudioParamForGestureControl() {
        // initialising audio manager
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        // getting the maximum system volume
        maxSystemVolume = audioManager!!.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        // getting the minimum system volume - default is 0
        minSystemVolume = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            audioManager!!.getStreamMinVolume(AudioManager.STREAM_MUSIC)
        } else {
            0
        }

        // considering 100 as max progress we are dividing 100 by max volume
        // so that we can set progress on volume progress bar with each volume increase accordingly
        volumeLevel = 100/maxSystemVolume

        // getting the current system volume
        currentVolume = audioManager!!.getStreamVolume(AudioManager.STREAM_MUSIC)
        // calculating progress based on current system volume
        currentVolumeProgress = if (currentVolume == 0) {
            0
        } else {
//            (currentVolume * volumeLevel)
            currentVolume
        }
        // setting the volume progress based on current system volume
        progressVolume.progress = currentVolumeProgress
        progressVolume.max = maxSystemVolume
    }

    public override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && null == videoPlayer) {
            initializePlayer()
        } else {
            if (null == videoPlayer) {
                initializePlayer()
            } else {
                // playing the video again when user opens the app again after minimizing it
                videoPlayer?.play()
            }
        }
        playerView.onResume()
        playerView.setPlayerActionBarListener(this)
        playerView.setPlayerGestureControlListener(this)
    }

    override fun onPause() {
        super.onPause()
        videoPlayer?.pause()
        playerView.onPause()
    }

    public override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }

    /**
     * this method initialises player
     * sets player in player view
     * prepares the player with list of videos
     * seeks to the video position which was
     * selected by user
     * and hides the controller for the first time
     */

    private fun initializePlayer() {

        if (null == videoPlayer) {
            videoPlayer = PiPlayerFactory.newPiPlayer(this)
            setPlayerToPlayerView()
        }

        when {
            videoList.size > 0 -> preparePlayerWithVideos()
        }
        // seeking to the user selected video
        // user can select any video from a folder to play
        // player is prepared by all the videos present in the folder
        // by default player will start playing from first video
        // but if user has selected 3rd video from folder to player
        // so we have to seek to the 3rd video
        seekTo(currentWindow, playbackPosition)

        // hiding controllers as we dont want to show
        // them as soon as video is started playing
        // user can invoke the controllers any time by single tap on
        // screen any where
        hideController()
    }

    /**
     * sets player to playerView
     */

    private fun setPlayerToPlayerView() {
        playerView.setPlayer(videoPlayer!!)
    }

    /**
     * prepare player with list of videos
     */

    private fun preparePlayerWithVideos() {
        videoPlayer?.prepare(videoList, resetPosition = false, resetState = false)
    }

    /**
     * seeks to selected videos (windowIndex)
     * and position (playbackPosition) in milliseconds
     *
     * @param windowIndex Int
     * @param positionMs Long
     */

    private fun seekTo(windowIndex: Int, positionMs: Long) {
        videoPlayer?.seekTo(windowIndex, positionMs)
    }

    /**
     * this method checks if controller is visible
     * or not if it is visible then it hides it
     */

    private fun hideController() {
        if (playerView.isControllerVisible())
            playerView.hideController()
    }

    /**
     * Releases the player. This method must be called when the player is no longer required. The
     * player must not be used after calling this method.
     */

    private fun releasePlayer() {
        // If video player is already null then ?. operator won't do release operation
        // since it is already null
        videoPlayer?.release()
        // making video player null to be sure
        videoPlayer = null
    }

    /**
     * Saves data like current window which is being played
     * and current play back position when ever activity destroys
     * Useful when screen orientations changes
     * when ever screen orientation changes an activity destroys
     * and gets recreated in new orientation
     * before activity destroys we save data and restore it when
     * it gets recreated
     * we save the data in a bundle
     *
     * @param outState Bundle
     */

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        videoPlayer?.let {
            playbackPosition = it.getCurrentPosition()
            currentWindow = it.getCurrentWindowIndex()
        }

        outState.putLong(Constants.Playback.PLAYBACK_POSITION, playbackPosition)
        outState.putInt(Constants.Playback.CURRENT_POSITION, currentWindow)
        outState.putParcelableArrayList(Constants.Playback.VIDEO_LIST, videoList)
    }

    /**
     * restoring the data when activity gets recreated
     * saved data comes in bundle which we saved in onSaveInstanceState
     *
     * @param savedInstanceState Bundle
     */

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        playbackPosition = savedInstanceState.getLong(Constants.Playback.PLAYBACK_POSITION)
        currentWindow = savedInstanceState.getInt(Constants.Playback.CURRENT_POSITION)
        videoList = savedInstanceState.getParcelableArrayList<VideoMetaData>(Constants.Playback.VIDEO_LIST) as ArrayList<VideoMetaData>
    }

//    private fun getSystemNavigationParams(): Int {
//        val resources: Resources = resources
//        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
//        if (resourceId > 0) {
//            return resources.getDimensionPixelSize(resourceId)
//        }
//        return 0
//    }

    /**
     * this method calculates the orientation of video
     * and changes the orientation accordingly
     * NOTE: By default the orientation of this activity is
     * landscape
     */

    private fun rotateScreenBasedOnVideoOrientation() {
        try {
            //Create a new instance of MediaMetadataRetriever
            val retriever = MediaMetadataRetriever()
            //Declare the Bitmap
            val bmp: Bitmap

            var mVideoUri: Uri? = null
            if (this.intent.hasExtra(Constants.Playback.PLAYBACK_QUEUE)) {
                mVideoUri = Uri.parse(this.videoList[this.currentWindow].path)
            }
            //Set the video Uri as data source for MediaMetadataRetriever
            retriever.setDataSource(this, mVideoUri!!)
            //Get one "frame"/bitmap - * NOTE - no time was set, so the first available frame will be used
            bmp = retriever.frameAtTime

            //Get the bitmap width and height
            val videoWidth = bmp.width
            val videoHeight = bmp.height

            //If the width is bigger then the height then it means that the video was taken in landscape mode and we should set the orientation to landscape
            if (videoWidth > videoHeight) {
                //Set orientation to landscape
                this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
            //If the width is smaller then the height then it means that the video was taken in portrait mode and we should set the orientation to portrait
            if (videoWidth < videoHeight) {
                //Set orientation to portrait
                this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }

        } catch (ex: RuntimeException) {
            //error occurred
            ex.printStackTrace()
            Logger.e("MediaMetadataRetriever - Failed to rotate the video")
        }
    }

    /**
     * Method changes the orientation of screen based on
     * user's action
     */

    private fun rotateScreenOnPlayerViewAction() {
        try {

            if (CurrentMediaState.Video.orientation === Constants.Orientation.PORTRAIT) {
                //Set orientation to landscape
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                CurrentMediaState.Video.orientation = Constants.Orientation.LANDSCAPE
            } else if (CurrentMediaState.Video.orientation === Constants.Orientation.LANDSCAPE) {
                //Set orientation to portrait
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                CurrentMediaState.Video.orientation = Constants.Orientation.PORTRAIT
            }

        } catch (ex: RuntimeException) {
            //error occurred
            ex.printStackTrace()
            Logger.e("MediaMetadataRetriever - Failed to rotate the video")
        }
    }

    /**
     * this callback will be called when user
     * clicks on back button in player view
     */

    override fun onPlayerBackButtonPressed() {
        finish()
    }

    /**
     * this callback will be called when user clicks
     * on show current queue menu option
     * NOTE: currently not available hence, left empty
     */

    override fun onPlayerCurrentQueuePressed() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /**
     * this callback will be called when user explicitly
     * wants to change the screen orientation
     */

    override fun onScreenRotatePressed() {
        rotateScreenOnPlayerViewAction()
    }

    // player gesture control

    /**
     * This method will be called when volume up gesture will
     * be detected in player view
     */

    override fun onVolumeUp() {
        if (currentVolume < maxSystemVolume) {
            ++currentVolume

                audioManager?.setStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    currentVolume,
                    0
                )
                if (currentVolumeProgress < 100)
                    currentVolumeProgress += volumeLevel
                else
                    currentVolumeProgress = 100
                setVolumeProgress(currentVolume)
        }
        hideBrightnessProgress()
    }

    /**
     * This method will be called when volume down gesture will
     * be detected in player view
     */

    override fun onVolumeDown() {
        if (currentVolume > minSystemVolume) {
            --currentVolume
                audioManager?.setStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    currentVolume,
                    0
                )
                if (currentVolumeProgress > 0)
                    currentVolumeProgress -= volumeLevel
                else
                    currentVolumeProgress = 0
                setVolumeProgress(currentVolume)
        }
        hideBrightnessProgress()
    }

    /**
     * This method will be called when brightness up gesture will
     * be detected in player view
     */

    override fun onBrightnessUp() {
        if (window.attributes.screenBrightness < 1.0) {
            val b = window.attributes.screenBrightness + 0.05f
            layoutParams?.screenBrightness = b
            window.attributes = layoutParams
            if (currentBrightnessProgress < 100)
                currentBrightnessProgress += 5
            else
                currentBrightnessProgress = 100
        }
        hideVolumeProgress()
        setBrightnessProgress(currentBrightnessProgress)
    }

    /**
     * This method will be called when brightness down gesture will
     * be detected in player view
     */

    override fun onBrightnessDown() {
        if (window.attributes.screenBrightness > 0.1) {
            val b = window.attributes.screenBrightness - 0.05f
            layoutParams?.screenBrightness = b
            window.attributes = layoutParams
            if (currentBrightnessProgress > 5)
                currentBrightnessProgress -= 5
            else
                currentBrightnessProgress = 0
        }
        hideVolumeProgress()
        setBrightnessProgress(currentBrightnessProgress)
    }

    /**
     * This method will be called when fast forward gesture will
     * be detected in player view
     */

    override fun onFastForward() {
        videoPlayer?.let {
            it.seekTo(it.getCurrentWindowIndex(), it.getCurrentPosition() + it.DEFAULT_FAST_FORWARD_TIME)
        }
        hideBrightnessProgress()
        hideVolumeProgress()
    }

    /**
     * This method will be called when rewind gesture will
     * be detected in player view
     */

    override fun onRewind() {
        videoPlayer?.let {
            it.seekTo(it.getCurrentWindowIndex(), it.getCurrentPosition() - it.DEFAULT_REWIND_TIME)
        }
        hideBrightnessProgress()
        hideVolumeProgress()
    }

    /**
     * This method will be called when user
     * has ended the gesture
     */

    override fun onActionUp() {
        hideVolumeProgress()
        hideBrightnessProgress()
    }

    /**
     * hides brightness progress bar
     */

    private fun hideBrightnessProgress(){
        progressBrightness.visibility = View.GONE
    }

    /**
     * hides volume progress bar
     */

    private fun hideVolumeProgress(){
        progressVolume.visibility = View.GONE
    }

    /**
     * shows and sets brightness progress bar
     *
     * @param progress Int
     */

    private fun setBrightnessProgress(progress: Int){
        progressBrightness.visibility = View.VISIBLE
        progressBrightness.progress = progress
    }

    /**
     * shows and sets volume progress bar
     *
     * @param progress Int
     */

    private fun setVolumeProgress(progress: Int){
        progressVolume.visibility = View.VISIBLE
        progressVolume.progress = progress
    }
}