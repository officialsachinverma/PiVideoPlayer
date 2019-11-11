package com.project100pi.pivideoplayer.model

import android.os.Parcel
import android.os.Parcelable

data class FolderInfo(var videoName: String? = "", var folderId: String? = "", var path: String? = "", var isSong: Boolean = false):
    Parcelable {

    var songsList: ArrayList<FolderInfo> = ArrayList()

    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readByte() != 0.toByte()
    )

    constructor(
        folderName: String,
        fullPath: String,
        subFolder: String,
        songName: String,
        folderId: String
    ): this(folderName, folderId, fullPath)

    constructor(
        folderName: String,
        fullPath: String,
        subFolder: String,
        songName: String,
        folderId: String,
        isSong: Boolean
    ): this(folderName, folderId, fullPath, isSong)

    fun addSong(songName: String, folderId: String) {
        songsList.add(FolderInfo(songName, folderId, path + songName, true))

    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(videoName)
        parcel.writeString(folderId)
        parcel.writeString(path)
        parcel.writeByte(if (isSong) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<FolderInfo> {
        override fun createFromParcel(parcel: Parcel): FolderInfo {
            return FolderInfo(parcel)
        }

        override fun newArray(size: Int): Array<FolderInfo?> {
            return arrayOfNulls(size)
        }
    }

}