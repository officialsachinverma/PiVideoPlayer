package com.project100pi.pivideoplayer.ui.activity

import android.Manifest
import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.project100pi.pivideoplayer.listeners.OnClickListener
import com.project100pi.pivideoplayer.R
import com.project100pi.pivideoplayer.utils.Constants
import android.view.Menu
import android.view.MenuItem
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import butterknife.BindView
import butterknife.ButterKnife
import com.project100pi.pivideoplayer.ui.adapters.StorageFileAdapter
import com.project100pi.pivideoplayer.ui.activity.viewmodel.factory.DirectoryListViewModelFactory
import com.project100pi.pivideoplayer.listeners.ItemDeleteListener
import com.project100pi.pivideoplayer.model.FolderInfo
import com.project100pi.pivideoplayer.ui.activity.viewmodel.DirectoryListViewModel
import java.util.logging.Logger
import kotlin.collections.ArrayList


class DirectoryListActivity : AppCompatActivity(), OnClickListener, ItemDeleteListener{

    @BindView(R.id.rv_directory_file_list)
    lateinit var  rvVideoList: RecyclerView
    @BindView(R.id.anim_toolbar)
    lateinit var mToolbar: Toolbar
    @BindView(R.id.tv_no_directory_found_msg)
    lateinit var tvEmptyList: TextView
    @BindView(R.id.pg_directory_waiting)
    lateinit var pgWaiting: ProgressBar

    private lateinit var directoryListViewModel: DirectoryListViewModel
    private var adapter: StorageFileAdapter? = null
    private var actionModeCallback = ActionModeCallback()
    private var actionMode: ActionMode? = null
    private var videoListData: ArrayList<FolderInfo> = ArrayList()

    private var doubleBackToExitPressedOnce = false
    private var preferences: SharedPreferences? = null

    companion object {
        var mIsMultiSelectMode: Boolean = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_directory_list)
        ButterKnife.bind(this)
        setSupportActionBar(mToolbar)

        init()
    }

    private fun init() {

        val application = requireNotNull(this).application
        val viewModelFactory = DirectoryListViewModelFactory(this , application)
        directoryListViewModel = ViewModelProviders.of(this, viewModelFactory).get(
            DirectoryListViewModel::class.java)

        tvEmptyList.visibility = View.GONE
        rvVideoList.visibility = View.GONE
        pgWaiting.visibility = View.VISIBLE

        observeForObservers()

        preferences = getSharedPreferences(Constants.SHARED_PREFERENCES, Context.MODE_PRIVATE)
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
                openSearchActivity()
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

    private fun observeForObservers() {
        observeForFolderList()
    }

    private fun observeForFolderList() {
        directoryListViewModel.foldersList.observe(this, Observer {
            if (it != null) {
                if (it.size > 0) {
                    videoListData = it
                    tvEmptyList.visibility = View.GONE
                    pgWaiting.visibility = View.GONE
                    rvVideoList.visibility = View.VISIBLE
                    setAdapter()
                } else {
                    tvEmptyList.visibility = View.VISIBLE
                    pgWaiting.visibility = View.GONE
                    rvVideoList.visibility = View.VISIBLE
                }
            }
        })
    }

    private fun setAdapter(){

        if (adapter == null) {

            adapter = StorageFileAdapter(this, R.layout.row_directory_item, this)
            val linearLayout = LinearLayoutManager(this)
            linearLayout.orientation = LinearLayoutManager.VERTICAL
            rvVideoList.layoutManager = linearLayout
            rvVideoList.adapter = adapter

        }

        runOnUiThread {
            adapter?.submitList(videoListData)
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

    override fun onOverflowItemClick(position: Int, viewId: Int) {

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
            launchVideoListActivity(position)
        } else {
            toggleSelection(position)
        }

    }

    private fun launchVideoListActivity(position: Int) {
        try {
            val videoListIntent = Intent(this, VideoListActivity::class.java)
//        videoListIntent.putExtra("videoList", videoListData[position].songsList)
            videoListIntent.putExtra("directoryName", videoListData[position].folderName)
            videoListIntent.putExtra("directoryPath", videoListData[position].folderPath)
            startActivity(videoListIntent)
        } catch (e: ArrayIndexOutOfBoundsException) {
            e.printStackTrace()
            Toast.makeText(this, "Error Occured.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun playVideo() {
        directoryListViewModel.playMultipleVideos(adapter!!.getSelectedItems())
    }


    private fun playSelectedVideos() {
        playVideo()
    }

    private fun shareMultipleVideos() {
        directoryListViewModel.shareMultipleVideos(adapter!!.getSelectedItems())
    }

    override fun onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed()
            return
        }

        this.doubleBackToExitPressedOnce = true
        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show()

        Handler().postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
    }

    override fun showPermissionForSdCard() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        startActivityForResult(intent, 100)
    }

    override fun onDeleteSuccess(listOfIndexes: List<Int>) {
        for(position in listOfIndexes) {
            directoryListViewModel.removeElementAt(position)
            adapter!!.notifyItemRemoved(position)
        }
        Toast.makeText(
            this@DirectoryListActivity,
            "${listOfIndexes.size} " + getString(R.string.songs_deleted_toast),
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onDeleteError() {
        Toast.makeText(this, "Some error occurred while deleting video(s)", Toast.LENGTH_SHORT).show()
    }

    private fun openSearchActivity() {
        val intent = Intent(this@DirectoryListActivity, SearchActivity::class.java)
        intent.putExtra("reason", "general")
        startActivity(intent)
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
                    shareMultipleVideos()
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
            mIsMultiSelectMode = false
            actionMode = null
            mToolbar.visibility = View.VISIBLE
            adapter!!.clearSelection()
        }

    }

    private fun showMultiDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Delete")
            .setMessage("Are you sure you want to delete this ${adapter!!.getSelectedItemCount()} video(s)?")
            .setPositiveButton(android.R.string.yes) { _, _ ->
                directoryListViewModel.deleteFolderContents(adapter!!.getSelectedItems(), this)
            }
            .setNegativeButton(android.R.string.no, null)
            .setCancelable(false)
            .show()
    }
}
