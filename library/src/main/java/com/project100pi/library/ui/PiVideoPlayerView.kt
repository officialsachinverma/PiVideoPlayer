package com.project100pi.library.ui

import android.app.Activity
import android.content.Context
import android.graphics.Point
import android.os.SystemClock
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.*
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Observer
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
import com.project100pi.library.listeners.PlaybackControllerVisibilityListener
import com.project100pi.library.listeners.PlaybackGestureControlListener
import com.project100pi.library.listeners.PlayerViewActionsListener
import com.project100pi.library.misc.Constants.Gesture.MIN_DISTANCE_TO_TRIGGER_GESTURE
import com.project100pi.library.misc.CountDown
import com.project100pi.library.misc.CurrentMediaState
import com.project100pi.library.misc.Logger
import com.project100pi.library.misc.Util
import com.project100pi.library.model.VideoMetaData
import com.project100pi.library.player.PiVideoPlayer

class PiVideoPlayerView : FrameLayout, SRTFilePickerClickListener, OnItemClickListener {

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
    private var gestureCapture: View
    private var additionalNavigationHeight: ImageView
    private val messageView: TextView
    private val popupMenu: PopupMenu
    private lateinit var videoPlayer: PiVideoPlayer

    private var isScreenLocked: Boolean = false

    private var hideAction: Runnable

    private var hideSystemUI = false
    private var hasSoftNavBar = false

    private var hideAtMs: Long = 0
    private var showTimeoutMillis: Int = 5000

    private var currentPlayingList = arrayListOf<VideoMetaData>()
    private lateinit var currentPlaying: VideoMetaData

    // Gestures
    private val gestureDetector: GestureDetector
    private var systemWidth: Int = 0
    private var systemHeight: Int = 0
    private var gestureListener = PiGesture()
    private var activeGesture = ""
    private var isScrolling = false
    private var softNavBarHeight = 0

