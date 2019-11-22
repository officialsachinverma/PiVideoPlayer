package com.project100pi.pivideoplayer.ui.activity.viewmodel

import android.annotation.TargetApi
import android.app.Application
import android.content.Context
import android.content.Intent
import android.database.SQLException
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.project100pi.library.model.VideoMetaData
import com.project100pi.pivideoplayer.database.CursorFactory
import com.project100pi.pivideoplayer.listeners.ItemDeleteListener
import com.project100pi.pivideoplayer.model.FolderInfo
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


class DirectoryListViewModel(private val context: Context, application: Application):
    AndroidViewModel(application),
    Observer {

    private var _foldersList = MutableLiveData<ArrayList<FolderInfo>>()
    val foldersList: LiveData<ArrayList<FolderInfo>>
        get() = _foldersList

    private var foldersWithPathMap = HashMap<String, FolderInfo>()

    private val coroutineJob = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.IO + coroutineJob)

    init {
        // When a new video is added (eg: video download finished in background) and
        // when the app is open then this observer will get triggered
        observeForVideoChange()
        loadAllFolderData()
    }

    private fun observeForVideoChange() {
        VideoChangeObservable.addObserver(this)
    }


    fun deleteFolderContents(listOfIndexes: List<Int>, listener: ItemDeleteListener) {
        var errorOccurred = false
        coroutineScope.launch {
            for (position in listOfIndexes) {
                try {
                    val folder = foldersList.value!![position].folderPath
                    val file = File(folder)
                    if(file.exists()) {
                        if (file.delete()) {
                            context.applicationContext.sendBroadcast(
                                Intent(
                                    Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                                    Uri.fromFile(file)
                                )
                            )
                            errorOccurred = false
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
                                            errorOccurred = false
                                        }
                                    }
                                }
                            }
                            // checking if file is in sd card
                            val fileInSdcard = File(context.externalCacheDir, folder)
                            if (fileInSdcard.exists()) {
                                val targetDocument = getDocumentFile(fileInSdcard)
                                if (targetDocument?.delete() == true) {
                                    context.applicationContext.sendBroadcast(
                                        Intent(
                                            Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                                            Uri.fromFile(fileInSdcard)
                                        )
                                    )
                                    errorOccurred = false
                                }
                            }
                        }
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    errorOccurred = true
                    withContext(Dispatchers.Main) {
                        listener.onDeleteError()
                    }
                }
            }
            if (!errorOccurred) {
                withContext(Dispatchers.Main) {
                    listener.onDeleteSuccess(listOfIndexes)
                }
            }
        }
    }

    private fun loadAllFolderData() {
        foldersWithPathMap.clear()

        coroutineScope.launch {
            val cursor = CursorFactory.getAllVideoCursor(context)

            if (cursor != null && cursor.moveToFirst()) {
                // We populate something, only if the cursor is available
                do {
                    try {
                        //To get path of song
                        val path = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA))
                        //To get song id
                        val songId = cursor.getInt(cursor.getColumnIndex(MediaStore.Video.Media._ID))
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
                                        )
                                    )
                                }

                                foldersWithPathMap[key]?.addFile(songId, videoName, path, songDuration)

                            } else
                                continue
                        }

                    } catch (e: SQLException) {
                        e.printStackTrace()
                    }
                } while (cursor.moveToNext())
                cursor.close()
            }
            withContext(Dispatchers.Main)
                {
                    //Converting hash map to arraylist which will be submitted to adapter
                    val list = ArrayList(foldersWithPathMap.values.sortedWith(compareBy { it.folderName }))
                    _foldersList.value = list
                }
        }
    }

    fun getVideoMetaData(_id: Int): VideoMetaData? {
        val cursor = CursorFactory.getVideoMetaDataById(context, _id)
        return if (cursor != null && cursor.moveToFirst()) {
            VideoMetaData(cursor.getInt(cursor.getColumnIndex(MediaStore.Video.Media._ID)),
                cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.TITLE)),
                cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA)))
        } else
            null
    }

    fun shareMultipleVideos(selectedItemPosition: List<Int>) {
        coroutineScope.launch {

            val listOfVideoUris = ArrayList<Uri?>()
            for (position in selectedItemPosition) {
                for (video in foldersList.value!![position].filesList) {
                    listOfVideoUris.add(ContextMenuUtil.getVideoContentUri(context, File(video.filePath)))
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
        coroutineScope.launch {
            val playerIntent = Intent(context, PlayerActivity::class.java)
            val metaDataList = ArrayList<VideoMetaData>()
            for(position in selectedItemPosition) {
//                    metaDataList.add(directoryListViewModel.getVideoMetaData(videoListData[directoryListViewModel.currentSongFolderIndex].songsList[selectedItemPosition].folderId)!!)
                for (video in foldersList.value!![position].filesList) {
                    metaDataList.add(VideoMetaData(video._Id, video.fileName, video.filePath))
                }
            }
            playerIntent.putExtra(Constants.QUEUE, metaDataList)
            withContext(Dispatchers.Main) {
                context.startActivity(playerIntent)
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

    fun removeElementAt(position: Int) {
        foldersList.value?.removeAt(position)
    }

    override fun onCleared() {
        super.onCleared()
        coroutineJob.cancel()
    }

    override fun update(p0: Observable?, p1: Any?) {
        loadAllFolderData()
    }

}