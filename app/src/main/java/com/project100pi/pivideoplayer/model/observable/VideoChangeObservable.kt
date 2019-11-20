package com.project100pi.pivideoplayer.model.observable

import java.util.*

/**
 * Created by Sachin Verma on 2019-11-19.
 */

object VideoChangeObservable : Observable() {

    fun setChangedOverride() = setChanged()

    fun notifyObserversOverride() = notifyObservers()

}