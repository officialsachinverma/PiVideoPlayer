package com.project100pi.pivideoplayer.database

import android.content.Context
import android.database.Cursor
import android.provider.MediaStore

/**
 * Provides cursor on demand
 * this class is bound to provide cursor only
 */
object CursorFactory {

    /**
     * Fetch all video in device
     *
     * @param context Context
     * @return Cursor?
     */

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

    /**
     * Search for a video based on DATA
     *
     * @param context Context
     * @param searchData String
     * @return Cursor?
     */

    fun getVideoSearchData(context: Context, searchData: String): Cursor? {
        val sortOrder = MediaStore.Video.Media.DATA + " ASC"
        val projection = arrayOf(MediaStore.Video.Media.DATA, MediaStore.Video.Media._ID, MediaStore.Video.Media.TITLE, MediaStore.Video.Media.DURATION)
        // searching in MediaStore.Video.Media.DATA of videos file because few videos has same MediaStore.Video.Media.TITLE
        // For some videos Title meta data is same and is not related to actual video file name
        // user will search for video based on file name (title) but it is same
        // So we are searching in path which contains video file name (title)
        val selection = "${MediaStore.Video.Media.DATA} LIKE '%${searchData}%'"
        return context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )
    }

    /**
     * Fetch video meta data by video ID
     *
     * @param context Context
     * @param _id Int
     * @return Cursor?
     */

    fun getVideoMetaDataById(context: Context, _id: Int): Cursor? {
        val sortOrder = MediaStore.Video.Media.DATA + " ASC"
        val projection = arrayOf(MediaStore.Video.Media.DATA, MediaStore.Video.Media._ID, MediaStore.Video.Media.TITLE, MediaStore.Video.Media.DURATION)
        val selection = "${MediaStore.Video.Media._ID} = $_id"
        return context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )
    }

    /**
     * Fetch video meta data by video path
     *
     * @param context Context
     * @param path String
     * @return Cursor?
     */

    fun getVideoMetaDataByPath(context: Context, path: String): Cursor? {
        val sortOrder = MediaStore.Video.Media.DATA + " ASC"
        val projection = arrayOf(MediaStore.Video.Media.DATA, MediaStore.Video.Media._ID, MediaStore.Video.Media.TITLE, MediaStore.Video.Media.DURATION)
        val selection = "${MediaStore.Video.Media.DATA} LIKE \"${path}\""
        return context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )
    }
}