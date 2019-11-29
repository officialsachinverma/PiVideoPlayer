package com.project100pi.pivideoplayer.listeners

/**
 * This Interface provides three methods
 * The implementer of this interface has
 * precise control over the status of delete
 * operation, any error occurred while deletion operation
 * and if permission is required to delete a file from
 * sd card.
 *
 * @since v1
 *
 * [showPermissionForSdCard]
 * [onDeleteSuccess]
 * [onDeleteError]
 */

interface ItemDeleteListener {

    /**
     * Called when permission for sd card is required.
     */
    fun showPermissionForSdCard()

    /**
     * Called when delete operation is successful.
     *
     * @param listOfIndexes List<Int> List of indices of videos
     * which are deleted.
     */
    fun onDeleteSuccess(listOfIndexes: List<Int>)

    /**
     * Called when an error occurs while executing the delete operation.
     */
    fun onDeleteError()
}