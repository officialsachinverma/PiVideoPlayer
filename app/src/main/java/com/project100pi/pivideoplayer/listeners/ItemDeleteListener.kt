package com.project100pi.pivideoplayer.listeners

import java.io.File

interface ItemDeleteListener {
    fun showPermissionForSdCard()
    fun onDeleteSuccess(listOfIndexes: List<Int>)
    fun onDeleteError()
}