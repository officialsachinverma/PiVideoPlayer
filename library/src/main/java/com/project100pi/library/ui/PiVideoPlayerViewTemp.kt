package com.project100pi.library.ui

import android.app.Activity
import android.content.Context
import android.content.Context.AUDIO_SERVICE
import android.graphics.Matrix
import android.graphics.Point
import android.graphics.RectF
import android.media.AudioManager
import android.os.Build
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.AttributeSet
import android.view.*
import android.widget.FrameLayout
import android.widget.TextView
import android.view.GestureDetector
import android.widget.ImageButton
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.text.Cue
import com.google.android.exoplayer2.text.TextOutput
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout.ResizeMode
import com.google.android.exoplayer2.ui.PlayerControlView
import com.google.android.exoplayer2.ui.PlayerView.*
import com.google.android.exoplayer2.ui.SubtitleView
import com.google.android.exoplayer2.ui.spherical.SingleTapListener
import com.google.android.exoplayer2.ui.spherical.SphericalSurfaceView
import com.google.android.exoplayer2.util.Assertions
import com.google.android.exoplayer2.util.ErrorMessageProvider
import com.google.android.exoplayer2.video.VideoListener
import com.project100pi.library.R
import com.project100pi.library.misc.CountDown
import com.project100pi.library.misc.CurrentSettings
import com.project100pi.library.misc.Logger
import com.project100pi.library.player.PiVideoPlayer

class PiVideoPlayerViewTemp: FrameLayout {

    /** The default show timeout, in milliseconds.  */
    val DEFAULT_SHOW_TIMEOUT_MS = 5000

    private val SURFACE_TYPE_NONE = 0
    private val SURFACE_TYPE_SURFACE_VIEW = 1
    private val SURFACE_TYPE_TEXTURE_VIEW = 2
    private val SURFACE_TYPE_MONO360_VIEW = 3

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

    var toolbar: Toolbar
    private var mActionBar: ActionBar? = null
    private var contentFrame: AspectRatioFrameLayout
    private var shutterView: View
    private var surfaceView: View? = null
    private var subtitleView: SubtitleView
    private var bufferingView: View
    private var errorMessageView: TextView
    private var videoResizingView: TextView
    private var screenLock: ImageButton
    private var screenLockButton: View
    private var fullScreenButton: View
    private var controller: PlayerControlView? = null
    private var componentListener = ComponentListener()
    private var gestureListener = PiGesture()

    private var player: PiVideoPlayer? = null
    private var useController: Boolean = false
    @ShowBuffering
    private var showBuffering: Int = 0
    private var keepContentOnPlayerReset: Boolean = false
    private var errorMessageProvider: ErrorMessageProvider<in ExoPlaybackException>? = null
    private var customErrorMessage: CharSequence? = null
    private var controllerShowTimeoutMs: Int = 0
    private var controllerAutoShow: Boolean = true
    private var controllerHideOnTouch: Boolean = true
    private var textureViewRotation: Int = 0
    private var hideSystemUI = true
    private var hideAction: Runnable
    private var hideAtMs: Long = 0
    private var showTimeoutMillis: Int = 0
    private var activeGesture = ""

    private var isScreenLocked: Boolean = false

