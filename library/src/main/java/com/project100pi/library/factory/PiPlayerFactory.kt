package com.project100pi.library.factory

import android.content.Context
import com.project100pi.library.player.PiVideoPlayer

object PiPlayerFactory {

    /**
     * Creates an PiVideoPlayer instance
     *
     * @param context Context
     * @return PiVideoPlayer
     */
    fun newPiPlayer(context: Context): PiVideoPlayer {
        return PiVideoPlayer(context)
    }

}