package com.project100pi.pivideoplayer.ui.activity.viewmodel

import android.content.Context
import android.content.Intent
import android.database.SQLException
import android.net.Uri
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.project100pi.library.misc.Logger
import com.project100pi.library.model.VideoMetaData
import com.project100pi.pivideoplayer.database.CursorFactory
import com.project100pi.pivideoplayer.database.TinyDB
import com.project100pi.pivideoplayer.listeners.ItemDeleteListener
import com.project100pi.pivideoplayer.model.FolderInfo
import com.project100pi.pivideoplayer.model.observable.VideoChangeObservable
import com.project100pi.pivideoplayer.ui.activity.PlayerActivity
import com.project100pi.pivideoplayer.utils.Constants
import com.project100pi.pivideoplayer.utils.ContextMenuUtil
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList
import kotlin.system.measureTimeMillis


class DirectoryListViewModel(private val context: Context,
                             private val itemDeleteListener: ItemDeleteListener): ViewModel(), Observer {

    private var _foldersList = MutableLiveData<MutableList<FolderInfo>>()
    val foldersList: LiveData<MutableList<FolderInfo>>
        get() = _foldersList

    private var foldersWithPathMap = HashMap<String, FolderInfo>()

    /**
     * Allows to cancel all assigned jobs for this ViewModel
     */
    private var coroutineJob = Job()
    /**
     * we are pass [viewModelJob], any coroutine started in this coroutineScope can be cancelled
     * by calling viewModelJob.cancel()
     */
    private var coroutineScope = CoroutineScope(Dispatchers.IO + coroutineJob)

    /**
     * init block sets observer for video change which
     * will get fired when a new video is added when app is active
     * it also loads folder data
     */

    init {
        // When a new video is added (eg: video download finished in background) and
        // when the app is open then this observer will get triggered
        observeForVideoChange()
        loadAllFolderData()
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

    fun deleteFolderContents(listOfIndexes: List<Int>) {
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

                val videoList = _foldersList.value!![position].videoInfoList

                // We cannot delete whole folder so we have to delete each video from the folder
                for (path in videoList) {
                    val file = File(path.videoPath)

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
     * This method loads all videos available in device from media
     * database and forms a list of folder which again have a list
     * of videos
     */

    private fun loadAllFolderData() {

        foldersWithPathMap.clear()



        coroutineScope.launch {
//            withContext(Dispatchers.Main){
//                _foldersList.value=null
//            }
            val cursor = CursorFactory.getAllVideoCursor(context)

            if (cursor != null && cursor.moveToFirst()) {
                // We populate something, only if the cursor is available
                do {
                    try {
                        // To get path of video
                        val videoPath = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA))
                        // To get video id
                        val videoId = cursor.getInt(cursor.getColumnIndex(MediaStore.Video.Media._ID))
                        // To get video duration
                        val durationInMs = cursor.getLong(cursor.getColumnIndex(MediaStore.Video.Media.DURATION))

                        if (videoPath != null) {

                            // Splitting song path to list by using .split("/") to get elements from video path separated
                            // /storage/emulated/music/abc.mp3
                            // -> emulated will be folder name
                            // -> music will be subfolder name
                            // -> abc will be song name

                            val pathsList = videoPath.split("/")

                            // Getting folder name from path
                            // /storage/emulated/music/abc.mp3 -> music will be folder name
                            val folderName = pathsList[pathsList.size - 2]

                            // "key" Contains path of folder (excluding song name)
                            // Complete Path length - Song Name Length
                            // Video Name Length = pathsList.get(pathsList.size-1).length
                            val key = videoPath.substring(0, videoPath.length - pathsList[pathsList.size - 1].length)

                            val videoName = pathsList[pathsList.size - 1]

                            val dateAdded = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED))

                            val calendar = Calendar.getInstance()
                            // Multiply by 1000 as the date added is in seconds
                            val date = Date(dateAdded.toLong()*1000)
                            calendar.time = date

                             val months = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sept", "Oct", "Nov", "Dec")

                             val dateString = "${calendar.get(Calendar.DAY_OF_MONTH)} ${months[calendar.get(Calendar.MONTH)]}"

                            //If folder is not already present in the hashmap
                            if (!foldersWithPathMap.containsKey(key)) {
                                foldersWithPathMap[key] = FolderInfo(
                                    folderName,
                                    videoPath.substring(
                                        0,
                                        videoPath.length - pathsList[pathsList.size - 1].length
                                    )
                                )
                            }

                            foldersWithPathMap[key]?.addVideoInfoToList(videoId, videoName, videoPath, durationInMs, dateString)

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
            withContext(Dispatchers.Main)
            {
                //Converting hash map to array list which will be submitted to adapter
                val list = ArrayList(foldersWithPathMap.values.sortedWith(compareBy { it.folderName }))
                _foldersList.value = list
            }
        }

    }

    /**
     * This method fetch video meta data based on videos _id
     *
     * @param _id Int
     * @return VideoMetaData?
     */

    fun getVideoMetaData(_id: Int): VideoMetaData? {
        val cursor = CursorFactory.getVideoMetaDataById(context, _id)
        return if (cursor != null && cursor.moveToFirst()) {
            VideoMetaData(cursor.getInt(cursor.getColumnIndex(MediaStore.Video.Media._ID)),
                cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.TITLE)),
                cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA)))
        } else
            null
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
                // Loop through list of folders
                for (video in foldersList.value!![position].videoInfoList) {
                    // Loop through list of videos inside folder
                    listOfVideoUris.add(ContextMenuUtil.getVideoContentUri(context, File(video.videoPath)))
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

    /**
     * This method plays all the videos
     * from selected folders
     * NOTE: user can multi select folders
     *
     * @param selectedItemPosition List<Int>
     */

    fun playMultipleVideos(selectedItemPosition: List<Int>) {
        coroutineScope.launch {
            val metaDataList = ArrayList<VideoMetaData>()
            for(position in selectedItemPosition) {
//              metaDataList.add(directoryListViewModel
//              .getVideoMetaData(videoListData[directoryListViewModel
//              .currentSongFolderIndex].songsList[selectedItemPosition].folderId)!!)
                for (video in foldersList.value!![position].videoInfoList) {
                    metaDataList.add(VideoMetaData(video._Id, video.videoName, video.videoPath))
                }
            }
            withContext(Dispatchers.Main) {
                PlayerActivity.start(context, metaDataList)
            }
        }
    }

    /**
     * removes items from internal list
     *
     * @param position Int
     */

    fun removeElementAt(position: Int) {
        foldersList.value?.removeAt(position)
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

    override fun update(p0: Observable?, p1: Any?) {

        loadAllFolderData()
    }

}