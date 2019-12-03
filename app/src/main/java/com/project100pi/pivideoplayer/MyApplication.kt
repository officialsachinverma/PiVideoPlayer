package com.project100pi.pivideoplayer

import android.app.Application
import android.os.Handler
import android.provider.MediaStore
import com.project100pi.pivideoplayer.database.TinyDB
import com.project100pi.pivideoplayer.model.observable.OnMediaChangeContentObserver

/**
 * Created by Sachin Verma on 2019-11-19.
 */

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        registerListenerForMediaProvider()

        TinyDB.initPref(this)
    }

    /**
     * Registers listener for Media Provider
     *
     * When ever a modification (add, delete) will happen in MediaStore.Video.Media table
     * which contains the record of all the videos in device will tell the observer about the change
     * The change can be addition of a new record (new video is added) or deletion of a record (video is removed from device)
     */
    private fun registerListenerForMediaProvider() {

        val onMediaChangeContentObserver = OnMediaChangeContentObserver(Handler())

        contentResolver.registerContentObserver(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            true,
            onMediaChangeContentObserver)
    }

}