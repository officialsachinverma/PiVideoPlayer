package com.project100pi.pivideoplayer.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.project100pi.pivideoplayer.listeners.OnClickListener
import com.project100pi.pivideoplayer.R
import com.project100pi.pivideoplayer.utils.Constants
import androidx.core.content.ContextCompat
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import butterknife.BindView
import butterknife.ButterKnife
import com.project100pi.pivideoplayer.adapters.StorageFileAdapter
import com.project100pi.pivideoplayer.factory.VideoListViewModelFactory
import com.project100pi.pivideoplayer.listeners.ItemDeleteListener
import com.project100pi.pivideoplayer.model.FolderInfo
import com.project100pi.pivideoplayer.utils.ContextMenuUtil
import com.project100pi.pivideoplayer.utils.PermissionsUtil
import java.io.File
import kotlin.collections.ArrayList


class VideoListActivity : AppCompatActivity(), OnClickListener, ItemDeleteListener, PermissionsUtil.ShowAlertCallback{

    @BindView(R.id.rv_video_file_list)
    lateinit var  rvVideoList: RecyclerView
    @BindView(R.id.folder_view_container)
    lateinit var mFolderViewContainer: View
    @BindView(R.id.folder_up_text)
    lateinit var folderUpText: TextView
    @BindView(R.id.folder_up_image)
    lateinit var folderUpIconImage: ImageView
    @BindView(R.id.anim_toolbar)
    lateinit var mToolbar: Toolbar
    @BindView(R.id.tv_no_video_found_msg)
    lateinit var tvEmptyList: TextView

    private lateinit var model: VideoListViewModel
    private var adapter: StorageFileAdapter? = null
    private var actionModeCallback = ActionModeCallback()
    private var actionMode: ActionMode? = null
    private val mContext = this
    private var granted: Boolean = false
    private val permission =  Manifest.permission.WRITE_EXTERNAL_STORAGE
    private lateinit var permissionUtil: PermissionsUtil
    private var videoListData: ArrayList<FolderInfo> = ArrayList()

    companion object {
        var mIsMultiSelectMode: Boolean = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_list)
        ButterKnife.bind(this)
        setSupportActionBar(mToolbar)