    private var superContext: Context
    private var volumeFactor = 0
    private var brightnessFactor = 0
    private var seekFactor = 0
    private var oldy = 0
    private var newy = 0
    private var playerViewActionsListener: PlayerViewActionsListener? = null
    private var playerGestureListener: PlaybackGestureControlListener? = null
    private var playbackControllerVisibilityListener: PlaybackControllerVisibilityListener? = null

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {

        this.superContext = context

        val playerLayoutId = R.layout.pi_video_player_view
        LayoutInflater.from(context).inflate(playerLayoutId, this)

        playerView = findViewById(R.id.exo_player_view)

        controlView = playerView.findViewById(R.id.exo_controller)

        // Toolbar
        toolbar = findViewById(R.id.pi_toolbar)
        backButton = findViewById(R.id.back_button)
        toolbarTitle = findViewById(R.id.toolbar_title)
        toolbarSubtitle = findViewById(R.id.toolbar_subtitle)
        toolbarQueue = findViewById(R.id.toolbar_queue)
        toolbarMenu = findViewById(R.id.toolbar_menu)

        // Screen Lock
        screenUnlock = findViewById(R.id.pi_screen_unlock)

        // Screen Rotation
        screenRotation = findViewById(R.id.pi_screen_rotation)

        // Navigation bar height
        additionalNavigationHeight = findViewById(R.id.navigation_bar_height)

        // Video Resize Text
        messageView = findViewById(R.id.pi_video_resize)

        // controller
        controlView = playerView.findViewById(R.id.exo_controller)
        screenLockButton = this.controlView.findViewById(R.id.pi_screen_lock)
        fullScreenButton = this.controlView.findViewById(R.id.pi_full_screen)
        nextButton = this.controlView.findViewById(R.id.pi_next)
        prevButton = this.controlView.findViewById(R.id.pi_prev)

        // Gestures
        gestureCapture = findViewById(R.id.gesture_capture)
        gestureDetector = GestureDetector(context, gestureListener)

        setScreenSize()

        hideController()
        hideAction = Runnable { this.hide() }

        // Pop Menu
        popupMenu = PopupMenu(context, toolbarMenu)
        popupMenu.inflate(R.menu.player_menu)
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.shuffle -> {
                    videoPlayer.shuffle(!item.isChecked)
                    item.isChecked = !item.isChecked
                    if (item.isChecked)
                        messageView.text = resources.getString(R.string.shuffle_enabled)
                    else
                        messageView.text = resources.getString(R.string.shuffle_disabled)
                    messageView.visibility = View.VISIBLE
                    CountDown(2000, 1000, messageView)
                }
                R.id.repeat_off -> {
                    videoPlayer.repeatOff()
                    messageView.visibility = View.VISIBLE
                    messageView.text = resources.getString(R.string.repeat_off)
                    CountDown(2000, 1000, messageView)
                    item.isChecked = true
                }
                R.id.repeat_one -> {
                    videoPlayer.repeatOne()
                    messageView.visibility = View.VISIBLE
                    messageView.text = resources.getString(R.string.repeat_one)
                    CountDown(2000, 1000, messageView)
                    item.isChecked = true
                }
                R.id.repeat_all -> {
                    videoPlayer.repeatAll()
                    messageView.visibility = View.VISIBLE
                    messageView.text = resources.getString(R.string.repeat_all)
                    CountDown(2000, 1000, messageView)
                    item.isChecked = true
                }
            }
            true
        }

        setListeners()
    }

    /**
     * Sets listeners
     */
    private fun setListeners() {

        setOnClickListener()

        setOnLongClickListener()

        setOnTouchListener()
    }

    /**
     * Registers on click listeners
     */
    private fun setOnClickListener() {

        // Back Button
        backButton.setOnClickListener {
            playerViewActionsListener?.onPlayerBackButtonPressed()
        }

        // Toolbar subtitle icon
        toolbarSubtitle.setOnClickListener {
            currentPlaying.path.substring(0, currentPlaying.path.lastIndexOf("/"))
            SRTFilePicker(
                context,
                this@PiVideoPlayerView,
                currentPlaying.path.substring(0, currentPlaying.path.lastIndexOf("/"))
            ).show()
        }

        // Toolbar queue icon
        toolbarQueue.setOnClickListener {
            val queueDialog =
                CurrentPlayingQueueDialog(context, currentPlayingList, currentPlaying, this)
            queueDialog.setCanceledOnTouchOutside(false)
            queueDialog.show()
            playerViewActionsListener?.onPlayerCurrentQueuePressed()
        }

        // Toolbar menu icon
        toolbarMenu.setOnClickListener {
            popupMenu.show()
        }

        // Controller lock button
        screenLockButton.setOnClickListener {
            lockScreen()
        }

        // Screen rotation image
        screenRotation.setOnClickListener {
            if (hasSoftNavBar) {
                // Margin for soft navigation keys
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
                if (Util.orientation === "landscape") {
                    Util.orientation = "portrait"
                    lp.setMargins(0, 0, 0, 150)
                } else {
                    Util.orientation = "landscape"
                    lp.setMargins(0, 0, 0, 0)
                }
                additionalNavigationHeight.layoutParams = lp
            } else {
                if (Util.orientation === "landscape") {
                    Util.orientation = "portrait"
                } else {
                    Util.orientation = "landscape"
                }
            }

            playerViewActionsListener?.onScreenRotatePressed()
        }

        // Controller fullscreen button
        fullScreenButton.setOnClickListener {
            videoResize()
        }

        // Controller next button
        nextButton.setOnClickListener {
            if (videoPlayer.hasNext())
                videoPlayer.seekTo(videoPlayer.getNextWindowIndex(), 0)
            else
                videoPlayer.seekTo(0, 0)
        }

        // Controller previous button
        prevButton.setOnClickListener {
            if ((videoPlayer.getCurrentPosition() / 1000) > 10) {
                videoPlayer.seekTo(videoPlayer.getCurrentWindowIndex(), 0)
            } else {
                if (videoPlayer.hasPrevious())
                    videoPlayer.seekTo(videoPlayer.getPreviousWindowIndex(), 0)
                else {
                    if (currentPlayingList.size > 1)
                        videoPlayer.seekTo(currentPlayingList.size - 1, 0)
                    else
                        videoPlayer.seekTo(videoPlayer.getCurrentWindowIndex(), 0)
                }
            }
        }
    }

    fun setSoftNavBarMargin(margin: Int, isLandscape: Boolean) {
        if (hasSoftNavBar) {
            softNavBarHeight = margin
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            if (isLandscape)
                Util.orientation = "landscape"
            else
                Util.orientation = "portrait"
            lp.setMargins(0, 0, 0, margin)
            additionalNavigationHeight.layoutParams = lp
        } else
            softNavBarHeight = 0
    }

    /**
     * Registers on long listeners
     */
    private fun setOnLongClickListener() {
        // Screen unlock image
        screenUnlock.setOnLongClickListener {
            isScreenLocked = false
            screenUnlock.visibility = View.GONE
            showController()
            false
        }
    }

    /**
     * Registers on long listeners
     */
    private fun setOnTouchListener() {
        // Gesture Detection
        gestureCapture.setOnTouchListener { _: View, event: MotionEvent ->
            Logger.i("Gesture: gestureCapture.setOnTouchListener")

            if (!isScreenLocked) {
                gestureDetector.onTouchEvent(event)
                Logger.i("Gesture: gestureCapture.onTouchEvent")
            }

            if (event.action == MotionEvent.ACTION_UP) {
                if (isScrolling) {
                    Logger.d("Gesture: onScroll ended")
                    oldy = 0
                    activeGesture = ""
                    isScrolling = false
//                    handleScrollFinished()
                    playerGestureListener?.onActionUp()
                }
            }

            true
        }
    }

    /**
     * This method sets hasSoftNavBar
     * which tells whether the device support soft
     * navigation bar or not
     *
     * @param hasSoftNavBar Boolean
     */
    fun hasSoftNavBar(hasSoftNavBar: Boolean) {
        this.hasSoftNavBar = hasSoftNavBar
    }

    /**
     * Sets exo player to exo player's player view
     *
     * @param videoPlayer PiVideoPlayer
     */
    fun setPlayer(videoPlayer: PiVideoPlayer) {
        this.videoPlayer = videoPlayer
        playerView.player = videoPlayer.getExoPlayer()
        observeForObserver()
    }

    /**
     * Registers PlayerViewActionsListener
     *
     * @param playerViewActionsListener PlayerViewActionsListener
     */
    fun setPlayerActionBarListener(playerViewActionsListener: PlayerViewActionsListener) {
        this.playerViewActionsListener = playerViewActionsListener
    }

    /**
     * Registers PlaybackGestureControlListener
     *
     * @param playerGestureListener PlaybackGestureControlListener
     */
    fun setPlayerGestureControlListener(playerGestureListener: PlaybackGestureControlListener) {
        this.playerGestureListener = playerGestureListener
    }

    /**
     * Registers PlaybackControllerVisibilityListener
     *
     * @param playbackControllerVisibilityListener PlaybackControllerVisibilityListener
     */
    fun setPlaybackControllerVisibilityListener(playbackControllerVisibilityListener: PlaybackControllerVisibilityListener) {
        this.playbackControllerVisibilityListener = playbackControllerVisibilityListener
    }

    /**
     * Should be called when the player is visible to the user.
     *
     * This method should typically be called in {@link Activity#onStart()}, or {@link
     * Activity#onResume()} for API versions <= 23.
     */
    fun onResume() {
        playerView.onResume()
    }

    /**
     * Should be called when the player is no longer visible to the user.
     *
     * <p>This method should typically be called in {@link Activity#onStop()}, or {@link
     * Activity#onPause()} for API versions &lt;= 23.
     */
    fun onPause() {
        playerView.onPause()
    }

    /**
     * Returns whether the controller is currently visible.
     * */
    fun isControllerVisible() = playerView.isControllerVisible

    /**
     * Hides the playback controls. Does nothing if playback controls are disabled.
     * */
    fun hideController() {
        playerView.hideController()
        hideSystemUI()

        setMargin(false)
    }

    /**
     * Shows the playback controls. Does nothing if playback controls are disabled.
     *
     * The playback controls are automatically hidden during playback after 5 secs.
     * They are shown indefinitely when playback has not started yet,
     * is paused, has ended or failed.
     */
    fun showController() {
        playerView.showController()
        hideAfterTimeout()
        showSystemUI()

        setMargin(true)
    }

    /**
     * Sets bottom margin to the view which is responsible for
     * detecting gestures.
     * When ever controllers comes up it decreases the area of gesture
     * and gesture view always comes over the controller view, results in preventing
     * from passing the touch events. So to avoid the overlap we set the margin
     * of similar to the height of controller view. so basically it shrinks the area for
     * gestures
     *
     * @param show Boolean whether to show or not
     */
    private fun setMargin(show: Boolean) {
        Logger.i("Util.orientation : ${Util.orientation}")
        val layoutParams = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.MATCH_PARENT
        )
        if (Util.orientation === "landscape") {
            if (show)
                layoutParams.bottomMargin = 200 + softNavBarHeight
            else
                layoutParams.bottomMargin = 0
        } else { // Portrait
            if (show)
                layoutParams.bottomMargin = 50 + softNavBarHeight
            else
                layoutParams.bottomMargin = 0
        }

        gestureCapture.layoutParams = layoutParams
    }

    /**
     * Sends a msg after 5 secs to hide controller and system UI
     */
    private fun hideAfterTimeout() {
        removeCallbacks(hideAction)
        if (showTimeoutMillis > 0) {
            hideAtMs = SystemClock.uptimeMillis() + showTimeoutMillis
            postDelayed(hideAction, showTimeoutMillis.toLong())
        } else {
            // provides smallest positive number
            hideAtMs = C.TIME_UNSET
        }
    }

    /**
     * Hides system UI and controller
     */
    private fun hide() {
        if (!hideSystemUI) {
            hideController()
            removeCallbacks(hideAction)
            // provides smallest positive number
            hideAtMs = C.TIME_UNSET
        }
    }

    /**
     * Shows system UI such as status bar
     */
    private fun showSystemUI() {
        playbackControllerVisibilityListener?.showSystemUI()
        toolbar.visibility = View.VISIBLE
        screenRotation.visibility = View.VISIBLE
        hideSystemUI = false
    }

    /**
     * Hides system UI such as status bar
     */
    private fun hideSystemUI() {
        if (!hideSystemUI) {
            playbackControllerVisibilityListener?.hideSystemUI()
            toolbar.visibility = View.GONE
            screenRotation.visibility = View.GONE
            hideSystemUI = true
        }
    }

    override fun filePickerSuccessClickListener(absolutePath: String) {
        videoPlayer.addSubtitle(absolutePath)
    }

    /**
     * This method contains all other methods
     * who are observing to a particular thing
     */
    private fun observeForObserver() {
        observeNowPlaying()
        observePlaylist()
    }

    /**
     * Observes currently playing item
     */
    private fun observeNowPlaying() {
        videoPlayer.nowPlaying.observe(superContext as AppCompatActivity, Observer {
            currentPlaying = it
            toolbarTitle.text = it.title
            if (currentPlayingList.size == 0)
                toolbarQueue.visibility = View.GONE
        })
    }

    /**
     * Observes video playlist
     */
    private fun observePlaylist() {
        videoPlayer.videoList.observe(superContext as AppCompatActivity, Observer {
            currentPlayingList = it
            if (currentPlayingList.size > 1)
                toolbarQueue.visibility = View.GONE
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

    /**
     * Locks the screen
     * Hides system UI and controller
     * and disables touch event on screen
     *
     * User has to long press on unlock button to
     * enable touch event and gestures
     */
    private fun lockScreen() {
        isScreenLocked = true
        hideController()
        screenUnlock.visibility = View.VISIBLE
    }

    /**
     * Resize the video frame playing on screen
     * 0 -> Fit to screen
     * 1 -> Stretch
     * 2 -> Crop
     */
    private fun videoResize() {
        CurrentMediaState.Video.mode++

        if (CurrentMediaState.Video.mode > 2)
            CurrentMediaState.Video.mode = 0

        messageView.visibility = View.VISIBLE
        fullScreenButton.setImageResource(android.R.color.transparent)
        when (CurrentMediaState.Video.mode) {
            0 -> {
                playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                videoPlayer.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT)
                messageView.text = resources.getString(R.string.fit_to_screen_upper_case)
                fullScreenButton.setImageResource(R.drawable.ic_fullscreen_exit_black_24dp)
            }
            1 -> {
                playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                videoPlayer.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT)
                messageView.text = resources.getString(R.string.stretch_upper_case)
                fullScreenButton.setImageResource(R.drawable.ic_crop_black_24dp)
            }
            2 -> {
                playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                videoPlayer.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT)
                messageView.text = resources.getString(R.string.crop_upper_case)
                fullScreenButton.setImageResource(R.drawable.ic_fullscreen_black_24dp)
            }
        }
        CountDown(2000, 1000, messageView)
    }

    /**
     * Gets system screen size
     */
    fun setScreenSize() {
        val metrics = DisplayMetrics()
        val systemSize = Point()
        val display = (context as Activity).windowManager.defaultDisplay
        display.getSize(systemSize)
        this.systemWidth = systemSize.x
        this.systemHeight = systemSize.y

    }

    /***
     * Handles controller visibility
     * Hides controller if visible
     * @return Boolean
     */
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

    /**
     * Gesture Implementation
     */
    private inner class PiGesture : GestureDetector.SimpleOnGestureListener(), SingleTapListener {

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            Logger.d("Gesture: onSingleTapUp")
            return toggleControllerVisibility()
        }

        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent?,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            Logger.d("Gesture: onScroll")
            isScrolling = true

            if (oldy == 0) {
                oldy = e1!!.y.toInt()
            }

            newy = e2!!.y.toInt()

            // Calculating screen left, right and bottom
            // Left half of the screen for Brightness
            // Right half of the screen for Volume
            // Bottom half of the screen for seek
            if (activeGesture.equals("")) {
                activeGesture = when {
                    (e1!!.x < (systemWidth / 2) && e2!!.x < (systemWidth / 2) && ((e1.y - e2.y > MIN_DISTANCE_TO_TRIGGER_GESTURE) || (e2.y - e1.y > MIN_DISTANCE_TO_TRIGGER_GESTURE))) -> "Brightness"
                    (e1.x > (systemWidth / 2) && e2!!.x > (systemWidth / 2) && ((e1.y - e2.y > MIN_DISTANCE_TO_TRIGGER_GESTURE) || (e2.y - e1.y > MIN_DISTANCE_TO_TRIGGER_GESTURE))) -> "Volume"
                    ((e1.x - e2.x > MIN_DISTANCE_TO_TRIGGER_GESTURE) || (e2.x - e1.x > MIN_DISTANCE_TO_TRIGGER_GESTURE)) -> "Seek"

                    else -> ""
                }
            }

