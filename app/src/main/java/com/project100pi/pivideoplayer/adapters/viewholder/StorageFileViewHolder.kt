package com.project100pi.pivideoplayer.adapters.viewholder

import android.content.Context
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.project100pi.pivideoplayer.model.FolderInfo
import com.project100pi.pivideoplayer.R
import com.project100pi.pivideoplayer.adapters.listeners.OnClickListener

class StorageFileViewHolder(private val context: Context, itemView: View, var listener: OnClickListener):
    RecyclerView.ViewHolder(itemView),
    View.OnClickListener,
    View.OnLongClickListener {

    var clItemRow: ConstraintLayout = itemView.findViewById(R.id.cl_item_row)
    private var tvTitle: TextView = itemView.findViewById(R.id.tv_directory_name)
    private var ivThumbnail: ImageView = itemView.findViewById(R.id.iv_directory)
    private var ivOverFlow: ImageView = itemView.findViewById(R.id.iv_overflow_menu)

    private var mIsLongClickOn: Boolean = false

    fun bind(file: FolderInfo) {
        if (file.isSong) {
            ivThumbnail.background = ContextCompat.getDrawable(context, R.drawable.music_icon)
            ivOverFlow.visibility = View.VISIBLE
        }
        else {
            ivThumbnail.background = ContextCompat.getDrawable(context, R.drawable.ic_folder)
            ivOverFlow.visibility = View.GONE
        }
        tvTitle.text = file.songName

        clItemRow.setOnClickListener(this)
        clItemRow.setOnLongClickListener(this)
        ivOverFlow.setOnClickListener(this)
    }

    override fun onClick(view: View?) {
        when (view!!.id) {
            R.id.cl_item_row -> listener.onDirectorySelected(adapterPosition)
            R.id.iv_overflow_menu -> overflowItemClicked(view, adapterPosition)
        }
    }

    override fun onLongClick(p0: View?): Boolean {
        mIsLongClickOn = true
        return if (listener != null) {
            listener.onItemLongClicked(adapterPosition)
        } else false
    }

    private fun overflowItemClicked(view: View, position: Int) {
        if (!mIsLongClickOn) {
            val popupMenu = PopupMenu(context, view)
            popupMenu.inflate(R.menu.multi_choice_option)

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