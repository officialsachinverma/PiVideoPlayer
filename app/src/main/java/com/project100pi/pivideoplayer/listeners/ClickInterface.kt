package com.project100pi.pivideoplayer.listeners

interface ClickInterface {
    fun onItemClicked(position: Int)
    fun onBackFolderPressed()
    fun onItemLongClicked(position: Int): Boolean
}