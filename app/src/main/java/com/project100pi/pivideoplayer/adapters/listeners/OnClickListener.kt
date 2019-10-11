package com.project100pi.pivideoplayer.adapters.listeners

interface OnClickListener {
    fun onItemLongClicked(position: Int): Boolean
    fun onDirectorySelected(position: Int)
    fun onOverflowItemClick(position: Int, viewId: Int)
}