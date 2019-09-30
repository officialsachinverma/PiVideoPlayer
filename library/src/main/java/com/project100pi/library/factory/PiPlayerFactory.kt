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

    /**
     * Creates a [SimpleExoPlayer] instance.
     *
     * @param context A [Context].
     * @param trackSelector The [TrackSelector] that will be used by the instance.
     * @param loadControl The [LoadControl] that will be used by the instance.
     * @param drmSessionManager An optional [DrmSessionManager]. May be null if the instance
     * will not be used for DRM protected playbacks.
     * @param extensionRendererMode The extension renderer mode, which determines if and how available
     * extension renderers are used. Note that extensions must be included in the application
     * build for them to be considered available.
     */

    fun newPiPlayer(context: Context): PiVideoPlayer {
        return PiVideoPlayer(context)
    }

}