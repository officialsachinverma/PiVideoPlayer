package com.project100pi.pivideoplayer.ui.activity.viewmodel.factory

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.project100pi.pivideoplayer.ui.activity.viewmodel.DirectoryListViewModel

class DirectoryListViewModelFactory(private val context: Context,
                                    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DirectoryListViewModel::class.java)) {
            return DirectoryListViewModel(
                context,
                application
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

}