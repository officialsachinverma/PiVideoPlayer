package com.project100pi.pivideoplayer.utils

object Constants {

    object Playback {
        const val WINDOW = "Window"
        const val PLAYBACK_POSITION = "playbackPosition"
        const val CURRENT_POSITION = "currentWindow"
        const val VIDEO_LIST = "videoList"
        const val PLAYBACK_QUEUE = "playBackQueue"
    }

    object Permission {
        const val WRITE_PERMISSION_REQUEST_CODE = 100
        const val SD_CARD_WRITE_PERMISSION_REQUEST_CODE = 200
    }

    object TinyDB {
        const val SHARED_PREFERENCES = "SharedPreferences"
    }

    object ExternalSDCard {
        const val SD_CARD_URI = "sdCardUri"
    }

    object Storage {
        const val DIRECTORY_NAME = "directoryName"
        const val DIRECTORY_PATH = "directoryPath"
    }

    object SearchSource {
        const val SEARCH_LOCAL = "searchLocal"
    }

    object Orientation {
        const val PORTRAIT = "portrait"
        const val LANDSCAPE = "landscape"
    }

    object Delete {
        const val SUCCESS = 0
        const val FAILURE = 1
        const val SD_CARD_PERMISSION_REQUIRED = 2
    }
}