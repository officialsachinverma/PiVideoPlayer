package com.project100pi.pivideoplayer.ui.adapters.viewholder

import android.content.Context
import android.net.Uri
import android.view.View
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.project100pi.pivideoplayer.R
import com.project100pi.pivideoplayer.listeners.OnClickListener
import com.project100pi.pivideoplayer.model.VideoTrackInfo
import com.project100pi.pivideoplayer.ui.activity.SearchActivity
import com.project100pi.pivideoplayer.ui.adapters.SearchResultAdapter
import com.project100pi.pivideoplayer.utils.UtilFunctions
import java.io.File

/**
 * Created by Sachin Verma on 2019-11-28.
 */

class SearchResultViewHolder(private val context: Context, itemView: View, private val listener: OnClickListener, private val adapter: SearchResultAdapter):
    RecyclerView.ViewHolder(itemView),
    View.OnClickListener,
    View.OnLongClickListener {

    private var clItemRow: ConstraintLayout = itemView.findViewById(R.id.cl_video_item_row)
    private var tvTitle: TextView = itemView.findViewById(R.id.tv_video_name)
    private var tvDuration: TextView = itemView.findViewById(R.id.tv_video_duration)
    private var ivThumbnail: ImageView = itemView.findViewById(R.id.iv_video_thumnail)
    private var ivOverFlow: ImageView = itemView.findViewById(R.id.iv_video_overflow_menu)
    private var tvDateAdded: TextView = itemView.findViewById(R.id.tv_date_added)

    /**
     * Sets data on views
     *
     * @param videoTrack VideoTrackInfo contains information of video
     * @param position Int Position of the item
     */
    fun bind(videoTrack: VideoTrackInfo, position: Int) {

        if(adapter.isSelected(position)){
            clItemRow.setBackgroundColor(context.resources.getColor(android.R.color.holo_green_dark))
        }
        else{
            clItemRow.setBackgroundColor(context.resources.getColor(android.R.color.white))
        }

        Glide
            .with(context)
            .asBitmap()
            .centerCrop()
            .load(Uri.fromFile(File(videoTrack.videoPath)))
            .thumbnail(0.1f)
            .into(ivThumbnail)

        ivOverFlow.visibility = View.VISIBLE
        tvDuration.visibility = View.VISIBLE
        tvDuration.text = UtilFunctions.convertSecondsToHMmSs(videoTrack.durationInMs)
        tvTitle.text = videoTrack.videoName
        tvDateAdded.text = videoTrack.dateAdded

        // Setting Listeners

        clItemRow.setOnClickListener(this)
        clItemRow.setOnLongClickListener(this)
        ivOverFlow.setOnClickListener(this)
    }

    override fun onClick(view: View?) {
        when (view!!.id) {
            R.id.cl_video_item_row -> listener.onItemSelected(adapterPosition)
            R.id.iv_video_overflow_menu -> if (!SearchActivity.mIsMultiSelectMode) overflowItemClicked(view, adapterPosition)
        }
    }

    override fun onLongClick(p0: View?) = listener.onItemLongClicked(adapterPosition)

    private fun overflowItemClicked(view: View, position: Int) {
        if (!SearchActivity.mIsMultiSelectMode) {

            val popupMenu = PopupMenu(context, view)

            popupMenu.inflate(R.menu.item_overflow_menu)

            popupMenu.setOnMenuItemClickListener { item ->
                listener.onOverflowItemClick(position, item.itemId)
                true
            }

            popupMenu.show()
        }
    }

}