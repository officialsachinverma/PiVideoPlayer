package com.project100pi.pivideoplayer.model

import android.os.Parcel
import android.os.Parcelable

data class FolderInfo(var videoName: String = "",
                      var folderId: String = "",
                      var path: String = "",
                      var isSong: Boolean = false,
                      var duration: Long = 0){

    var songsList: ArrayList<FileInfo> = ArrayList()

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readByte() != 0.toByte(),
        parcel.readLong()
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
        isSong: Boolean,
        duration: Long
    ): this(folderName, folderId, fullPath, isSong, duration)

    fun addSong(fileId: String, fileName: String, filePath: String, fileDuration: Long) {
        songsList.add(FileInfo(fileId, fileName, filePath, fileDuration))

    }

}