package com.project100pi.pivideoplayer.dialogs

import android.content.Context
import android.os.Environment
import androidx.fragment.app.DialogFragment
import android.view.View
import butterknife.BindView
import butterknife.ButterKnife
import android.os.Bundle
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.Window
import com.project100pi.pivideoplayer.R
import com.project100pi.pivideoplayer.dialogs.listeners.SRTFilePickerClickListener
import java.util.Collections
import kotlin.collections.ArrayList
import android.widget.*
import java.io.File
import java.io.FilenameFilter
import android.R.string
import android.net.Uri


class SRTFilePicker(private val mContext: Context,
                    private val SRTFilePickerClickListener: SRTFilePickerClickListener)
    : DialogFragment(), AdapterView.OnItemClickListener {

    private val DEFAULT_INITIAL_DIRECTORY = Environment.getExternalStorageDirectory().absolutePath
    private var mDirectory: File? = null
    private var mFiles: ArrayList<File>? = null
    private var mShowHiddenFiles = false
    private var acceptedFileExtensions: Array<String>? = null

    private var mAdapter: FilePickerListAdapter? = null
    @BindView(R.id.listView)
    lateinit var listView: ListView
    @BindView(R.id.empty)
    lateinit var empty: View
    @BindView(R.id.srt_view_container)
    lateinit var mSRTViewContainer: View
    @BindView(R.id.srt_up_text)
    lateinit var srtUpText: TextView
    @BindView(R.id.srt_up_image)
    lateinit var srtUpIconImage: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val fragmentView = inflater.inflate(R.layout.dialog_file_picker, container, false)
        dialog!!.window!!.requestFeature(Window.FEATURE_NO_TITLE)
        ButterKnife.bind(this, fragmentView)
        return fragmentView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        try {
            mDirectory = File(DEFAULT_INITIAL_DIRECTORY)
            mFiles = arrayListOf()
            mAdapter = FilePickerListAdapter(mContext, mFiles!!)
            listView.adapter = mAdapter
            listView.onItemClickListener = this
            listView.emptyView = empty
            acceptedFileExtensions = arrayOf("vtt", "srt")

            mSRTViewContainer.setOnClickListener {

                //mDirectory = File(mDirectory!!.name.substring(0, mDirectory!!.name.lastIndexOf("/") - 1))
                mDirectory = File(DEFAULT_INITIAL_DIRECTORY)
                refreshFilesList()
            }

            refreshFilesList()
        } catch (e: Exception) {
           e.printStackTrace()
        }

    }

    override fun onStart() {
        super.onStart()
        val d = dialog
        if (d != null) {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.MATCH_PARENT
//            val width = 800
//            val height = 600
            d.window!!.setLayout(width, height)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.MY_DIALOG)
    }

    private fun refreshFilesList() {
        mFiles!!.clear()
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
                        mFiles!!.add(f)
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

            var row: View?

            row = if (convertView == null) {
                val inflater =
                    context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                inflater.inflate(R.layout.file_picker_list_item, parent, false)
            } else {
                convertView
            }

            val `object` = mObjects[position]

            val imageView = row!!.findViewById<View>(R.id.file_picker_image)
            val textView = row.findViewById<TextView>(R.id.file_picker_text)
            // Set single line
            textView.isSingleLine = true
            val fileName = `object`.name

            var title = when {
                fileName.contains("UsbDriveA") -> "USB Drive"
                fileName.contains("sdcard0") -> "Internal Storage"
                fileName.contains("extSdCard") -> "SD Card"
                else -> fileName
            }
            textView.text = title
            if (`object`.isFile) {
                // Show the file icon
                imageView.background = context.resources.getDrawable(R.drawable.ic_file)
            } else {
                // Show the folder icon
                imageView.background = context.resources.getDrawable(R.drawable.ic_folder)
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