package com.project100pi.library.misc

object CurrentSettings {

    object MediaButtonController {
        //The following two variables are used for detecting double click in headset buttons.
        var lastPressTime: Long = 0
        var mHasDoubleClicked = false
        var mHasTripleClicked = false
    }

    object MediaSession {
        var avaiable = false
    }

    object Playback {
        var playing = false
        var pause = false
        var stopped = false
    }

    object Video {
        var mode = 0
    }

}