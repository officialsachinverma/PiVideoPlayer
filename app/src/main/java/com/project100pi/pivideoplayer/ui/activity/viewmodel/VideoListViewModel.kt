package com.project100pi.pivideoplayer.ui.activity.viewmodel

import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.project100pi.library.misc.Logger
import com.project100pi.library.model.VideoMetaData
import com.project100pi.pivideoplayer.database.CursorFactory
import com.project100pi.pivideoplayer.database.TinyDB
import com.project100pi.pivideoplayer.listeners.ItemDeleteListener
import com.project100pi.pivideoplayer.model.VideoTrackInfo
import com.project100pi.pivideoplayer.model.observable.VideoChangeObservable
import com.project100pi.pivideoplayer.ui.activity.PlayerActivity
import com.project100pi.pivideoplayer.utils.Constants
import com.project100pi.pivideoplayer.utils.ContextMenuUtil
import com.project100pi.pivideoplayer.utils.FileExtension
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList


class VideoListViewModel (private val context: Context,
                          private val folderPath: String,
                          application: Application):
    AndroidViewModel(application), Observer {

    private var _filesList = MutableLiveData<ArrayList<VideoTrackInfo>>()
    val filesList: LiveData<ArrayList<VideoTrackInfo>>
        get() = _filesList

    private val videoList = arrayListOf<VideoTrackInfo>()

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
                    val folder = _filesList.value!![position].videoPath
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

    fun removeElementAt(position: Int) {
        videoList.removeAt(position)
    }

    fun shareMultipleVideos(selectedItemPosition: List<Int>) {
        coroutineScope.launch {

            val listOfVideoUris = ArrayList<Uri?>()
            for (position in selectedItemPosition) {
                listOfVideoUris.add(ContextMenuUtil.getVideoContentUri(context, File(videoList[position].videoPath)))
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
                    metaDataList.add(VideoMetaData(videoList[position]._Id, videoList[position].videoName, videoList[position].videoPath))
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
            val cursor = CursorFactory.getVideoMetaDataByPath(context, folderPath)
            if (cursor != null && cursor.moveToFirst()) {
                // We populate something, only if the cursor is available
                do {
                    try {
                        // To get path of song
                        val videoPath = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA))
                        //To get song id
                        val videoId = cursor.getInt(cursor.getColumnIndex(MediaStore.Video.Media._ID))
                        // To get song title
                        // Not using title as of now because it's been observed
                        // that for some videos title is not corresponding to the file name
                        // Eg.: there was a video provided by udemy
                        // title of that video was "Udemy Tutorial video"
                        // file name was something "1. Introduction to APIs"
                        // so as of now we are using last path segment as video title (for UI only)
                        val videoTitle = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.TITLE))
                        // To get song duration
                        val videoDuration = cursor.getLong(cursor.getColumnIndex(MediaStore.Video.Media.DURATION))

                        if (FileExtension.isVideo(videoPath)) {
                            if (videoPath != null) {

                                // Splitting song path to list by using .split("/") to get elements from song path separated
                                // /storage/emulated/music/abc.mp3
                                // -> music will be folder name
                                // -> emulated will be subfolder name
                                // -> abc will be song name

                                val pathsList = videoPath.split("/")

                                val videoName = pathsList[pathsList.size - 1]

                                videoList.add(
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
                        Logger.e(e.message.toString())
                    }
                } while (cursor.moveToNext())
                cursor.close()
            }
            withContext(Dispatchers.Main) {
                _filesList.value = videoList
            }
        }
    }

    /*
     * The following method getDocumentFile will get the DocumentFile for the given File
     * if TinyDB has the TreeURI for the root folder of the SD Card.
     * If the TreeURI for the root folder fo the SD Card is not there, this will return NULL.
     */
    private fun getDocumentFile(file: File): DocumentFile? {
        val baseFolder: String = getExtSdCardFolder(file) ?: return null
        var relativePath: String? = null
        relativePath = try {
            val fullPath = file.canonicalPath
            fullPath.substring(baseFolder.length + 1)
        } catch (e: IOException) {
            return null
        }
//        val treeUri = Uri.parse(TinyDBHelper.getInstance().getURIForSDCard()) ?: return null
        // start with root of SD card and then parse through document tree.
        var document = DocumentFile.fromFile(file)
        val parts = relativePath.split("\\/").toTypedArray()
        for (i in parts.indices) {
            document = document.findFile(parts[i])!!
            if (document == null) break
        }
        return document
    }

    private fun getExtSdCardFolder(file: File): String? {
        val extSdPaths: Array<String> = getExtSdCardPaths()
        try {
            for (i in extSdPaths.indices) {
                if (file.canonicalPath.startsWith(extSdPaths[i])) {
                    return extSdPaths[i]
                }
            }
        } catch (e: IOException) {
            return null
        }
        return null
    }

    /**
     * Get a list of external SD card paths. (Kitkat or higher.)
     *
     * @return A list of external SD card paths.
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private fun getExtSdCardPaths(): Array<String> {
        val paths = arrayListOf<String>()
        for (file in context.getExternalFilesDirs("external")) {
            if (file != null && file != context.getExternalFilesDir("external")) {
                val index = file.absolutePath.lastIndexOf("/Android/data")
                if (index < 0) { //Log.w(Application.TAG, "Unexpected external file dir: " + file.getAbsolutePath());
                } else {
                    var path = file.absolutePath.substring(0, index)
                    try {
                        path = File(path).canonicalPath
                    } catch (e: IOException) { // Keep non-canonical path.
                    }
                    paths.add(path)
                }
            }
        }
        return paths.toTypedArray()
    }

    override fun onCleared() {
        super.onCleared()
        coroutineJob.cancel()
    }

    override fun update(p0: Observable?, p1: Any?) {
        loadAllData()
    }

}