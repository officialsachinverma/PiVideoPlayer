package com.project100pi.library.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import android.os.Looper
import android.os.SystemClock
import android.util.AttributeSet
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.ui.DefaultTimeBar
import com.google.android.exoplayer2.ui.TimeBar
import com.google.android.exoplayer2.util.Assertions
import com.google.android.exoplayer2.util.RepeatModeUtil
import com.google.android.exoplayer2.util.Util
import com.project100pi.library.R
import java.util.*
import kotlin.math.min

class PiVideoPlayerControlView: FrameLayout {

    /** Listener to be notified about changes of the visibility of the UI control.  */
    interface VisibilityListener {

        /**
         * Called when the visibility changes.
         *
         * @param visibility The new visibility. Either [View.VISIBLE] or [View.GONE].
         */
        fun onVisibilityChange(visibility: Int)
    }

    /** Listener to be notified when progress has been updated.  */
    interface ProgressUpdateListener {

        /**
         * Called when progress needs to be updated.
         *
         * @param position The current position.
         * @param bufferedPosition The current buffered position.
         */
        fun onProgressUpdate(position: Long, bufferedPosition: Long)
    }

    /** The default fast forward increment, in milliseconds.  */
    val DEFAULT_FAST_FORWARD_MS = 15000
    /** The default rewind increment, in milliseconds.  */
    val DEFAULT_REWIND_MS = 5000
    /** The default show timeout, in milliseconds.  */
    val DEFAULT_SHOW_TIMEOUT_MS = 5000
    /** The default repeat toggle modes.  */
    @RepeatModeUtil.RepeatToggleModes
    val DEFAULT_REPEAT_TOGGLE_MODES = RepeatModeUtil.REPEAT_TOGGLE_MODE_NONE
    /** The default minimum interval between time bar position updates.  */
    val DEFAULT_TIME_BAR_MIN_UPDATE_INTERVAL_MS = 200
    /** The maximum number of windows that can be shown in a multi-window time bar.  */
    val MAX_WINDOWS_FOR_MULTI_WINDOW_TIME_BAR = 100

    private val MAX_POSITION_FOR_SEEK_TO_PREVIOUS: Long = 3000
    /** The maximum interval between time bar position updates.  */
    private val MAX_UPDATE_INTERVAL_MS = 1000

    private var isPiAttachedToWindow = false
    private var showMultiWindowTimeBar: Boolean = false
    private var multiWindowTimeBar: Boolean = false
    private var scrubbing: Boolean = false
    private var rewindMs: Int = 0
    private var fastForwardMs: Int = 0
    var showTimeoutMillis: Int = 0
    private var timeBarMinUpdateIntervalMs = 0
    @RepeatModeUtil.RepeatToggleModes
    private var repeatToggleModes: Int = 0
    private var hideAtMs: Long = 0
    private var currentWindowOffset: Long = 0

    private var componentListener: ComponentListener
    private var previousButton: View
    private var nextButton: View
    private var playButton: View
    private var pauseButton: View
    private var fastForwardButton: View
    private var rewindButton: View
    private var repeatToggleButton: ImageView
    private var durationView: TextView
    private var positionView: TextView
    private var timeBar: TimeBar?
    private var formatBuilder: StringBuilder
    private var formatter: Formatter
    private var period: Timeline.Period
    private var window: Timeline.Window
    private var updateProgressAction: Runnable
    private var hideAction: Runnable

    private var repeatOffButtonDrawable: Drawable?
    private var repeatOneButtonDrawable: Drawable?
    private var repeatAllButtonDrawable: Drawable?
    private var repeatOffButtonContentDescription: String
    private var repeatOneButtonContentDescription: String
    private var repeatAllButtonContentDescription: String

    var player: Player? = null
    private var controlDispatcher: ControlDispatcher
    private var visibilityListener: VisibilityListener? = null
    private var progressUpdateListener: ProgressUpdateListener? = null
    private var playbackPreparer: PlaybackPreparer? = null

