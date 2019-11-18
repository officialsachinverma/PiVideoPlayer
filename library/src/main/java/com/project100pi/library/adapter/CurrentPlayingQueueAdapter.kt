package com.project100pi.library.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.project100pi.library.adapter.viewholder.CurrentPlayingQueueViewHolder
import com.project100pi.library.dialogs.listeners.OnItemClickListener
import com.project100pi.library.model.VideoMetaData
import com.project100pi.library.listeners.OnStartDragListener
import android.view.MotionEvent
import androidx.core.view.MotionEventCompat
import androidx.recyclerview.widget.RecyclerView
import java.util.*
import kotlin.collections.ArrayList


class CurrentPlayingQueueAdapter(
    private val context: Context,
    var view: Int,
    private val currentPlayingList: ArrayList<VideoMetaData>,
    private val nowPlaying: VideoMetaData,
    var listener: OnItemClickListener
): RecyclerView.Adapter<CurrentPlayingQueueViewHolder>(), ItemTouchHelperAdapter {

    var nowPlayingViewHolder: CurrentPlayingQueueViewHolder? = null
    private var mDragStartListener: OnStartDragListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
            = CurrentPlayingQueueViewHolder(context,
        LayoutInflater.from(parent.context)
            .inflate(view, parent, false), listener, this)

    override fun onBindViewHolder(holder: CurrentPlayingQueueViewHolder, position: Int) {
        if (currentPlayingList[position]._id == nowPlaying._id)
            nowPlayingViewHolder = holder
        holder.bind(currentPlayingList[position], nowPlaying)
        holder.ivRearrange.setOnTouchListener { _, event ->
            if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
                mDragStartListener?.onStartDrag(holder)
            }
            false
        }
    }

    override fun getItemCount() = currentPlayingList.size

    //override fun onItemClicked(position: Int) = listener.onItemClicked(position)

    override fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        Collections.swap(currentPlayingList, fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
        listener.onItemMoved(fromPosition = fromPosition, toPosition = toPosition)
        return true
    }

    override fun onItemDismiss(position: Int) {
        currentPlayingList.removeAt(position)
        notifyItemRemoved(position)
    }

    class PlayerDiffUtil: DiffUtil.ItemCallback<VideoMetaData>() {
        override fun areItemsTheSame(oldItem: VideoMetaData, newItem: VideoMetaData): Boolean {
            return oldItem._id == newItem._id
        }

        override fun areContentsTheSame(oldItem: VideoMetaData, newItem: VideoMetaData): Boolean {
            return oldItem == newItem
        }

    }

}