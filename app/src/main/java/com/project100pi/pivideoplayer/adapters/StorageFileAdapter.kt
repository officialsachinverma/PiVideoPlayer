package com.project100pi.pivideoplayer.adapters

import android.content.Context
import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.project100pi.pivideoplayer.adapters.listeners.OnClickListener
import com.project100pi.pivideoplayer.adapters.viewholder.StorageFileViewHolder
import com.project100pi.pivideoplayer.model.FolderInfo

class StorageFileAdapter(
    private val context: Context,
    var view: Int,
    var listener: OnClickListener): ListAdapter<FolderInfo, StorageFileViewHolder>(PlayerDiffUtil()) {

    private var selectedItems = SparseBooleanArray()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
            = StorageFileViewHolder(context,
        LayoutInflater.from(parent.context)
            .inflate(view, parent, false), listener)

    override fun onBindViewHolder(holder: StorageFileViewHolder, position: Int) {
        holder.bind(getItem(position))
        holder.clItemRow.setOnClickListener {
            listener.onDirectorySelected(position)
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
            notifyItemChanged(i!!)
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

}