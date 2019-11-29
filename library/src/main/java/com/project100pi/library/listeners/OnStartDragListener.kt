package com.project100pi.library.listeners

import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView
import android.view.View


/**
 * Created by Sachin Verma on 2019-11-18.
 *
 * This interface provides one method
 * the caller of this interface should implement
 * [View.OnTouchListener] and should call the method
 * when MotionEvent == [MotionEvent.ACTION_DOWN]
 *
 * @since v1
 *
 * [onStartDrag]
 *
 */

interface OnStartDragListener {

    /**
     * Called when a view is requesting a start of a drag.
     *
     * @param viewHolder The holder of the view to drag.
     */
    fun onStartDrag(viewHolder: RecyclerView.ViewHolder)

}