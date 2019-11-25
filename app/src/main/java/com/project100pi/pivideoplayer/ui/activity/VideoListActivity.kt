package com.project100pi.pivideoplayer.ui.activity

import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import com.project100pi.library.misc.Logger
import com.project100pi.library.model.VideoMetaData
import com.project100pi.pivideoplayer.R
import com.project100pi.pivideoplayer.listeners.ItemDeleteListener
import com.project100pi.pivideoplayer.listeners.OnClickListener
import com.project100pi.pivideoplayer.model.FileInfo
import com.project100pi.pivideoplayer.model.observable.VideoChangeObservable
import com.project100pi.pivideoplayer.ui.activity.viewmodel.VideoListViewModel
import com.project100pi.pivideoplayer.ui.activity.viewmodel.factory.VideoListViewModelFactory
import com.project100pi.pivideoplayer.ui.adapters.VideoFilesAdapter
import com.project100pi.pivideoplayer.utils.Constants
import com.project100pi.pivideoplayer.utils.Constants.SHARED_PREFERENCES
import com.project100pi.pivideoplayer.utils.ContextMenuUtil
import java.io.File


class VideoListActivity : AppCompatActivity(), OnClickListener, ItemDeleteListener {

    @BindView(R.id.rv_video_file_list)
    lateinit var  rvVideoList: RecyclerView
    @BindView(R.id.video_list_toolbar)
    lateinit var mToolbar: Toolbar
    @BindView(R.id.tv_no_video_found_msg)
    lateinit var tvEmptyList: TextView
    @BindView(R.id.pg_video_waiting)
    lateinit var pgWaiting: ProgressBar

    private lateinit var videoListViewModel: VideoListViewModel

    private var videoListData = arrayListOf<FileInfo>()
    private var directoryName = ""
    private var directoryPath = ""

    private var adapter: VideoFilesAdapter? = null

    private var actionModeCallback = ActionModeCallback()
    private var actionMode: ActionMode? = null
    private var preferences: SharedPreferences? = null