    constructor(context: Context): this(context, null)

    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int): super(context, attrs, defStyleAttr) {

        var shutterColorSet = false
        var shutterColor = 0
        var playerLayoutId = R.layout.pi_video_player_view_temp
        var useController = true
        var surfaceType = SURFACE_TYPE_SURFACE_VIEW
        var resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        var controllerShowTimeoutMs = PlayerControlView.DEFAULT_SHOW_TIMEOUT_MS
        var controllerHideOnTouch = true
        var controllerAutoShow = true
        var showBuffering = SHOW_BUFFERING_NEVER
        hideAtMs = C.TIME_UNSET
        showTimeoutMillis = DEFAULT_SHOW_TIMEOUT_MS
        if (attrs != null) {
            val a = context.theme.obtainStyledAttributes(attrs, R.styleable.PlayerView, 0, 0)
            try {
                shutterColorSet = a.hasValue(R.styleable.PlayerView_shutter_background_color)
                shutterColor =
                    a.getColor(R.styleable.PlayerView_shutter_background_color, shutterColor)
                playerLayoutId =
                    a.getResourceId(R.styleable.PlayerView_player_layout_id, playerLayoutId)
                useController = a.getBoolean(R.styleable.PlayerView_use_controller, useController)
                surfaceType = a.getInt(R.styleable.PlayerView_surface_type, surfaceType)
                resizeMode = a.getInt(R.styleable.PlayerView_resize_mode, resizeMode)
                controllerShowTimeoutMs =
                    a.getInt(R.styleable.PlayerView_show_timeout, controllerShowTimeoutMs)
                controllerHideOnTouch =
                    a.getBoolean(R.styleable.PlayerView_hide_on_touch, controllerHideOnTouch)
                controllerAutoShow =
                    a.getBoolean(R.styleable.PlayerView_auto_show, controllerAutoShow)
                showBuffering = a.getInteger(R.styleable.PlayerView_show_buffering, showBuffering)
                keepContentOnPlayerReset = a.getBoolean(
                    R.styleable.PlayerView_keep_content_on_player_reset, keepContentOnPlayerReset
                )
            } finally {
                a.recycle()
            }
        }

        LayoutInflater.from(context).inflate(playerLayoutId, this)
        descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS

        // Toolbar
        toolbar = findViewById(R.id.pi_toolbar_placeholder)
        (context as AppCompatActivity).setSupportActionBar(toolbar)
        mActionBar = context.supportActionBar
        mActionBar?.setDisplayHomeAsUpEnabled(true)

        // Content frame.
        contentFrame = findViewById(R.id.pi_content_frame)
        setResizeModeRaw(contentFrame, resizeMode)

        // Shutter view.
        shutterView = findViewById(R.id.pi_shutter)
        if (shutterColorSet) {
            shutterView.setBackgroundColor(shutterColor)
        }

        // Create a surface view and insert it into the content frame, if there is one.
        if (surfaceType != SURFACE_TYPE_NONE) {
            val params = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
            surfaceView = when (surfaceType) {
                SURFACE_TYPE_TEXTURE_VIEW -> TextureView(context)
                SURFACE_TYPE_MONO360_VIEW -> {
                    val sphericalSurfaceView = SphericalSurfaceView(context)
                    sphericalSurfaceView.setSingleTapListener(gestureListener)
                    sphericalSurfaceView
                }
                else -> SurfaceView(context)
            }
            surfaceView!!.layoutParams = params
            contentFrame.addView(surfaceView, 0)
        } else {
            surfaceView = null
        }

        // Buffering view.
        bufferingView = findViewById(R.id.pi_buffering)
        bufferingView.visibility = View.GONE
        this.showBuffering = showBuffering

        // Subtitle view.
        subtitleView = findViewById(R.id.pi_subtitles)
        subtitleView.setUserDefaultStyle()
        subtitleView.setUserDefaultTextSize()
        subtitleView.setCues(null)

        // Video Resizing view.
        videoResizingView = findViewById(R.id.pi_video_resize)
        videoResizingView.visibility = View.GONE

        // Error message view.
        errorMessageView = findViewById(R.id.pi_error_message)
        errorMessageView.visibility = View.GONE

        // Screen Lock
        screenLock = findViewById(R.id.pi_screen_lock)
        screenLock.visibility = View.GONE
        screenLock.setOnLongClickListener {
            isScreenLocked = false
            screenLock.visibility = View.GONE
            showController(false)
            false
        }

        mGestureDetector = GestureDetector(context, gestureListener)
        am = context.applicationContext.getSystemService(AUDIO_SERVICE) as AudioManager
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

        // Playback control view.
        val customController = findViewById<PlayerControlView>(R.id.pi_controller)
        val controllerPlaceholder = findViewById<View>(R.id.pi_controller_placeholder)
        when {
            customController != null -> this.controller = customController
            controllerPlaceholder != null -> {
                // Propagate attrs as playbackAttrs so that PlayerControlView's custom attributes are
                // transferred, but standard attributes (e.g. background) are not.
                this.controller = PlayerControlView(context, null, 0, attrs)
                //controller!!.playerControllerListener = this@PiVideoPlayerViewTemp
                this.controller!!.id = R.id.pi_controller
                this.controller!!.layoutParams = controllerPlaceholder.layoutParams
                val parent = controllerPlaceholder.parent as ViewGroup
                val controllerIndex = parent.indexOfChild(controllerPlaceholder)
                parent.removeView(controllerPlaceholder)
                parent.addView(controller, controllerIndex)

            }
            else -> this.controller = null
        }
        this.controllerShowTimeoutMs = if (controller != null) controllerShowTimeoutMs else 0
        this.controllerHideOnTouch = controllerHideOnTouch
        this.controllerAutoShow = controllerAutoShow
        this.useController = useController && controller != null
        hideController()
        hideAction = Runnable { this.hide() }

        screenLockButton = this.controller!!.findViewById(R.id.pi_screen_lock)
        fullScreenButton = this.controller!!.findViewById(R.id.pi_full_screen)

        screenLockButton.setOnClickListener {
            lockScreen()
        }
        fullScreenButton.setOnClickListener {
            videoResize()
        }
    }

    fun setPlayer(videoPlayer: PiVideoPlayer?) {
        Assertions.checkState(Looper.myLooper() == Looper.getMainLooper())
        Assertions.checkArgument(
            videoPlayer == null || videoPlayer.getApplicationLooper() == Looper.getMainLooper()
        )
        if (this.player === videoPlayer!!) {
            return
        }
        if (this.player != null) {
            this.player?.removeListener(componentListener)
            val oldVideoComponent = videoPlayer?.getVideoComponent()
            if (oldVideoComponent != null) {
                oldVideoComponent.removeVideoListener(componentListener)
                when (surfaceView) {
                    is TextureView -> oldVideoComponent.clearVideoTextureView(surfaceView as TextureView)
                    is SurfaceView -> oldVideoComponent.clearVideoSurfaceView(surfaceView as SurfaceView)
                }
            }
            val oldTextComponent = videoPlayer!!.getTextComponent()
            oldTextComponent?.removeTextOutput(componentListener)
        }
        this.player = videoPlayer
        if (useController) {
            controller!!.player = videoPlayer!!.getExoPlayer()
        }
        if (subtitleView != null) {
            subtitleView!!.setCues(null)
        }
        updateBuffering()
        updateErrorMessage()
        updateForCurrentTrackSelections(/* isNewPlayer= */true)
        if (videoPlayer != null) {
            val newVideoComponent = videoPlayer.getVideoComponent()
            if (newVideoComponent != null) {
                when (surfaceView) {
                    is TextureView -> newVideoComponent.setVideoTextureView(surfaceView as TextureView)
                    is SurfaceView -> newVideoComponent.setVideoSurfaceView(surfaceView as SurfaceView)
                }
                newVideoComponent.addVideoListener(componentListener)
            }
            val newTextComponent = videoPlayer!!.getTextComponent()
            if (newTextComponent != null) {
                newTextComponent!!.addTextOutput(componentListener)
            }
            player!!.addListener(componentListener)
            maybeShowController(false)
        } else {
            hideController()
        }
    }

    override fun setVisibility(visibility: Int) {
        super.setVisibility(visibility)
        if (surfaceView is SurfaceView) {
            // Work around https://github.com/google/ExoPlayer/issues/3160.
            surfaceView!!.visibility = visibility
        }
    }

    private fun setResizeModeRaw(aspectRatioFrame: AspectRatioFrameLayout, resizeMode: Int) {
        aspectRatioFrame.resizeMode = resizeMode
    }

    private fun closeShutter() {
        shutterView.visibility = View.VISIBLE
    }

    private fun updateBuffering() {
        val showBufferingSpinner = (player != null
                && player?.getPlaybackState() == Player.STATE_BUFFERING
                && (showBuffering == SHOW_BUFFERING_ALWAYS || showBuffering == SHOW_BUFFERING_WHEN_PLAYING && player!!.playWhenReady))
        bufferingView.visibility = if (showBufferingSpinner) View.VISIBLE else View.GONE
    }

    private fun updateErrorMessage() {
        if (customErrorMessage != null) {
            errorMessageView.text = customErrorMessage
            errorMessageView.visibility = View.VISIBLE
            return
        }
        var error: ExoPlaybackException? = null
        if (player != null
            && player?.getPlaybackState() == Player.STATE_IDLE
            && errorMessageProvider != null
        ) {
            error = player?.getPlaybackError()
        }
        if (error != null) {
            val errorMessage = errorMessageProvider!!.getErrorMessage(error).second
            errorMessageView.text = errorMessage
            errorMessageView.visibility = View.VISIBLE
        } else {
            errorMessageView.visibility = View.GONE
        }
    }

    fun hide(){
        if (!hideSystemUI) {
            hideSystemUI()
            removeCallbacks(hideAction)
            hideAtMs = C.TIME_UNSET
        }
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

    /** Hides the playback controls. Does nothing if playback controls are disabled.  */
    fun hideController() {
        controller?.hide()
        hideSystemUI()
    }

    /** Returns whether the controller is currently visible.  */
    fun isControllerVisible() = controller != null && controller!!.isVisible()

    /**
     * Sets the background color of the `exo_shutter` view.
     *
     * @param color The background color.
     */
    fun setShutterBackgroundColor(color: Int) {
        if (shutterView != null) {
            shutterView.setBackgroundColor(color)
        }
    }

    /**
     * Called when there's a change in the aspect ratio of the content being displayed. The default
     * implementation sets the aspect ratio of the content frame to that of the content, unless the
     * content view is a [SphericalSurfaceView] in which case the frame's aspect ratio is
     * cleared.
     *
     * @param contentAspectRatio The aspect ratio of the content.
     * @param contentFrame The content frame, or `null`.
     * @param contentView The view that holds the content being displayed, or `null`.
     */
    private fun onContentAspectRatioChanged(
        contentAspectRatio: Float,
        contentFrame: AspectRatioFrameLayout?,
        contentView: View?
    ) {
        contentFrame?.setAspectRatio(
            if (contentView is SphericalSurfaceView) 0F else contentAspectRatio
        )
    }

    private fun updateForCurrentTrackSelections(isNewPlayer: Boolean) {
        if (player == null || player?.getCurrentTrackGroups()!!.isEmpty) {
            if (!keepContentOnPlayerReset) {
                closeShutter()
            }
            return
        }

        if (isNewPlayer && !keepContentOnPlayerReset) {
            // Hide any video from the previous player.
            closeShutter()
        }

        // Video disabled so the shutter must be closed.
        closeShutter()

    }

    /** Shows the playback controls, but only if forced or shown indefinitely.  */
    private fun maybeShowController(isForced: Boolean) {
        if (useController) {
            val wasShowingIndefinitely = controller!!.isVisible() && controller!!.showTimeoutMs <= 0
            val shouldShowIndefinitely = shouldShowControllerIndefinitely()
            if (isForced || wasShowingIndefinitely || shouldShowIndefinitely) {
                showController(shouldShowIndefinitely)
            }
        }
    }

    private fun showController(showIndefinitely: Boolean) {
        if (!useController)
            return
        if (isScreenLocked)
            return
        controller!!.showTimeoutMs = if (showIndefinitely) 0 else controllerShowTimeoutMs
        controller!!.show()
        hideAfterTimeout()
        showSystemUI()
    }

    private fun shouldShowControllerIndefinitely(): Boolean {
        if (player == null) {
            return true
        }
        val playbackState = player?.getPlaybackState()
        return controllerAutoShow && (playbackState == Player.STATE_IDLE
                || playbackState == Player.STATE_ENDED
                || !player!!.playWhenReady)
    }

    private fun toggleControllerVisibility(): Boolean {
        if (!useController || player == null) {
            return false
        }
        if (!controller!!.isVisible()) {
            maybeShowController(true)
        } else if (controllerHideOnTouch) {
            controller?.hide()
            hideSystemUI()
        }
        return true
    }

    // Shows the system bars by removing all the flags
    // except for the ones that make the content appear under the system bars.

    private fun showSystemUI() {
        (context as AppCompatActivity).window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        (context as AppCompatActivity).supportActionBar?.show()
        hideSystemUI = false
    }

    private fun hideSystemUI() {
        if (!hideSystemUI) {
            // Enables regular immersive mode.
            // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
            // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            (context as Activity).window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                    // Set the content to appear under the system bars so that the
                    // content doesn't resize when the system bars hide and show.
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    // Hide the nav bar and status bar
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
            (context as AppCompatActivity).supportActionBar?.hide()
            hideSystemUI = true
        }
    }

    /** Applies a texture rotation to a [TextureView].  */
    private fun applyTextureViewRotation(textureView: TextureView, textureViewRotation: Int) {
        val textureViewWidth = textureView.width.toFloat()
        val textureViewHeight = textureView.height.toFloat()
        if (textureViewWidth == 0f || textureViewHeight == 0f || textureViewRotation == 0) {
            textureView.setTransform(null)
        } else {
            val transformMatrix = Matrix()
            val pivotX = textureViewWidth / 2
            val pivotY = textureViewHeight / 2
            transformMatrix.postRotate(textureViewRotation.toFloat(), pivotX, pivotY)

            // After rotation, scale the rotated texture to fit the TextureView size.
            val originalTextureRect = RectF(0f, 0f, textureViewWidth, textureViewHeight)
            val rotatedTextureRect = RectF()
            transformMatrix.mapRect(rotatedTextureRect, originalTextureRect)
            transformMatrix.postScale(
                textureViewWidth / rotatedTextureRect.width(),
                textureViewHeight / rotatedTextureRect.height(),
                pivotX,
                pivotY
            )
            textureView.setTransform(transformMatrix)
        }
    }

    // Controller View Actions Starts

    private fun lockScreen() {
        isScreenLocked = true
        hideController()
        screenLock.visibility = View.VISIBLE
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
                setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT)
                player?.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT)
                videoResizingView.text = "FIT TO SCREEN"
