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
import android.widget.Toast
import com.project100pi.library.misc.Logger
import com.project100pi.library.model.VideoMetaData
import com.project100pi.pivideoplayer.listeners.ItemDeleteListener
import com.project100pi.pivideoplayer.utils.ContextMenuUtil
import java.util.concurrent.TimeUnit


class DirectoryListViewModel(private val context: Context, application: Application): AndroidViewModel(application), ClickInterface {

    private val allFilePaths = mutableListOf<String>()
    private var foldersList = MutableLiveData<ArrayList<FolderInfo>>()
    val foldersListExposed: LiveData<ArrayList<FolderInfo>>
        get() = foldersList
    private var pathToIdInfo = HashMap<String, String>()
    private var foldersWithPathMap = HashMap<String, FolderInfo>()

    private val coroutineJob = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.IO + coroutineJob)

    //Mode to know whether folders are visible or songs of folder are visible
    var listViewMode = Constants.FOLDER_VIEW

    var currentSongFolderIndex = -1

    init {
        loadAllFolderData()
    }

    override fun onItemClicked(position: Int) {
        currentSongFolderIndex = position
        listViewMode = Constants.SONG_VIEW
    }

    override fun onBackFolderPressed() {
        currentSongFolderIndex = -1
        listViewMode = Constants.FOLDER_VIEW
    }

    override fun onItemLongClicked(position: Int): Boolean {
        return false
    }

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

    private fun loadAllFolderData() {
        foldersWithPathMap.clear()
        allFilePaths.clear()
        pathToIdInfo.clear()

        coroutineScope.launch {
            val cursor = CursorFactory.getAllVideoCursor(context)

            if (cursor != null && cursor.moveToFirst()) {
                // We populate something, only if the cursor is available
                do {
                    try {
                        //To get path of song
                        val path = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA))
                        //To get song id
                        val songId = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media._ID))
                        //To get song title
                        val songTitle = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.TITLE))
                        //To get song duration
                        val songDuration = cursor.getLong(cursor.getColumnIndex(MediaStore.Video.Media.DURATION))

                        if (FileExtension.isVideo(path)) {
                            if (path != null) {
                                allFilePaths.add(path)
                                pathToIdInfo[path] = songId // change to video id

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
                                val key = path.substring(0, path.length - pathsList[pathsList.size - 1].length)

                                val videoName = pathsList[pathsList.size - 1]
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

                                var seconds: Long = songDuration / 1000
                                var minutes: Long
                                var hours: Long

                                minutes = seconds / 60
                                seconds %= 60
                                hours = minutes / 60
                                minutes %= 60

                                val duration = if (hours > 0){
                                    "$hours:$minutes:$seconds"
                                } else {
                                    "$minutes:$seconds"
                                }

                                foldersWithPathMap[key]?.addSong(videoName, songId, duration)

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
                    //Converting hash map to arraylist which will be submitted to adapter
                    val list = ArrayList(foldersWithPathMap.values.sortedWith(compareBy { it.videoName }))
                    foldersList.value = list
                }
        }
    }

    fun getVideoMetaData(_id: String): VideoMetaData? {
        val cursor = CursorFactory.getVideoMetaDataById(context, _id)
        if (cursor != null && cursor.moveToFirst()) {
            return VideoMetaData(cursor.getInt(cursor.getColumnIndex(MediaStore.Video.Media._ID)),
                cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.TITLE)),
                cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA)))
        } else
            return null
    }

    fun shareMultipleVideos(selectedItemPosition: List<Int>) {
        coroutineScope.launch {

            val listOfVideoUris = ArrayList<Uri?>()
            for (position in selectedItemPosition) {
                for (video in foldersList.value!![position].songsList) {
                    listOfVideoUris.add(ContextMenuUtil.getVideoContentUri(context, File(video.path)))
                }
            }

            withContext(Dispatchers.Main) {
                context.startActivity(Intent.createChooser(Intent().setAction(Intent.ACTION_SEND_MULTIPLE)
                    .setType("video/*")
                    .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    .putExtra(Intent.EXTRA_STREAM,  listOfVideoUris), "Share Video"))
            }
        }
    }

    fun playMultipleVideos(selectedItemPosition: List<Int>) {
        try {

            coroutineScope.launch {
                val playerIntent = Intent(context, PlayerActivity::class.java)
                val metaDataList = ArrayList<VideoMetaData>()
                for(position in selectedItemPosition) {
//                    metaDataList.add(directoryListViewModel.getVideoMetaData(videoListData[directoryListViewModel.currentSongFolderIndex].songsList[selectedItemPosition].folderId)!!)
                    for (video in foldersList.value!![position].songsList) {
                        metaDataList.add(VideoMetaData(video.folderId.toInt(), video.videoName, video.path))
                    }
                }
                playerIntent.putExtra(Constants.QUEUE, metaDataList)
                withContext(Dispatchers.Main) {
                    context.startActivity(playerIntent)
                }
            }

        } catch (e: java.lang.Exception) {
            Logger.i(e.toString())
            Toast.makeText(context, "Failed to play with video.", Toast.LENGTH_SHORT).show()
        }
    }

    fun removeElementAt(position: Int) {
        foldersList.value!![currentSongFolderIndex].songsList.removeAt(position)
    }

}