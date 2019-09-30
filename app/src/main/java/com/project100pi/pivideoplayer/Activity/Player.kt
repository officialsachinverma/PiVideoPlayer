package com.project100pi.pivideoplayer.Activity

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.project100pi.library.factory.PiPlayerFactory
import com.project100pi.library.misc.Util
import com.project100pi.library.player.PiVideoPlayer
import com.project100pi.library.ui.PiVideoPlayerView
import com.project100pi.pivideoplayer.R
import com.project100pi.pivideoplayer.Utils.Constants

class Player : AppCompatActivity() {

    private var path: String = ""

    private lateinit var playerView: PiVideoPlayerView
    private var videoPlayer: PiVideoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        this.path = if (this.intent != null) {
            intent.getStringExtra(Constants.FILE_PATH)
        } else {
            ""
        }

        playerView = findViewById(R.id.pv_player)
//        playerView.setErrorMessageProvider(PlayerErrorMessageProvider())
        playerView.requestFocus()
        playerView.showController(true)

    }

    public override fun onStart() {
        super.onStart()
        if (Util.SDK_INT > 23) {
            playerView.onResume()
        }
    }

    public override fun onResume() {
        super.onResume()
        if (Util.SDK_INT <= 23 || null == videoPlayer) {
            initializePlayer()
            playerView.onResume()
        }
    }

    public override fun onPause() {
        super.onPause()
        if (Util.SDK_INT <= 23) {
            playerView.onPause()
            releasePlayer()
        }
    }

    public override fun onStop() {
        super.onStop()
        if (Util.SDK_INT > 23) {
            playerView.onPause()
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
            playerView?.setPlayer(videoPlayer)
        }
        videoPlayer?.prepare(path)
        videoPlayer?.play()
    }

    private fun releasePlayer() {
        if (videoPlayer != null) {
            videoPlayer?.release()
            videoPlayer = null
        }
    }

}