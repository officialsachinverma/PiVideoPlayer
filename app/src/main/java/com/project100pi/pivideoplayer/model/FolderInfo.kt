package com.project100pi.pivideoplayer.model

/**
 * Created by Sachin Verma on 2019-11-20.
 *
 * This class holds information about a folder
 * which contains videos. the information of those videos are getting
 * stored in videoFilesInfoList
 *
 * @since v1
 */

data class FolderInfo(var folderName: String = "",
                      var folderPath: String = ""){

    /**
     * This list holds information about videos under this folder
     */
    val videoInfoList: ArrayList<VideoTrackInfo> = ArrayList()

    /**
     * Adds a new information to the videoFilesInfoList
     *
     * @param videoId Int Stores value of _id in media database
     * @param videoName String Stores video name
     * @param videoPath String Stores video path
     * @param durationInMs Long Stores video duration in millis
     */
    fun addVideoInfoToList(videoId: Int, videoName: String, videoPath: String, durationInMs: Long) {
        videoInfoList.add(VideoTrackInfo(videoId, videoName, videoPath, durationInMs))

    }

}