package com.project100pi.pivideoplayer.adapters.viewholder

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.project100pi.pivideoplayer.R
import com.project100pi.pivideoplayer.listeners.OnClickListener
import com.project100pi.pivideoplayer.model.FolderInfo

class SRTFileViewHolder(private val context: Context, itemView: View, var listener: OnClickListener):
    RecyclerView.ViewHolder(itemView),
    View.OnClickListener{

    var clItemRow: ConstraintLayout = itemView.findViewById(R.id.cl_item_row)
    private var tvTitle: TextView = itemView.findViewById(R.id.tv_directory_name)
    private var ivThumbnail: ImageView = itemView.findViewById(R.id.iv_directory)

    fun bind(file: FolderInfo) {
        if (file.isSong) {
            ivThumbnail.background = ContextCompat.getDrawable(context, R.drawable.ic_file)
        }
        else {
            ivThumbnail.background = ContextCompat.getDrawable(context, R.drawable.ic_folder)
        }
        tvTitle.text = file.songName

        clItemRow.setOnClickListener(this)
    }

    override fun onClick(p0: View?) {
        listener.onDirectorySelected(adapterPosition)
    }

}