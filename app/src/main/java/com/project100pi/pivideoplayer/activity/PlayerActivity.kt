package com.project100pi.pivideoplayer.activity

import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import butterknife.BindView
import butterknife.ButterKnife
import com.project100pi.library.factory.PiPlayerFactory
import com.project100pi.library.misc.Util
import com.project100pi.library.player.PiVideoPlayer
import com.project100pi.library.ui.PiVideoPlayerView
import com.project100pi.pivideoplayer.R
import com.project100pi.pivideoplayer.dialogs.SRTFilePicker
import com.project100pi.pivideoplayer.dialogs.listeners.SRTFilePickerClickListener
import com.project100pi.pivideoplayer.utils.Constants
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.project100pi.library.misc.Logger


class PlayerActivity : AppCompatActivity(), SRTFilePickerClickListener {

    private var mediaPath: String? = null
    private var srtPath: String? = null
    private var videoList: ArrayList<String?>? = null

    private lateinit var mToolbar: Toolbar
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
                this.videoList = this.intent.getStringArrayListExtra(Constants.QUEUE)
            if (this.intent.extras != null && this.intent.hasExtra(Constants.FILE_PATH))
                this.mediaPath = this.intent.extras!!.getString(Constants.FILE_PATH)
            if (this.intent.hasExtra(Constants.Playback.WINDOW))
                this.currentWindow = this.intent.getIntExtra(Constants.Playback.WINDOW, 0)
        }

        playerView.requestFocus()
        mToolbar = playerView.toolbar

        rotateScreen()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.player_option, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
            R.id.item_subtitle -> {
                // SRTSelector(this).show(this@PlayerActivity.supportFragmentManager, "Subtitle Selector")

                SRTFilePicker(this, this).show(this@PlayerActivity.supportFragmentManager, "Subtitle Selector")

                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        finish()
    }

    public override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && null == videoPlayer) {
            initializePlayer()
        }
    }

    public override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }

    private fun initializePlayer() {

        if (null == videoPlayer) {
            videoPlayer = PiPlayerFactory.newPiPlayer(this)
            playerView.setPlayer(videoPlayer)
        }

        when {
            videoList != null && null == srtPath -> videoPlayer?.prepare(videoList, resetPosition = false, resetState = false)
            null != srtPath -> videoPlayer?.prepare(mediaPath!!, srtPath!!, resetPosition = false, resetState = false)
            else -> videoPlayer?.prepare(mediaPath!!)
        }
        videoPlayer?.seekTo(currentWindow, playbackPosition)
        currentWindow = 0
        playbackPosition = 0
        if (playerView.isControllerVisible())
            playerView.hideController()
        mToolbar.title = setToolbarTitle()
    }

    private fun releasePlayer() {
        if (null != videoPlayer) {
            videoPlayer?.release()
            videoPlayer = null
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        videoPlayer?.let {
            playbackPosition = it.getCurrentPosition()
            currentWindow = it.getCurrentWindowIndex()
        }
        val tempPlayer = videoPlayer
        if (null != tempPlayer) {
            playbackPosition = tempPlayer.getCurrentPosition()
            currentWindow = tempPlayer.getCurrentWindowIndex()
        }

        outState.putLong("playbackPosition", playbackPosition)
        outState.putInt("currentWindow", currentWindow)
        outState.putString("mediaPath", mediaPath)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        playbackPosition = savedInstanceState.getLong("playbackPosition")
        currentWindow = savedInstanceState.getInt("currentWindow")
        mediaPath = savedInstanceState.getString("mediaPath")
    }

    private fun setToolbarTitle(): String {
        val segments = mediaPath!!.split("/")
        return segments[segments.size - 1]
    }

    override fun filePickerSuccessClickListener(absolutePath: String) {
        this.srtPath = absolutePath
        if (null != videoPlayer) {
            playbackPosition = videoPlayer!!.getCurrentPosition()
            currentWindow = videoPlayer!!.getCurrentWindowIndex()
        }
        videoPlayer?.prepare(mediaPath!!, srtPath!!, resetPosition = true, resetState = true)
        videoPlayer?.seekTo(currentWindow, playbackPosition)
    }

    private fun rotateScreen() {
        try {
            //Create a new instance of MediaMetadataRetriever
            val retriever = MediaMetadataRetriever()
            //Declare the Bitmap
            val bmp: Bitmap

            var mVideoUri: Uri? = null
            if (this.intent.hasExtra(Constants.QUEUE)) {
                mVideoUri = Uri.parse(this.videoList?.get(this.currentWindow) ?: "")
            }
            if (this.intent.extras != null && this.intent.hasExtra(Constants.FILE_PATH)) {
                mVideoUri = Uri.parse(this.mediaPath)
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

}