        permissionUtil = PermissionsUtil(this, this)
    }

    override fun onStart() {
        super.onStart()
        if (!granted)
            permissionUtil.checkorRequestPermission(permission)
    }

    private fun init() {

        val application = requireNotNull(this).application
        val viewModelFactory = VideoListViewModelFactory(this , application)
        model = ViewModelProviders.of(this, viewModelFactory).get(VideoListViewModel::class.java)

        val folderUpIcon = ContextCompat.getDrawable(this, R.drawable.folder_up)
        this.folderUpIconImage.setImageDrawable(folderUpIcon)

        this.mFolderViewContainer.setOnClickListener{
            model.MODE = Constants.FOLDER_VIEW
            setAdapter()
            folderUpText.text = "..."
            model.onBackFolderPressed()
            mFolderViewContainer.visibility = View.GONE
        }

        tvEmptyList.visibility = View.VISIBLE
        rvVideoList.visibility = View.GONE

        observeForObservers()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.itemSettings -> {
                return true
            }
            R.id.itemSearch -> {
                openSearchActivity()
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Permissions

    private fun requestPermission() {
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.INTERNET),
            Constants.PERMISSION_REQUEST_CODE)
    }

    private fun showBrightnessPermissionDialog(context: Context) {
        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
        intent.data = Uri.parse("package:" + context.packageName)
        context.startActivity(intent)
    }

    override fun showAlert() {
        requestPermission()
    }

    override fun permissionGranted() {
        init()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            Constants.PERMISSION_REQUEST_CODE -> {
                permissionUtil.checkorRequestPermission(permission = permissions[0])
            }
        }
    }

    private fun observeForObservers() {
        model.foldersListExposed.observe(this, Observer {
            if (it != null) {
                videoListData = it
                tvEmptyList.visibility = View.GONE
                rvVideoList.visibility = View.VISIBLE
                setAdapter()
            }
        })
    }

    private fun setAdapter(){

        if (adapter == null) {

            adapter = StorageFileAdapter(this, R.layout.row_folder_item, this)
            val linearLayout = LinearLayoutManager(this)
            linearLayout.orientation = LinearLayoutManager.VERTICAL
            rvVideoList.layoutManager = linearLayout
            rvVideoList.adapter = adapter

        }

        if (model.MODE == Constants.FOLDER_VIEW) {
            setFoldersList()
        } else if (model.MODE == Constants.SONG_VIEW) {
            folderUpText.text = videoListData[model.CURRENT_SONG_FOLDER_INDEX].path
            mFolderViewContainer.visibility = View.VISIBLE
            setSongsList()
        }
    }

    private fun setFoldersList() {
        adapter?.submitList(videoListData)
    }

    private fun setSongsList() {
        adapter?.submitList(videoListData[model.CURRENT_SONG_FOLDER_INDEX]?.songsList)
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
        if (!mIsMultiSelectMode) {
            doActionOnOverflowItemClick(position, viewId)
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
            if (model.MODE != Constants.SONG_VIEW) {
                model.MODE = Constants.SONG_VIEW
                folderUpText.text = videoListData[position].path
                mFolderViewContainer.visibility = View.VISIBLE
                model.onItemClicked(position)
                setAdapter()
            } else {
                launchPlayerActivity(position)
            }
        } else {
            toggleSelection(position)
        }

    }

    private fun launchPlayerActivity(position: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.System.canWrite(this)) {
                playVideo(position, false)
            } else {
                showBrightnessPermissionDialog(this)
            }
        } else {
            playVideo(position, false)
        }

    }

    private fun playVideo(position: Int, isMultiple: Boolean) {
        val playerIntent = Intent(this, PlayerActivity::class.java)
       if (!isMultiple) {
           val currentVideo = videoListData[model.CURRENT_SONG_FOLDER_INDEX].songsList[position]
           playerIntent.putExtra(Constants.FILE_PATH, currentVideo.path)
           val pathsList = ArrayList<String?>()
           for (folder in videoListData[model.CURRENT_SONG_FOLDER_INDEX].songsList) {
               pathsList.add(folder.path)
           }
           playerIntent.putExtra(Constants.Playback.WINDOW, position)
           playerIntent.putExtra(Constants.QUEUE, pathsList)
       } else {
           val pathsList = ArrayList<String?>()
           for(position in adapter!!.getSelectedItems()) {
               pathsList.add(videoListData[model.CURRENT_SONG_FOLDER_INDEX].songsList[position].path)
           }
           playerIntent.putExtra(Constants.QUEUE, pathsList)
       }
        startActivity(playerIntent)
    }


    private fun playSelectedVideos() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.System.canWrite(this)) {
                playVideo(-1, true)
            } else {
                showBrightnessPermissionDialog(this)
            }
        } else {
            playVideo(-1, true)
        }

    }

    private fun doActionOnOverflowItemClick(position: Int, viewId: Int) {
        //val data = videoListData[model.CURRENT_SONG_FOLDER_INDEX].songsList[position]

        when (viewId) {
            R.id.itemPlay -> {
                launchPlayerActivity(position)
            }
            R.id.itemShare -> {
                shareVideos(position)
            }
            R.id.itemDelete -> {
                model.delete(listOf(position), false, this)
            }
        }
    }

    private fun shareVideos(position: Int) {
        val currentVideo = videoListData[model.CURRENT_SONG_FOLDER_INDEX].songsList[position]

        startActivity(Intent.createChooser(Intent().setAction(Intent.ACTION_SEND)
            .setType("video/*")
            .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .putExtra(Intent.EXTRA_STREAM,  ContextMenuUtil.getAudioContentUri(this@VideoListActivity, File(currentVideo.path))), "Share Video"))
    }

    private fun shareMultipleVideos() {
        val listOfVideoUris = ArrayList<Uri?>()
        for (position in adapter!!.getSelectedItems()) {
            val currentVideo = videoListData[model.CURRENT_SONG_FOLDER_INDEX].songsList[position]
            listOfVideoUris.add(ContextMenuUtil.getAudioContentUri(this@VideoListActivity, File(currentVideo.path)))
        }
        startActivity(Intent.createChooser(Intent().setAction(Intent.ACTION_SEND_MULTIPLE)
            .setType("video/*")
            .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .putExtra(Intent.EXTRA_STREAM,  listOfVideoUris), "Share Video"))
    }

    override fun onBackPressed() {
        if (model.MODE == Constants.SONG_VIEW) {
            model.MODE = Constants.FOLDER_VIEW
            setAdapter()
            folderUpText.text = "..."
            model.onBackFolderPressed()
            mFolderViewContainer.visibility = View.GONE
        }
    }

    override fun onDeleteSuccess(listOfIndexes: List<Int>) {
        for(position in listOfIndexes) {
            model.removeElementAt(position)
            adapter!!.notifyItemRemoved(position)
        }
        Toast.makeText(
            this@VideoListActivity,
            "${listOfIndexes.size} " + getString(R.string.songs_deleted_toast),
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onDeleteError() {
        Toast.makeText(mContext, "Some error occurred while deleting video(s)", Toast.LENGTH_SHORT).show()
    }

    private fun openSearchActivity() {
        val intent = Intent(this@VideoListActivity, SearchActivity::class.java)
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
                    if (model.MODE == Constants.SONG_VIEW)
                        model.delete(adapter!!.getSelectedItems(), false, mContext)
                    else
                        model.delete(adapter!!.getSelectedItems(), true, mContext)
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
}
