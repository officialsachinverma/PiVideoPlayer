package com.project100pi.pivideoplayer.model

data class FolderInfo(var folderName: String = "",
                      var folderPath: String = ""){

    val filesList: ArrayList<FileInfo> = ArrayList()

    fun addFile(fileId: Int, fileName: String, filePath: String, fileDuration: Long) {
        filesList.add(FileInfo(fileId, fileName, filePath, fileDuration))

    }

}