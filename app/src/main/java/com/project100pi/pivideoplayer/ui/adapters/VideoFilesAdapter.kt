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
import com.project100pi.pivideoplayer.model.VideoTrackInfo

class VideoFilesAdapter(
    private val context: Context,
    var view: Int,
    private var listener: OnClickListener): ListAdapter<VideoTrackInfo, VideoFilesViewHolder>(PlayerDiffUtil()) {

    private var selectedItems = SparseBooleanArray()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
            = VideoFilesViewHolder(
                context,
                LayoutInflater.from(parent.context).inflate(view, parent, false),
                listener,
        this)

    override fun onBindViewHolder(holder: VideoFilesViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    class PlayerDiffUtil: DiffUtil.ItemCallback<VideoTrackInfo>() {

        override fun areItemsTheSame(oldItem: VideoTrackInfo, newItem: VideoTrackInfo): Boolean {
            return oldItem._Id == newItem._Id
        }

        override fun areContentsTheSame(oldItem: VideoTrackInfo, newItem: VideoTrackInfo): Boolean {
            return oldItem == newItem
        }

    }

    /**
     * Tells if item on position is selected or not
     *
     * @param position Int Position of item
     * @return Boolean true if selected, otherwise false
     */
    fun isSelected(position: Int): Boolean {
        return getSelectedItems().contains(position)
    }

    /**
     * Toggles Selection
     *
     * @param position Int Position of item
     */
    fun toggleSelection(position: Int) {

        if (selectedItems.get(position, false)) {
            selectedItems.delete(position)
        } else {
            selectedItems.put(position, true)
        }
        notifyItemChanged(position)
    }

    /**
     * Clears the selection
     */
    fun clearSelection() {
        val selection = getSelectedItems()
        selectedItems.clear()
        for (i in selection) {
            notifyItemChanged(i)
        }
    }

    /**
     * Returns the count of selected items
     *
     * @return Int
     */
    fun getSelectedItemCount(): Int {
        return selectedItems.size()
    }

    /**
     * Returns a list indices of the selected items
     *
     * @return List<Int>
     */
    fun getSelectedItems(): List<Int> {
        val items = ArrayList<Int>(selectedItems.size())
        for (i in 0 until selectedItems.size()) {
            items.add(selectedItems.keyAt(i))
        }
        return items
    }

    /**
     * Toggles all item's selection true
     */
    fun selectAllItems() {
        if (VideoListActivity.mIsMultiSelectMode) {
            this.clearSelection()
            var i = 0
            val size: Int = itemCount
            while (i < size) {
                listener.onItemSelected(i)
                i++
            }
        }
    }
}