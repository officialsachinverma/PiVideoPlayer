package com.project100pi.pivideoplayer.ui.adapters.viewholder

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.project100pi.pivideoplayer.model.FolderInfo
import com.project100pi.pivideoplayer.R
import com.project100pi.pivideoplayer.ui.activity.DirectoryListActivity
import com.project100pi.pivideoplayer.ui.adapters.FoldersAdapter
import com.project100pi.pivideoplayer.listeners.OnClickListener

class FoldersViewHolder(private val context: Context, itemView: View, private val listener: OnClickListener, private val adapter: FoldersAdapter):
    RecyclerView.ViewHolder(itemView),
    View.OnClickListener,
    View.OnLongClickListener {

    private var clItemRow: ConstraintLayout = itemView.findViewById(R.id.cl_item_row)
    private var tvTitle: TextView = itemView.findViewById(R.id.tv_directory_name)
    private var tvItemCount: TextView = itemView.findViewById(R.id.tv_directory_item_count)
    //private var tvDuration: TextView = itemView.findViewById(R.id.tv_duration)
    private var ivThumbnail: ImageView = itemView.findViewById(R.id.iv_directory)
    private var ivOverFlow: ImageView = itemView.findViewById(R.id.iv_overflow_menu)

    /**
     * Sets data on views
     *
     * @param folder FolderInfo contains information of folder
     * @param position Int Position of the item
     */
    fun bind(folder: FolderInfo, position: Int) {

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
            .load(R.drawable.ic_folder)
            .thumbnail(0.1f)
            .into(ivThumbnail)

        ivOverFlow.visibility = View.GONE
        tvItemCount.visibility = View.VISIBLE
        //tvDuration.visibility = View.GONE
        tvItemCount.text = "${adapter.getInternalItem(position).videoInfoList.size} Videos"

        tvTitle.text = folder.folderName

        // Setting Listeners

        clItemRow.setOnClickListener(this)
        clItemRow.setOnLongClickListener(this)
        ivOverFlow.setOnClickListener(this)
    }

    override fun onClick(view: View?) {
            when (view!!.id) {
                R.id.cl_item_row -> listener.onItemSelected(adapterPosition)
                R.id.iv_overflow_menu -> if (!DirectoryListActivity.mIsMultiSelectMode) overflowItemClicked(view, adapterPosition)
            }
    }

    override fun onLongClick(p0: View?) = listener.onItemLongClicked(adapterPosition)

    private fun overflowItemClicked(view: View, position: Int) {
        if (!DirectoryListActivity.mIsMultiSelectMode) {

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