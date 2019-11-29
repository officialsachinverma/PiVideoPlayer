package com.project100pi.pivideoplayer.listeners

/**
 * This interface provides three methods
 * The caller of this interface methods
 * should implement [View.OnClickListener]
 * and [View.OnLongClickListener]
 * the implementer of this interface will get the
 * LongClicked and ClickEvent
 *
 * @since v1
 *
 * [onItemLongClicked]
 * [onItemSelected]
 * [onOverflowItemClick]
 */
interface OnClickListener {

    /**
     * Called when a user clicked on an item for a long time.
     *
     * @param position Int the position of the item which is clicked.
     * @return Boolean
     */
    fun onItemLongClicked(position: Int): Boolean

    /**
     * Called when a user clicked on an item.
     *
     * @param position Int
     */
    fun onItemSelected(position: Int)

    /**
     * Called when overflow menu of an item is clicked
     *
     * @param position Int
     * @param viewId Int
     */
    fun onOverflowItemClick(position: Int, viewId: Int)
}