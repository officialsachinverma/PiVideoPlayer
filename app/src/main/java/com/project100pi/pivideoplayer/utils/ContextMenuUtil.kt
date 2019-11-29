package com.project100pi.pivideoplayer.utils

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import java.io.File

object ContextMenuUtil {

    /**
     * Return the URI for a file. This URI is used for
     * sharing of video.
     * NOTE: You cannot share a file by file path.
     *
     * @param context Context
     * @param videoFile File
     * @return Uri?
     */
    fun getVideoContentUri(context: Context, videoFile: File): Uri? {
        var uri: Uri? = null
        val filePath = videoFile.absolutePath
        val cursor = context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Video.Media._ID),
            MediaStore.Video.Media.DATA + "=? ",
            arrayOf(filePath), null)

        if (cursor != null && cursor.moveToFirst()) {
            val id = cursor.getInt(cursor
                .getColumnIndex(MediaStore.MediaColumns._ID))
            val baseUri = Uri.parse("content://media/external/video/media")
            uri = Uri.withAppendedPath(baseUri, "" + id)
        } else if (videoFile.exists()) {
            val values = ContentValues()
            values.put(MediaStore.Video.Media.DATA, filePath)
            uri = context.contentResolver.insert(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
        }

        closeCursor(cursor)
        return uri
    }

    /**
     * Closes the provided cursor
     *
     * @param cursor Cursor?
     */
    private fun closeCursor(cursor: Cursor?) {

        if (cursor != null && !cursor.isClosed)
            cursor.close()
    }
}