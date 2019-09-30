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
    private var showShuffleButton: Boolean = false
    private var hideAtMs: Long = 0
    private var adGroupTimesMs = LongArray(0)
    private var playedAdGroups = BooleanArray(0)
    private var extraAdGroupTimesMs = LongArray(0)
    private var extraPlayedAdGroups = BooleanArray(0)
    private var currentWindowOffset: Long = 0

    private var componentListener: ComponentListener
    private var previousButton: View
    private var nextButton: View
    private var playButton: View
    private var pauseButton: View
    private var fastForwardButton: View
    private var rewindButton: View
    private var repeatToggleButton: ImageView
    private var shuffleButton: View
    private var vrButton: View
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
                showShuffleButton = a.getBoolean(
                    R.styleable.PlayerControlView_show_shuffle_button,
                    showShuffleButton
                )
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
        adGroupTimesMs = LongArray(0)
        playedAdGroups = BooleanArray(0)
        extraAdGroupTimesMs = LongArray(0)
        extraPlayedAdGroups = BooleanArray(0)
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

        // Fast Forward
        fastForwardButton = findViewById(R.id.pi_ffwd)
        fastForwardButton.setOnClickListener(componentListener)

        // Repeat
        repeatToggleButton = findViewById(R.id.pi_repeat_toggle)
        repeatToggleButton.setOnClickListener(componentListener)

        // Shuffle
        shuffleButton = findViewById(R.id.pi_shuffle)
        shuffleButton.setOnClickListener(componentListener)

        // Virtual Reality
        vrButton = findViewById(R.id.pi_vr)
        setShowVrButton(false)

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
     * Returns the [Player] currently being controlled by this view, or null if no player is
     * set.
     */
    fun getPiPlayer(): Player? {
        return player
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
     * Sets whether the time bar should show all windows, as opposed to just the current one. If the
     * timeline has a period with unknown duration or more than [ ][.MAX_WINDOWS_FOR_MULTI_WINDOW_TIME_BAR] windows the time bar will fall back to showing a single
     * window.
     *
     * @param showMultiWindowTimeBar Whether the time bar should show all windows.
     */
    fun setShowMultiWindowTimeBar(showMultiWindowTimeBar: Boolean) {
        this.showMultiWindowTimeBar = showMultiWindowTimeBar
        updateTimeline()
    }

    /**
     * Sets the millisecond positions of extra ad markers relative to the start of the window (or
     * timeline, if in multi-window mode) and whether each extra ad has been played or not. The
     * markers are shown in addition to any ad markers for ads in the player's timeline.
     *
     * @param extraAdGroupTimesMs The millisecond timestamps of the extra ad markers to show, or
     * `null` to show no extra ad markers.
     * @param extraPlayedAdGroups Whether each ad has been played. Must be the same length as `extraAdGroupTimesMs`, or `null` if `extraAdGroupTimesMs` is `null`.
     */
    fun setExtraAdGroupMarkers(
        extraAdGroupTimesMs: LongArray?, extraPlayedAdGroups: BooleanArray?
    ) {
        var extraPlayedAdGroups = extraPlayedAdGroups
        if (extraAdGroupTimesMs == null) {
            this.extraAdGroupTimesMs = LongArray(0)
            this.extraPlayedAdGroups = BooleanArray(0)
        } else {
            extraPlayedAdGroups = Assertions.checkNotNull(extraPlayedAdGroups)
            Assertions.checkArgument(extraAdGroupTimesMs.size == extraPlayedAdGroups!!.size)
            this.extraAdGroupTimesMs = extraAdGroupTimesMs
            this.extraPlayedAdGroups = extraPlayedAdGroups
        }
        updateTimeline()
    }

    /**
     * Sets the [VisibilityListener].
     *
     * @param listener The listener to be notified about visibility changes.
     */
    fun setVisibilityListener(listener: VisibilityListener) {
        this.visibilityListener = listener
    }

    /**
     * Sets the [ProgressUpdateListener].
     *
     * @param listener The listener to be notified about when progress is updated.
     */
    fun setProgressUpdateListener(listener: ProgressUpdateListener?) {
        this.progressUpdateListener = listener
    }

    /**
     * Sets the [PlaybackPreparer].
     *
     * @param playbackPreparer The [PlaybackPreparer].
     */
    fun setPlaybackPreparer(playbackPreparer: PlaybackPreparer?) {
        this.playbackPreparer = playbackPreparer
    }

    /**
     * Sets the [com.google.android.exoplayer2.ControlDispatcher].
     *
     * @param controlDispatcher The [com.google.android.exoplayer2.ControlDispatcher], or null
     * to use [com.google.android.exoplayer2.DefaultControlDispatcher].
     */
    fun setControlDispatcher(
        controlDispatcher: ControlDispatcher?
    ) {
        this.controlDispatcher =
            controlDispatcher ?: DefaultControlDispatcher()
    }

    /**
     * Sets the rewind increment in milliseconds.
     *
     * @param rewindMs The rewind increment in milliseconds. A non-positive value will cause the
     * rewind button to be disabled.
     */
    fun setRewindIncrementMs(rewindMs: Int) {
        this.rewindMs = rewindMs
        updateNavigation()
    }

    /**
     * Sets the fast forward increment in milliseconds.
     *
     * @param fastForwardMs The fast forward increment in milliseconds. A non-positive value will
     * cause the fast forward button to be disabled.
     */
    fun setFastForwardIncrementMs(fastForwardMs: Int) {
        this.fastForwardMs = fastForwardMs
        updateNavigation()
    }

    /**
     * Returns the playback controls timeout. The playback controls are automatically hidden after
     * this duration of time has elapsed without user input.
     *
     * @return The duration in milliseconds. A non-positive value indicates that the controls will
     * remain visible indefinitely.
     */
    fun getShowTimeoutMs(): Int {
        return showTimeoutMillis
    }

    /**
     * Sets the playback controls timeout. The playback controls are automatically hidden after this
     * duration of time has elapsed without user input.
     *
     * @param showTimeoutMs The duration in milliseconds. A non-positive value will cause the controls
     * to remain visible indefinitely.
     */
    fun setShowTimeoutMs(showTimeoutMs: Int) {
        this.showTimeoutMillis = showTimeoutMs
        if (isVisible()) {
            // Reset the timeout.
            hideAfterTimeout()
        }
    }

    /**
     * Returns which repeat toggle modes are enabled.
     *
     * @return The currently enabled [RepeatModeUtil.RepeatToggleModes].
     */
    @RepeatModeUtil.RepeatToggleModes
    fun getRepeatToggleModes(): Int {
        return repeatToggleModes
    }

    /**
     * Sets which repeat toggle modes are enabled.
     *
     * @param repeatToggleModes A set of [RepeatModeUtil.RepeatToggleModes].
     */
    fun setRepeatToggleModes(@RepeatModeUtil.RepeatToggleModes repeatToggleModes: Int) {
        this.repeatToggleModes = repeatToggleModes
        if (player != null) {
            @Player.RepeatMode val currentMode = player!!.repeatMode
            if (repeatToggleModes == RepeatModeUtil.REPEAT_TOGGLE_MODE_NONE && currentMode != Player.REPEAT_MODE_OFF) {
                controlDispatcher.dispatchSetRepeatMode(player, Player.REPEAT_MODE_OFF)
            } else if (repeatToggleModes == RepeatModeUtil.REPEAT_TOGGLE_MODE_ONE && currentMode == Player.REPEAT_MODE_ALL) {
                controlDispatcher.dispatchSetRepeatMode(player, Player.REPEAT_MODE_ONE)
            } else if (repeatToggleModes == RepeatModeUtil.REPEAT_TOGGLE_MODE_ALL && currentMode == Player.REPEAT_MODE_ONE) {
                controlDispatcher.dispatchSetRepeatMode(player, Player.REPEAT_MODE_ALL)
            }
        }
        updateRepeatModeButton()
    }

    /** Returns whether the shuffle button is shown.  */
    fun getShowShuffleButton(): Boolean {
        return showShuffleButton
    }

    /**
     * Sets whether the shuffle button is shown.
     *
     * @param showShuffleButton Whether the shuffle button is shown.
     */
    fun setShowShuffleButton(showShuffleButton: Boolean) {
        this.showShuffleButton = showShuffleButton
        updateShuffleButton()
    }

    /** Returns whether the VR button is shown.  */
    fun getShowVrButton(): Boolean {
        return vrButton.visibility == View.VISIBLE
    }

    /**
     * Sets whether the VR button is shown.
     *
     * @param showVrButton Whether the VR button is shown.
     */
    fun setShowVrButton(showVrButton: Boolean) {
        vrButton.visibility = if (showVrButton) View.VISIBLE else View.GONE
    }

    /**
     * Sets listener for the VR button.
     *
     * @param onClickListener Listener for the VR button, or null to clear the listener.
     */
    fun setVrButtonListener(onClickListener: OnClickListener?) {
        vrButton.setOnClickListener(onClickListener)
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
        updateShuffleButton()
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
                enableRewind = isSeekable && rewindMs > 0
                enableFastForward = isSeekable && fastForwardMs > 0
                enableNext = window.isDynamic || player!!.hasNext()
            }
        }

        setButtonEnabled(enablePrevious, previousButton)
        setButtonEnabled(enableRewind, rewindButton)
        setButtonEnabled(enableFastForward, fastForwardButton)
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

    private fun updateShuffleButton() {
        if (!isVisible() || !isPiAttachedToWindow) {
            return
        }
        if (!showShuffleButton) {
            shuffleButton.visibility = View.GONE
        } else if (player == null) {
            setButtonEnabled(false, shuffleButton)
        } else {
            shuffleButton.alpha = if (player!!.shuffleModeEnabled) 1f else 0.3f
            shuffleButton.isEnabled = true
            shuffleButton.visibility = View.VISIBLE
        }
    }

    private fun updateTimeline() {
        if (player == null) {
            return
        }
        multiWindowTimeBar =
            showMultiWindowTimeBar && canShowMultiWindowTimeBar(player!!.currentTimeline, window)
        currentWindowOffset = 0
        var durationUs: Long = 0
        var adGroupCount = 0
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
                    val periodAdGroupCount = period.adGroupCount
                    for (adGroupIndex in 0 until periodAdGroupCount) {
                        var adGroupTimeInPeriodUs = period.getAdGroupTimeUs(adGroupIndex)
                        if (adGroupTimeInPeriodUs == C.TIME_END_OF_SOURCE) {
                            if (period.durationUs == C.TIME_UNSET) {
                                // Don't show ad markers for postrolls in periods with unknown duration.
                                continue
                            }
                            adGroupTimeInPeriodUs = period.durationUs
                        }
                        val adGroupTimeInWindowUs =
                            adGroupTimeInPeriodUs + period.positionInWindowUs
                        if (adGroupTimeInWindowUs >= 0 && adGroupTimeInWindowUs <= window.durationUs) {
                            if (adGroupCount == adGroupTimesMs.size) {
                                val newLength =
                                    if (adGroupTimesMs.isEmpty()) 1 else adGroupTimesMs.size * 2
                                adGroupTimesMs = adGroupTimesMs.copyOf(newLength)
                                playedAdGroups = playedAdGroups.copyOf(newLength)
                            }
                            adGroupTimesMs[adGroupCount] =
                                C.usToMs(durationUs + adGroupTimeInWindowUs)
                            playedAdGroups[adGroupCount] = period.hasPlayedAdGroup(adGroupIndex)
                            adGroupCount++
                        }
                    }
                }
                durationUs += window.durationUs
            }
        }
        val durationMs = C.usToMs(durationUs)

        durationView.text = Util.getStringForTime(formatBuilder, formatter, durationMs)

        if (timeBar != null) {
            timeBar?.setDuration(durationMs)
            val extraAdGroupCount = extraAdGroupTimesMs.size
            val totalAdGroupCount = adGroupCount + extraAdGroupCount
            if (totalAdGroupCount > adGroupTimesMs.size) {
                adGroupTimesMs = Arrays.copyOf(adGroupTimesMs, totalAdGroupCount)
                playedAdGroups = Arrays.copyOf(playedAdGroups, totalAdGroupCount)
            }
            System.arraycopy(
                extraAdGroupTimesMs,
                0,
                adGroupTimesMs,
                adGroupCount,
                extraAdGroupCount
            )
            System.arraycopy(
                extraPlayedAdGroups,
                0,
                playedAdGroups,
                adGroupCount,
                extraAdGroupCount
            )
            timeBar?.setAdGroupTimesMs(adGroupTimesMs, playedAdGroups, totalAdGroupCount)
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
            updateShuffleButton()
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
                shuffleButton === view -> controlDispatcher.dispatchSetShuffleModeEnabled(
                    player,
                    !player.shuffleModeEnabled
                )
            }
        }
    }

}