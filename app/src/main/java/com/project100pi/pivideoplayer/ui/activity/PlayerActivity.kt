package com.project100pi.pivideoplayer.ui.activity

import android.content.Context
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
import com.project100pi.library.misc.CurrentSettings
import com.project100pi.library.misc.Logger
import com.project100pi.library.model.VideoMetaData
import com.project100pi.library.ui.PiVideoPlayerView


class PlayerActivity : AppCompatActivity(),
    PlayerViewActionsListener,
    PlaybackGestureControlListener {

    private var srtPath = ""
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

    private var layout: WindowManager.LayoutParams? = null
    private var currentBrightnessProgress = 0
    private var am: AudioManager? = null
    private var maxSystemVolume: Int = 0
    private var minSystemVolume: Int = 0
    private var currentVolume = 0
    private var currentBrightness = 0f
    private var currentVolumeProgress = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
        ButterKnife.bind(this)

        layout = window.attributes

        if (this.intent != null) {
            if (this.intent.hasExtra(Constants.QUEUE))
                this.videoList = this.intent.getParcelableArrayListExtra(Constants.QUEUE) ?: arrayListOf()
            if (this.intent.hasExtra(Constants.Playback.WINDOW))
                this.currentWindow = this.intent.getIntExtra(Constants.Playback.WINDOW, 0)
        }

        playerView.requestFocus()

        rotateScreenBasedOnVideoOrientation()

        am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        maxSystemVolume = am!!.getStreamMaxVolume(AudioManager.STREAM_MUSIC) * 3
        minSystemVolume = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            am!!.getStreamMinVolume(AudioManager.STREAM_MUSIC)
        } else {
            0
        }

        currentVolume = am!!.getStreamVolume(AudioManager.STREAM_MUSIC)
        currentVolumeProgress = (100/currentVolume)
        progressVolume.progress = currentVolumeProgress

        currentBrightness = window.attributes.screenBrightness
        currentBrightnessProgress = currentBrightness.toInt() * 100
        progressBrightness.progress = currentBrightnessProgress
    }

    override fun onBackPressed() {
        finish()
    }

    public override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && null == videoPlayer) {
            initializePlayer()
        } else {
            videoPlayer?.play()
        }
        playerView.onResume()
        playerView.setPlayerActionBarListener(this)
        playerView.setPlayerGestureControlListener(this)

        hideBrightnessProgress()
        hideVolumeProgress()
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

    private fun initializePlayer() {

        if (null == videoPlayer) {
            videoPlayer = PiPlayerFactory.newPiPlayer(this)
            playerView.setPlayer(videoPlayer!!)
        }

        when {
            videoList.size > 0 && srtPath.isEmpty() -> videoPlayer?.prepare(videoList, resetPosition = false, resetState = false)
        }
        videoPlayer?.seekTo(currentWindow, playbackPosition)
        if (playerView.isControllerVisible())
            playerView.hideController()

//       Handler().postDelayed({
//            Logger.i("exe from activity: player.seekTo()")
//            videoPlayer?.seekTo(4, 0)
//        },  10000)
    }

    private fun releasePlayer() {
        videoPlayer?.let {
            it.release()
            videoPlayer = null
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        videoPlayer?.let {
            playbackPosition = it.getCurrentPosition()
            currentWindow = it.getCurrentWindowIndex()
        }

        outState.putLong("playbackPosition", playbackPosition)
        outState.putInt("currentWindow", currentWindow)
        outState.putParcelableArrayList("videoList", videoList)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        playbackPosition = savedInstanceState.getLong("playbackPosition")
        currentWindow = savedInstanceState.getInt("currentWindow")
        videoList = savedInstanceState.getParcelableArrayList<VideoMetaData>("videoList") as ArrayList<VideoMetaData>
    }

//    private fun getSystemNavigationParams(): Int {
//        val resources: Resources = resources
//        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
//        if (resourceId > 0) {
//            return resources.getDimensionPixelSize(resourceId)
//        }
//        return 0
//    }

    private fun rotateScreenBasedOnVideoOrientation() {
        try {
            //Create a new instance of MediaMetadataRetriever
            val retriever = MediaMetadataRetriever()
            //Declare the Bitmap
            val bmp: Bitmap

            var mVideoUri: Uri? = null
            if (this.intent.hasExtra(Constants.QUEUE)) {
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

    private fun rotateScreenOnPlayerViewAction() {
        try {

            if (CurrentSettings.Video.orientation === "portrait") {
                //Set orientation to landscape
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                CurrentSettings.Video.orientation = "landscape"
            } else if (CurrentSettings.Video.orientation === "landscape") {
                //Set orientation to portrait
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                CurrentSettings.Video.orientation = "portrait"
            }

        } catch (ex: RuntimeException) {
            //error occurred
            Logger.e("MediaMetadataRetriever - Failed to rotate the video")
        }
    }

    override fun onPlayerBackButtonPressed() {
        finish()
    }

    override fun onPlayerCurrentQueuePressed() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onScreenRotatePressed() {
        rotateScreenOnPlayerViewAction()
    }

    // player gesture control

    override fun onVolumeUp() {
        if (currentVolume < maxSystemVolume) {
            ++currentVolume
            if (currentVolume % 3 == 0) {
                am?.setStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    currentVolume,
                    0
                )
            }
            if (currentVolumeProgress < 100)
                currentVolumeProgress += 7
            else
                currentVolumeProgress = 100
        }
        hideBrightnessProgress()
        setVolumeProgress(currentVolumeProgress)
    }

    override fun onVolumeDown() {
        if (currentVolume > minSystemVolume) {
            --currentVolume
            if (currentVolume % 3 == 0) {
                am?.setStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    currentVolume,
                    0
                )
            }
            if (currentVolumeProgress > 0)
                currentVolumeProgress -= 7
            else
                currentVolumeProgress = 0
        }
        hideBrightnessProgress()
        setVolumeProgress(currentVolumeProgress)
    }

    override fun onBrightnessUp() {
        if (window.attributes.screenBrightness < 1.0) {
            val b = window.attributes.screenBrightness + 0.05f
            layout?.screenBrightness = b
            window.attributes = layout
            if (currentBrightnessProgress < 100)
                currentBrightnessProgress += 5
            else
                currentBrightnessProgress = 100
        }
        hideVolumeProgress()
        setBrightnessProgress(currentBrightnessProgress)
    }

    override fun onBrightnessDown() {
        if (window.attributes.screenBrightness > 0.1) {
            val b = window.attributes.screenBrightness - 0.05f
            layout?.screenBrightness = b
            window.attributes = layout
            if (currentBrightnessProgress > 5)
                currentBrightnessProgress -= 5
            else
                currentBrightnessProgress = 0
        }
        hideVolumeProgress()
        setBrightnessProgress(currentBrightnessProgress)
    }

    override fun onFastForward() {
        videoPlayer?.let {
            it.seekTo(it.getCurrentWindowIndex(), it.getCurrentPosition() + it.DEFAULT_FAST_FORWARD_TIME)
        }
        hideBrightnessProgress()
        hideVolumeProgress()
    }

    override fun onRewind() {
        videoPlayer?.let {
            it.seekTo(it.getCurrentWindowIndex(), it.getCurrentPosition() - it.DEFAULT_REWIND_TIME)
        }
        hideBrightnessProgress()
        hideVolumeProgress()
    }

    override fun onActionUp() {
        hideVolumeProgress()
        hideBrightnessProgress()
    }

    private fun hideBrightnessProgress(){
        progressBrightness.visibility = View.GONE
    }

    private fun hideVolumeProgress(){
        progressVolume.visibility = View.GONE
    }

    private fun setBrightnessProgress(progress: Int){
        progressBrightness.visibility = View.VISIBLE
        progressBrightness.progress = progress
    }

    private fun setVolumeProgress(progress: Int){
        progressVolume.visibility = View.VISIBLE
        progressVolume.progress = progress
    }
}