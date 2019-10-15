package com.project100pi.pivideoplayer.listeners

interface ItemDeleteListener {
    fun onDeleteSuccess(listOfIndexes: List<Int>)
    fun onDeleteError()
}