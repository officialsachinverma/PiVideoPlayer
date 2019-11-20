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
import com.project100pi.pivideoplayer.model.FolderInfo
import com.project100pi.pivideoplayer.R
import com.project100pi.pivideoplayer.activity.DirectoryListActivity
import com.project100pi.pivideoplayer.adapters.StorageFileAdapter
import com.project100pi.pivideoplayer.listeners.OnClickListener
import java.io.File

class StorageFileViewHolder(private val context: Context, itemView: View, private val listener: OnClickListener, private val adapter: StorageFileAdapter):
    RecyclerView.ViewHolder(itemView),
    View.OnClickListener,
    View.OnLongClickListener {

    var clItemRow: ConstraintLayout = itemView.findViewById(R.id.cl_item_row)
    private var tvTitle: TextView = itemView.findViewById(R.id.tv_directory_name)
    private var tvItemCount: TextView = itemView.findViewById(R.id.tv_directory_item_count)
    private var tvDuration: TextView = itemView.findViewById(R.id.tv_duration)
    private var ivThumbnail: ImageView = itemView.findViewById(R.id.iv_directory)
    private var ivOverFlow: ImageView = itemView.findViewById(R.id.iv_overflow_menu)

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
                .load(R.drawable.ic_folder)
                .thumbnail(0.1f)
                .into(ivThumbnail)
//        ivThumbnail.setImageResource(R.drawable.ic_folder)
        ivOverFlow.visibility = View.GONE
        tvItemCount.visibility = View.VISIBLE
        tvDuration.visibility = View.GONE
        tvItemCount.text = "${adapter.getInternalItem(position).songsList.size} Videos"

        tvTitle.text = file.videoName

        clItemRow.setOnClickListener(this)
        clItemRow.setOnLongClickListener(this)
        ivOverFlow.setOnClickListener(this)
    }

    override fun onClick(view: View?) {
            when (view!!.id) {
                R.id.cl_item_row -> listener.onDirectorySelected(adapterPosition)
                R.id.iv_overflow_menu -> if (!DirectoryListActivity.mIsMultiSelectMode) overflowItemClicked(view, adapterPosition)
            }
    }

    override fun onLongClick(p0: View?) = listener.onItemLongClicked(adapterPosition)

    private fun overflowItemClicked(view: View, position: Int) {
        if (!DirectoryListActivity.mIsMultiSelectMode) {
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