package com.project100pi.pivideoplayer.activity

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
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



class Player : AppCompatActivity(), SRTFilePickerClickListener {

    private var mediaPath: String? = null
    private var srtPath: String? = null
    private var videoList: ArrayList<String?>? = null

    @BindView(R.id.anim_toolbar) lateinit var mToolbar: Toolbar
    @BindView(R.id.pv_player) lateinit var playerView: PiVideoPlayerView
    private var videoPlayer: PiVideoPlayer? = null

    private var currentWindow = 0
    private var playbackPosition: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_player)
        ButterKnife.bind(this)

        setSupportActionBar(mToolbar)

        val mActionbar = supportActionBar
        mActionbar?.setDisplayHomeAsUpEnabled(true)


        if (this.intent != null) {
            if (this.intent.hasExtra(Constants.QUEUE))
                this.videoList = this.intent.getStringArrayListExtra(Constants.QUEUE)
            if (this.intent.extras != null && this.intent.hasExtra(Constants.FILE_PATH))
                this.mediaPath = this.intent.extras!!.getString(Constants.FILE_PATH)
        }

        hideSystemUI()

        playerView.requestFocus()
        playerView.setOnClickListener {
            if (!playerView.isControllerVisible()) {
                playerView.showController(true)
                showSystemUI()
            } else {
                playerView.hideController()
                hideSystemUI()
            }
        }
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
                // SRTSelector(this).show(this@Player.supportFragmentManager, "Subtitle Selector")

                SRTFilePicker(this, this).show(this@Player.supportFragmentManager, "Subtitle Selector")

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
        if (Util.SDK_INT <= 23 || null == videoPlayer) {
            initializePlayer()
        }
    }

    public override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }

    private fun initializePlayer() {

        if (videoPlayer == null) {
            videoPlayer = PiPlayerFactory.newPiPlayer(this)
            playerView.setPlayer(videoPlayer)
            videoPlayer?.seekTo(currentWindow, playbackPosition)
        }
        when {
            videoList != null && null == srtPath -> videoPlayer?.prepare(videoList!!, resetPosition = true, resetState = false)
            null != srtPath -> videoPlayer?.prepare(mediaPath!!, srtPath!!)
            else -> videoPlayer?.prepare(mediaPath!!)
        }
        currentWindow = 0
        playbackPosition = 0
        if (playerView.isControllerVisible())
            playerView.hideController()
        mToolbar.title = setToolbarTitle()
    }

    private fun releasePlayer() {
        if (videoPlayer != null) {
            videoPlayer?.release()
            videoPlayer = null
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        if (videoPlayer != null) {
            playbackPosition = videoPlayer!!.getCurrentPosition()
            currentWindow = videoPlayer!!.getCurrentWindowIndex()
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

    private fun hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                // Hide the nav bar and status bar
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
        supportActionBar!!.hide()
    }

    // Shows the system bars by removing all the flags
    // except for the ones that make the content appear under the system bars.
    private fun showSystemUI() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        supportActionBar!!.show()
    }

    private fun setToolbarTitle(): String {
        val segments = mediaPath!!.split("/")
        return segments[segments.size - 1]
    }

    override fun filePickerSuccessClickListener(absolutePath: String) {
        this.srtPath = absolutePath
        if (videoPlayer != null) {
            playbackPosition = videoPlayer!!.getCurrentPosition()
            currentWindow = videoPlayer!!.getCurrentWindowIndex()
        }
        releasePlayer()
        initializePlayer()
    }

}