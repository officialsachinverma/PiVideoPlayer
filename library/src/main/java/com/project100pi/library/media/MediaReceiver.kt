package com.project100pi.library.media

import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.os.Handler
import android.os.Message
import android.view.KeyEvent
import android.view.KeyEvent.*
import com.project100pi.library.misc.CurrentMediaState
import com.project100pi.library.misc.Logger


object MediaReceiver: BroadcastReceiver() {

    private val DOUBLE_PRESS_INTERVAL: Long = 500 // in millis

    override fun onReceive(context: Context?, intent: Intent?) {

        Logger.i("onReceive() called with: context = [$context], intent = [$intent]")

        if (intent == null) {
            Logger.i("onReceive() : Intent is null. Shouldnot have been. Quitting the receiver")
            return
        }

        if (intent.action == Intent.ACTION_MEDIA_BUTTON) {

            handleMediaButtonIntent(context!!.applicationContext, intent)

        }

    }

    /**
     * Handles media button actions
     * Single Tap -> Play/Pause
     * Double Tap -> Skip to next
     * Triple Tap -> Skip to previous
     *
     * @param appContext Context
     * @param intent Intent
     */
    fun handleMediaButtonIntent(appContext: Context, intent: Intent) {
        if (intent.extras == null)
            return
        val keyEvent = intent.extras!!.get(Intent.EXTRA_KEY_EVENT) as KeyEvent?

        if (keyEvent == null || keyEvent.action != ACTION_DOWN)
        // This is throwing NPE sometimes . So, return if KeyEvent is null
            return

        when (keyEvent.keyCode) {
            KEYCODE_HEADSETHOOK -> {
                // Get current time in nano seconds.
                val pressTime = System.currentTimeMillis()

                if (pressTime - CurrentMediaState.MediaButtonController.lastPressTime <= DOUBLE_PRESS_INTERVAL) {
                    if (CurrentMediaState.MediaButtonController.mHasDoubleClicked) {
                        //If we had clicked twice already, if we come here, it means we have clicked another time. Go to previous song.
                        CurrentMediaState.MediaButtonController.mHasDoubleClicked = false
                        CurrentMediaState.MediaButtonController.mHasTripleClicked = true
                        MediaCommandHandlerUtil.handlePrevious(appContext)
                        Logger.i(" KEYCODE : KEYCODE_HEADSET_HOOK ---> Triple Click")
                    } else {
                        CurrentMediaState.MediaButtonController.mHasDoubleClicked = true
                        val mySecondHandler = object : Handler() {
                            override fun handleMessage(m: Message) {
                                if (CurrentMediaState.MediaButtonController.mHasDoubleClicked && !CurrentMediaState.MediaButtonController.mHasTripleClicked) {
                                    MediaCommandHandlerUtil.handleNext(appContext)
                                    Logger.i(" KEYCODE : KEYCODE_HEADSET_HOOK ---> Double Click")
                                }
                            }
                        }
                        val m = Message()
                        mySecondHandler.sendMessageDelayed(m, DOUBLE_PRESS_INTERVAL)
                    }
                } else {
                    // If not double click or triple click....
                    CurrentMediaState.MediaButtonController.mHasDoubleClicked = false
                    CurrentMediaState.MediaButtonController.mHasTripleClicked = false
                    val myHandler = object : Handler() {
                        override fun handleMessage(m: Message) {
                            if (!CurrentMediaState.MediaButtonController.mHasDoubleClicked && !CurrentMediaState.MediaButtonController.mHasTripleClicked) {
                                if (CurrentMediaState.Playback.playing) {
                                    MediaCommandHandlerUtil.handlePause(appContext)
                                } else {
                                    MediaCommandHandlerUtil.handlePlay(appContext)
                                }
                                Logger.i(" KEYCODE : KEYCODE_HEADSET_HOOK ---> Single Click")
                            }
                        }
                    }
                    val m = Message()
                    myHandler.sendMessageDelayed(m, DOUBLE_PRESS_INTERVAL)
                }
                // record the last time the menu button was pressed.
                CurrentMediaState.MediaButtonController.lastPressTime = pressTime
            }
            KEYCODE_MEDIA_PLAY, KEYCODE_MEDIA_PAUSE, KEYCODE_MEDIA_PLAY_PAUSE -> {
                Logger.i(" Keycode : KEYCODE_MEDIA_PLAY")
                Logger.i(" Keycode : KEYCODE_MEDIA PAUSE")
                Logger.i(" Keycode: KEYCODE_MEDIA_PLAY_PAUSE")

                if (CurrentMediaState.Playback.playing) {
                    MediaCommandHandlerUtil.handlePause(appContext)
                } else {
                    MediaCommandHandlerUtil.handlePlay(appContext)
                }

            }
            KEYCODE_MEDIA_STOP -> {}
            KEYCODE_MEDIA_NEXT -> {
                Logger.i(" Keycode : KEYCODE_MEDIA NEXT")
                MediaCommandHandlerUtil.handleNext(appContext)
            }
            KEYCODE_MEDIA_PREVIOUS -> {
                Logger.i(" Keycode : KEYCODE_MEDIA PREVIOUS")
                MediaCommandHandlerUtil.handlePrevious(appContext)
            }
        }
    }

}