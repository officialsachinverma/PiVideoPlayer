package com.project100pi.pivideoplayer.AdapterAndListeners

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.project100pi.pivideoplayer.AdapterAndListeners.Listeners.OnTrackSelected
import com.project100pi.pivideoplayer.AdapterAndListeners.ViewHolder.PlayerViewHolder
import com.project100pi.pivideoplayer.Model.Track

class PlayerAdapter(var context: Context?,
                    var view: Int,
                    var listener: OnTrackSelected,
                    val trackList: MutableList<Track>): ListAdapter<Track, PlayerViewHolder>(PlayerDiffUtil()) {

    private var rowIndex: Int = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
            = PlayerViewHolder(
        LayoutInflater.from(parent.context)
            .inflate(view, parent, false))

    override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
        holder.bind(getItem(position))
        holder.clItemRow.setOnClickListener {
            getItem(position).isPlaying = true
            listener.onClick(getItem(position))
            rowIndex = position
            notifyDataSetChanged()
        }
        if(rowIndex==position){
            holder.clItemRow.setBackgroundColor(Color.parseColor("#567845"));
        }
        else
        {
            holder.clItemRow.setBackgroundColor(Color.parseColor("#ffffff"));
        }
    }

    /*override fun getItemCount(): Int {
        return itemCount
    }*/

    class PlayerDiffUtil: DiffUtil.ItemCallback<Track>() {
        override fun areItemsTheSame(oldItem: Track, newItem: Track): Boolean {
            return oldItem.audioId == newItem.audioId
        }

        override fun areContentsTheSame(oldItem: Track, newItem: Track): Boolean {
            return oldItem == newItem
        }

    }

}