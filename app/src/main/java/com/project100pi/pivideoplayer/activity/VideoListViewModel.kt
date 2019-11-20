package com.project100pi.pivideoplayer.activity

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.project100pi.library.misc.Logger
import com.project100pi.library.model.VideoMetaData
import com.project100pi.pivideoplayer.database.CursorFactory
import com.project100pi.pivideoplayer.listeners.ItemDeleteListener
import com.project100pi.pivideoplayer.model.FileInfo
import com.project100pi.pivideoplayer.model.observable.VideoChangeObservable
import com.project100pi.pivideoplayer.utils.Constants
import com.project100pi.pivideoplayer.utils.ContextMenuUtil
import com.project100pi.pivideoplayer.utils.FileExtension
import kotlinx.coroutines.*
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

class VideoListViewModel (private val context: Context,
                          private val folderPath: String,
                          application: Application):
    AndroidViewModel(application), Observer {

    private var filesList = MutableLiveData<ArrayList<FileInfo>>()
    val filesListExposed: LiveData<ArrayList<FileInfo>>
        get() = filesList

    private val videoList = arrayListOf<FileInfo>()

    private val coroutineJob = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.IO + coroutineJob)

    init {
        loadAllData()
        observeForVideoChange()
    }

    private fun observeForVideoChange() {
        VideoChangeObservable.addObserver(this)
    }

    fun delete(listOfIndexes: List<Int>, listener: ItemDeleteListener) {
        coroutineScope.launch {

            for (position in listOfIndexes) {
                try {
                    val folder = videoList[position]
                    val file = File(folder.filePath)
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

    override fun onCleared() {
        super.onCleared()
        coroutineJob.cancel()
    }

    fun removeElementAt(position: Int) {
        videoList.removeAt(position)
    }

    fun shareMultipleVideos(selectedItemPosition: List<Int>) {
        coroutineScope.launch {

            val listOfVideoUris = ArrayList<Uri?>()
            for (position in selectedItemPosition) {
                listOfVideoUris.add(ContextMenuUtil.getVideoContentUri(context, File(videoList[position].filePath)))
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
                    metaDataList.add(VideoMetaData(videoList[position]._Id.toInt(), videoList[position].fileName, videoList[position].filePath))
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

    private fun loadAllData() {
        coroutineScope.launch {
            videoList.clear()
            val files = File(folderPath).listFiles()
            for (file in files) {
                val cursor = CursorFactory.getVideoMetaDataByPath(context, file.absolutePath)
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

                                    videoList.add(
                                        FileInfo(
                                            songId,
                                            videoName,
                                            path,
                                            songDuration))

                                } else
                                    continue
                            }

                        } catch (e: Exception) { // catch specific exception
                            e.printStackTrace()
                        }
                    } while (cursor.moveToNext())
                    cursor.close()
                }
            }
            withContext(Dispatchers.Main) {
                filesList.value = videoList
            }
        }
    }

    override fun update(p0: Observable?, p1: Any?) {
        loadAllData()
    }

}