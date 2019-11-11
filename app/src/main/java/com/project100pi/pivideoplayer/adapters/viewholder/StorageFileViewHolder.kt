package com.project100pi.pivideoplayer.adapters.viewholder

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.project100pi.pivideoplayer.model.FolderInfo
import com.project100pi.pivideoplayer.R
import com.project100pi.pivideoplayer.activity.VideoListActivity
import com.project100pi.pivideoplayer.adapters.StorageFileAdapter
import com.project100pi.pivideoplayer.listeners.OnClickListener

class StorageFileViewHolder(private val context: Context, itemView: View, var listener: OnClickListener, private val adapter: StorageFileAdapter):
    RecyclerView.ViewHolder(itemView),
    View.OnClickListener,
    View.OnLongClickListener {

    var clItemRow: ConstraintLayout = itemView.findViewById(R.id.cl_item_row)
    private var tvTitle: TextView = itemView.findViewById(R.id.tv_directory_name)
    private var ivThumbnail: ImageView = itemView.findViewById(R.id.iv_directory)
    private var ivOverFlow: ImageView = itemView.findViewById(R.id.iv_overflow_menu)

    fun bind(file: FolderInfo, position: Int) {
        if(adapter.isSelected(position)){
            clItemRow.setBackgroundColor(context.resources.getColor(android.R.color.holo_green_dark))
        }
        else{
            clItemRow.setBackgroundColor(context.resources.getColor(android.R.color.white))
        }
        if (file.isSong) {
            ivThumbnail.background = ContextCompat.getDrawable(context, R.drawable.music_icon)
            ivOverFlow.visibility = View.VISIBLE
        }
        else {
            ivThumbnail.background = ContextCompat.getDrawable(context, R.drawable.ic_folder)
            ivOverFlow.visibility = View.GONE
        }
        tvTitle.text = file.videoName

        clItemRow.setOnClickListener(this)
        clItemRow.setOnLongClickListener(this)
        ivOverFlow.setOnClickListener(this)
    }

    override fun onClick(view: View?) {
            when (view!!.id) {
                R.id.cl_item_row -> listener.onDirectorySelected(adapterPosition)
                R.id.iv_overflow_menu -> if (!VideoListActivity.mIsMultiSelectMode) overflowItemClicked(view, adapterPosition)
            }
    }

    override fun onLongClick(p0: View?): Boolean {
        return if (listener != null) {
            listener.onItemLongClicked(adapterPosition)
        } else false
    }

    private fun overflowItemClicked(view: View, position: Int) {
        if (!VideoListActivity.mIsMultiSelectMode) {
            val popupMenu = PopupMenu(context, view)
            popupMenu.inflate(R.menu.item_overflow_menu)

            //handleContextMenuOptions(popupMenu, position)

            popupMenu.setOnMenuItemClickListener { item ->
                if (listener != null) {
                    listener.onOverflowItemClick(position, item.itemId)
                }
                true
            }
            popupMenu.show()
        }
    }

}