//                videoResizingView.text = "FIT"
            }
            1 -> {
                setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FILL)
                player?.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT)
                videoResizingView.text = "STRETCH"
            }
            2 -> {
                setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM)
                player?.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT)
                videoResizingView.text = "CROP"
            }
        }
        CountDown(2000, 1000, videoResizingView)
    }

    /**
     * Sets the [ResizeMode].
     *
     * @param resizeMode The [ResizeMode].
     */
    private fun setResizeMode(@ResizeMode resizeMode: Int) {
        Assertions.checkState(contentFrame != null)
        contentFrame.resizeMode = resizeMode
    }

    // Controller View Actions Ends

    private inner class ComponentListener : Player.EventListener,
        TextOutput,
        VideoListener,
        OnLayoutChangeListener{

        // TextOutput implementation

        override fun onCues(cues: List<Cue>) {
            subtitleView.onCues(cues)
        }

        // VideoListener implementation

        override fun onVideoSizeChanged(
            width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float
        ) {
            var videoAspectRatio: Float =
                if (height == 0 || width == 0) 1F else width * pixelWidthHeightRatio / height

            if (surfaceView is TextureView) {
                // Try to apply rotation transformation when our surface is a TextureView.
                if (unappliedRotationDegrees == 90 || unappliedRotationDegrees == 270) {
                    // We will apply a rotation 90/270 degree to the output texture of the TextureView.
                    // In this case, the output video's width and height will be swapped.
                    videoAspectRatio = 1.div(videoAspectRatio)
                }
                if (textureViewRotation != 0) {
                    surfaceView!!.removeOnLayoutChangeListener(this)
                }
                textureViewRotation = unappliedRotationDegrees
                if (textureViewRotation != 0) {
                    // The texture view's dimensions might be changed after layout step.
                    // So add an OnLayoutChangeListener to apply rotation after layout step.
                    surfaceView!!.addOnLayoutChangeListener(this)
                }
                applyTextureViewRotation((surfaceView as TextureView?)!!, textureViewRotation)
            }

            onContentAspectRatioChanged(videoAspectRatio, contentFrame, surfaceView)
        }

        override fun onRenderedFirstFrame() {
            shutterView.visibility = View.INVISIBLE
        }

        override fun onTracksChanged(trackGroups: TrackGroupArray, trackSelections: TrackSelectionArray) {
            updateForCurrentTrackSelections(/* isNewPlayer= */false)
        }

        // Player.EventListener implementation

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            updateBuffering()
            updateErrorMessage()
            maybeShowController(false)
        }

        override fun onPositionDiscontinuity(@Player.DiscontinuityReason reason: Int) {
            hideController()
        }

        // OnLayoutChangeListener implementation

        override fun onLayoutChange(
            view: View,
            left: Int,
            top: Int,
            right: Int,
            bottom: Int,
            oldLeft: Int,
            oldTop: Int,
            oldRight: Int,
            oldBottom: Int
        ) {
            applyTextureViewRotation(view as TextureView, textureViewRotation)
        }
    }

    private fun getScreenSize() {
        val display = (context as Activity).windowManager.defaultDisplay
        display.getSize(systemSize)
        systemWidth = systemSize.x
        systemHeight = systemSize.y
    }


    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (!isScreenLocked)
            mGestureDetector.onTouchEvent(event)
        return true
    }

    private inner class PiGesture: GestureDetector.SimpleOnGestureListener(), SingleTapListener{

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            return toggleControllerVisibility()
        }

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, p2: Float, p3: Float): Boolean {

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
                    if (screenBrightness > minSystemBrightness)
                        Settings.System.putInt(context!!.contentResolver, Settings.System.SCREEN_BRIGHTNESS, --screenBrightness)
                }
                if(e1.y > e2.y){
                    Logger.d("Gesture: Scroll Up")
                    if (screenBrightness < maxSystemBrightness)
                        Settings.System.putInt(context!!.contentResolver, Settings.System.SCREEN_BRIGHTNESS, ++screenBrightness)
                }
            }

            // for volume
            if (e1!!.x > (systemWidth / 2) && e2!!.x > (systemWidth / 2) && activeGesture == "Volume") {
                Logger.d("Gesture: Volume")
                if (e1!!.y < e2!!.y){
                    Logger.d("Gesture: Scroll Down")

                    if (streamVolume > minSystemVolume) {
                        --streamVolume
                        if (streamVolume % 3 == 0) {
                            am.setStreamVolume(
                                AudioManager.STREAM_MUSIC,
                                streamVolume / 3,
                                AudioManager.FLAG_SHOW_UI
                            )
                        }
                    }
                }
                if(e1.y > e2.y){
                    Logger.d("Gesture: Scroll Up")
                    if (streamVolume < maxSystemVolume) {
                        ++streamVolume
                        if (streamVolume % 3 == 0) {
                            am.setStreamVolume(
                                AudioManager.STREAM_MUSIC,
                                streamVolume / 3,
                                AudioManager.FLAG_SHOW_UI
                            )
                        }
                    }
                }
            }

            // for seek
            if (e1!!.y > (systemHeight / 2) && e2!!.y > (systemHeight / 2) && activeGesture == "Seek") {
                Logger.d("Gesture: Seek")
                if (e1!!.x < e2!!.x){
                    Logger.d("Gesture: Left to Right swipe: "+ e1.x + " - " + e2.x)
                    player!!.seekTo(player!!.getCurrentWindowIndex(), player!!.getCurrentPosition() + player!!.DEFAULT_FAST_FORWARD_TIME)
                }
                if (e1.x > e2.x) {
                    Logger.d("Gesture: Right to Left swipe: "+ e1.x + " - " + e2.x)
                    player!!.seekTo(player!!.getCurrentWindowIndex(), player!!.getCurrentPosition() - player!!.DEFAULT_REWIND_TIME)
                }
            }

            activeGesture = ""

            return true
        }

    }

}

