package com.project100pi.library.dialogs.listeners

import android.widget.AdapterView

/**
 * This interface provides one methods
 * The caller of this interface methods
 * should implement [AdapterView.OnItemClickListener]
 * the implementer of this interface will get the
 * ClickEvent
 *
 * @since v1
 *
 * [filePickerSuccessClickListener]
 */
interface SRTFilePickerClickListener {

    /**
     * Called when a subtitle file is selected
     *
     * @param absolutePath Path of the subtitle file
     */
    fun filePickerSuccessClickListener(absolutePath: String)
}