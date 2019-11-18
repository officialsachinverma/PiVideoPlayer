package com.project100pi.library.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.project100pi.library.R
import com.project100pi.library.adapter.CurrentPlayingQueueAdapter
import com.project100pi.library.dialogs.listeners.OnItemClickListener
import com.project100pi.library.model.VideoMetaData
import androidx.recyclerview.widget.ItemTouchHelper
import com.project100pi.library.listeners.ItemTouchHelperCallback

class CurrentPlayingQueueDialog(context: Context,
                                private val currentPlayingList: ArrayList<VideoMetaData>,
                                private val nowPlaying: VideoMetaData,
                                private val listener: OnItemClickListener) :
    Dialog(context),
    View.OnClickListener{

    private lateinit var rvCurrentPlaying: RecyclerView
    private lateinit var ivClose: ImageView
    private lateinit var adapter: CurrentPlayingQueueAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_current_playing_queue)

        rvCurrentPlaying = findViewById(R.id.current_playing_recycler_view)
        ivClose = findViewById(R.id.close_current_playing_image_view)
        ivClose.setOnClickListener(this)

        adapter = CurrentPlayingQueueAdapter(context, R.layout.row_current_playing_queue, currentPlayingList, nowPlaying, listener)
        val linearLayout = LinearLayoutManager(context)
        linearLayout.orientation = LinearLayoutManager.VERTICAL
        rvCurrentPlaying.layoutManager = linearLayout
        rvCurrentPlaying.adapter = adapter
        //adapter.submitList(currentPlayingList)

        val callback = ItemTouchHelperCallback(adapter)
        val touchHelper = ItemTouchHelper(callback)
        touchHelper.attachToRecyclerView(rvCurrentPlaying)

    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.close_current_playing_image_view -> {
                dismiss()
            }
        }
    }

}