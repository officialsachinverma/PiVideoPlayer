package com.project100pi.library.factory

import android.content.Context
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.LoadControl
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.drm.DrmSessionManager
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelector
import com.project100pi.library.player.PiVideoPlayer

object PiPlayerFactory {

    fun newPiPlayer(context: Context): PiVideoPlayer {
        return PiVideoPlayer(context)
    }

}