    constructor(context: Context): this(context, null)

    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int): this(context, attrs, defStyleAttr, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, playbackAttrs: AttributeSet?): super(context, attrs, defStyleAttr) {

        var controllerLayoutId = R.layout.pi_player_control_view

        rewindMs = DEFAULT_REWIND_MS
        fastForwardMs = DEFAULT_FAST_FORWARD_MS
        showTimeoutMillis = DEFAULT_SHOW_TIMEOUT_MS
        repeatToggleModes = DEFAULT_REPEAT_TOGGLE_MODES
        timeBarMinUpdateIntervalMs = DEFAULT_TIME_BAR_MIN_UPDATE_INTERVAL_MS
        hideAtMs = C.TIME_UNSET

        if (playbackAttrs != null) {
            val a = context
                .theme
                .obtainStyledAttributes(playbackAttrs, R.styleable.PlayerControlView, 0, 0)
            try {
                rewindMs = a.getInt(R.styleable.PlayerControlView_rewind_increment, rewindMs)
                fastForwardMs =
                    a.getInt(R.styleable.PlayerControlView_fastforward_increment, fastForwardMs)
                showTimeoutMillis = a.getInt(R.styleable.PlayerControlView_show_timeout, showTimeoutMillis)
                controllerLayoutId = a.getResourceId(
                    R.styleable.PlayerControlView_controller_layout_id,
                    controllerLayoutId
                )
                repeatToggleModes = getRepeatToggleModes(a, repeatToggleModes)

                setTimeBarMinUpdateInterval(
                    a.getInt(
                        R.styleable.PlayerControlView_time_bar_min_update_interval,
                        timeBarMinUpdateIntervalMs
                    )
                )
            } finally {
                a.recycle()
            }
        }

        period = Timeline.Period()
        window = Timeline.Window()
        window.isSeekable = true
        formatBuilder = StringBuilder()
        formatter = Formatter(formatBuilder, Locale.getDefault())
        componentListener = ComponentListener()
        controlDispatcher = DefaultControlDispatcher()
        updateProgressAction = Runnable { this.updateProgress() }
        hideAction = Runnable { this.hide() }

        LayoutInflater.from(context).inflate(controllerLayoutId, this)
        descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS

        val customTimeBar = findViewById<DefaultTimeBar>(R.id.pi_progress)
        val timeBarPlaceholder = findViewById<View>(R.id.pi_progress_placeholder)
        when {
            customTimeBar != null -> timeBar = customTimeBar
            timeBarPlaceholder != null -> {
                // Propagate attrs as timebarAttrs so that DefaultTimeBar's custom attributes are transferred,
                // but standard attributes (e.g. background) are not.
                val defaultTimeBar = DefaultTimeBar(context, null, 0, playbackAttrs)
                defaultTimeBar.id = R.id.pi_progress
                defaultTimeBar.layoutParams = timeBarPlaceholder.layoutParams
                val parent = timeBarPlaceholder.parent as ViewGroup
                val timeBarIndex = parent.indexOfChild(timeBarPlaceholder)
                parent.removeView(timeBarPlaceholder)
                parent.addView(defaultTimeBar, timeBarIndex)
                timeBar = defaultTimeBar
            }
            else -> timeBar = null
        }

        durationView = findViewById(R.id.pi_duration)
        positionView = findViewById(R.id.pi_position)

        if (timeBar != null) {
            timeBar?.addListener(componentListener)
        }

        // Play
        playButton = findViewById(R.id.pi_play)
        playButton.setOnClickListener(componentListener)

        // Pause
        pauseButton = findViewById(R.id.pi_pause)
        pauseButton.setOnClickListener(componentListener)

        // Previous
        previousButton = findViewById(R.id.pi_prev)
        previousButton.setOnClickListener(componentListener)

        // Next
        nextButton = findViewById(R.id.pi_next)
        nextButton.setOnClickListener(componentListener)

        // Rewind
        rewindButton = findViewById(R.id.pi_rew)
        rewindButton.setOnClickListener(componentListener)
        rewindButton.visibility = View.GONE

        // Fast Forward
        fastForwardButton = findViewById(R.id.pi_ffwd)
        fastForwardButton.setOnClickListener(componentListener)
        fastForwardButton.visibility = View.GONE

        // Repeat
        repeatToggleButton = findViewById(R.id.pi_repeat_toggle)
        repeatToggleButton.setOnClickListener(componentListener)

        val resources = context.resources
        repeatOffButtonDrawable = ResourcesCompat.getDrawable(getResources(), R.drawable.exo_controls_repeat_off, null)
        repeatOneButtonDrawable = ResourcesCompat.getDrawable(getResources(), R.drawable.exo_controls_repeat_one, null)
        repeatAllButtonDrawable = ResourcesCompat.getDrawable(getResources(), R.drawable.exo_controls_repeat_all, null)
        repeatOffButtonContentDescription =
            resources.getString(R.string.exo_controls_repeat_off_description)
        repeatOneButtonContentDescription =
            resources.getString(R.string.exo_controls_repeat_one_description)
        repeatAllButtonContentDescription =
            resources.getString(R.string.exo_controls_repeat_all_description)
    }

    @RepeatModeUtil.RepeatToggleModes
    private fun getRepeatToggleModes(
        a: TypedArray, @RepeatModeUtil.RepeatToggleModes repeatToggleModes: Int
    ): Int {
        return a.getInt(R.styleable.PlayerControlView_repeat_toggle_modes, repeatToggleModes)
    }

    /**
     * Sets the [Player] to control.
     *
     * @param player The [Player] to control, or `null` to detach the current player. Only
     * players which are accessed on the main thread are supported (`player.getApplicationLooper() == Looper.getMainLooper()`).
     */
    fun setPiPlayer(player: Player?) {
        Assertions.checkState(Looper.myLooper() == Looper.getMainLooper())
        Assertions.checkArgument(
            player == null || player.applicationLooper == Looper.getMainLooper()
        )
        if (this.player === player) {
            return
        }
        if (this.player != null) {
            this.player!!.removeListener(componentListener)
        }
        this.player = player
        player?.addListener(componentListener)
        updateAll()
    }

    /**
     * Sets the minimum interval between time bar position updates.
     *
     *
     * Note that smaller intervals, e.g. 33ms, will result in a smooth movement but will use more
     * CPU resources while the time bar is visible, whereas larger intervals, e.g. 200ms, will result
     * in a step-wise update with less CPU usage.
     *
     * @param minUpdateIntervalMs The minimum interval between time bar position updates, in
     * milliseconds.
     */
    private fun setTimeBarMinUpdateInterval(minUpdateIntervalMs: Int) {
        // Do not accept values below 16ms (60fps) and larger than the maximum update interval.
        timeBarMinUpdateIntervalMs =
            Util.constrainValue(minUpdateIntervalMs, 16, MAX_UPDATE_INTERVAL_MS)
    }

    /**
     * Shows the playback controls. If [.getShowTimeoutMillis] is positive then the controls will
     * be automatically hidden after this duration of time has elapsed without user input.
     */
    fun show() {
        if (!isVisible()) {
            visibility = View.VISIBLE
            if (visibilityListener != null) {
                visibilityListener!!.onVisibilityChange(visibility)
            }
            updateAll()
            requestPlayPauseFocus()
        }
        // Call hideAfterTimeout even if already visible to reset the timeout.
        hideAfterTimeout()
    }

    /** Hides the controller.  */
    fun hide() {
        if (isVisible()) {
            visibility = View.GONE
            if (visibilityListener != null) {
                visibilityListener!!.onVisibilityChange(visibility)
            }
            removeCallbacks(updateProgressAction)
            removeCallbacks(hideAction)
            hideAtMs = C.TIME_UNSET
        }
    }

    /** Returns whether the controller is currently visible.  */
    fun isVisible(): Boolean {
        return visibility == View.VISIBLE
    }

    private fun hideAfterTimeout() {
        removeCallbacks(hideAction)
        if (showTimeoutMillis > 0) {
            hideAtMs = SystemClock.uptimeMillis() + showTimeoutMillis
            if (isPiAttachedToWindow) {
                postDelayed(hideAction, showTimeoutMillis.toLong())
            }
        } else {
            hideAtMs = C.TIME_UNSET
        }
    }

    private fun updateAll() {
        updatePlayPauseButton()
        updateNavigation()
        updateRepeatModeButton()
        updateTimeline()
    }

    private fun updatePlayPauseButton() {
        if (!isVisible() || !isPiAttachedToWindow) {
            return
        }
        var requestPlayPauseFocus = false
        val playing = isPlaying()

        requestPlayPauseFocus = requestPlayPauseFocus or (playing && playButton.isFocused)
        playButton.visibility = if (playing) View.GONE else View.VISIBLE

        requestPlayPauseFocus = requestPlayPauseFocus or (!playing && pauseButton.isFocused)
        pauseButton.visibility = if (!playing) View.GONE else View.VISIBLE

        if (requestPlayPauseFocus) {
            requestPlayPauseFocus()
        }
    }

    private fun updateNavigation() {
        if (!isVisible() || !isPiAttachedToWindow) {
            return
        }
        var enableSeeking = false
        var enablePrevious = false
        var enableRewind = false
        var enableFastForward = false
        var enableNext = false
        if (player != null) {
            val timeline = player!!.currentTimeline
            if (!timeline.isEmpty && !player!!.isPlayingAd) {
                timeline.getWindow(player!!.currentWindowIndex, window)
                val isSeekable = window.isSeekable
                enableSeeking = isSeekable
                enablePrevious = isSeekable || !window.isDynamic || player!!.hasPrevious()
                //enableRewind = isSeekable && rewindMs > 0 // Commented because we dont want rewind as user can seek
                //enableFastForward = isSeekable && fastForwardMs > 0 // Commented because we don't want ''orward as user can seek
                enableNext = window.isDynamic || player!!.hasNext()
            }
        }

        setButtonEnabled(enablePrevious, previousButton)
        //setButtonEnabled(enableRewind, rewindButton)
        //setButtonEnabled(enableFastForward, fastForwardButton)
        setButtonEnabled(enableNext, nextButton)
        if (timeBar != null) {
            timeBar?.setEnabled(enableSeeking)
        }
    }

    private fun updateRepeatModeButton() {
        if (!isVisible() || !isPiAttachedToWindow) {
            return
        }
        if (repeatToggleModes == RepeatModeUtil.REPEAT_TOGGLE_MODE_NONE) {
            repeatToggleButton.visibility = View.GONE
            return
        }
        if (player == null) {
            setButtonEnabled(false, repeatToggleButton)
            return
        }
        setButtonEnabled(true, repeatToggleButton)
        when (player!!.repeatMode) {
            Player.REPEAT_MODE_OFF -> {
                repeatToggleButton.setImageDrawable(repeatOffButtonDrawable)
                repeatToggleButton.contentDescription = repeatOffButtonContentDescription
            }
            Player.REPEAT_MODE_ONE -> {
                repeatToggleButton.setImageDrawable(repeatOneButtonDrawable)
                repeatToggleButton.contentDescription = repeatOneButtonContentDescription
            }
            Player.REPEAT_MODE_ALL -> {
                repeatToggleButton.setImageDrawable(repeatAllButtonDrawable)
                repeatToggleButton.contentDescription = repeatAllButtonContentDescription
            }
        }// Never happens.
        repeatToggleButton.visibility = View.VISIBLE
    }

    private fun updateTimeline() {
        if (player == null) {
            return
        }
        multiWindowTimeBar =
            showMultiWindowTimeBar && canShowMultiWindowTimeBar(player!!.currentTimeline, window)
        currentWindowOffset = 0
        var durationUs: Long = 0
        val timeline = player!!.currentTimeline
        if (!timeline.isEmpty) {
            val currentWindowIndex = player!!.currentWindowIndex
            val firstWindowIndex = if (multiWindowTimeBar) 0 else currentWindowIndex
            val lastWindowIndex =
                if (multiWindowTimeBar) timeline.windowCount - 1 else currentWindowIndex
            for (i in firstWindowIndex..lastWindowIndex) {
                if (i == currentWindowIndex) {
                    currentWindowOffset = C.usToMs(durationUs)
                }
                timeline.getWindow(i, window)
                if (window.durationUs == C.TIME_UNSET) {
                    Assertions.checkState(!multiWindowTimeBar)
                    break
                }
                for (j in window.firstPeriodIndex..window.lastPeriodIndex) {
                    timeline.getPeriod(j, period)
                }
                durationUs += window.durationUs
            }
        }
        val durationMs = C.usToMs(durationUs)

        durationView.text = Util.getStringForTime(formatBuilder, formatter, durationMs)

        if (timeBar != null) {
            timeBar?.setDuration(durationMs)
        }
        updateProgress()
    }

    private fun updateProgress() {
        if (!isVisible() || !isPiAttachedToWindow) {
            return
        }

        var position: Long = 0
        var bufferedPosition: Long = 0
        if (player != null) {
            position = currentWindowOffset + player!!.contentPosition
            bufferedPosition = currentWindowOffset + player!!.contentBufferedPosition
        }
        if (!scrubbing) {
            positionView.text = Util.getStringForTime(formatBuilder, formatter, position)
        }
        if (timeBar != null) {
            timeBar?.setPosition(position)
            timeBar?.setBufferedPosition(bufferedPosition)
        }
        if (progressUpdateListener != null) {
            progressUpdateListener!!.onProgressUpdate(position, bufferedPosition)
        }

        // Cancel any pending updates and schedule a new one if necessary.
        removeCallbacks(updateProgressAction)
        val playbackState = if (player == null) Player.STATE_IDLE else player!!.playbackState
        if (playbackState == Player.STATE_READY && player!!.playWhenReady) {
            var mediaTimeDelayMs =
                if (timeBar != null) timeBar!!.preferredUpdateDelay else MAX_UPDATE_INTERVAL_MS.toLong()

            // Limit delay to the start of the next full second to ensure position display is smooth.
            val mediaTimeUntilNextFullSecondMs = 1000 - position % 1000
            mediaTimeDelayMs = min(mediaTimeDelayMs, mediaTimeUntilNextFullSecondMs)

            // Calculate the delay until the next update in real time, taking playbackSpeed into account.
            val playbackSpeed = player!!.playbackParameters.speed
            var delayMs: Long =
                if (playbackSpeed > 0) (mediaTimeDelayMs / playbackSpeed).toLong() else 1000

            // Constrain the delay to avoid too frequent / infrequent updates.
            delayMs = Util.constrainValue(delayMs, timeBarMinUpdateIntervalMs.toLong(), 1000
            )
            postDelayed(updateProgressAction, delayMs)
        } else if (playbackState != Player.STATE_ENDED && playbackState != Player.STATE_IDLE) {
            postDelayed(updateProgressAction, MAX_UPDATE_INTERVAL_MS.toLong())
        }
    }

    private fun requestPlayPauseFocus() {
        val playing = isPlaying()
        if (!playing) {
            playButton.requestFocus()
        } else if (playing) {
            pauseButton.requestFocus()
        }
    }

    private fun setButtonEnabled(enabled: Boolean, view: View?) {
        if (view == null) {
            return
        }
        view.isEnabled = enabled
        view.alpha = if (enabled) 1f else 0.3f
        view.visibility = View.VISIBLE
    }

    private fun previous(player: Player) {
        val timeline = player.currentTimeline
        if (timeline.isEmpty || player.isPlayingAd) {
            return
        }
        val windowIndex = player.currentWindowIndex
        timeline.getWindow(windowIndex, window)
        val previousWindowIndex = player.previousWindowIndex
        if (previousWindowIndex != C.INDEX_UNSET && (player.currentPosition <= MAX_POSITION_FOR_SEEK_TO_PREVIOUS || window.isDynamic && !window.isSeekable)) {
            seekTo(player, previousWindowIndex, C.TIME_UNSET)
        } else {
            seekTo(player, 0)
        }
    }

    private fun next(player: Player) {
        val timeline = player.currentTimeline
        if (timeline.isEmpty || player.isPlayingAd) {
            return
        }
        val windowIndex = player.currentWindowIndex
        val nextWindowIndex = player.nextWindowIndex
        if (nextWindowIndex != C.INDEX_UNSET) {
            seekTo(player, nextWindowIndex, C.TIME_UNSET)
        } else if (timeline.getWindow(windowIndex, window).isDynamic) {
            seekTo(player, windowIndex, C.TIME_UNSET)
        }
    }

    private fun rewind(player: Player) {
        if (player.isCurrentWindowSeekable && rewindMs > 0) {
            seekTo(player, player.currentPosition - rewindMs)
        }
    }

    private fun fastForward(player: Player) {
        if (player.isCurrentWindowSeekable && fastForwardMs > 0) {
            seekTo(player, player.currentPosition + fastForwardMs)
        }
    }

    private fun seekTo(player: Player, positionMs: Long) {
        seekTo(player, player.currentWindowIndex, positionMs)
    }

    private fun seekTo(player: Player, windowIndex: Int, positionMs: Long): Boolean {
        var positionMs = positionMs
        val durationMs = player.duration
        if (durationMs != C.TIME_UNSET) {
            positionMs = min(positionMs, durationMs)
        }
        positionMs = Math.max(positionMs, 0)
        return controlDispatcher.dispatchSeekTo(player, windowIndex, positionMs)
    }

    private fun seekToTimeBarPosition(player: Player, positionMs: Long) {
        var positionMs = positionMs
        var windowIndex: Int
        val timeline = player.currentTimeline
        if (multiWindowTimeBar && !timeline.isEmpty) {
            val windowCount = timeline.windowCount
            windowIndex = 0
            while (true) {
                val windowDurationMs = timeline.getWindow(windowIndex, window).durationMs
                if (positionMs < windowDurationMs) {
                    break
                } else if (windowIndex == windowCount - 1) {
                    // Seeking past the end of the last window should seek to the end of the timeline.
                    positionMs = windowDurationMs
                    break
                }
                positionMs -= windowDurationMs
                windowIndex++
            }
        } else {
            windowIndex = player.currentWindowIndex
        }
        val dispatched = seekTo(player, windowIndex, positionMs)
        if (!dispatched) {
            // The seek wasn't dispatched then the progress bar scrubber will be in the wrong position.
            // Trigger a progress update to snap it back.
            updateProgress()
        }
    }

    public override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isPiAttachedToWindow = true
        if (hideAtMs != C.TIME_UNSET) {
            val delayMs = hideAtMs - SystemClock.uptimeMillis()
            if (delayMs <= 0) {
                hide()
            } else {
                postDelayed(hideAction, delayMs)
            }
        } else if (isVisible()) {
            hideAfterTimeout()
        }
        updateAll()
    }

    public override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        isPiAttachedToWindow = false
        removeCallbacks(updateProgressAction)
        removeCallbacks(hideAction)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            removeCallbacks(hideAction)
        } else if (ev.action == MotionEvent.ACTION_UP) {
            hideAfterTimeout()
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        return dispatchMediaKeyEvent(event) || super.dispatchKeyEvent(event)
    }

    /**
     * Called to process media key events. Any [KeyEvent] can be passed but only media key
     * events will be handled.
     *
     * @param event A key event.
     * @return Whether the key event was handled.
     */
    private fun dispatchMediaKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        if (player == null || !isHandledMediaKey(keyCode)) {
            return false
        }
        if (event.action == KeyEvent.ACTION_DOWN) {
            when {
                keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> fastForward(player!!)
                keyCode == KeyEvent.KEYCODE_MEDIA_REWIND -> rewind(player!!)
                event.repeatCount == 0 -> when (keyCode) {
                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> controlDispatcher.dispatchSetPlayWhenReady(
                        player,
                        !player!!.playWhenReady
                    )
                    KeyEvent.KEYCODE_MEDIA_PLAY -> controlDispatcher.dispatchSetPlayWhenReady(
                        player,
                        true
                    )
                    KeyEvent.KEYCODE_MEDIA_PAUSE -> controlDispatcher.dispatchSetPlayWhenReady(
                        player,
                        false
                    )
                    KeyEvent.KEYCODE_MEDIA_NEXT -> next(player!!)
                    KeyEvent.KEYCODE_MEDIA_PREVIOUS -> previous(player!!)
                    else -> {
                    }
                }
            }
        }
        return true
    }

    private fun isPlaying(): Boolean {
        return (player != null
                && player!!.playbackState != Player.STATE_ENDED
                && player!!.playbackState != Player.STATE_IDLE
                && player!!.playWhenReady)
    }

    @SuppressLint("InlinedApi")
    private fun isHandledMediaKey(keyCode: Int): Boolean {
        return (keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD
                || keyCode == KeyEvent.KEYCODE_MEDIA_REWIND
                || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY
                || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE
                || keyCode == KeyEvent.KEYCODE_MEDIA_NEXT
                || keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS)
    }

    /**
     * Returns whether the specified `timeline` can be shown on a multi-window time bar.
     *
     * @param timeline The [Timeline] to check.
     * @param window A scratch [Timeline.Window] instance.
     * @return Whether the specified timeline can be shown on a multi-window time bar.
     */
    private fun canShowMultiWindowTimeBar(timeline: Timeline, window: Timeline.Window): Boolean {
        if (timeline.windowCount > MAX_WINDOWS_FOR_MULTI_WINDOW_TIME_BAR) {
            return false
        }
        val windowCount = timeline.windowCount
        for (i in 0 until windowCount) {
            if (timeline.getWindow(i, window).durationUs == C.TIME_UNSET) {
                return false
            }
        }
        return true
    }

    private inner class ComponentListener : Player.EventListener, TimeBar.OnScrubListener,
        OnClickListener {

        override fun onScrubStart(timeBar: TimeBar, position: Long) {
            scrubbing = true
            positionView.text = Util.getStringForTime(formatBuilder, formatter, position)
        }

        override fun onScrubMove(timeBar: TimeBar, position: Long) {
            positionView.text = Util.getStringForTime(formatBuilder, formatter, position)
        }

        override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
            scrubbing = false
            if (!canceled && player != null) {
                seekToTimeBarPosition(player!!, position)
            }
        }

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            updatePlayPauseButton()
            updateProgress()
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            updateRepeatModeButton()
            updateNavigation()
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            updateNavigation()
        }

        override fun onPositionDiscontinuity(@Player.DiscontinuityReason reason: Int) {
            updateNavigation()
            updateTimeline()
        }

        override fun onTimelineChanged(
            timeline: Timeline?, manifest: Any?, @Player.TimelineChangeReason reason: Int
        ) {
            updateNavigation()
            updateTimeline()
        }

        override fun onClick(view: View) {
            val player = this@PiVideoPlayerControlView.player ?: return
            when {
                nextButton === view -> next(player)
                previousButton === view -> previous(player)
                fastForwardButton === view -> fastForward(player)
                rewindButton === view -> rewind(player)
                playButton === view -> {
                    if (player.playbackState == Player.STATE_IDLE) {
                        if (playbackPreparer != null) {
                            playbackPreparer!!.preparePlayback()
                        }
                    } else if (player.playbackState == Player.STATE_ENDED) {
                        controlDispatcher.dispatchSeekTo(
                            player,
                            player.currentWindowIndex,
                            C.TIME_UNSET
                        )
                    }
                    controlDispatcher.dispatchSetPlayWhenReady(player, true)
                    updatePlayPauseButton()
                }
                pauseButton === view -> {
                    controlDispatcher.dispatchSetPlayWhenReady(player, false)
                    updatePlayPauseButton()
                }
                repeatToggleButton === view -> controlDispatcher.dispatchSetRepeatMode(
                    player,
                    RepeatModeUtil.getNextRepeatMode(player.repeatMode, repeatToggleModes)
                )
            }
        }
    }

}