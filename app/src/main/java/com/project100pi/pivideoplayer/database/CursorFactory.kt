package com.project100pi.pivideoplayer.database

import android.content.Context
import android.database.Cursor
import android.provider.MediaStore

object CursorFactory {

     fun getAllVideoCursor(context: Context): Cursor? {
         //val sortOrder = MediaStore.Video.Media.TITLE + " ASC"
         val projection = arrayOf(MediaStore.Video.Media.DATA, MediaStore.Video.Media._ID, MediaStore.Video.Media.TITLE, MediaStore.Video.Media.DURATION)
         return context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
             projection,
            null,
             null,
             null
        )
    }

    fun getVideoSearchData(context: Context, searchData: String): Cursor? {
        val sortOrder = MediaStore.Video.Media.TITLE + " ASC"
        val projection = arrayOf(MediaStore.Video.Media.DATA, MediaStore.Video.Media._ID, MediaStore.Video.Media.TITLE, MediaStore.Video.Media.DURATION)
        val selection = "${MediaStore.Video.Media.DATA} LIKE '%${searchData}%'" // searching in data because few videos title is same - udemy
        return context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )
    }

    fun getVideoMetaDataById(context: Context, _id: String): Cursor? {
        val sortOrder = MediaStore.Video.Media.TITLE + " ASC"
        val projection = arrayOf(MediaStore.Video.Media.DATA, MediaStore.Video.Media._ID, MediaStore.Video.Media.TITLE)
        val selection = "${MediaStore.Video.Media._ID} LIKE '${_id}'"
        return context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )
    }

    fun getVideoMetaDataByPath(context: Context, path: String): Cursor? {
        val sortOrder = MediaStore.Video.Media.TITLE + " ASC"
        val projection = arrayOf(MediaStore.Video.Media.DATA, MediaStore.Video.Media._ID, MediaStore.Video.Media.TITLE, MediaStore.Video.Media.DURATION)
        val selection = "${MediaStore.Video.Media.DATA} LIKE '${path}%'"
        return context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )
    }
}