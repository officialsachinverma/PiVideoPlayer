package com.project100pi.pivideoplayer.activity

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.project100pi.pivideoplayer.listeners.ClickInterface
import com.project100pi.pivideoplayer.model.FolderInfo
import com.project100pi.pivideoplayer.utils.Constants
import com.project100pi.pivideoplayer.database.CursorFactory
import com.project100pi.pivideoplayer.utils.FileExtension
import kotlinx.coroutines.*
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import android.content.Intent
import com.project100pi.pivideoplayer.listeners.ItemDeleteListener

class MainViewModel(private val context: Context?, application: Application): AndroidViewModel(application), ClickInterface {

    private var viewModelJob = Job()
    private val viewModelScope = CoroutineScope(viewModelJob + Dispatchers.IO)
    private val allFilePaths = mutableListOf<String>()
    private var foldersList = MutableLiveData<ArrayList<FolderInfo>>()
    val foldersListExposed: LiveData<ArrayList<FolderInfo>>
        get() = foldersList
    private var pathToIdInfo = HashMap<String, String>()
    private var foldersWithPathMap = HashMap<String, FolderInfo>()
    private var TAG = "MainViewModel"

    //Mode to know whether folders are visible or songs of folder are visible
    var MODE = Constants.FOLDER_VIEW

    var CURRENT_SONG_FOLDER_INDEX = -1

    override fun onItemClicked(position: Int) {
        CURRENT_SONG_FOLDER_INDEX = position
        MODE = Constants.SONG_VIEW
    }

    override fun onBackFolderPressed() {
        CURRENT_SONG_FOLDER_INDEX = -1
        MODE = Constants.FOLDER_VIEW
    }
    override fun onItemLongClicked(position: Int): Boolean {
        return false
    }

    fun delete(listOfIndexes: List<Int>, isDirectory: Boolean, listener: ItemDeleteListener) {
        viewModelScope.launch {

            for (position in listOfIndexes) {
                try {
                    val file = if (!isDirectory) {
                        val folder = foldersList.value!![CURRENT_SONG_FOLDER_INDEX].songsList[position]
                        File(folder.path!!)
                    } else {
                        val folder = foldersList.value!![position].path!!
                        File(folder)
                    }
                    if(file.exists()) {
                        file.delete()
                        context!!.applicationContext.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)))
                    }
                    if (file.exists()) {
                        file.canonicalFile.delete()
                        if (file.exists()) {
                            file.delete()
                            context!!.applicationContext.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)))
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

    fun loadAllFolderData() {
        foldersWithPathMap.clear()
        allFilePaths.clear()
        pathToIdInfo.clear()

        viewModelScope.launch {
            val cursor = CursorFactory.getAllVideoCursor(context!!)

            if (cursor != null && cursor.moveToFirst()) {
                // We populate something, only if the cursor is available
                do {
                    try {
                        //To get path of song
                        val path = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA))
                        //To get song id
                        val songId = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media._ID))

                        if (FileExtension.isVideo(path)) {
                            if (path != null) {
                                allFilePaths.add(path)
                                pathToIdInfo[path] = songId


                                //Splitting song path to list by using .split("/") to get elements from song path separated
                                // /storage/emulated/music/abc.mp3
                                // -> music will be folder name
                                // -> emulated will be subfolder name
                                // -> abc will be song name

                                val pathsList = path.split("/")

                                //Getting folder name from path Example
                                // /storage/emulated/music/abc.mp3 -> music will be folder name
                                val folderName = pathsList[pathsList.size - 2]

                                //If there exists a sub folder then add its name
                                // /storage/emulated/music/abc.mp3 -> emulated will be subfolder name
                                var subFolderName = ""
                                if (pathsList.size > 2) {
                                    subFolderName = pathsList[pathsList.size - 3]
                                }
                                //"key" Contains path of song excluding song name
                                //Complete Path length - Song Name Length
                                //Song Name Length = pathsList.get(pathsList.size-1).length
                                var key = path.substring(0, path.length - pathsList[pathsList.size - 1].length)

                                var videoName = pathsList[pathsList.size - 1]
                                //If folder is not already present in the hashmap
                                if (!foldersWithPathMap.containsKey(key)) {
                                    foldersWithPathMap[key] = FolderInfo(
                                        folderName,
                                        path.substring(
                                            0,
                                            path.length - pathsList[pathsList.size - 1].length
                                        ),
                                        subFolderName,
                                        videoName,
                                        songId
                                    )
                                }

                                foldersWithPathMap[key]?.addSong(videoName, songId)

                            } else
                                continue
                        }

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } while (cursor.moveToNext())
                cursor.close()
            }
            withContext(Dispatchers.Main)
                {
                    //Converting hash map to arraylist which will be submitted to adapter
                    var list = ArrayList(foldersWithPathMap.values.sortedWith(compareBy { it.songName }))
                    foldersList.value = list
                }

        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    fun removeElementAt(position: Int) {
        foldersList.value!!.get(CURRENT_SONG_FOLDER_INDEX).songsList.removeAt(position)
    }

}