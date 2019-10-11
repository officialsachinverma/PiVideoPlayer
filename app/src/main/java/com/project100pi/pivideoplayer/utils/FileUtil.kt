package com.project100pi.pivideoplayer.utils

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.IOException
import java.util.ArrayList

object FileUtil {

    fun canListFiles(f: File): Boolean {
        return f.canRead() && f.isDirectory
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    fun getExtSdCardPathsForActivity(context: Context): Array<String> {
        val paths = ArrayList<String>()
        for (file in context.getExternalFilesDirs("external")) {
            if (file != null) {
                val index = file.absolutePath.lastIndexOf("/Android/data")
                if (index < 0) {
                    Log.w("getExtSdCardPaths", "Unexpected external file dir: " + file.absolutePath)
                } else {
                    var path = file.absolutePath.substring(0, index)
                    try {
                        path = File(path).canonicalPath
                    } catch (e: IOException) {
                        // Keep non-canonical path.
                    }

                    paths.add(path)
                }
            }
        }
        if (paths.isEmpty()) paths.add("/storage/sdcard1")
        return paths.toTypedArray()
    }

}