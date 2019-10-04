package com.project100pi.pivideoplayer.AdapterAndListeners.Listeners

import com.project100pi.pivideoplayer.Model.Track

interface OnTrackSelected {
    fun onClick(track: Track)

    fun onDirectorySelected(position: Int)
}