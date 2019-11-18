package com.project100pi.pivideoplayer.factory

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.project100pi.pivideoplayer.activity.VideoListViewModel
import com.project100pi.pivideoplayer.model.FolderInfo

class VideoListViewModelFactory(private val context: Context,
                                private val videoListData: ArrayList<FolderInfo>,
                                private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VideoListViewModel::class.java)) {
            return VideoListViewModel(context, videoListData, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

}