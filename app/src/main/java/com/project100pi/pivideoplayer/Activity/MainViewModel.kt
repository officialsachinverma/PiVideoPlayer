package com.project100pi.pivideoplayer.Activity

import android.app.Application
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.project100pi.pivideoplayer.Model.Track
import kotlinx.coroutines.*

class MainViewModel(application: Application): AndroidViewModel(application) {

    private val videoList: MutableLiveData<MutableList<Track>> = MutableLiveData()
    val videoListExposed: LiveData<MutableList<Track>>
        get() = videoList

    private val viewModelJob = Job()
    private val viewModelScope = CoroutineScope(Dispatchers.IO + viewModelJob)

    fun loadVideoFiles(context: Context?){
        viewModelScope.launch {

            val list = mutableListOf<Track>()
            val uri: Uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            //val selection = MediaStore.Video.Media.IS_PRIVATE + "!= 0"
            val sortOrder = MediaStore.Video.Media.DATE_MODIFIED + " DESC"
            val cursor: Cursor? = context?.contentResolver?.query(
                uri, // Uri
                null, // Projection
                null, // Selection
                null, // Selection arguments
                sortOrder // Sort order
            )
            if (cursor!= null && cursor.moveToFirst()){

                do {
                    val audioId:Long = cursor.getLong(cursor.getColumnIndex(MediaStore.Video.VideoColumns._ID))
                    val audioTitle:String = cursor.getString(cursor.getColumnIndex(MediaStore.Video.VideoColumns.TITLE))
                    val artistName:String = cursor.getString(cursor.getColumnIndex(MediaStore.Video.VideoColumns.ARTIST))
                    val durationLong: Long = cursor.getLong(cursor.getColumnIndex(MediaStore.Video.VideoColumns.DURATION))
                    val minutes = durationLong / 1000 / 60
                    val seconds = durationLong / 1000 % 60

                    val duration:String = "$minutes:$seconds"
                    val albumArt: Bitmap? = ThumbnailUtils.createVideoThumbnail(
                        cursor.getString(cursor.getColumnIndex(MediaStore.Video.VideoColumns.DATA)),
                        MediaStore.Video.Thumbnails.MICRO_KIND)
                    val filePath:String = cursor.getString(cursor.getColumnIndex(MediaStore.Video.VideoColumns.DATA))

                    list.add(Track(audioId,audioTitle,artistName,duration,albumArt,filePath, false))
                }while (cursor.moveToNext())
            }

            withContext(Dispatchers.Main){
                videoList.value = list
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

}