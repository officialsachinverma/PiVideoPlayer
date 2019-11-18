package com.project100pi.pivideoplayer.activity

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import com.project100pi.library.misc.Logger
import com.project100pi.library.model.VideoMetaData
import com.project100pi.pivideoplayer.listeners.ItemDeleteListener
import com.project100pi.pivideoplayer.model.FolderInfo
import com.project100pi.pivideoplayer.utils.Constants
import com.project100pi.pivideoplayer.utils.ContextMenuUtil
import kotlinx.coroutines.*
import java.io.File

class VideoListViewModel (private val context: Context, private val videoList: ArrayList<FolderInfo>, application: Application): AndroidViewModel(application) {

    private val coroutineJob = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.IO + coroutineJob)

    fun delete(listOfIndexes: List<Int>, listener: ItemDeleteListener) {
        coroutineScope.launch {

            for (position in listOfIndexes) {
                try {
                    val folder = videoList[position]
                    val file = File(folder.path)
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
                for (video in videoList[position].songsList) {
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
                    for (video in videoList[position].songsList) {
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

}