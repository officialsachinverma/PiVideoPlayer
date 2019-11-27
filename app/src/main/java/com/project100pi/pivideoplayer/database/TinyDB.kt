package com.project100pi.pivideoplayer.database

import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.os.Environment
import android.preference.PreferenceManager
import android.text.TextUtils
import android.util.Log
import com.project100pi.pivideoplayer.utils.Constants
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

/**
 * Created by Sachin Verma on 2019-11-26.
 */

object TinyDB {

    private var preferences: SharedPreferences? = null
    private var DEFAULT_APP_IMAGEDATA_DIRECTORY = ""
    private var lastImagePath = ""

    fun initPref(appContext: Context) {
        if (preferences == null)
            preferences = appContext.getSharedPreferences(Constants.SHARED_PREFERENCES, Context.MODE_PRIVATE)
    }

    /**
     * Decodes the Bitmap from 'path' and returns it
     *
     * @param path image path
     * @return the Bitmap from 'path'
     */
    fun getImage(path: String?): Bitmap? {
        var bitmapFromPath: Bitmap? = null
        try {
            bitmapFromPath = BitmapFactory.decodeFile(path)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return bitmapFromPath
    }

    /**
     * Returns the String path of the last saved image
     *
     * @return string path of the last saved image
     */
    fun getSavedImagePath(): String? {
        return lastImagePath
    }

    /**
     * Saves 'theBitmap' into folder 'theFolder' with the name 'theImageName'
     *
     * @param theFolder    the folder path dir you want to save it to e.g "DropBox/WorkImages"
     * @param theImageName the name you want to assign to the image file e.g "MeAtLunch.png"
     * @param theBitmap    the image you want to save as a Bitmap
     * @return true if image was saved, false otherwise
     */
    fun putImage(
        theFolder: String?,
        theImageName: String?,
        theBitmap: Bitmap?
    ): Boolean {
        if (theFolder == null || theImageName == null || theBitmap == null) return false
        DEFAULT_APP_IMAGEDATA_DIRECTORY = theFolder
        val mFullPath = setupFullPath(theImageName)
        if (mFullPath != "") {
            lastImagePath = mFullPath
            return saveBitmap(mFullPath, theBitmap)
        }
        return false
    }

    /**
     * Saves 'theBitmap' into 'fullPath'
     *
     * @param fullPath  full path of the image file e.g. "Images/MeAtLunch.png"
     * @param theBitmap the image you want to save as a Bitmap
     * @return true if image was saved, false otherwise
     */
    fun putImageWithFullPath(fullPath: String?, theBitmap: Bitmap?): Boolean {
        return !(fullPath == null || theBitmap == null) && saveBitmap(fullPath, theBitmap)
    }

    /**
     * Creates the path for the image with name 'imageName' in DEFAULT_APP.. directory
     *
     * @param imageName name of the image
     * @return the full path of the image. If it failed to create directory, return empty string
     */
    private fun setupFullPath(imageName: String): String {
        val mFolder = File(
            Environment.getExternalStorageDirectory(),
            DEFAULT_APP_IMAGEDATA_DIRECTORY
        )
        if (isExternalStorageReadable() && isExternalStorageWritable() && !mFolder.exists()) {
            if (!mFolder.mkdirs()) {
                Log.e("ERROR", "Failed to setup folder")
                return ""
            }
        }
        return mFolder.path + '/' + imageName
    }

    /**
     * Saves the Bitmap as a PNG file at path 'fullPath'
     *
     * @param fullPath path of the image file
     * @param bitmap   the image as a Bitmap
     * @return true if it successfully saved, false otherwise
     */
    private fun saveBitmap(fullPath: String?, bitmap: Bitmap?): Boolean {
        if (fullPath == null || bitmap == null) return false
        var fileCreated = false
        var bitmapCompressed = false
        var streamClosed = false
        val imageFile = File(fullPath)
        if (imageFile.exists()) if (!imageFile.delete()) return false
        try {
            fileCreated = imageFile.createNewFile()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        var out: FileOutputStream? = null
        try {
            out = FileOutputStream(imageFile)
            bitmapCompressed = bitmap.compress(CompressFormat.PNG, 100, out)
        } catch (e: Exception) {
            e.printStackTrace()
            bitmapCompressed = false
        } finally {
            if (out != null) {
                try {
                    out.flush()
                    out.close()
                    streamClosed = true
                } catch (e: IOException) {
                    e.printStackTrace()
                    streamClosed = false
                }
            }
        }
        return fileCreated && bitmapCompressed && streamClosed
    }
// Getters

    // Getters
    /**
     * Get int value from SharedPreferences at 'key'. If key not found, return 'defaultValue'
     *
     * @param key          SharedPreferences key
     * @param defaultValue int value returned if key was not found
     * @return int value at 'key' or 'defaultValue' if key not found
     */
    fun getInt(key: String?, defaultValue: Int): Int {
        return preferences!!.getInt(key, defaultValue)
    }

    /**
     * Get parsed ArrayList of Integers from SharedPreferences at 'key'
     *
     * @param key SharedPreferences key
     * @return ArrayList of Integers
     */
    fun getListInt(key: String?): ArrayList<Int>? {
        val myList =
            TextUtils.split(preferences!!.getString(key, ""), "‚‗‚")
        val arrayToList =
            ArrayList(Arrays.asList(*myList))
        val newList = ArrayList<Int>()
        for (item in arrayToList) newList.add(item.toInt())
        return newList
    }

    /**
     * Get long value from SharedPreferences at 'key'. If key not found, return 'defaultValue'
     *
     * @param key          SharedPreferences key
     * @param defaultValue long value returned if key was not found
     * @return long value at 'key' or 'defaultValue' if key not found
     */
    fun getLong(key: String?, defaultValue: Long): Long {
        return preferences!!.getLong(key, defaultValue)
    }

    /**
     * Get float value from SharedPreferences at 'key'. If key not found, return 'defaultValue'
     *
     * @param key          SharedPreferences key
     * @param defaultValue float value returned if key was not found
     * @return float value at 'key' or 'defaultValue' if key not found
     */
    fun getFloat(key: String?, defaultValue: Float): Float {
        return preferences!!.getFloat(key, defaultValue)
    }

    /**
     * Get double value from SharedPreferences at 'key'. If exception thrown, return 'defaultValue'
     *
     * @param key          SharedPreferences key
     * @param defaultValue double value returned if exception is thrown
     * @return double value at 'key' or 'defaultValue' if exception is thrown
     */
    fun getDouble(key: String?, defaultValue: Double): Double {
        val number = getString(key)
        return try {
            number!!.toDouble()
        } catch (e: NumberFormatException) {
            defaultValue
        }
    }

    /**
     * Get parsed ArrayList of Double from SharedPreferences at 'key'
     *
     * @param key SharedPreferences key
     * @return ArrayList of Double
     */
    fun getListDouble(key: String?): ArrayList<Double>? {
        val myList =
            TextUtils.split(preferences!!.getString(key, ""), "â€šâ€—â€š")
        val arrayToList =
            ArrayList(Arrays.asList(*myList))
        val newList = ArrayList<Double>()
        for (item in arrayToList) newList.add(item.toDouble())
        return newList
    }

    /**
     * Get String value from SharedPreferences at 'key'. If key not found, return ""
     *
     * @param key SharedPreferences key
     * @return String value at 'key' or "" (empty String) if key not found
     */
    fun getString(key: String?): String? {
        return preferences!!.getString(key, "")
    }

    /**
     * Get parsed ArrayList of String from SharedPreferences at 'key'
     *
     * @param key SharedPreferences key
     * @return ArrayList of String
     */
    fun getListString(key: String?): ArrayList<String> {
        return ArrayList(
            Arrays.asList(
                *TextUtils.split(
                    preferences!!.getString(key, ""),
                    "‚‗‚"
                )
            )
        )
    }

    /**
     * Get boolean value from SharedPreferences at 'key'. If key not found, return 'defaultValue'
     *
     * @param key          SharedPreferences key
     * @param defaultValue boolean value returned if key was not found
     * @return boolean value at 'key' or 'defaultValue' if key not found
     */
    fun getBoolean(key: String?, defaultValue: Boolean): Boolean {
        return preferences!!.getBoolean(key, defaultValue)
    }

    /**
     * Get parsed ArrayList of Boolean from SharedPreferences at 'key'
     *
     * @param key SharedPreferences key
     * @return ArrayList of Boolean
     */
    fun getListBoolean(key: String?): ArrayList<Boolean>? {
        val myList = getListString(key)
        val newList = ArrayList<Boolean>()
        for (item in myList) {
            if (item == "true") {
                newList.add(true)
            } else {
                newList.add(false)
            }
        }
        return newList
    }
    // Put methods

    // Put methods
    /**
     * Put int value into SharedPreferences with 'key' and save
     *
     * @param key   SharedPreferences key
     * @param value int value to be added
     */
    fun putInt(key: String?, value: Int) {
        preferences!!.edit().putInt(key, value).apply()
    }

    /**
     * Put ArrayList of Integer into SharedPreferences with 'key' and save
     *
     * @param key     SharedPreferences key
     * @param intList ArrayList of Integer to be added
     */
    fun putListInt(key: String?, intList: ArrayList<Int>) {
        val myIntList = intList.toTypedArray()
        preferences!!.edit().putString(key, TextUtils.join("‚‗‚", myIntList)).apply()
    }

    /**
     * Put long value into SharedPreferences with 'key' and save
     *
     * @param key   SharedPreferences key
     * @param value long value to be added
     */
    fun putLong(key: String?, value: Long) {
        preferences!!.edit().putLong(key, value).apply()
    }

    /**
     * Put float value into SharedPreferences with 'key' and save
     *
     * @param key   SharedPreferences key
     * @param value float value to be added
     */
    fun putFloat(key: String?, value: Float) {
        preferences!!.edit().putFloat(key, value).apply()
    }

    /**
     * Put double value into SharedPreferences with 'key' and save
     *
     * @param key   SharedPreferences key
     * @param value double value to be added
     */
    fun putDouble(key: String?, value: Double) {
        putString(key, value.toString())
    }

    /**
     * Put ArrayList of Double into SharedPreferences with 'key' and save
     *
     * @param key        SharedPreferences key
     * @param doubleList ArrayList of Double to be added
     */
    fun putListDouble(
        key: String?,
        doubleList: ArrayList<Double>
    ) {
        val myDoubleList = doubleList.toTypedArray()
        preferences!!.edit().putString(key, TextUtils.join("â€šâ€—â€š", myDoubleList)).apply()
    }

    /**
     * Put String value into SharedPreferences with 'key' and save
     *
     * @param key   SharedPreferences key
     * @param value String value to be added
     */
    fun putString(key: String?, value: String?) {
        preferences!!.edit().putString(key, value).apply()
    }

    /**
     * Put ArrayList of String into SharedPreferences with 'key' and save
     *
     * @param key        SharedPreferences key
     * @param stringList ArrayList of String to be added
     */
    fun putListString(
        key: String?,
        stringList: ArrayList<String>
    ) {
        val myStringList = stringList.toTypedArray()
        preferences!!.edit().putString(key, TextUtils.join("‚‗‚", myStringList)).apply()
    }


    /**
     * Put ArrayList of String into SharedPreferences with 'key' and save
     *
     * @param key        SharedPreferences key
     * @param stringList List of String to be added
     */
    fun putListString(
        key: String?,
        stringList: List<String>
    ) {
        val myStringList = stringList.toTypedArray()
        preferences!!.edit().putString(key, TextUtils.join("‚‗‚", myStringList)).apply()
    }

    /**
     * Put boolean value into SharedPreferences with 'key' and save
     *
     * @param key   SharedPreferences key
     * @param value boolean value to be added
     */
    fun putBoolean(key: String?, value: Boolean) {
        preferences!!.edit().putBoolean(key, value).apply()
    }

    /**
     * Put ArrayList of Boolean into SharedPreferences with 'key' and save
     *
     * @param key      SharedPreferences key
     * @param boolList ArrayList of Boolean to be added
     */
    fun putListBoolean(
        key: String?,
        boolList: ArrayList<Boolean>
    ) {
        val newList = ArrayList<String>()
        for (item in boolList) {
            if (item) {
                newList.add("true")
            } else {
                newList.add("false")
            }
        }
        putListString(key, newList)
    }

    /**
     * Remove SharedPreferences item with 'key'
     *
     * @param key SharedPreferences key
     */
    fun remove(key: String?) {
        preferences!!.edit().remove(key).apply()
    }

    /**
     * Delete image file at 'path'
     *
     * @param path path of image file
     * @return true if it successfully deleted, false otherwise
     */
    fun deleteImage(path: String?): Boolean {
        return File(path).delete()
    }

    /**
     * Clear SharedPreferences (remove everything)
     */
    fun clear() {
        preferences!!.edit().clear().apply()
    }

    /**
     * Retrieve all values from SharedPreferences. Do not modify collection return by method
     *
     * @return a Map representing a list of key/value pairs from SharedPreferences
     */
    fun getAll(): Map<String?, *>? {
        return preferences!!.all
    }

    /**
     * Register SharedPreferences change listener
     *
     * @param listener listener object of OnSharedPreferenceChangeListener
     */
    fun registerOnSharedPreferenceChangeListener(
        listener: OnSharedPreferenceChangeListener?
    ) {
        preferences!!.registerOnSharedPreferenceChangeListener(listener)
    }

    /**
     * Unregister SharedPreferences change listener
     *
     * @param listener listener object of OnSharedPreferenceChangeListener to be unregistered
     */
    fun unregisterOnSharedPreferenceChangeListener(
        listener: OnSharedPreferenceChangeListener?
    ) {
        preferences!!.unregisterOnSharedPreferenceChangeListener(listener)
    }

    /**
     * Check if external storage is writable or not
     *
     * @return true if writable, false otherwise
     */
    fun isExternalStorageWritable(): Boolean {
        return Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()
    }

    /**
     * Check if external storage is readable or not
     *
     * @return true if readable, false otherwise
     */
    fun isExternalStorageReadable(): Boolean {
        val state = Environment.getExternalStorageState()
        return Environment.MEDIA_MOUNTED == state || Environment.MEDIA_MOUNTED_READ_ONLY == state
    }

}