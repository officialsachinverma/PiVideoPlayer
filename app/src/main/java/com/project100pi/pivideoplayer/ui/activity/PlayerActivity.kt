package com.project100pi.pivideoplayer.ui.activity

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
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.project100pi.library.listeners.PlayerViewActionsListener
import com.project100pi.library.misc.CurrentSettings
import com.project100pi.library.misc.Logger
import com.project100pi.library.model.VideoMetaData
import com.project100pi.library.ui.PiVideoPlayerView


class PlayerActivity : AppCompatActivity(), PlayerViewActionsListener {

    private var mediaPath: VideoMetaData? = null
    private var srtPath = ""
    private var videoList = arrayListOf<VideoMetaData>()

    @BindView(R.id.pv_player)
    lateinit var playerView: PiVideoPlayerView

    private var videoPlayer: PiVideoPlayer? = null

    private var currentWindow = 0
    private var playbackPosition: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
        ButterKnife.bind(this)

        if (this.intent != null) {
            if (this.intent.hasExtra(Constants.QUEUE))
                this.videoList = this.intent.getParcelableArrayListExtra(Constants.QUEUE) ?: arrayListOf()
            if (this.intent.extras != null && this.intent.hasExtra(Constants.FILE_PATH))
                this.mediaPath = this.intent.extras!!.getParcelable(Constants.FILE_PATH)
            if (this.intent.hasExtra(Constants.Playback.WINDOW))
                this.currentWindow = this.intent.getIntExtra(Constants.Playback.WINDOW, 0)
        }

        playerView.requestFocus()

        rotateScreenBasedOnVideoOrientation()
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
            srtPath.isNotEmpty() -> videoPlayer?.prepare(mediaPath!!,
                srtPath, resetPosition = false, resetState = false)
            else -> videoPlayer?.prepare(mediaPath!!)
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
        outState.putParcelable("mediaPath", mediaPath!!)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        playbackPosition = savedInstanceState.getLong("playbackPosition")
        currentWindow = savedInstanceState.getInt("currentWindow")
        mediaPath = savedInstanceState.getParcelable("mediaPath")
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
            if (this.intent.extras != null && this.intent.hasExtra(Constants.FILE_PATH)) {
                mVideoUri = Uri.parse(this.mediaPath!!.path)
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
}