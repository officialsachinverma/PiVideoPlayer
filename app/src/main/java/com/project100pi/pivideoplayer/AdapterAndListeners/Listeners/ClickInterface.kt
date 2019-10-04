package com.project100pi.pivideoplayer.AdapterAndListeners.Listeners

interface ClickInterface {
    fun onItemClicked(position: Int)
    fun onBackFolderPressed()
    fun onItemLongClicked(position: Int): Boolean
}