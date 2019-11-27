package com.project100pi.pivideoplayer.ui.activity.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.project100pi.pivideoplayer.database.CursorFactory
import com.project100pi.pivideoplayer.database.TinyDB
import com.project100pi.pivideoplayer.listeners.ItemDeleteListener
import com.project100pi.pivideoplayer.model.VideoTrackInfo
import com.project100pi.pivideoplayer.utils.Constants
import com.project100pi.pivideoplayer.utils.FileExtension
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException

class SearchViewModel(private val context: Context, application: Application): AndroidViewModel(application) {

    private var _searchResultList = MutableLiveData<ArrayList<VideoTrackInfo>>()
    val searchResultList: LiveData<ArrayList<VideoTrackInfo>>
        get() = _searchResultList

    private val coroutineJob = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.IO + coroutineJob)

    fun deleteSearchedVideos(listOfIndexes: List<Int>, listener: ItemDeleteListener) {
        coroutineScope.launch {
            for (position in listOfIndexes) {
                try {
                    val folder = _searchResultList.value!![position].videoPath
                    val file = File(folder)
                    if(file.exists()) {
                        if (file.delete()) {
                            context.applicationContext.sendBroadcast(
                                Intent(
                                    Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                                    Uri.fromFile(file)
                                )
                            )
                            withContext(Dispatchers.Main) {
                                listener.onDeleteSuccess(listOfIndexes)
                            }
                        } else {
                            if (file.exists()) {
                                // checking if file still exist in it actual true location
                                if (file.canonicalFile.delete()) {
                                    if (file.exists()) {
                                        if (context.applicationContext.deleteFile(file.name)) {
                                            context.applicationContext.sendBroadcast(
                                                Intent(
                                                    Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                                                    Uri.fromFile(file)
                                                )
                                            )
                                            withContext(Dispatchers.Main) {
                                                listener.onDeleteSuccess(listOfIndexes)
                                            }
                                        }
                                    }
                                }
                            }
                            // checking for file is in sd card and sdcard uri
                            if (TinyDB.getString(Constants.SD_CARD_URI).isNullOrEmpty()) {
                                listener.showPermissionForSdCard()
                            } else {
                                val sdCardUri = TinyDB.getString(Constants.SD_CARD_URI)
                                var documentFile = DocumentFile.fromTreeUri(context, Uri.parse(sdCardUri))
                                if (file.exists() && sdCardUri!!.isNotEmpty()) {
                                    val parts: List<String> = file.path.split("/")
                                    // findFile method will search documentFile for the first file
                                    // with the expected `DisplayName`

                                    // We skip first three items because we are already on it.(sdCardUri = /storage/extSdCard)
                                    for (strs in parts.subList(3, parts.size)) {
                                        if (documentFile != null) {
                                            documentFile = documentFile.findFile(strs)
                                        }
                                    }
                                    if (documentFile != null) {
                                        if (documentFile.delete()) {
                                            context.applicationContext.sendBroadcast(
                                                Intent(
                                                    Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                                                    Uri.fromFile(file)
                                                )
                                            )
                                            withContext(Dispatchers.Main) {
                                                listener.onDeleteSuccess(listOfIndexes)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        listener.onDeleteError()
                    }
                }
            }
        }
    }

    fun performSearch(queryText: String) {
        val searchResult = ArrayList<VideoTrackInfo>()
        try {
            coroutineScope.launch  {
                val cursor = CursorFactory.getVideoSearchData(context, queryText)
                if (cursor != null && cursor.moveToFirst()) {
                    // We populate something, only if the cursor is available
                    do {
                        try {
                            //To get path of song
                            val videoPath = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA))
                            //To get song id
                            val videoId = cursor.getInt(cursor.getColumnIndex(MediaStore.Video.Media._ID))
                            //To get song duration
                            val videoDuration = cursor.getLong(cursor.getColumnIndex(MediaStore.Video.Media.DURATION))

                            if (FileExtension.isVideo(videoPath)) {
                                if (videoPath != null) {

                                    //Splitting song path to list by using .split("/") to get elements from song path separated
                                    // /storage/emulated/music/abc.mp3
                                    // -> music will be folder name
                                    // -> emulated will be subfolder name
                                    // -> abc will be song name

                                    val pathsList = videoPath.split("/")

                                    val videoName = pathsList[pathsList.size - 1]

                                    searchResult.add(
                                        VideoTrackInfo(
                                            videoId,
                                            videoName,
                                            videoPath,
                                            videoDuration))

                                } else
                                    continue
                            }

                        } catch (e: Exception) { // catch specific exception
                            e.printStackTrace()
                        }
                    } while (cursor.moveToNext())
                    cursor.close()
                }
                withContext(Dispatchers.Main)
                {
                    _searchResultList.value = searchResult
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCleared() {
        super.onCleared()
        coroutineJob.cancel()
    }

    fun removeElementAt(position: Int) {
        _searchResultList.value!!.removeAt(position)
    }

}