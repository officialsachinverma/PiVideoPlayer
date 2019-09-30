package com.project100pi.library.ui

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Looper
import android.util.AttributeSet
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.IntDef
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.metadata.Metadata
import com.google.android.exoplayer2.metadata.flac.PictureFrame
import com.google.android.exoplayer2.metadata.id3.ApicFrame
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.text.Cue
import com.google.android.exoplayer2.text.TextOutput
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerControlView
import com.google.android.exoplayer2.ui.PlayerView.*
import com.google.android.exoplayer2.ui.SubtitleView
import com.google.android.exoplayer2.ui.spherical.SingleTapListener
import com.google.android.exoplayer2.ui.spherical.SphericalSurfaceView
import com.google.android.exoplayer2.util.Assertions
import com.google.android.exoplayer2.util.ErrorMessageProvider
import com.google.android.exoplayer2.video.VideoListener
import com.project100pi.library.R
import com.project100pi.library.misc.Util
import com.project100pi.library.player.PiVideoPlayer

class PiVideoPlayerView: FrameLayout {

    // LINT.IfChange
    /**
     * Determines when the buffering view is shown. One of [.SHOW_BUFFERING_NEVER], [ ][.SHOW_BUFFERING_WHEN_PLAYING] or [.SHOW_BUFFERING_ALWAYS].
     */
    @MustBeDocumented
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(SHOW_BUFFERING_NEVER, SHOW_BUFFERING_WHEN_PLAYING, SHOW_BUFFERING_ALWAYS)
    annotation class ShowBuffering

    /** The buffering view is never shown.  */
    val SHOW_BUFFERING_NEVER = 0
    /**
     * The buffering view is shown when the player is in the [buffering][Player.STATE_BUFFERING]
     * state and [playWhenReady][Player.getPlayWhenReady] is `true`.
     */
    val SHOW_BUFFERING_WHEN_PLAYING = 1
    /**
     * The buffering view is always shown when the player is in the [ buffering][Player.STATE_BUFFERING] state.
     */
    val SHOW_BUFFERING_ALWAYS = 2
    // LINT.ThenChange(../../../../../../res/values/attrs.xml)

    // LINT.IfChange
    private val SURFACE_TYPE_NONE = 0
    private val SURFACE_TYPE_SURFACE_VIEW = 1
    private val SURFACE_TYPE_TEXTURE_VIEW = 2
    private val SURFACE_TYPE_MONO360_VIEW = 3
    // LINT.ThenChange(../../../../../../res/values/attrs.xml)

    private lateinit var contentFrame: AspectRatioFrameLayout
    private lateinit var shutterView: View
    private var surfaceView: View? = null
    private lateinit var artworkView: ImageView
    private lateinit var subtitleView: SubtitleView
    private lateinit var bufferingView: View
    private lateinit var errorMessageView: TextView
    private var controller: PiVideoPlayerControlView? = null
    private var componentListener = ComponentListener()

    private var player: PiVideoPlayer? = null
    private var useController: Boolean = false
    private var useArtwork: Boolean = false
    private var defaultArtwork: Drawable? = null
    @ShowBuffering
    private var showBuffering: Int = 0
    private var keepContentOnPlayerReset: Boolean = false
    private var errorMessageProvider: ErrorMessageProvider<in ExoPlaybackException>? = null
    private var customErrorMessage: CharSequence? = null
    private var controllerShowTimeoutMs: Int = 0
    private var controllerAutoShow: Boolean = false
    private var controllerHideDuringAds: Boolean = false
    private var controllerHideOnTouch: Boolean = false
    private var textureViewRotation: Int = 0
    private var isTouching: Boolean = false
    private val PICTURE_TYPE_FRONT_COVER = 3
    private val PICTURE_TYPE_NOT_SET = -1

