package com.project100pi.pivideoplayer.AdapterAndListeners.ViewHolder

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.project100pi.pivideoplayer.Model.FolderInfo
import com.project100pi.pivideoplayer.R

class StorageFileViewHolder(val context: Context, itemView: View): RecyclerView.ViewHolder(itemView) {

    var clItemRow: ConstraintLayout = itemView.findViewById(R.id.cl_item_row)
    private var tvTitle: TextView = itemView.findViewById(R.id.tv_directory_name)
    private var ivThumbnail: ImageView = itemView.findViewById(R.id.iv_directory)

    fun bind(file: FolderInfo) {
        if (file.isSong)
            ivThumbnail.background = ContextCompat.getDrawable(context, R.drawable.music_icon)
        else
            ivThumbnail.background = ContextCompat.getDrawable(context, R.drawable.ic_folder)
        tvTitle.text = file.songName
    }

}