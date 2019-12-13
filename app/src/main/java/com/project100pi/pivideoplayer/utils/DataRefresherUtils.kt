package com.project100pi.pivideoplayer.utils

import android.os.Handler
import com.project100pi.library.misc.Logger
import com.project100pi.pivideoplayer.model.observable.VideoChangeObservable
import java.util.*

/**
 * Created by Sachin Verma on 2019-11-19.
 */

object DataRefresherUtils {

    private var isDataRefreshing = false
    private val handler: Handler = Handler()

    /**
     * Triggers refresh timer
     */
    fun tryRefreshingAllData() {
        Logger.i("tryRefreshingAllData --> We are gonna refresh all the data in 5 seconds")
        startDataRefresherTimerTask()
    }

    /**
     * Starts a timer for 5 secs after that it triggers DataRefreshers
     * Here we wait for 5 secs after any changes happen in case any other change
     * has to happen. If not then we refresh the data, other wise timer is reset
     * and again start wait for 5 sec
     */
    private fun startDataRefresherTimerTask() {
        if(isDataRefreshing){
            handler.removeCallbacks(runnable)

        }
        isDataRefreshing = true
        handler.postDelayed(runnable,5000)

//        val timer = Timer()
//        val execTime: Long = 5000 // Wait for 5 seconds before loading the data.
//        //Also for 5 seconds the isDataLoading flag is held as true so that no subsequent calls will be triggered
//        // This is just to make sure that as we have registered for multiple URIs ,
//        // 1) Adding a song can trigger multiple calls
//        // 2) User might add multiple songs concurrently. In that case, refresh happens every 5 sec only
//        timer.schedule(
//            DataRefresherTimerTask(),
//            execTime
//        )
    }
    private val runnable = Runnable {
        isDataRefreshing = false
        notifyObservers()
    }
    /**
     * Marks the object as changed and notify it's observers
     */
    private fun notifyObservers() {
        VideoChangeObservable.setChangedOverride()
        VideoChangeObservable.notifyObservers()
    }

    /**
     * TimerTask to refresh all the data
     */
    private class DataRefresherTimerTask : TimerTask() {
        override fun run() {
            Logger.i("DataRefresherTimerTask --> We are gonna refresh all the data NOW")
            notifyObservers()
        }
    }

}