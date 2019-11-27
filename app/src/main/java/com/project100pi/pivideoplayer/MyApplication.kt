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

    private fun registerListenerForMediaProvider() {
        val onMediaChangeContentObserver =
            OnMediaChangeContentObserver(Handler())
        contentResolver.registerContentObserver(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            true,
            onMediaChangeContentObserver
        )
    }

}