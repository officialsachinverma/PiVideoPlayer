package com.project100pi.library.misc

import android.os.Build

object Util {

    /**
     * Like [android.os.Build.VERSION.SDK_INT], but in a place where it can be conveniently
     * overridden for local testing.
     */
    val SDK_INT = Build.VERSION.SDK_INT

    /**
     * Like [Build.DEVICE], but in a place where it can be conveniently overridden for local
     * testing.
     */
    val DEVICE = Build.DEVICE

    /**
     * Like [Build.MANUFACTURER], but in a place where it can be conveniently overridden for
     * local testing.
     */
    val MANUFACTURER = Build.MANUFACTURER

    /**
     * Like [Build.MODEL], but in a place where it can be conveniently overridden for local
     * testing.
     */
    val MODEL = Build.MODEL

    /**
     * A concise description of the device that it can be useful to log for debugging purposes.
     */
    val DEVICE_DEBUG_INFO = (DEVICE + ", " + MODEL + ", " + MANUFACTURER + ", "
            + SDK_INT)

    /** An empty byte array.  */
    val EMPTY_BYTE_ARRAY = ByteArray(0)

}