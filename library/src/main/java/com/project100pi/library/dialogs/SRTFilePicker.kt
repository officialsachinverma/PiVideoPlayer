package com.project100pi.library.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import com.bumptech.glide.Glide
import com.project100pi.library.R
import com.project100pi.library.dialogs.listeners.SRTFilePickerClickListener
import java.io.File
import java.io.FilenameFilter
import java.lang.NullPointerException
import java.util.*

class SRTFilePicker(private val mContext: Context,
                    private val SRTFilePickerClickListener: SRTFilePickerClickListener)
    : Dialog(mContext), AdapterView.OnItemClickListener {

    private val defaultInternalDirectory = Environment.getExternalStorageDirectory().absolutePath
    private var mDirectory: File? = null
    private var mFiles = arrayListOf<File>()
    private var mShowHiddenFiles = false
    private var acceptedFileExtensions = arrayOf("vtt", "srt")

    private var mAdapter: FilePickerListAdapter? = null
    private lateinit var listView: ListView
    private lateinit var empty: View
    private lateinit var srtUpText: TextView
    private lateinit var srtUpIconImage: ImageView
    private lateinit var closeIconImage: ImageView
    private lateinit var toolbar: View

    override fun onStart() {
        super.onStart()
        val d: Dialog = this
        val width = ViewGroup.LayoutParams.MATCH_PARENT
        val height = ViewGroup.LayoutParams.MATCH_PARENT
        d.window!!.setLayout(width, height)
    }

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
            mAdapter = FilePickerListAdapter(mContext, mFiles)
            listView.adapter = mAdapter
            listView.onItemClickListener = this
            listView.emptyView = empty

            closeIconImage.setOnClickListener {
                dismiss()
            }

            toolbar.setOnClickListener {

                if (srtUpText.text.toString() != defaultInternalDirectory && srtUpText.text.toString().isNotEmpty()) {
                    srtUpText.text = mDirectory!!.path.substring(0, mDirectory!!.path.lastIndexOf("/"))
                    mDirectory = File(mDirectory!!.path.substring(0, mDirectory!!.path.lastIndexOf("/")))
                    refreshFilesList()
                }
            }

            refreshFilesList()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Refreshes the list with new data when ever user selects a folder/file
     * or user navigates previous folder
     */
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
            newFile?.let {
                if (it.isFile) {

                    when (it.path.substring(it.path.lastIndexOf(".") + 1)) {
                        "vtt", "srt" -> {
                            SRTFilePickerClickListener.filePickerSuccessClickListener(it.absolutePath)
                            dismiss()
                        }
                        else -> Toast.makeText(mContext, "Please select subtitle file", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    srtUpText.text = it.path
                    mDirectory = it
                    refreshFilesList()
                }
            }
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }

    }

    /**
     * Adapter for subtitle file picker
     *
     * @property mObjects List<File>
     * @constructor
     */
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

            val item = mObjects[position]

            val imageView = row!!.findViewById<ImageView>(R.id.file_picker_image)
            val textView = row.findViewById<TextView>(R.id.file_picker_text)
            // Set single line
            textView.isSingleLine = true
            val fileName = item.path.substring(item.path.lastIndexOf("/"))

            textView.text = fileName
            if (item.isFile) {
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

    /**
     * Compares and sort files
     * Compares with name
     * Sort in order like show folder first then files
     * or show files first then folders
     */
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

    /**
     * Takes an array for string which contains file extensions
     * it will check whether the file has one of the extension from
     * the array.
     * We pass this filter while listing out the files from directories.
     *
     * @property mExtensions Array<String>?
     * @constructor
     */
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

}