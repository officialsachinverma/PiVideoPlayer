package com.project100pi.library.dialogs.listeners

interface OnItemClickListener {
    fun onItemClicked(position: Int)
    fun onItemMoved(fromPosition: Int, toPosition: Int)
}