package com.project100pi.pivideoplayer.ui.adapters

import android.content.Context
import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.project100pi.pivideoplayer.ui.activity.VideoListActivity
import com.project100pi.pivideoplayer.ui.adapters.viewholder.VideoFilesViewHolder
import com.project100pi.pivideoplayer.listeners.OnClickListener
import com.project100pi.pivideoplayer.model.FileInfo

class VideoFilesAdapter(
    private val context: Context,
    var view: Int,
    private var listener: OnClickListener
): ListAdapter<FileInfo, VideoFilesViewHolder>(PlayerDiffUtil()) {

    private var selectedItems = SparseBooleanArray()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
            = VideoFilesViewHolder(context,
        LayoutInflater.from(parent.context)
            .inflate(view, parent, false), listener, this)

    override fun onBindViewHolder(holder: VideoFilesViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    class PlayerDiffUtil: DiffUtil.ItemCallback<FileInfo>() {
        override fun areItemsTheSame(oldItem: FileInfo, newItem: FileInfo): Boolean {
            return oldItem._Id == newItem._Id
        }

        override fun areContentsTheSame(oldItem: FileInfo, newItem: FileInfo): Boolean {
            return oldItem == newItem
        }

    }

    fun isSelected(position: Int): Boolean {
        return getSelectedItems().contains(position)
    }

    fun toggleSelection(position: Int) {

        if (selectedItems.get(position, false)) {
            selectedItems.delete(position)
        } else {
            selectedItems.put(position, true)
        }
        notifyItemChanged(position)
    }

    fun clearSelection() {
        val selection = getSelectedItems()
        selectedItems.clear()
        for (i in selection) {
            notifyItemChanged(i)
        }
    }

    fun getSelectedItemCount(): Int {
        return selectedItems.size()
    }

    fun getSelectedItems(): List<Int> {
        val items = ArrayList<Int>(selectedItems.size())
        for (i in 0 until selectedItems.size()) {
            items.add(selectedItems.keyAt(i))
        }
        return items
    }

    fun selectAllItems() {
        if (VideoListActivity.mIsMultiSelectMode) {
            this.clearSelection()
            var i = 0
            val size: Int = itemCount
            while (i < size) {
                listener.onDirectorySelected(i)
                i++
            }
        }
    }
}