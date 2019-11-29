package com.project100pi.pivideoplayer.ui.activity.viewmodel.factory

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.project100pi.pivideoplayer.listeners.ItemDeleteListener
import com.project100pi.pivideoplayer.ui.activity.viewmodel.DirectoryListViewModel

class DirectoryListViewModelFactory(private val context: Context,
                                    private val itemDeleteListener: ItemDeleteListener
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {

        if (modelClass.isAssignableFrom(DirectoryListViewModel::class.java)) {

            return DirectoryListViewModel(context, itemDeleteListener) as T

        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

}