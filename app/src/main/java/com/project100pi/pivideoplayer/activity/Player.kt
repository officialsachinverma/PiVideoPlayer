package com.project100pi.pivideoplayer.activity

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import butterknife.BindView
import butterknife.ButterKnife
import com.project100pi.library.factory.PiPlayerFactory
import com.project100pi.library.misc.Util
import com.project100pi.library.player.PiVideoPlayer
import com.project100pi.library.ui.PiVideoPlayerView
import com.project100pi.pivideoplayer.R
import com.project100pi.pivideoplayer.utils.Constants

class Player : AppCompatActivity() {

    private var path: String? = null
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
                this.path = this.intent.extras!!.getString(Constants.FILE_PATH)
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
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        finish()
    }

    public override fun onStart() {
        super.onStart()
        if (Util.SDK_INT > 23) {
            //playerView.onResume()
        }
    }

    public override fun onResume() {
        super.onResume()
        if (Util.SDK_INT <= 23 || null == videoPlayer) {
            initializePlayer()
            //playerView.onResume()
        }
    }

    public override fun onPause() {
        super.onPause()
        if (Util.SDK_INT <= 23) {
            //playerView.onPause()
            releasePlayer()
        }
    }

    public override fun onStop() {
        super.onStop()
        if (Util.SDK_INT > 23) {
            //playerView.onPause()
            releasePlayer()
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
        if (videoList != null)
            videoPlayer?.prepare(videoList!!, resetPosition = true, resetState = false)
        else
            videoPlayer?.prepare(path!!)
        videoPlayer?.play()
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
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        playbackPosition = savedInstanceState.getLong("playbackPosition")
        currentWindow = savedInstanceState.getInt("currentWindow")
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
        val segments = path!!.split("/")
        return segments[segments.size - 1]
    }

}