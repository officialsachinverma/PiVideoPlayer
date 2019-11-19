package com.project100pi.pivideoplayer.activity

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.project100pi.pivideoplayer.database.CursorFactory
import com.project100pi.pivideoplayer.listeners.ItemDeleteListener
import com.project100pi.pivideoplayer.model.FolderInfo
import com.project100pi.pivideoplayer.utils.FileExtension
import kotlinx.coroutines.*
import java.io.File

class SearchViewModel(private val context: Context, application: Application): AndroidViewModel(application) {

    private var foldersList = MutableLiveData<ArrayList<FolderInfo>>()
    val foldersListExposed: LiveData<ArrayList<FolderInfo>>
        get() = foldersList

    private val coroutineJob = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.IO + coroutineJob)

    fun delete(listOfIndexes: List<Int>, listener: ItemDeleteListener) {
        coroutineScope.launch {

            for (position in listOfIndexes) {
                try {
                    val folder = foldersList.value!![position].path
                    val file = File(folder)
                    if(file.exists()) {
                        file.delete()
                        context.applicationContext.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)))
                    }
                    if (file.exists()) {
                        file.canonicalFile.delete()
                        if (file.exists()) {
                            file.delete()
                            context.applicationContext.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)))
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        listener.onDeleteError()
                    }
                }
            }

            withContext(Dispatchers.Main) {
                listener.onDeleteSuccess(listOfIndexes)
            }

        }
    }

    fun performSearch(queryText: String) {
        val searchResult = ArrayList<FolderInfo>()
        try {
            coroutineScope.launch  {
                val cursor = CursorFactory.getVideoSearchData(context, queryText)
                if (cursor != null && cursor.moveToFirst()) {
                    // We populate something, only if the cursor is available
                    do {
                        try {
                            //To get path of song
                            val path = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA))
                            //To get song id
                            val songId = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media._ID))
                            //To get song duration
                            val songDuration = cursor.getLong(cursor.getColumnIndex(MediaStore.Video.Media.DURATION))
                            if (FileExtension.isVideo(path)) {
                                if (path != null) {

                                    //Splitting song path to list by using .split("/") to get elements from song path separated
                                    // /storage/emulated/music/abc.mp3
                                    // -> music will be folder name
                                    // -> emulated will be subfolder name
                                    // -> abc will be song name

                                    val pathsList = path.split("/")

                                    val videoName = pathsList[pathsList.size - 1]

                                    searchResult.add(FolderInfo(
                                        videoName,
                                        path,
                                        "",
                                        videoName,
                                        songId,
                                        true,
                                        songDuration
                                    ))

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
                    foldersList.value = searchResult
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
        foldersList.value!!.removeAt(position)
    }

}