//            if(seekFactor == 0){
//                seekFactor = ((e1!!.y - e2!!.y).toInt()) / 30
//            }

            // for seek
            // if start and end point is below bottom half of the screen
//            e1!!.y < (systemHeight / 2) && e2!!.y < (systemHeight / 2) &&
                    if (activeGesture .equals( "Seek")) {
//                if (seekFactor!=((e1!!.y - e2!!.y).toInt() / 30)) {
//                    seekFactor = (e1!!.y - e2!!.y).toInt() / 30
                // if scroll was from left to right
                if (e1!!.x < e2.x) {
                    //Logger.d("Gesture: Left to Right swipe: "+ e1.x + " - " + e2.x)
                    playerGestureListener?.onFastForward()
                } else { // if scroll was from right to left
                    //Logger.d("Gesture: Right to Left swipe: "+ e1.x + " - " + e2.x)
                    playerGestureListener?.onRewind()
                }
                activeGesture = ""
                return true
//                }
            }

            if (brightnessFactor == 0) {
                brightnessFactor = ((e1!!.y - e2!!.y).toInt()) / 30
            }

            // for brightness
            if (e1!!.x < (systemWidth / 2) && e2!!.x < (systemWidth / 2) && activeGesture == "Brightness") {
                // if start and end point on the left side of the screen
                if (brightnessFactor != ((e1!!.y - e2!!.y).toInt() / 30)) {
                    brightnessFactor = (e1!!.y - e2!!.y).toInt() / 30
                    // if scroll is from top to bottom
                    if (oldy < newy) {
                        playerGestureListener?.onBrightnessDown()
                    } else { // if scroll is from bottom to top
                        playerGestureListener?.onBrightnessUp()
                    }
                    activeGesture = ""
                    return true
                }
            }

            if (volumeFactor == 0) {
                volumeFactor = ((e2.y - e1.y).toInt()) / 30
            }

            // for volume
            if (e1.x > (systemWidth / 2) && e2!!.x > (systemWidth / 2) && activeGesture == "Volume") {
                // if start and end point on the right side of the screen
                if (volumeFactor != (e2.y.toInt() - e1.y.toInt()) / 30) {
                    volumeFactor = (e2.y.toInt() - e1.y.toInt()) / 30
                    // if scroll is from top to bottom
                    if (oldy < newy) {
                        playerGestureListener?.onVolumeDown()
                    } else { // if scroll is from bottom to top
                        playerGestureListener?.onVolumeUp()
                    }
                    activeGesture = ""
                    return true
                }
            }
            oldy = e2.y.toInt()

            return false
        }

    }

}