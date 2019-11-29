package com.project100pi.library.dialogs.listeners

/**
 * This interface provides two methods
 * The caller of this interface methods
 * should implement [View.OnClickListener]
 * the implementer of this interface will get the
 * ClickEvent
 *
 * @since v1
 *
 * [onItemClicked]
 * [onItemMoved]
 */
interface OnItemClickListener {


    /**
     * Called when an item has been clicked.
     *
     * @param position Int Position of item clicked.
     */
    fun onItemClicked(position: Int)


    /**
     * Called when an item has been dragged far enough to trigger a move. This is called every time
     * an item is shifted, and **not** at the end of a "drop" event.<br></br>
     *
     * Implementations should call RecyclerView.Adapter.notifyItemMoved after
     * adjusting the underlying data to reflect this move.
     *
     * @param fromPosition The start position of the moved item.
     * @param toPosition   Then resolved position of the moved item.
     *
     */
    fun onItemMoved(fromPosition: Int, toPosition: Int)
}