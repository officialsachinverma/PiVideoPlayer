package com.project100pi.pivideoplayer.model

/**
 * Created by Sachin Verma on 2019-11-20.
 */

/**
 * this class holds information about a video
 *
 * @param _Id Int Stores value of _id in media database
 * @param videoName String Stores video name
 * @param videoPath String Stores video path
 * @param durationInMs Long Stores video duration in millis
 * @constructor
 */
data class VideoTrackInfo(var _Id: Int = -1,
                          var videoName: String = "",
                          var videoPath: String = "",
                          var durationInMs: Long = 0,
                          var dateAdded: String = "")