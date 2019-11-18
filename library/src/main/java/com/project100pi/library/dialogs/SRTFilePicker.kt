package com.project100pi.library.dialogs

import android.app.Dialog
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.fragment.app.DialogFragment
import android.view.View
import android.os.Bundle
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.Window
import com.project100pi.library.dialogs.listeners.SRTFilePickerClickListener
import java.util.Collections
import android.widget.*
import com.bumptech.glide.Glide
import com.project100pi.library.R
import java.io.File
import java.io.FilenameFilter

class SRTFilePicker(private val mContext: Context,
                    private val SRTFilePickerClickListener: SRTFilePickerClickListener)
    : Dialog(mContext), AdapterView.OnItemClickListener {

    private val defaultInternalDirectory = Environment.getExternalStorageDirectory().absolutePath
    private var mDirectory: File? = null
    private var mFiles = mutableListOf<File>()
    private var mShowHiddenFiles = false
    private var acceptedFileExtensions: Array<String>? = null

    private var mAdapter: FilePickerListAdapter? = null
    private lateinit var listView: ListView
    private lateinit var empty: View
    private lateinit var srtUpText: TextView
    private lateinit var srtUpIconImage: ImageView
    private lateinit var closeIconImage: ImageView
    private lateinit var toolbar: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_file_picker)

        toolbar = findViewById(R.id.subtitle_toolbar)
        listView = findViewById(R.id.file_picker_listView)
        empty = findViewById(R.id.file_picker_empty)
        srtUpText = findViewById(R.id.srt_up_text)
        srtUpIconImage = findViewById(R.id.srt_up_image)
        closeIconImage = findViewById(R.id.srt_close_image)

        try {
            mDirectory = File(defaultInternalDirectory)
            mFiles = arrayListOf()
            mAdapter = FilePickerListAdapter(mContext, mFiles)
            listView.adapter = mAdapter
            listView.onItemClickListener = this
            listView.emptyView = empty
            acceptedFileExtensions = arrayOf("vtt", "srt")

            closeIconImage.setOnClickListener {
                dismiss()
            }

            toolbar.setOnClickListener {

                // srtUpText.text = mDirectory!!.name.substring(0, mDirectory!!.name.lastIndexOf("/") - 1)
                //mDirectory = File(mDirectory!!.name.substring(0, mDirectory!!.name.lastIndexOf("/") - 1))
                srtUpText.text = "..."
                mDirectory = File(defaultInternalDirectory)
                refreshFilesList()
            }

            refreshFilesList()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun refreshFilesList() {
        mFiles.clear()
        val filter = ExtensionFilenameFilter(acceptedFileExtensions)

        val files = mDirectory!!.listFiles(filter)
        if (files != null && files.isNotEmpty()) {
            for (f in files) {
                if (f.isHidden && !mShowHiddenFiles) {
                    // Don't add the file
                    continue
                }

                if (!f.toString().equals("/storage/emulated", ignoreCase = true)) {
                    if (isFileExist(f))
                        mFiles.add(f)
                }
            }

            Collections.sort(mFiles, FileComparator())
        }
        mAdapter!!.notifyDataSetChanged()
    }

    override fun onItemClick(adapterView: AdapterView<*>, view: View, i: Int, l: Long) {
        try {
            val newFile = mAdapter!!.getItem(i)
            if (newFile!!.isFile) {

                when (newFile.name.substring(newFile.name.lastIndexOf(".") + 1)) {
                    "vtt", "srt" -> {
                        SRTFilePickerClickListener.filePickerSuccessClickListener(newFile.absolutePath)
                        dismiss()
                    }
                    else -> Toast.makeText(mContext, "Please select subtitle file", Toast.LENGTH_SHORT).show()
                }
            } else {
                srtUpText.text = newFile.absolutePath
                mDirectory = newFile
                refreshFilesList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private inner class FilePickerListAdapter(context: Context, private val mObjects: List<File>) :
        ArrayAdapter<File>(context, R.layout.file_picker_list_item, android.R.id.text1, mObjects) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

            val row: View?

            row = if (convertView == null) {
                val inflater =
                    context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                inflater.inflate(R.layout.file_picker_list_item, parent, false)
            } else {
                convertView
            }

            val `object` = mObjects[position]

            val imageView = row!!.findViewById<ImageView>(R.id.file_picker_image)
            val textView = row.findViewById<TextView>(R.id.file_picker_text)
            // Set single line
            textView.isSingleLine = true
            val fileName = `object`.name

            val title = when {
                fileName.contains("UsbDriveA") -> "USB Drive"
                fileName.contains("sdcard0") -> "Internal Storage"
                fileName.contains("extSdCard") -> "SD Card"
                else -> fileName
            }
            textView.text = title
            if (`object`.isFile) {
                // Show the file icon
                //imageView.background = context.resources.getDrawable(R.drawable.ic_file)
                Glide
                    .with(context)
                    .asBitmap()
                    .load(R.drawable.ic_file)
                    .thumbnail(0.1f)
                    .into(imageView)
            } else {
                // Show the folder icon
                //imageView.background = context.resources.getDrawable(R.drawable.ic_folder)
                Glide
                    .with(context)
                    .asBitmap()
                    .load(R.drawable.ic_folder)
                    .thumbnail(0.1f)
                    .into(imageView)
            }
            return row
        }
    }

    private inner class FileComparator : Comparator<File> {
        override fun compare(f1: File, f2: File): Int {
            if (f1 === f2) {
                return 0
            }
            if (f1.isDirectory && f2.isFile) {
                // Show directories above files
                return -1
            }
            return if (f1.isFile && f2.isDirectory) {
                // Show files below directories
                1
            } else f1.name.compareTo(f2.name, ignoreCase = true)
            // Sort the directories alphabetically
        }
    }

    private inner class ExtensionFilenameFilter(private val mExtensions: Array<String>?) :
        FilenameFilter {

        override fun accept(dir: File, filename: String): Boolean {
            if (File(dir, filename).isDirectory) {
                // Accept all directory names
                return true
            }
            if (mExtensions != null && mExtensions.isNotEmpty()) {
                for (i in mExtensions.indices) {
                    if (filename.endsWith(mExtensions[i])) {
                        // The filename ends with the extension
                        return true
                    }
                }
                // The filename did not match any of the extensions
                return false
            }
            // No extensions has been set. Accept all file extensions.
            return true
        }
    }

    private fun isFileExist(file: File): Boolean {
        val name = file.absolutePath
        if (name.contains("UsbDriveB")) {
            return false
        } else if (name.contains("UsbDriveC")) {
            return false
        } else if (name.contains("UsbDriveD")) {
            return false
        } else if (name.contains("UsbDriveE")) {
            return false
        } else if (name.contains("UsbDriveF")) {
            return false
        }
        return file.exists()
    }

}