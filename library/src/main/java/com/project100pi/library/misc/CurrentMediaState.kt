package com.project100pi.library.misc

object CurrentMediaState {

    object MediaButtonController {
        var lastPressTime: Long = 0
        var mHasDoubleClicked = false
        var mHasTripleClicked = false
    }

    object MediaSession {
        var avaiable = false
    }

    object Playback {
        var playing = false
        const val DEFAULT_REWIND_TIME = 3000 // 3 secs
        const val DEFAULT_FAST_FORWARD_TIME = 3000 // 3 secs
    }

    object Video {
        var mode = 0
        var orientation = "landscape"
    }

}