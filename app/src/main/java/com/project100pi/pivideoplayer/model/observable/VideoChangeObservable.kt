package com.project100pi.pivideoplayer.model.observable

import java.util.*

/**
 * Created by Sachin Verma on 2019-11-19.
 */

object VideoChangeObservable : Observable() {

    /**
     * Marks this object as changed.
     */
    fun setChangedOverride() = setChanged()

    /**
     * Notifies all the observers who are
     * observing for the change to happen
     */
    fun notifyObserversOverride() = notifyObservers()

}