package com.project100pi.pivideoplayer.factory

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.project100pi.pivideoplayer.activity.VideoListViewModel

class VideoListViewModelFactory(private val context: Context?,
                                private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VideoListViewModel::class.java)) {
            return VideoListViewModel(context, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

}