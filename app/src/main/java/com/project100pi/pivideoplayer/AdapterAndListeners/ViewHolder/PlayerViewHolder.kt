package com.project100pi.pivideoplayer.AdapterAndListeners.ViewHolder

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.project100pi.pivideoplayer.Model.Track
import com.project100pi.pivideoplayer.R

class PlayerViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {

    var clItemRow: ConstraintLayout = itemView.findViewById(R.id.cl_item_row)
    private var ivThumbnail: ImageView = itemView.findViewById(R.id.tv_thumbnail)
    private var tvTitle: TextView = itemView.findViewById(R.id.tv_track_name)
    private var tvArtist: TextView = itemView.findViewById(R.id.tv_artist_name)
    private var tvDuration: TextView = itemView.findViewById(R.id.tv_duration)

    fun bind(track: Track) {
        if (track.thumbnail != null)
            ivThumbnail.setImageBitmap(track.thumbnail)
        tvTitle.text = track.trackName
        tvArtist.text = track.artistName
        tvDuration.text = track.duration
    }

}