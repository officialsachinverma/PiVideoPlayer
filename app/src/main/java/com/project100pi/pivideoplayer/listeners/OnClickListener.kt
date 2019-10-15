package com.project100pi.pivideoplayer.listeners

interface OnClickListener {
    fun onItemLongClicked(position: Int): Boolean
    fun onDirectorySelected(position: Int)
    fun onOverflowItemClick(position: Int, viewId: Int)
}