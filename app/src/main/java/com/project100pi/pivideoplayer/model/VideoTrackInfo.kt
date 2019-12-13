package com.project100pi.pivideoplayer.model

import android.os.Parcel
import android.os.Parcelable

/**
 * Created by Sachin Verma on 2019-11-20.
 */

/**
 * this class holds information about a video
 *
 * @param _Id Int Stores value of _id in media database
 * @param videoName String Stores video name
 * @param videoPath String Stores video path
 * @param durationInMs Long Stores video duration in millis
 * @constructor
 */
data class VideoTrackInfo(var _Id: Int = -1,
                          var videoName: String = "",
                          var videoPath: String = "",
                          var durationInMs: Long = 0,
                          var dateAdded: String = ""): Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readLong(),
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(_Id)
        parcel.writeString(videoName)
        parcel.writeString(videoPath)
        parcel.writeLong(durationInMs)
        parcel.writeString(dateAdded)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<VideoTrackInfo> {
        override fun createFromParcel(parcel: Parcel): VideoTrackInfo {
            return VideoTrackInfo(parcel)
        }

        override fun newArray(size: Int): Array<VideoTrackInfo?> {
            return arrayOfNulls(size)
        }
    }

}