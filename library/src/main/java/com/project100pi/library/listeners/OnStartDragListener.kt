package com.project100pi.library.listeners

import androidx.recyclerview.widget.RecyclerView

/**
 * Created by Sachin Verma on 2019-11-18.
 */

interface OnStartDragListener {

    /**
     * Called when a view is requesting a start of a drag.
     *
     * @param viewHolder The holder of the view to drag.
     */
    fun onStartDrag(viewHolder: RecyclerView.ViewHolder)

}