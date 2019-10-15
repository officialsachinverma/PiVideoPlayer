package com.project100pi.pivideoplayer.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.project100pi.pivideoplayer.listeners.OnClickListener
import com.project100pi.pivideoplayer.adapters.viewholder.SRTFileViewHolder
import com.project100pi.pivideoplayer.model.FolderInfo

class SRTFileAdapter(private val context: Context,
                     var view: Int,
                     private var listener: OnClickListener): ListAdapter<FolderInfo, SRTFileViewHolder>(PlayerDiffUtil()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
            = SRTFileViewHolder(context,
        LayoutInflater.from(parent.context)
            .inflate(view, parent, false), listener)

    override fun onBindViewHolder(holder: SRTFileViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class PlayerDiffUtil: DiffUtil.ItemCallback<FolderInfo>() {
        override fun areItemsTheSame(oldItem: FolderInfo, newItem: FolderInfo): Boolean {
            return oldItem.folderId == newItem.folderId
        }

        override fun areContentsTheSame(oldItem: FolderInfo, newItem: FolderInfo): Boolean {
            return oldItem == newItem
        }

    }
}