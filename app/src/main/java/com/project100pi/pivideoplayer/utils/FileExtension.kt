package com.project100pi.pivideoplayer.utils

object FileExtension  {

    private val exts = listOf("mp4", "m4a", "m4v", "f4v", "f4a",
        "m4b", "m4r", "m4b", "mov", "mkv", "fly", "webm", "wmv", "avi")

    fun isVideo(path: String): Boolean {
        var ext = path.substring(path.lastIndexOf(".") + 1)
        return  exts.contains(ext)
    }

}