package com.project100pi.pivideoplayer.Model

data class FolderInfo(var songName: String = "", var folderId: String = "", var path: String = "", var isSong: Boolean = false) {

    var songsList: ArrayList<FolderInfo> = ArrayList()

    constructor(
        folderName: String,
        fullPath: String,
        subFolder: String,
        songName: String,
        folderId: String
    ): this(folderName, folderId, fullPath)

    fun addSong(songName: String, folderId: String) {
        songsList.add(FolderInfo(songName, folderId, path + songName, true))

    }

}