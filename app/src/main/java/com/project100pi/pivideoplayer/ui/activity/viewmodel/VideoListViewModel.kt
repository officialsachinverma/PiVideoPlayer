package com.project100pi.pivideoplayer.ui.activity.viewmodel

import android.content.Context
import android.content.Intent
import android.database.SQLException
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.project100pi.library.misc.Logger
import com.project100pi.library.model.VideoMetaData
import com.project100pi.pivideoplayer.database.CursorFactory
import com.project100pi.pivideoplayer.database.TinyDB
import com.project100pi.pivideoplayer.listeners.ItemDeleteListener
import com.project100pi.pivideoplayer.model.VideoTrackInfo
import com.project100pi.pivideoplayer.model.observable.OnMediaChangeContentObserver
import com.project100pi.pivideoplayer.model.observable.VideoChangeObservable
import com.project100pi.pivideoplayer.ui.activity.PlayerActivity
import com.project100pi.pivideoplayer.utils.Constants
import com.project100pi.pivideoplayer.utils.ContextMenuUtil
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.system.measureTimeMillis


class VideoListViewModel (private val context: Context,
                          private val folderPath: String,
                          private val folderName: String,
                          private val itemDeleteListener: ItemDeleteListener): ViewModel(), Observer {

    private var _filesList = MutableLiveData<MutableList<VideoTrackInfo>>()
    val filesList: LiveData<MutableList<VideoTrackInfo>>
        get() = _filesList

    /**
     * Allows to cancel all assigned jobs for this ViewModel
     */
    private val coroutineJob = Job()
    /**
     * we are pass [viewModelJob], any coroutine started in this coroutineScope can be cancelled
     * by calling viewModelJob.cancel()
     */
    private val coroutineScope = CoroutineScope(Dispatchers.IO + coroutineJob)

    /**
     * init block sets observer for video change which
     * will get fired when a new video is added when app is active
     * it also loads folder data
     */

    init {
        // When a new video is added (eg: video download finished in background) and
        // when the app is open then this observer will get triggered
        observeForVideoChange()
        loadAllVideoData()
    }

    /**
     * This method observe for video change
     * Eg.: if a new video gets added when app is running
     * like user downloaded a new video and download gets
     * completed when the application is open
     */

    private fun observeForVideoChange() {
        VideoChangeObservable.addObserver(this)
    }

    /**
     * This method takes list of indices which are supposed to be deleted
     * and it takes ItemDeleteListener which will be called when either
     * the operation is successful or not
     *
     * @param listOfIndexes List<Int>
     */

    fun deleteVideo(listOfIndexes: List<Int>) {

        coroutineScope.launch {
            when (delete(listOfIndexes)) {
                Constants.Delete.SUCCESS -> {
                    onDeleteSuccess(listOfIndexes)
                    Logger.i("Delete --> SUCCESS")
                }
                Constants.Delete.FAILURE -> {
                    onDeleteError()
                    Logger.i("Delete --> FAILURE")
                }
                Constants.Delete.SD_CARD_PERMISSION_REQUIRED -> {
                    // Ask for SD card permission
                    itemDeleteListener.showPermissionForSdCard()
                    Logger.i("Delete --> SD_CARD_PERMISSION_REQUIRED")
                }
            }
        }

    }

    /**
     * This method called when deletion operation
     * executes successfully for provided list
     * of indices
     *
     * @param listOfIndexes List<Int>
     */
    private suspend fun onDeleteSuccess(listOfIndexes: List<Int>) {
        Logger.i("Delete --> onDeleteSuccess")
        withContext(Dispatchers.Main) {
            itemDeleteListener.onDeleteSuccess(listOfIndexes)
        }
    }

    /**
     * This method called when any error occurs
     * while executing the delete operation
     */
    private suspend fun onDeleteError() {
        Logger.i("Delete --> onDeleteError")
        withContext(Dispatchers.Main) {
            itemDeleteListener.onDeleteError()
        }
    }

    /**
     * This method called when delete operation is
     * successful and we need to tell android system
     * to scan for media files and update the media db
     *
     * @param fileUri Uri
     */
    private fun sendDeleteBroadcast(fileUri: Uri) {
        context.applicationContext.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, fileUri))
    }

    /**
     * This method executes delete operation
     *
     * @param listOfIndexes List<Int>
     *
     * @return Int The status of delete operation (Success/Failure/Need SD Card Permission)
     */
    private fun delete(listOfIndexes: List<Int>): Int {
        Logger.i("Delete --> initiated")
        for (position in listOfIndexes) {

            try {

                val folder = _filesList.value!![position].videoPath
//                val folder = videoList[position].videoPath

                val file = File(folder)

                if(file.exists()) {

                    if (file.delete()) {

                        // If file is deleted successfully then we
                        // have to tell android system to re scan all media files
                        // tp update it's database. After scanning android will get
                        // get to know that some files are missing/deleted/moved/added
                        // then it will accordingly add or remove media files records
                        sendDeleteBroadcast(Uri.fromFile(file))
                        Logger.i("Delete --> sendDeleteBroadcast")

                    } else {

                        // If still file exists then file is in external sd card
                        if (file.exists()) {
                            // checking for file is in sd card and sdcard uri
                            if (TinyDB.getString(Constants.ExternalSDCard.SD_CARD_URI).isNullOrEmpty()) {

                                return Constants.Delete.SD_CARD_PERMISSION_REQUIRED

                            } else {

                                if (!deleteForSdCard(file)){
                                    Logger.i("Delete --> FAILURE")
                                    return Constants.Delete.FAILURE
                                }
                            }
                        }
                    }
                }
            } catch (e: IOException) {

                e.printStackTrace()

                Logger.e("Delete --> " + e.message.toString())

                return Constants.Delete.FAILURE

            }
        }

        return Constants.Delete.SUCCESS
    }

    /**
     *
     * Deletes a file which is in SDCard
     *
     * @param file File
     * @return Boolean
     */
    private fun deleteForSdCard(file: File): Boolean {
        val sdCardUri = TinyDB.getString(Constants.ExternalSDCard.SD_CARD_URI)

        var documentFile = DocumentFile.fromTreeUri(context, Uri.parse(sdCardUri))

        if (file.exists() && sdCardUri!!.isNotEmpty()) {

            val parts: List<String> = file.path.split("/")

            // findFile method will search documentFile for the first file
            // with the expected `DisplayName`

            // We skip first three items because we are already on it.(sdCardUri = /storage/extSdCard)
            for (strs in parts.subList(3, parts.size)) {

                if (documentFile != null)
                    documentFile = documentFile.findFile(strs)

            }
            if (documentFile != null) {

                if (documentFile.delete()) {

                    // If file is deleted successfully then we
                    // have to tell android system to re scan all media files
                    // tp update it's database. After scanning android will get
                    // get to know that some files are missing/deleted/moved/added
                    // then it will accordingly add or remove media files records
                    sendDeleteBroadcast(Uri.fromFile(file))
                    Logger.i("Delete --> sendDeleteBroadcast")

                } else
                    return false

            } else
                return false
        }
        return true
    }

    /**
     * This method loads all videos available in device from media
     * database and forms a list of folder which again have a list
     * of videos
     */

    private fun loadAllVideoData() {
        val videoList = arrayListOf<VideoTrackInfo>()
        //videoList.clear()

        coroutineScope.launch {
            val cursor = CursorFactory.getVideoMetaDataByPath(context, folderName)
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
//                            val videoTitle = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.TITLE))
                        // To get song duration
                        val videoDuration = cursor.getLong(cursor.getColumnIndex(MediaStore.Video.Media.DURATION))

                        val dateAdded = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED))

                        val calendar = Calendar.getInstance()
                        // Multiply by 1000 as the date added is in seconds
                        val date = Date(dateAdded.toLong()*1000)
                        calendar.time = date

                        val months = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sept", "Oct", "Nov", "Dec")

                        val dateString = "${calendar.get(Calendar.DAY_OF_MONTH)} ${months[calendar.get(Calendar.MONTH)]}"

                        if (videoPath != null) {

                            // Splitting song path to list by using .split("/") to get elements from song path separated
                            // /storage/emulated/video/abc.wav
                            // -> music will be folder name
                            // -> emulated will be subfolder name
                            // -> abc will be song name

                            val pathsList = videoPath.split("/")

                            val folderPath = folderPath.split("/")

                            val videoName = pathsList[pathsList.size - 1]

                            // checking if sub-folder of the folder on which user has clicked
                            // and sub-folder of the current cursor position item is same
//                                 Eg.: /storage/emulated/video/downloaded/abc.wav and
//                                 /storage/emulated/video/xyz.wav now the folder in which abc.wav is,
//                                 is a sub folder of video and xyz.wav is in folder video
//                                 which is a sub folder of emulated
//                                 so cursor will give both files but we want the one which is in folder video
//                                 but not in the subfolder of folder video
                            if (pathsList[pathsList.size - 2] == folderPath[folderPath.size - 2]
                                && pathsList[pathsList.size - 2] == folderName
                                && folderPath[folderPath.size - 2] == folderName) {
                                videoList.add(
                                    VideoTrackInfo(
                                        videoId,
                                        videoName,
                                        videoPath,
                                        videoDuration,
                                        dateString))
                            }

                        } else
                            continue

                    } catch (e: SQLException) {
                        e.printStackTrace()
                        Logger.e(e.message.toString())
                    } catch (e: ArrayIndexOutOfBoundsException) {
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

    /**
     * This method creates an intent chooser to share
     * the selected videos
     *
     * @param selectedItemPosition List<Int>
     */

    fun shareMultipleVideos(selectedItemPosition: List<Int>) {
        coroutineScope.launch {

            val listOfVideoUris = ArrayList<Uri?>()
            for (position in selectedItemPosition) {
                listOfVideoUris.add(ContextMenuUtil.getVideoContentUri(context, File(filesList.value!![position].videoPath)))
            }

            withContext(Dispatchers.Main) {
                context.startActivity(Intent.createChooser(Intent().setAction(Intent.ACTION_SEND_MULTIPLE)
                    .setType("video/*")
                    .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    .putExtra(Intent.EXTRA_STREAM,  listOfVideoUris), "Share Video"))
            }
        }
    }

    /**
     * removes items from internal list
     *
     * @param position Int
     */

    fun removeElementAt(position: Int):Boolean {
        if (position < filesList.value!!.size)
        {
            filesList.value!!.removeAt(position)
            return true
        }
        return false
    }

    /**
     * Canceling all the jobs when view model
     * is not active anymore
     */

    override fun onCleared() {
        super.onCleared()
        coroutineJob.cancel()
    }

    /**
     * When ever Video change observable gets
     * changed then this method will be called
     * If data is changed then we have to load
     * all data again
     *
     * @param p0 Observable
     * @param p1 Any
     */

    override fun update(observable: Observable?, p1: Any?) {
        Logger.i("update --> actually going to refresh list")
        if(observable is VideoChangeObservable)
        loadAllVideoData()
    }

}