package com.project100pi.pivideoplayer.database

import android.content.Context
import android.database.Cursor
import android.provider.MediaStore

object CursorFactory {

     fun getAllVideoCursor(context: Context): Cursor? {
         val sortOrder = MediaStore.Video.Media.TITLE + " ASC"
         val projection = arrayOf(MediaStore.Video.Media.DATA, MediaStore.Video.Media._ID)
         return context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
             projection,
            null,
             null,
             sortOrder
        )
    }

    fun getVideoSearchData(context: Context, searchData: String): Cursor? {
        val sortOrder = MediaStore.Video.Media.TITLE + " ASC"
        val projection = arrayOf(MediaStore.Video.Media.DATA, MediaStore.Video.Media._ID)
        val selection = "${MediaStore.Video.Media.TITLE} LIKE '${searchData}%'"
        return context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )
    }
}