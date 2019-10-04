package com.project100pi.pivideoplayer.AdapterAndListeners

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.project100pi.pivideoplayer.AdapterAndListeners.Listeners.OnTrackSelected
import com.project100pi.pivideoplayer.AdapterAndListeners.ViewHolder.StorageFileViewHolder
import com.project100pi.pivideoplayer.Model.FolderInfo

class StorageFileAdapter(val context: Context,
                         var view: Int,
                         var listener: OnTrackSelected): ListAdapter<FolderInfo, StorageFileViewHolder>(PlayerDiffUtil()) {

    private var rowIndex: Int = -1


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
            = StorageFileViewHolder(context,
        LayoutInflater.from(parent.context)
            .inflate(view, parent, false))

    override fun onBindViewHolder(holder: StorageFileViewHolder, position: Int) {
        holder.bind(getItem(position))
        holder.clItemRow.setOnClickListener {
            listener.onDirectorySelected(position)
            rowIndex = position
        }
    }

    class PlayerDiffUtil: DiffUtil.ItemCallback<FolderInfo>() {
        override fun areItemsTheSame(oldItem: FolderInfo, newItem: FolderInfo): Boolean {
            return oldItem.folderId == newItem.folderId
        }

        override fun areContentsTheSame(oldItem: FolderInfo, newItem: FolderInfo): Boolean {
            return oldItem.equals(newItem)
        }

    }

}