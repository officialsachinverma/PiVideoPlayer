package com.project100pi.pivideoplayer.ui.activity.viewmodel.factory

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.project100pi.pivideoplayer.listeners.ItemDeleteListener
import com.project100pi.pivideoplayer.ui.activity.viewmodel.VideoListViewModel

class VideoListViewModelFactory(private val context: Context,
                                private val folderPath: String,
                                private val folderName: String,
                                private val itemDeleteListener: ItemDeleteListener) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {

        if (modelClass.isAssignableFrom(VideoListViewModel::class.java)) {

            return VideoListViewModel(context, folderPath, folderName, itemDeleteListener) as T

        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

}