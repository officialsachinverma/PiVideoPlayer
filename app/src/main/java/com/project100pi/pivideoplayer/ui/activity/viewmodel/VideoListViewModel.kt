package com.project100pi.pivideoplayer.ui.activity.viewmodel

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
import com.project100pi.pivideoplayer.ui.activity.PlayerActivity
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

    private var _filesList = MutableLiveData<ArrayList<FileInfo>>()
    val filesList: LiveData<ArrayList<FileInfo>>
        get() = _filesList

    private val videoList = arrayListOf<FileInfo>()

    private val coroutineJob = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.IO + coroutineJob)

    init {
        loadAllData()
        // When a new video is added (eg: video download finished in background) and
        // when the app is open then this observer will get triggered
        observeForVideoChange()
    }

    private fun observeForVideoChange() {
        VideoChangeObservable.addObserver(this)
    }

    fun deleteVideo(listOfIndexes: List<Int>, listener: ItemDeleteListener) {
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
                    metaDataList.add(VideoMetaData(videoList[position]._Id, videoList[position].fileName, videoList[position].filePath))
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
                            val videoPath = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA))
                            //To get song id
                            val videoId = cursor.getInt(cursor.getColumnIndex(MediaStore.Video.Media._ID))
                            //To get song title
                            val videoTitle = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.TITLE))
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

                                    videoList.add(
                                        FileInfo(
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
            }
            withContext(Dispatchers.Main) {
                _filesList.value = videoList
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        coroutineJob.cancel()
    }

    override fun update(p0: Observable?, p1: Any?) {
        loadAllData()
    }

}