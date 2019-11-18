package com.project100pi.library.ui

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Point
import android.media.AudioManager
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.util.AttributeSet
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerControlView
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.ui.spherical.SingleTapListener
import com.project100pi.library.R
import com.project100pi.library.dialogs.CurrentPlayingQueueDialog
import com.project100pi.library.dialogs.SRTFilePicker
import com.project100pi.library.dialogs.listeners.OnItemClickListener
import com.project100pi.library.dialogs.listeners.SRTFilePickerClickListener
import com.project100pi.library.misc.CountDown
import com.project100pi.library.misc.CurrentSettings
import com.project100pi.library.misc.Logger
import com.project100pi.library.model.VideoMetaData
import com.project100pi.library.player.PiVideoPlayer

class PiVideoPlayerView: FrameLayout, SRTFilePickerClickListener, OnItemClickListener {

    private val playerView: PlayerView
    private var controlView: PlayerControlView
    private val toolbar: View
    private val backButton: ImageView
    private val toolbarTitle: TextView
    private val toolbarSubtitle: ImageView
    private val toolbarMenu: ImageView
    private val toolbarQueue: ImageView
    private val screenUnlock: ImageButton
    private val screenRotation: ImageButton
    private val screenLockButton: View
    private val fullScreenButton: ImageView
    private val nextButton: View
    private val prevButton: View
    private val progressBrightness: ProgressBar
    private val progressVolume: ProgressBar
    private var gestureCapture: View
    private val videoResizingView: TextView
    private lateinit var videoPlayer: PiVideoPlayer

    private var isScreenLocked: Boolean = false

    private var hideAction: Runnable

    private var hideSystemUI = false

    private var hideAtMs: Long = 0
    private var showTimeoutMillis: Int = 5000

    private var currentPlayingList = arrayListOf<VideoMetaData>()
    private lateinit var currentPlaying: VideoMetaData
//    private val currentPlayingQueueDialog: CurrentPlayingQueueDialog

    // Gestures
    private val mGestureDetector: GestureDetector
    private val am: AudioManager
    private var systemWidth: Int = 0
    private var systemHeight: Int = 0
    private var systemSize: Point = Point()
    private var streamVolume: Int
    private var maxSystemVolume: Int
    private var minSystemVolume: Int
    private var screenBrightness: Int
    private var maxSystemBrightness: Int = 255
    private var minSystemBrightness: Int = 0
    private var singleProgressBrightness = 255/100
    private var singleProgressVolume = 15/100
    private var gestureListener = PiGesture()
    private var activeGesture = ""

    private var superContext: Context

