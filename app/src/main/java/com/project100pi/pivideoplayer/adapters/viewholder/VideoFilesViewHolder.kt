package com.project100pi.pivideoplayer.adapters.viewholder

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
import com.project100pi.pivideoplayer.activity.VideoListActivity
import com.project100pi.pivideoplayer.adapters.VideoFilesAdapter
import com.project100pi.pivideoplayer.listeners.OnClickListener
import com.project100pi.pivideoplayer.model.FolderInfo
import com.project100pi.pivideoplayer.utils.UtilFunctions
import java.io.File

class VideoFilesViewHolder(private val context: Context, itemView: View, private val listener: OnClickListener, private val adapter: VideoFilesAdapter):
    RecyclerView.ViewHolder(itemView),
    View.OnClickListener,
    View.OnLongClickListener {

    private var clItemRow: ConstraintLayout = itemView.findViewById(R.id.cl_video_item_row)
    private var tvTitle: TextView = itemView.findViewById(R.id.tv_video_name)
    private var tvDuration: TextView = itemView.findViewById(R.id.tv_video_duration)
    private var ivThumbnail: ImageView = itemView.findViewById(R.id.iv_video_thumnail)
    private var ivOverFlow: ImageView = itemView.findViewById(R.id.iv_video_overflow_menu)

    fun bind(file: FolderInfo, position: Int) {
        if(adapter.isSelected(position)){
            clItemRow.setBackgroundColor(context.resources.getColor(android.R.color.holo_green_dark))
        }
        else{
            clItemRow.setBackgroundColor(context.resources.getColor(android.R.color.white))
        }
        Glide
            .with(context)
            .asBitmap()
            .load(Uri.fromFile(File(file.path)))
            .thumbnail(0.1f)
            .into(ivThumbnail)
        ivOverFlow.visibility = View.VISIBLE
        tvDuration.visibility = View.VISIBLE
        tvDuration.text = UtilFunctions.convertSecondsToHMmSs(file.duration)
        tvTitle.text = file.videoName

        clItemRow.setOnClickListener(this)
        clItemRow.setOnLongClickListener(this)
        ivOverFlow.setOnClickListener(this)
    }

    override fun onClick(view: View?) {
        when (view!!.id) {
            R.id.cl_video_item_row -> listener.onDirectorySelected(adapterPosition)
            R.id.iv_video_overflow_menu -> if (!VideoListActivity.mIsMultiSelectMode) overflowItemClicked(view, adapterPosition)
        }
    }

    override fun onLongClick(p0: View?) = listener.onItemLongClicked(adapterPosition)

    private fun overflowItemClicked(view: View, position: Int) {
        if (!VideoListActivity.mIsMultiSelectMode) {
            val popupMenu = PopupMenu(context, view)
            popupMenu.inflate(R.menu.item_overflow_menu)

            //handleContextMenuOptions(popupMenu, position)

            popupMenu.setOnMenuItemClickListener { item ->
                listener.onOverflowItemClick(position, item.itemId)
                true
            }
            popupMenu.show()
        }
    }

}