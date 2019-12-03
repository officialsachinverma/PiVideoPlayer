package com.project100pi.pivideoplayer.ui.activity.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.project100pi.library.misc.Logger
import com.project100pi.pivideoplayer.database.CursorFactory
import com.project100pi.pivideoplayer.database.TinyDB
import com.project100pi.pivideoplayer.listeners.ItemDeleteListener
import com.project100pi.pivideoplayer.model.VideoTrackInfo
import com.project100pi.pivideoplayer.utils.Constants
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException

class SearchViewModel(private val context: Context,
                      private val itemDeleteListener: ItemDeleteListener): ViewModel() {

    private var _searchResultList = MutableLiveData<ArrayList<VideoTrackInfo>>()
    val searchResultList: LiveData<ArrayList<VideoTrackInfo>>
        get() = _searchResultList

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
     * This method takes list of indices which are supposed to be deleted
     * and it takes ItemDeleteListener which will be called when either
     * the operation is successful or not
     *
     * @param listOfIndexes List<Int>
     */

    fun deleteSearchedVideos(listOfIndexes: List<Int>) {

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
        withContext(Dispatchers.Main) {
            itemDeleteListener.onDeleteSuccess(listOfIndexes)
        }
    }

    /**
     * This method called when any error occurs
     * while executing the delete operation
     */
    private suspend fun onDeleteError() {
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
        context.applicationContext.sendBroadcast(
            Intent(
                Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                fileUri
            )
        )
    }

    /**
     * This method executes delete operation
     *
     * @param listOfIndexes List<Int>
     *
     * @return Int The status of delete operation (Success/Failure/Need SD Card Permission)
     */
    private fun delete(listOfIndexes: List<Int>): Int {
        for (position in listOfIndexes) {

            try {

                val folder = _searchResultList.value!![position].videoPath

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
     * This method will be called when user will start entering chars
     * in search field. We are fetching data based on entered text and
     * generating search results.
     *
     * @param queryText String
     */

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

    /**
     * Canceling all the jobs when view model
     * is not active anymore
     */

    override fun onCleared() {
        super.onCleared()
        coroutineJob.cancel()
    }

    /**
     * removes items from internal list
     *
     * @param position Int
     */

    fun removeElementAt(position: Int) {
        _searchResultList.value!!.removeAt(position)
    }

}