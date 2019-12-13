package com.project100pi.pivideoplayer.model.observable

import android.database.ContentObserver
import android.os.Handler
import com.project100pi.library.misc.Logger
import com.project100pi.pivideoplayer.utils.DataRefresherUtils

/**
 * Created by Sachin Verma on 2019-11-19.
 */

class OnMediaChangeContentObserver(handler: Handler) : ContentObserver(handler) {

    /**
     * Called when object changes
     *
     * @param selfChange Boolean
     */
    override fun onChange(selfChange: Boolean) {
        // when ever content changes, need to load all data
        Logger.i("DataRefresherUtils.tryRefreshingAllData --> onChange")

        DataRefresherUtils.tryRefreshingAllData()
    }

}