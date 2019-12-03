package com.project100pi.library.adapter.viewholder

import android.content.Context
import android.net.Uri
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.project100pi.library.R
import com.project100pi.library.adapter.CurrentPlayingQueueAdapter
import com.project100pi.library.dialogs.listeners.OnItemClickListener
import com.project100pi.library.model.VideoMetaData
import java.io.File

class CurrentPlayingQueueViewHolder(private val context: Context,
                                    itemView: View,
                                    private val listener: OnItemClickListener,
                                    private val adapter: CurrentPlayingQueueAdapter): RecyclerView.ViewHolder(itemView), View.OnClickListener,
    ItemTouchHelperViewHolder {

    private val clItemRow: ConstraintLayout = itemView.findViewById(R.id.cl_current_playing)
    val ivRearrange: ImageView = itemView.findViewById(R.id.rearrange_image_view)
    private var ivThumbNail: ImageView = itemView.findViewById(R.id.thumbnail_image_view)
    private var tvVideoName: TextView = itemView.findViewById(R.id.tv_video_name)

    fun bind(videoMetaData: VideoMetaData, nowPlaying: VideoMetaData) {
        if (videoMetaData._id == nowPlaying._id) {
            clItemRow.background = ContextCompat.getDrawable(context, R.drawable.soundbars_blue_static)
        } else {
            clItemRow.background = ContextCompat.getDrawable(context, R.color.blackTransparent)
        }

        Glide
            .with(context)
            .asBitmap()
            .load(Uri.fromFile(File(videoMetaData.path)))
            .thumbnail(0.1f)
            .into(ivThumbNail)

        tvVideoName.text = videoMetaData.title

        clItemRow.setOnClickListener(this)
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.cl_current_playing -> {
                adapter.nowPlayingViewHolder?.clItemRow?.background = ContextCompat.getDrawable(context, R.color.blackTransparent)
                clItemRow.background = ContextCompat.getDrawable(context, R.drawable.soundbars_blue_static)
                listener.onItemClicked(adapterPosition)
                adapter.nowPlayingViewHolder = this
            }
        }
    }

    override fun onItemSelected() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onItemClear() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}