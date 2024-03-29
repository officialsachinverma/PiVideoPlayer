package com.project100pi.library.adapter.viewholder

import androidx.recyclerview.widget.ItemTouchHelper

/**
 * This interface provides two methods
 * The caller of this interface methods
 * should implement [AdapterView.OnItemClickListener]
 * the implementer of this interface will get the
 * ClickEvent
 *
 * @since v1
 *
 * [onItemSelected]
 * [onItemClear]
 */
interface ItemTouchHelperViewHolder {

    /**
     * Called when the [ItemTouchHelper] first registers an item as being moved or swiped.
     * Implementations should update the item view to indicate it's active state.
     */
    fun onItemSelected()


    /**
     * Called when the [ItemTouchHelper] has completed the move or swipe, and the active item
     * state should be cleared.
     */
    fun onItemClear()
}