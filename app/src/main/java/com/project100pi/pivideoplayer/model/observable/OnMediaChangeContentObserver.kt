package com.project100pi.pivideoplayer.model.observable

import android.database.ContentObserver
import android.os.Handler
import com.project100pi.pivideoplayer.utils.DataRefresherUtils

/**
 * Created by Sachin Verma on 2019-11-19.
 */

class OnMediaChangeContentObserver(handler: Handler) : ContentObserver(handler) {

    override fun onChange(selfChange: Boolean) {
        super.onChange(selfChange) // Doing this knowing that Android doesnot do anything
        DataRefresherUtils.tryRefreshingAllData()
    }

}