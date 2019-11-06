package com.project100pi.library.misc

import android.util.Log
import com.project100pi.library.BuildConfig

object Logger {

    private val DEBUG = true

    private val className: String
        get() {
            val fileName = Thread.currentThread().stackTrace[5].fileName
            return fileName.substring(0, fileName.length - 5)
        }

    private val methodName: String
        get() = Thread.currentThread().stackTrace[5].methodName

    private val lineNumber: Int
        get() = Thread.currentThread().stackTrace[5].lineNumber

    private val packageName: String
        get() = BuildConfig.APPLICATION_ID

    private val classNameMethodNameAndLineNumber: String
        get() = "[$packageName::$className.$methodName()-$lineNumber]: "

    fun v(tag: String, msg: String) {
        if (DEBUG) Log.v(classNameMethodNameAndLineNumber, msg)
    }

    fun d(msg: String) {
        if (DEBUG) Log.d(classNameMethodNameAndLineNumber, msg)
    }

    fun i(msg: String) {
        if (DEBUG) Log.i(classNameMethodNameAndLineNumber, msg)
    }

    fun w(msg: String) {
        if (DEBUG) Log.w(classNameMethodNameAndLineNumber, msg)
    }

    fun e(msg: String) {
        if (DEBUG) Log.e(classNameMethodNameAndLineNumber, msg)
    }

    fun e(msg: String, e: Exception) {
        if (DEBUG) Log.e(classNameMethodNameAndLineNumber, msg, e)
    }

    fun wtf(msg: String) {
        if (DEBUG) Log.wtf(classNameMethodNameAndLineNumber, msg)
    }

}