    constructor(context: Context): this(context, null)

    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int): super(context, attrs, defStyleAttr) {

        this.superContext = context

        val playerLayoutId = R.layout.pi_video_player_view
        LayoutInflater.from(context).inflate(playerLayoutId, this)
        descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS

        playerView = findViewById(R.id.exo_player_view)

        controlView = playerView.findViewById(R.id.exo_controller)

        toolbar = findViewById(R.id.pi_toolbar)
        backButton = findViewById(R.id.back_button)
        toolbarTitle = findViewById(R.id.toolbar_title)
        toolbarSubtitle = findViewById(R.id.toolbar_subtitle)
        toolbarQueue = findViewById(R.id.toolbar_queue)
        toolbarMenu = findViewById(R.id.toolbar_menu)

        // Screen Lock
        screenUnlock = findViewById(R.id.pi_screen_unlock)
        screenUnlock.visibility = View.GONE

        // Screen Rotation
        screenRotation = findViewById(R.id.pi_screen_rotation)

        // Video Resize Text
        videoResizingView = findViewById(R.id.pi_video_resize)
        videoResizingView.visibility = View.GONE

        // Progress Bar Brightness
        progressBrightness = findViewById(R.id.pb_brightness)
        progressBrightness.visibility = View.GONE

        // Progress Bar Volume
        progressVolume = findViewById(R.id.pb_volume)
        progressVolume.visibility = View.GONE

        // controller
        controlView = playerView.findViewById(R.id.exo_controller)
        screenLockButton = this.controlView.findViewById(R.id.pi_screen_lock)
        fullScreenButton = this.controlView.findViewById(R.id.pi_full_screen)
        nextButton = this.controlView.findViewById(R.id.pi_next)
        prevButton = this.controlView.findViewById(R.id.pi_prev)

        // Gestures
        gestureCapture = findViewById(R.id.gesture_capture)
        mGestureDetector = GestureDetector(context, gestureListener)
        am = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        maxSystemVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC) * 3
        minSystemVolume = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            am.getStreamMinVolume(AudioManager.STREAM_MUSIC)
        } else {
            0
        }

        streamVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        getScreenSize()

        screenBrightness = Settings.System.getInt(
            context.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS,
            0
        )

        Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)

        progressBrightness.max = maxSystemBrightness
        progressVolume.max = maxSystemVolume

        hideController()
        hideAction = Runnable { this.hide() }

        // On clicks

        backButton.setOnClickListener {
            (context as AppCompatActivity).finish()
        }

        toolbarSubtitle.setOnClickListener {
            SRTFilePicker(context, this@PiVideoPlayerView).show()
        }

        toolbarQueue.setOnClickListener {
            val queueDialog = CurrentPlayingQueueDialog(context, currentPlayingList, currentPlaying, this)
            queueDialog.setCanceledOnTouchOutside(false)
            queueDialog.show()
        }

        toolbarMenu.setOnClickListener {
            val popupMenu = PopupMenu(context, it)
            popupMenu.inflate(R.menu.player_menu)

            popupMenu.setOnMenuItemClickListener { item ->
                when(item.itemId) {
                    R.id.repeat_off -> {
                        videoPlayer.repeatOff()
                        videoResizingView.visibility = View.VISIBLE
                        videoResizingView.text = "Repeat Off"
                        CountDown(2000, 1000, videoResizingView)

                    }
                    R.id.repeat_one -> {
                        videoPlayer.repeatOne()
                        videoResizingView.visibility = View.VISIBLE
                        videoResizingView.text = "Repeat One"
                        CountDown(2000, 1000, videoResizingView)
                    }
                    R.id.repeat_all -> {
                        videoPlayer.repeatAll()
                        videoResizingView.visibility = View.VISIBLE
                        videoResizingView.text = "Repeat All"
                        CountDown(2000, 1000, videoResizingView)
                    }
                }
                true
            }
            popupMenu.show()
        }

        screenUnlock.setOnLongClickListener {
            isScreenLocked = false
            screenUnlock.visibility = View.GONE
            showController()
            false
        }

        screenLockButton.setOnClickListener {
            lockScreen()
        }

        screenRotation.setOnClickListener {
            rotateScreen()
        }

        fullScreenButton.setOnClickListener {
            videoResize()
        }

        nextButton.setOnClickListener {
            if (videoPlayer.hasNext())
                videoPlayer.seekTo(videoPlayer.getCurrentWindowIndex()+1, 0)
            else
                videoPlayer.seekTo(0, 0)
        }

        prevButton.setOnClickListener {
            if ((videoPlayer.getCurrentPosition()/1000) > 10) {
                videoPlayer.seekTo(videoPlayer.getCurrentWindowIndex(), 0)
            } else {
                if (videoPlayer.hasPrevious())
                    videoPlayer.seekTo(videoPlayer.getCurrentWindowIndex()-1, 0)
                else
                    videoPlayer.seekTo(videoPlayer.getCurrentWindowIndex(), 0)
            }
        }

        gestureCapture.setOnTouchListener {
                _: View, event: MotionEvent ->
            Logger.i("Gesture: gestureCapture.setOnTouchListener")
            if (!isScreenLocked) {
                mGestureDetector.onTouchEvent(event)
                Logger.i("Gesture: gestureCapture.onTouchEvent")
            }
            true
        }

    }

    fun setPlayer(videoPlayer: PiVideoPlayer) {
        this.videoPlayer = videoPlayer
        playerView.player = videoPlayer.getExoPlayer()
        observeForObserver()
    }

    fun onResume() {
        playerView.onResume()
    }

    fun onPause() {
        playerView.onPause()
    }

    fun isControllerVisible() = playerView.isControllerVisible

    fun hideController() {
        playerView.hideController()
        hideSystemUI()

        val layoutParams = ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT)
        layoutParams.bottomMargin = 0
        gestureCapture.layoutParams = layoutParams
    }

    fun showController() {
        playerView.showController()
        hideAfterTimeout()
        showSystemUI()

        val layoutParams = ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT)
        layoutParams.bottomMargin = 100
        gestureCapture.layoutParams = layoutParams
    }

    private fun hideAfterTimeout() {
        removeCallbacks(hideAction)
        if (showTimeoutMillis > 0) {
            hideAtMs = SystemClock.uptimeMillis() + showTimeoutMillis
            postDelayed(hideAction, showTimeoutMillis.toLong())
        } else {
            hideAtMs = C.TIME_UNSET
        }
    }

    private fun hide(){
        if (!hideSystemUI) {
            hideController()
            removeCallbacks(hideAction)
            hideAtMs = C.TIME_UNSET
        }
    }

    // Shows the system bars by removing all the flags
    // except for the ones that make the content appear under the system bars.

    private fun showSystemUI() {
        (context as AppCompatActivity).window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        toolbar.visibility = View.VISIBLE
        screenRotation.visibility = View.VISIBLE
        hideSystemUI = false
    }

    private fun hideSystemUI() {
        if (!hideSystemUI) {
            // Enables regular immersive mode.
            // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
            // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            (context as AppCompatActivity).window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                    // Set the content to appear under the system bars so that the
                    // content doesn't resize when the system bars hide and show.
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    // Hide the nav bar and status bar
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
            toolbar.visibility = View.GONE
            screenRotation.visibility = View.GONE
            hideSystemUI = true
        }
    }

    override fun filePickerSuccessClickListener(absolutePath: String) {
        videoPlayer.addSubtitle(absolutePath)
    }

    private fun observeForObserver() {
        videoPlayer.nowPlayingExposed.observe(superContext as AppCompatActivity, Observer {
            currentPlaying = it
            toolbarTitle.text = it.title
            if (currentPlayingList.size == 0)
                toolbarQueue.visibility = View.GONE
        })
        videoPlayer.videoListExposed.observe(superContext as AppCompatActivity, Observer {
            currentPlayingList = it
            if (currentPlayingList.size > 1)
                toolbarQueue.visibility = View.VISIBLE
            else
                toolbarQueue.visibility = View.GONE
        })
    }

    override fun onItemClicked(position: Int) {
        videoPlayer.seekTo(position, 0)
    }

    override fun onItemMoved(fromPosition: Int, toPosition: Int) {
        val mediaSource = videoPlayer.concatenatingMediaSource.getMediaSource(fromPosition)
        videoPlayer.concatenatingMediaSource.removeMediaSource(fromPosition)
        videoPlayer.concatenatingMediaSource.addMediaSource(toPosition, mediaSource)
    }

    private fun lockScreen() {
        isScreenLocked = true
        hideController()
        screenUnlock.visibility = View.VISIBLE
    }

    private fun videoResize() {
        CurrentSettings.Video.mode++

        if (CurrentSettings.Video.mode > 2)
            CurrentSettings.Video.mode = 0

        videoResizingView.visibility = View.VISIBLE

        when (CurrentSettings.Video.mode) {
//            0 -> {
//                setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FILL)
//                player?.setVideoScalingMode(C.VIDEO_SCALING_MODE_DEFAULT)
//                videoResizingView.text = "DEFAULT"
//            }
            0 -> {
                playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                videoPlayer.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT)
                videoResizingView.text = "FIT TO SCREEN"
                Glide
                    .with(context)
                    .asBitmap()
                    .load(R.drawable.ic_fullscreen_exit_black_24dp)
                    .thumbnail(0.1f)
                    .into(fullScreenButton)
                //fullScreenButton.background = superContext.getDrawable(R.drawable.ic_fullscreen_exit_black_24dp)
            }
            1 -> {
                playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                videoPlayer.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT)
                videoResizingView.text = "STRETCH"
                Glide
                    .with(context)
                    .asBitmap()
                    .load(R.drawable.ic_crop_black_24dp)
                    .thumbnail(0.1f)
                    .into(fullScreenButton)
                //fullScreenButton.background = superContext.getDrawable(R.drawable.ic_crop_black_24dp)
            }
            2 -> {
                playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                videoPlayer.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT)
                videoResizingView.text = "CROP"
                Glide
                    .with(context)
                    .asBitmap()
                    .load(R.drawable.ic_fullscreen_black_24dp)
                    .thumbnail(0.1f)
                    .into(fullScreenButton)
                //fullScreenButton.background = superContext.getDrawable(R.drawable.ic_fullscreen_black_24dp)
            }
        }
        CountDown(2000, 1000, videoResizingView)
    }

    private fun getScreenSize() {
        val display = (context as Activity).windowManager.defaultDisplay
        display.getSize(systemSize)
        systemWidth = systemSize.x
        systemHeight = systemSize.y
    }

    private fun toggleControllerVisibility(): Boolean {
        if (!playerView.useController) {
            return false
        }
        if (!controlView.isVisible) {
            showController()
        } else if (playerView.controllerHideOnTouch) {
            hideController()
        }
        return true
    }

    private inner class PiGesture: GestureDetector.SimpleOnGestureListener(), SingleTapListener {

        override fun onDown(e: MotionEvent?): Boolean {
            progressVolume.visibility = View.GONE
            progressBrightness.visibility = View.GONE
            return true
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            Logger.d("Gesture: onSingleTapUp")
            return toggleControllerVisibility()
        }

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, p2: Float, p3: Float): Boolean {
            Logger.d("Gesture: onScroll")
            activeGesture = when {
                (e1!!.x < (systemWidth / 2) && e2!!.x < (systemWidth / 2) && activeGesture.isEmpty()) -> "Brightness"
                (e1!!.x > (systemWidth / 2) && e2!!.x > (systemWidth / 2) && activeGesture.isEmpty()) -> "Volume"
                (e1!!.y > (systemHeight / 2) && e2!!.y > (systemHeight / 2) && activeGesture.isEmpty()) -> "Seek"
                else -> ""
            }

            // for brightness
            if (e1!!.x < (systemWidth / 2) && e2!!.x < (systemWidth / 2) && activeGesture == "Brightness") {
                Logger.d("Gesture: Brightness")
                if (e1!!.y < e2!!.y){
                    Logger.d("Gesture: Scroll Down")
                    if (screenBrightness > minSystemBrightness) {
                        screenBrightness -= singleProgressBrightness
                        Settings.System.putInt(
                            context!!.contentResolver,
                            Settings.System.SCREEN_BRIGHTNESS,
                            screenBrightness
                        )
                    }
                }
                if(e1.y > e2.y){
                    Logger.d("Gesture: Scroll Up")
                    if (screenBrightness < maxSystemBrightness) {
                        screenBrightness += singleProgressBrightness
                        Settings.System.putInt(
                            context!!.contentResolver,
                            Settings.System.SCREEN_BRIGHTNESS,
                            screenBrightness
                        )
                    }
                }
                progressVolume.visibility = View.GONE
                progressBrightness.visibility = View.VISIBLE
                progressBrightness.progress = screenBrightness
            }

            // for volume
            if (e1!!.x > (systemWidth / 2) && e2!!.x > (systemWidth / 2) && activeGesture == "Volume") {
                Logger.d("Gesture: Volume")
                if (e1!!.y < e2!!.y){
                    Logger.d("Gesture: Scroll Down")

                    if (streamVolume > minSystemVolume) {
//                        streamVolume -= singleProgressVolume
                        --streamVolume
                        if (streamVolume % 3 == 0) {
                            am.setStreamVolume(
                                AudioManager.STREAM_MUSIC,
                                streamVolume,
                                0
                            )
                        }
                    }
                }
                if(e1.y > e2.y){
                    Logger.d("Gesture: Scroll Up")
                    if (streamVolume < maxSystemVolume) {
//                        streamVolume += singleProgressVolume
                        ++streamVolume
                        if (streamVolume % 3 == 0) {
                            am.setStreamVolume(
                                AudioManager.STREAM_MUSIC,
                                streamVolume,
                                0
                            )
                        }
                    }
                }
                progressBrightness.visibility = View.GONE
                progressVolume.visibility = View.VISIBLE
                progressVolume.progress = streamVolume
            }

            // for seek
            if (e1!!.y > (systemHeight / 2) && e2!!.y > (systemHeight / 2) && activeGesture == "Seek") {
                Logger.d("Gesture: Seek")
                if (e1!!.x < e2!!.x){
                    Logger.d("Gesture: Left to Right swipe: "+ e1.x + " - " + e2.x)
                    videoPlayer.seekTo(videoPlayer.getCurrentWindowIndex(), videoPlayer.getCurrentPosition() + videoPlayer.DEFAULT_FAST_FORWARD_TIME)
                }
                if (e1.x > e2.x) {
                    Logger.d("Gesture: Right to Left swipe: "+ e1.x + " - " + e2.x)
                    videoPlayer.seekTo(videoPlayer.getCurrentWindowIndex(), videoPlayer.getCurrentPosition() - videoPlayer.DEFAULT_REWIND_TIME)
                }
            }

            activeGesture = ""

            return true
        }

    }

    private fun rotateScreen() {
        try {

            if (CurrentSettings.Video.orientation === "portrait") {
                //Set orientation to landscape
                (context as AppCompatActivity).requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                CurrentSettings.Video.orientation = "landscape"
            } else if (CurrentSettings.Video.orientation === "landscape") {
                //Set orientation to portrait
                (context as AppCompatActivity).requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                CurrentSettings.Video.orientation = "portrait"
            }

        } catch (ex: RuntimeException) {
            //error occurred
            Logger.e("MediaMetadataRetriever - Failed to rotate the video")
        }
    }

}