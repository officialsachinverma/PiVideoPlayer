package com.project100pi.pivideoplayer.database

import android.content.Context
import android.database.Cursor
import android.provider.MediaStore

object CursorFactory {

     fun getAllVideoCursor(mContext: Context): Cursor? {

         val sortOrder = MediaStore.Video.Media.TITLE + " ASC"

         return mContext.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
             null,
            null,
             null,
             sortOrder
        )
    }
}