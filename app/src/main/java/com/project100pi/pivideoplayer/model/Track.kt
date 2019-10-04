package com.project100pi.pivideoplayer.model

import android.graphics.Bitmap

data class Track(var audioId: Long,
                 var trackName: String,
                 var artistName: String,
                 var duration: String,
                 var thumbnail: Bitmap?,
                 var filePath: String,
                 var isPlaying: Boolean)