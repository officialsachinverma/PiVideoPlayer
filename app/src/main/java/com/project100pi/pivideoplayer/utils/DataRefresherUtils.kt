package com.project100pi.pivideoplayer.utils

import com.project100pi.library.misc.Logger
import com.project100pi.pivideoplayer.model.observable.VideoChangeObservable
import java.util.*

/**
 * Created by Sachin Verma on 2019-11-19.
 */

object DataRefresherUtils {

    fun tryRefreshingAllData() {
        Logger.i("tryRefreshingAllData --> We are gonna refresh all the data in 5 seconds")
        startDataRefresherTimerTask()
    }

    private fun startDataRefresherTimerTask() {
        val timer = Timer()
        val execTime: Long = 5000 // Wait for 5 seconds before loading the data.
        //Also for 5 seconds the isDataLoading flag is held as true so taht no subsequent calls will be triggered
        // This is just to make sure that as we have registered for multiple URIs ,
        // 1) Adding a song can trigger multiple calls
        // 2) User might add multiple songs concurrently. In that case, refresh happens every 5 sec only
        timer.schedule(
            DataRefresherTimerTask(),
            execTime
        )
    }

    private fun notifyObservers() {
        VideoChangeObservable.setChangedOverride()
    }

    private class DataRefresherTimerTask : TimerTask() {
        override fun run() {
            Logger.i("DataRefresherTimerTask --> We are gonna refresh all the data NOW")
            notifyObservers()
        }
    }

}