    constructor(context: Context): this(context, null)

    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int): super(context, attrs, defStyleAttr) {

        if (isInEditMode) {
            /*contentFrame = null
            shutterView = null
            surfaceView = null
            artworkView = null
            subtitleView = null
            bufferingView = null
            errorMessageView = null
            controller = null
            componentListener = null*/
            val logo = ImageView(context)
            if (Util.SDK_INT >= 23) {
                configureEditModeLogoV23(resources, logo)
            } else {
                configureEditModeLogo(resources, logo)
            }
            addView(logo)
            return
        }

        var shutterColorSet = false
        var shutterColor = 0
        var playerLayoutId = R.layout.pi_video_player_view
        var useArtwork = true
        var defaultArtworkId = 0
        var useController = true
        var surfaceType = SURFACE_TYPE_SURFACE_VIEW
        var resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        var controllerShowTimeoutMs = PlayerControlView.DEFAULT_SHOW_TIMEOUT_MS
        var controllerHideOnTouch = true
        var controllerAutoShow = true
        var controllerHideDuringAds = true
        var showBuffering = SHOW_BUFFERING_NEVER
        if (attrs != null) {
            val a = context.theme.obtainStyledAttributes(attrs, R.styleable.PlayerView, 0, 0)
            try {
                shutterColorSet = a.hasValue(R.styleable.PlayerView_shutter_background_color)
                shutterColor =
                    a.getColor(R.styleable.PlayerView_shutter_background_color, shutterColor)
                playerLayoutId =
                    a.getResourceId(R.styleable.PlayerView_player_layout_id, playerLayoutId)
                useArtwork = a.getBoolean(R.styleable.PlayerView_use_artwork, useArtwork)
                defaultArtworkId =
                    a.getResourceId(R.styleable.PlayerView_default_artwork, defaultArtworkId)
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
                controllerHideDuringAds =
                    a.getBoolean(R.styleable.PlayerView_hide_during_ads, controllerHideDuringAds)
            } finally {
                a.recycle()
            }
        }

        LayoutInflater.from(context).inflate(playerLayoutId, this)
        descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS

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
                    sphericalSurfaceView.setSingleTapListener(componentListener)
                    sphericalSurfaceView
                }
                else -> SurfaceView(context)
            }
            surfaceView!!.layoutParams = params
            contentFrame.addView(surfaceView, 0)
        } else {
            surfaceView = null
        }

        // Ad overlay frame layout.
        //adOverlayFrameLayout = findViewById<FrameLayout>(R.id.pi_ad_overlay)

        // Overlay frame layout.
        //overlayFrameLayout = findViewById<FrameLayout>(R.id.pi_overlay)

        // Artwork view.
        artworkView = findViewById(R.id.pi_artwork)
        this.useArtwork = useArtwork
        if (defaultArtworkId != 0) {
            defaultArtwork = ContextCompat.getDrawable(getContext(), defaultArtworkId)
        }

        // Subtitle view.
        subtitleView = findViewById(R.id.pi_subtitles)
        subtitleView.setUserDefaultStyle()
        subtitleView.setUserDefaultTextSize()

        // Buffering view.
        bufferingView = findViewById(R.id.pi_buffering)
        bufferingView.visibility = View.GONE
        this.showBuffering = showBuffering

        // Error message view.
        errorMessageView = findViewById(R.id.pi_error_message)
        errorMessageView.visibility = View.GONE

        // Playback control view.
        val customController = findViewById<PiVideoPlayerControlView>(R.id.pi_controller)
        val controllerPlaceholder = findViewById<View>(R.id.pi_controller_placeholder)
        when {
            customController != null -> this.controller = customController
            controllerPlaceholder != null -> {
                // Propagate attrs as playbackAttrs so that PlayerControlView's custom attributes are
                // transferred, but standard attributes (e.g. background) are not.
                this.controller = PiVideoPlayerControlView(context, null, 0, attrs)
                controller!!.id = R.id.pi_controller
                controller!!.layoutParams = controllerPlaceholder.layoutParams
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
        this.controllerHideDuringAds = controllerHideDuringAds
        this.useController = useController && controller != null
        hideController()
    }

    /**
     * Switches the view targeted by a given [Player].
     *
     * @param player The player whose target view is being switched.
     * @param oldPlayerView The old view to detach from the player.
     * @param newPlayerView The new view to attach to the player.
     */
    fun switchTargetView(
        player: PiVideoPlayer, oldPlayerView: PiVideoPlayerView?, newPlayerView: PiVideoPlayerView?
    ) {
        if (oldPlayerView === newPlayerView) {
            return
        }
        // We attach the new view before detaching the old one because this ordering allows the player
        // to swap directly from one surface to another, without transitioning through a state where no
        // surface is attached. This is significantly more efficient and achieves a more seamless
        // transition when using platform provided video decoders.
        newPlayerView?.setPlayer(player)
        oldPlayerView?.setPlayer(null)
    }

    /**
     * Should be called when the player is visible to the user and if `surface_type` is `spherical_view`. It is the counterpart to [.onPause].
     *
     *
     * This method should typically be called in [Activity.onStart], or [ ][Activity.onResume] for API versions &lt;= 23.
     */
    fun onResume() {
        if (surfaceView is SphericalSurfaceView) {
            (surfaceView as SphericalSurfaceView).onResume()
        }
    }

    /**
     * Should be called when the player is no longer visible to the user and if `surface_type`
     * is `spherical_view`. It is the counterpart to [.onResume].
     *
     *
     * This method should typically be called in [Activity.onStop], or [ ][Activity.onPause] for API versions &lt;= 23.
     */
    fun onPause() {
        if (surfaceView is SphericalSurfaceView) {
            (surfaceView as SphericalSurfaceView).onPause()
        }
    }

    /** Returns the player currently set on this view, or null if no player is set.  */
    fun getPlayer(): Player? {
        return player!!.getPlayer()
    }

    /**
     * Set the [Player] to use.
     *
     *
     * To transition a [Player] from targeting one view to another, it's recommended to use
     * [.switchTargetView] rather than this method. If you do
     * wish to use this method directly, be sure to attach the videoPlayer to the new view *before*
     * calling `setPlayer(null)` to detach it from the old one. This ordering is significantly
     * more efficient and may allow for more seamless transitions.
     *
     * @param videoPlayer The [Player] to use, or `null` to detach the current videoPlayer. Only
     * players which are accessed on the main thread are supported (`videoPlayer.getApplicationLooper() == Looper.getMainLooper()`).
     */
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
            val oldVideoComponent = this.player?.getVideoComponent()
            if (oldVideoComponent != null) {
                oldVideoComponent.removeVideoListener(componentListener)
                when (surfaceView) {
                    is TextureView -> oldVideoComponent.clearVideoTextureView(surfaceView as TextureView)
                    is SphericalSurfaceView -> (surfaceView as SphericalSurfaceView).setVideoComponent(null)
                    is SurfaceView -> oldVideoComponent.clearVideoSurfaceView(surfaceView as SurfaceView)
                }
            }
            val oldTextComponent = this.player?.getTextComponent()
            oldTextComponent?.removeTextOutput(componentListener)
        }
        this.player = videoPlayer
        if (useController) {
            controller!!.setPiPlayer(videoPlayer!!.getPlayer())
        }
        subtitleView.setCues(null)
        updateBuffering()
        updateErrorMessage()
        updateForCurrentTrackSelections(/* isNewPlayer= */true)
        if (videoPlayer != null) {
            val newVideoComponent = videoPlayer.getVideoComponent()
            if (newVideoComponent != null) {
                when (surfaceView) {
                    is TextureView -> newVideoComponent.setVideoTextureView(surfaceView as TextureView)
                    is SphericalSurfaceView -> (surfaceView as SphericalSurfaceView).setVideoComponent(newVideoComponent)
                    is SurfaceView -> newVideoComponent.setVideoSurfaceView(surfaceView as SurfaceView)
                }
                newVideoComponent.addVideoListener(componentListener)
            }
            val newTextComponent = videoPlayer.getTextComponent()
            newTextComponent.addTextOutput(componentListener)
            videoPlayer.addListener(componentListener)
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

    @TargetApi(23)
    private fun configureEditModeLogoV23(resources: Resources, logo: ImageView) {
        logo.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.exo_edit_mode_logo, null))
        logo.setBackgroundColor(ResourcesCompat.getColor(resources, R.color.exo_edit_mode_background_color, null))
    }

    private fun configureEditModeLogo(resources: Resources, logo: ImageView) {
        logo.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.exo_edit_mode_logo, null))
        logo.setBackgroundColor(ResourcesCompat.getColor(resources, R.color.exo_edit_mode_background_color, null))
    }

    private fun setArtworkFromMetadata(metadata: Metadata): Boolean {
        var isArtworkSet = false
        var currentPictureType = PICTURE_TYPE_NOT_SET
        for (i in 0 until metadata.length()) {
            val metadataEntry = metadata.get(i)
            val pictureType: Int
            val bitmapData: ByteArray
            if (metadataEntry is ApicFrame) {
                bitmapData = metadataEntry.pictureData
                pictureType = metadataEntry.pictureType
            } else if (metadataEntry is PictureFrame) {
                bitmapData = metadataEntry.pictureData
                pictureType = metadataEntry.pictureType
            } else {
                continue
            }
            // Prefer the first front cover picture. If there aren't any, prefer the first picture.
            if (currentPictureType == PICTURE_TYPE_NOT_SET || pictureType == PICTURE_TYPE_FRONT_COVER) {
                val bitmap = BitmapFactory.decodeByteArray(bitmapData, 0, bitmapData.size)
                isArtworkSet = setDrawableArtwork(BitmapDrawable(resources, bitmap))
                currentPictureType = pictureType
                if (currentPictureType == PICTURE_TYPE_FRONT_COVER) {
                    break
                }
            }
        }
        return isArtworkSet
    }

    private fun setDrawableArtwork(drawable: Drawable?): Boolean {
        if (drawable != null) {
            val drawableWidth = drawable.intrinsicWidth
            val drawableHeight = drawable.intrinsicHeight
            if (drawableWidth > 0 && drawableHeight > 0) {
                val artworkAspectRatio = drawableWidth.toFloat() / drawableHeight
                onContentAspectRatioChanged(artworkAspectRatio, contentFrame, artworkView)
                artworkView.setImageDrawable(drawable)
                artworkView.visibility = View.VISIBLE
                return true
            }
        }
        return false
    }

    private fun hideArtwork() {
        artworkView.setImageResource(android.R.color.transparent) // Clears any bitmap reference.
        artworkView.visibility = View.INVISIBLE
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

    /** Hides the playback controls. Does nothing if playback controls are disabled.  */
    fun hideController() {
        controller?.hide()
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
                hideArtwork()
                closeShutter()
            }
            return
        }

        if (isNewPlayer && !keepContentOnPlayerReset) {
            // Hide any video from the previous player.
            closeShutter()
        }

        val selections = player?.getCurrentTrackSelections()
        for (i in 0 until selections!!.length) {
            if (player?.getRendererType(i) == C.TRACK_TYPE_VIDEO && selections.get(i) != null) {
                // Video enabled so artwork must be hidden. If the shutter is closed, it will be opened in
                // onRenderedFirstFrame().
                hideArtwork()
                return
            }
        }

        // Video disabled so the shutter must be closed.
        closeShutter()
        // Display artwork if enabled and available, else hide it.
        if (useArtwork) {
            for (i in 0 until selections.length) {
                val selection = selections.get(i)
                if (selection != null) {
                    for (j in 0 until selection.length()) {
                        val metadata = selection.getFormat(j).metadata
                        if (metadata != null && setArtworkFromMetadata(metadata)) {
                            return
                        }
                    }
                }
            }
            if (setDrawableArtwork(defaultArtwork)) {
                return
            }
        }
        // Artwork disabled or unavailable.
        hideArtwork()
    }

    private fun isPlayingAd(): Boolean {
        return player != null && player!!.isPlayingAd() && player!!.playWhenReady
    }

    /** Shows the playback controls, but only if forced or shown indefinitely.  */
    private fun maybeShowController(isForced: Boolean) {
        if (isPlayingAd() && controllerHideDuringAds) {
            return
        }
        if (useController) {
            val wasShowingIndefinitely = controller!!.isVisible() && controller!!.showTimeoutMillis <= 0
            val shouldShowIndefinitely = shouldShowControllerIndefinitely()
            if (isForced || wasShowingIndefinitely || shouldShowIndefinitely) {
                showController(shouldShowIndefinitely)
            }
        }
    }

    fun showController(showIndefinitely: Boolean) {
        if (!useController) {
            return
        }
        controller!!.showTimeoutMillis = if (showIndefinitely) 0 else controllerShowTimeoutMs
        controller?.show()
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
        }
        return true
    }

    private inner class ComponentListener : Player.EventListener, TextOutput, VideoListener,
        OnLayoutChangeListener,
        SingleTapListener {

        // TextOutput implementation

        override fun onCues(cues: List<Cue>) {
            subtitleView.onCues(cues)
        }

        // VideoListener implementation

        override fun onVideoSizeChanged(
            width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float
        ) {
            var videoAspectRatio: Float = if (height == 0 || width == 0) 1F else width * pixelWidthHeightRatio / height

            if (surfaceView is TextureView) {
                // Try to apply rotation transformation when our surface is a TextureView.
                if (unappliedRotationDegrees == 90 || unappliedRotationDegrees == 270) {
                    // We will apply a rotation 90/270 degree to the output texture of the TextureView.
                    // In this case, the output video's width and height will be swapped.
                    videoAspectRatio = 1 / videoAspectRatio
                }
                if (textureViewRotation != 0) {
                    surfaceView?.removeOnLayoutChangeListener(this)
                }
                textureViewRotation = unappliedRotationDegrees
                if (textureViewRotation != 0) {
                    // The texture view's dimensions might be changed after layout step.
                    // So add an OnLayoutChangeListener to apply rotation after layout step.
                    surfaceView?.addOnLayoutChangeListener(this)
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
            if (isPlayingAd() && controllerHideDuringAds) {
                hideController()
            } else {
                maybeShowController(false)
            }
        }

        override fun onPositionDiscontinuity(@Player.DiscontinuityReason reason: Int) {
            if (isPlayingAd() && controllerHideDuringAds) {
                hideController()
            }
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

        // SingleTapListener implementation

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            return toggleControllerVisibility()
        }
    }

}

