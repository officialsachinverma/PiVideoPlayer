package com.project100pi.library.listeners

import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player

interface PiPlayerEventListener {
    fun onPlayerTrackCompleted()
    fun onTracksChanged()
    fun onPlayerError(error: ExoPlaybackException?)
}