    companion object {
        var mIsMultiSelectMode: Boolean = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_list)
        ButterKnife.bind(this)
        setSupportActionBar(mToolbar)
        this.intent?.let {
            //videoListData = intent.getParcelableArrayListExtra("videoList") ?: arrayListOf()
            directoryName = intent.getStringExtra("directoryName") ?: ""
            directoryPath = intent.getStringExtra("directoryPath") ?: ""
            init()
            //setAdapter()
            observeForObservable()
        }
    }

    private fun init() {

        val application = requireNotNull(this).application
        val viewModelFactory = VideoListViewModelFactory(this, directoryPath, application)
        videoListViewModel = ViewModelProviders.of(this, viewModelFactory).get(VideoListViewModel::class.java)

        tvEmptyList.visibility = View.GONE
        rvVideoList.visibility = View.GONE
        pgWaiting.visibility = View.VISIBLE

        preferences = getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE)
    }

    private fun observeForObservable() {
        observeForVideoList()
    }

    private fun observeForVideoList(){
        videoListViewModel.filesList.observe(this, Observer {
            videoListData = it
            setAdapter()
        })
    }

    private fun setAdapter(){

        if (adapter == null) {

            adapter = VideoFilesAdapter(this, R.layout.row_video_item, this)
            val linearLayout = LinearLayoutManager(this)
            linearLayout.orientation = LinearLayoutManager.VERTICAL
            rvVideoList.layoutManager = linearLayout
            rvVideoList.adapter = adapter

        }

        if (videoListData.size > 0) {
            tvEmptyList.visibility = View.GONE
            rvVideoList.visibility = View.VISIBLE
            pgWaiting.visibility = View.GONE
        } else {
            tvEmptyList.visibility = View.VISIBLE
            rvVideoList.visibility = View.GONE
            pgWaiting.visibility = View.GONE
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = directoryName
        adapter?.submitList(videoListData)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) =
        when (item.itemId) {
//            R.id.itemSettings -> {
//                true
//            }
            R.id.itemSearch -> {
                launchSearchActivity()
                true
            }
            android.R.id.home -> {
                finish()
                true
            }
            else -> false
        }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            100 -> {
                if (resultCode == RESULT_OK && data != null) {
                    val sdCardUri = data.data
                    preferences?.let {
                        it.edit().putString("sdCardUri", sdCardUri.toString()).apply()
                    }
                    // Persist access permissions.
                    val takeFlags = data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    contentResolver.takePersistableUriPermission(sdCardUri!!, takeFlags)

                    Toast.makeText(this, "Please do the operation again.", Toast.LENGTH_SHORT).show()
//                    videoListViewModel.deleteVideo()
                }
            }
        }
    }

    private fun launchSearchActivity() {
        val intent = Intent(this@VideoListActivity, SearchActivity::class.java)
        intent.putExtra("reason", "general")
        startActivity(intent)
    }

    private fun launchPlayerActivity(position: Int) {
        playVideo(position, false)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    private fun playSelectedVideos() {
        playVideo(-1, true)
    }

    private fun playVideo(position: Int, isMultiple: Boolean) {
        try {
            val playerIntent = Intent(this, PlayerActivity::class.java)
            val metaDataList = arrayListOf<VideoMetaData>()
            if (!isMultiple) {
//                val metadata = directoryListViewModel.getVideoMetaData(currentVideo.folderId)
//           playerIntent.putExtra(Constants.FILE_PATH, currentVideo.path)
                //playerIntent.putExtra(Constants.FILE_PATH, metadata)

                for (folder in videoListData) {
                    metaDataList.add(VideoMetaData(folder._Id, folder.fileName, folder.filePath))
                }
                playerIntent.putExtra(Constants.Playback.WINDOW, position)
            } else {
                for(selectedItemPosition in adapter!!.getSelectedItems()) {
//                    metaDataList.add(directoryListViewModel.getVideoMetaData(videoListData[directoryListViewModel.currentSongFolderIndex].songsList[selectedItemPosition].folderId)!!)
                    metaDataList.add(
                        VideoMetaData(
                            videoListData[selectedItemPosition]._Id,
                            videoListData[selectedItemPosition].fileName,
                            videoListData[selectedItemPosition].filePath)
                    )
                }
            }
            playerIntent.putParcelableArrayListExtra(Constants.QUEUE, metaDataList)
            startActivity(playerIntent)
        } catch (e: Exception) {
            Logger.i(e.toString())
            Toast.makeText(this, "Failed to play this video.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun doActionOnOverflowItemClick(position: Int, viewId: Int) {

        when (viewId) {
            R.id.itemPlay -> {
                launchPlayerActivity(position)
            }
            R.id.itemShare -> {
                shareVideos(position)
            }
            R.id.itemDelete -> {
                showDeleteConfirmation(position)
            }
        }
    }

    private fun shareVideos(position: Int) {
        val currentVideo = videoListData[position]

        startActivity(Intent.createChooser(Intent().setAction(Intent.ACTION_SEND)
            .setType("video/*")
            .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .putExtra(Intent.EXTRA_STREAM,  ContextMenuUtil.getVideoContentUri(this@VideoListActivity, File(currentVideo.filePath))), "Share Video"))
    }

    inner class ActionModeCallback: ActionMode.Callback {

        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            mIsMultiSelectMode = true
            mToolbar.visibility = View.GONE
            mode!!.menuInflater.inflate(R.menu.multi_choice_option, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?) = true

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            when (item!!.itemId) {
                R.id.multiChoiceSelectAll -> {
                    adapter!!.selectAllItems()
                }
                R.id.multiChoicePlay -> {
                    playSelectedVideos()
                }
                R.id.multiChoiceShare -> {
                    videoListViewModel.shareMultipleVideos(adapter!!.getSelectedItems())
                }
                R.id.multiChoiceDelete -> {
                    showMultiDeleteConfirmation()
                }
            }
            // We have to end the multi select, if the user clicks on an option other than select all
            if (item.itemId != R.id.multiChoiceSelectAll)
                actionMode!!.finish()
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            DirectoryListActivity.mIsMultiSelectMode = false
            actionMode = null
            mToolbar.visibility = View.VISIBLE
            adapter!!.clearSelection()
        }

    }

    private fun toggleSelection(position: Int) {
        adapter!!.toggleSelection(position)
        val count = adapter!!.getSelectedItemCount()

        if (count == 0) {
            actionMode!!.finish()
        } else {
            val title = StringBuilder(count.toString())
            title.append(" ")
            title.append(getString(R.string.n_items_selected_toast))
            actionMode!!.title = title.toString()
            actionMode!!.invalidate()
        }
    }

    override fun onItemLongClicked(position: Int): Boolean {
        if (actionMode == null) {
            actionMode = startSupportActionMode(actionModeCallback)
        }

        toggleSelection(position)

        return true
    }

    override fun onDirectorySelected(position: Int) {
        if (!mIsMultiSelectMode) {
            launchPlayerActivity(position)
        } else {
            toggleSelection(position)
        }
    }

    override fun onOverflowItemClick(position: Int, viewId: Int) {
        if (!mIsMultiSelectMode) {
            doActionOnOverflowItemClick(position, viewId)
        }
    }

    override fun showPermissionForSdCard() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        startActivityForResult(intent, 100)
    }

    override fun onDeleteSuccess(listOfIndexes: List<Int>) {
        for(position in listOfIndexes) {
            videoListViewModel.removeElementAt(position)
            adapter!!.notifyItemRemoved(position)
        }
        VideoChangeObservable.setChangedOverride()
        Toast.makeText(
            this@VideoListActivity,
            "${listOfIndexes.size} " + getString(R.string.songs_deleted_toast),
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onDeleteError() {
        Toast.makeText(this, "Some error occurred while deleting video(s)", Toast.LENGTH_SHORT).show()
    }

    private fun showMultiDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Delete")
            .setMessage("Are you sure you want to delete this ${adapter!!.getSelectedItemCount()} video(s)?")
            .setPositiveButton(android.R.string.yes) { _, _ ->
                videoListViewModel.deleteVideo(adapter!!.getSelectedItems(), this)
            }
            .setNegativeButton(android.R.string.no, null)
            .setCancelable(false)
            .show()
    }

    private fun showDeleteConfirmation(position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete")
            .setMessage("Are you sure you want to delete this video?")
            .setPositiveButton(android.R.string.yes) { _, _ ->
                videoListViewModel.deleteVideo(listOf(position), this)
            }
            .setNegativeButton(android.R.string.no, null)
            .setCancelable(false)
            .show()
    }

}