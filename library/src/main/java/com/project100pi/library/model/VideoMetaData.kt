package com.project100pi.library.model

import android.os.Parcel
import android.os.Parcelable

data class VideoMetaData(
    val _id: Int,
    val title: String,
    val path: String
): Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(_id)
        parcel.writeString(title)
        parcel.writeString(path)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<VideoMetaData> {
        override fun createFromParcel(parcel: Parcel): VideoMetaData {
            return VideoMetaData(parcel)
        }

        override fun newArray(size: Int): Array<VideoMetaData?> {
            return arrayOfNulls(size)
        }
    }

}