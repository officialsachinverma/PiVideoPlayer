package com.project100pi.pivideoplayer.ui.activity

import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
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
import com.project100pi.pivideoplayer.database.TinyDB
import com.project100pi.pivideoplayer.listeners.ItemDeleteListener
import com.project100pi.pivideoplayer.listeners.OnClickListener
import com.project100pi.pivideoplayer.model.VideoTrackInfo
import com.project100pi.pivideoplayer.model.observable.VideoChangeObservable
import com.project100pi.pivideoplayer.ui.activity.viewmodel.VideoListViewModel
import com.project100pi.pivideoplayer.ui.activity.viewmodel.factory.VideoListViewModelFactory
import com.project100pi.pivideoplayer.ui.adapters.VideoFilesAdapter
import com.project100pi.pivideoplayer.utils.Constants
import com.project100pi.pivideoplayer.utils.ContextMenuUtil
import java.io.File


class VideoListActivity : AppCompatActivity(), OnClickListener, ItemDeleteListener {

    @BindView(R.id.rv_video_file_list)
    lateinit var rvVideoList: RecyclerView

    @BindView(R.id.video_list_toolbar)
    lateinit var mToolbar: Toolbar

    @BindView(R.id.tv_no_video_found_msg)
    lateinit var tvEmptyList: TextView

    @BindView(R.id.pg_video_waiting)
    lateinit var pgWaiting: ProgressBar

    private lateinit var videoListViewModel: VideoListViewModel

    //private var videoListData = mutableListOf<VideoTrackInfo>()
    private var directoryName = ""
    private var directoryPath = ""

    private lateinit var adapter: VideoFilesAdapter

    private var actionModeCallback = ActionModeCallback()
    private var actionMode: ActionMode? = null

    companion object {

        var mIsMultiSelectMode: Boolean = false

        // starter pattern is more strict approach to starting an activity.
        // Main purpose is to improve more readability, while at the same time
        // decrease code complexity, maintenance costs, and coupling of your components.

        // Read more: https://blog.mindorks.com/learn-to-write-good-code-in-android-starter-pattern
        // https://www.programming-books.io/essential/android/starter-pattern-d2db17d348ca46ce8979c8af6504f018

        // Using starter pattern to start this activity
        fun start(
            context: Context,
            directoryName: String,
            directoryPath: String,
            videoListData: MutableList<VideoTrackInfo>
        ) {
            val intent = Intent(context, VideoListActivity::class.java)
            intent.putExtra(Constants.Storage.DIRECTORY_NAME, directoryName)
            intent.putExtra(Constants.Storage.DIRECTORY_PATH, directoryPath)
            //intent.putExtra(Constants.Storage.VIDEO_LIST, videoListData)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_list)
        ButterKnife.bind(this)
        setSupportActionBar(mToolbar)
        this.intent?.let {
            directoryName = it.getStringExtra(Constants.Storage.DIRECTORY_NAME) ?: ""
            directoryPath = it.getStringExtra(Constants.Storage.DIRECTORY_PATH) ?: ""
            //videoListData = it.getParcelableArrayListExtra<VideoTrackInfo>(Constants.Storage.VIDEO_LIST)
        }

        init()
    }

    /**
     * Initialises stuffs and performs some initial things
     */

    private fun init() {

        val viewModelFactory = VideoListViewModelFactory(this, directoryPath, directoryName, this)
        videoListViewModel =
            ViewModelProviders.of(this, viewModelFactory).get(VideoListViewModel::class.java)

        setTitleOnToolbar()
        setHomeUpAsButton()

        initAdapter()

        //setDataToAdapter()

        observeForObservable()
    }

    /**
     * This method contains all other methods
     * who are observing a particular thing
     */

    private fun observeForObservable() {
        observeForVideoList()
    }

    /**
     * This method observes Search Result List
     * whenever data is available we have to set
     * that data on the adapter
     */

    private fun observeForVideoList() {
        videoListViewModel.filesList.observe(this, Observer {
            setDataToAdapter(it)
        })
    }

    /**
     * This method submits list to the adapter
     * and hides/shows empty msg and waiting sings
     * based on data availability
     */

    private fun setDataToAdapter(it: MutableList<VideoTrackInfo>) {
        if (it.size > 0) {
            hideEmptyListMsg()
            showVideoList()
            hideWaitingSign()

            adapter.submitList(it)
//            adapter.notifyDataSetChanged()
            Toast.makeText(this,"Size is "+it.size,Toast.LENGTH_SHORT).show()
        } else {
            showEmptyListMsg()
            hideVideoList()
            hideWaitingSign()
        }
    }

    /**
     * sets toolbar title which is the folder name which
     * user has selected
     */

    private fun setTitleOnToolbar() {
        supportActionBar?.title = directoryName
    }

    /**
     * enables home back button
     */

    private fun setHomeUpAsButton() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    /**
     * this method initialises the adapter
     * WARNING: Adapter is a late init property
     * it has to be initialised before using anywhere
     * kotlin won't even allow you to put null check on it
     */

    private fun initAdapter() {
        adapter = VideoFilesAdapter(this, R.layout.row_video_item, this)
        val linearLayout = LinearLayoutManager(this)
        linearLayout.orientation = LinearLayoutManager.VERTICAL
        rvVideoList.layoutManager = linearLayout
        rvVideoList.adapter = adapter
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) =
        when (item.itemId) {
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
        hideWaitingSign()
        when (requestCode) {
            Constants.Permission.SD_CARD_WRITE_PERMISSION_REQUEST_CODE -> {
                if (resultCode == RESULT_OK && data != null) {
                    // when user will select the external sd card we will receive it's uri in data
                    val sdCardUri = data.data
                    // saving the sd card uri in tiny db (shared preferences)
                    TinyDB.putString(Constants.ExternalSDCard.SD_CARD_URI, sdCardUri.toString())
                    // Persist access permissions
                    val takeFlags =
                        data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    contentResolver.takePersistableUriPermission(sdCardUri!!, takeFlags)

                    Toast.makeText(this, R.string.do_operation_again, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * starts search activity
     */

    private fun launchSearchActivity() {
        SearchActivity.start(this)
    }

    /**
     * calls playVideo method which will generate data
     * and pass it to player activity
     * it takes the position of selected video
     *
     * @param position Int
     */

    private fun launchPlayerActivity(position: Int) {
        playVideo(position, false)
    }

    /**
     * This method calls playVideo which will generate
     * data and launches player activity
     */

    private fun playSelectedVideos() {
        playVideo(-1, true)
    }

    /**
     * This method generates data and starts player activity
     * it takes the position of selected video in case user
     * has selected a single video
     * it also takes a param isMultiple which shows whether the
     * selection is of multiple videos or not
     *
     * @param position Int
     * @param isMultiple Boolean
     */

    private fun playVideo(position: Int, isMultiple: Boolean) {
        try {

            val metaDataList = arrayListOf<VideoMetaData>()
            if (!isMultiple) {
//                val metadata = directoryListViewModel.getVideoMetaData(currentVideo.folderId)
//                playerIntent.putExtra(Constants.FILE_PATH, currentVideo.path)
                //playerIntent.putExtra(Constants.FILE_PATH, metadata)

                for (folder in videoListViewModel.filesList.value!!) {
                    metaDataList.add(VideoMetaData(folder._Id, folder.videoName, folder.videoPath))
                }
                PlayerActivity.start(this, metaDataList, position)
            } else {
                for (selectedItemPosition in adapter.getSelectedItems()) {
//                    metaDataList.add(directoryListViewModel
//                    .getVideoMetaData(videoListData[directoryListViewModel
//                    .currentSongFolderIndex].songsList[selectedItemPosition].folderId)!!)
                    metaDataList.add(
                        VideoMetaData(
                            videoListViewModel.filesList.value!![selectedItemPosition]._Id,
                            videoListViewModel.filesList.value!![selectedItemPosition].videoName,
                            videoListViewModel.filesList.value!![selectedItemPosition].videoPath
                        )
                    )
                }
                PlayerActivity.start(this, metaDataList)
            }

        } catch (e: ArrayIndexOutOfBoundsException) {
            Logger.i(e.toString())
            Toast.makeText(this, R.string.error_failed_to_play, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * This callback is called when overflow menu item is clicked
     * it takes the position of selected video and the Id of view (menu item)
     * on which user has clicked to perform that specific action only
     * NOTE: In case of folder we do not have overflow menus
     * as of now and hence, left empty
     *
     * @param position Int
     * @param viewId Int
     */

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

    /**
     * this method creates an intent chooser
     * to display all the apps through which
     * user can share the videos
     * it takes position of video which is selected by user
     *
     * @param position Int
     */

    private fun shareVideos(position: Int) {
        val currentVideo = videoListViewModel.filesList.value!![position]

        startActivity(
            Intent.createChooser(
                Intent().setAction(Intent.ACTION_SEND)
                    .setType("video/*")
                    .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    .putExtra(
                        Intent.EXTRA_STREAM,
                        ContextMenuUtil.getVideoContentUri(this, File(currentVideo.videoPath))
                    ), resources.getString(R.string.share_video)
            )
        )
    }

    inner class ActionModeCallback : ActionMode.Callback {

        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            mIsMultiSelectMode = true
            hideToolbar()
            mode!!.menuInflater.inflate(R.menu.multi_choice_option, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?) = true

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            when (item!!.itemId) {
                R.id.multiChoiceSelectAll -> {
                    adapter.selectAllItems()
                }
                R.id.multiChoicePlay -> {
                    playSelectedVideos()
                }
                R.id.multiChoiceShare -> {
                    videoListViewModel.shareMultipleVideos(adapter.getSelectedItems())
                }
                R.id.multiChoiceDelete -> {
                    showMultiDeleteConfirmation(adapter.getSelectedItems())
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
            showToolbar()
            clearAdapterSelection()
        }

    }

    /**
     * Clears the selected items in adapter
     */
    private fun clearAdapterSelection() {
        adapter.clearSelection()
    }

    /**
     * this method handles selection of an item in case of
     * multi selection. It takes the position of video which
     * is clicked or selected
     *
     * @param position Int
     */

    private fun toggleSelection(position: Int) {
        adapter.toggleSelection(position)
        val count = adapter.getSelectedItemCount()

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

    /**
     * This method triggers action mode when user long presses
     * on a video
     * it takes the position of video which user has selected
     *
     * @param position Int
     * @return Boolean
     */

    override fun onItemLongClicked(position: Int): Boolean {
        if (actionMode == null) {
            actionMode = startSupportActionMode(actionModeCallback)
        }

        toggleSelection(position)

        return true
    }

    /**
     * This method gets called when a user clicks/selects a
     * video.
     * If multi select mode is not active then, navigate user to player activity
     * where it will start playing the video, otherwise toggleSelection
     * it takes the position of video which user has selected
     *
     * @param position Int
     */

    override fun onItemSelected(position: Int) {
        if (!mIsMultiSelectMode) {
            launchPlayerActivity(position)
        } else {
            toggleSelection(position)
        }
    }

    /**
     * This callback is called when overflow menu item is clicked
     * it takes the position of selected video and the Id of view (menu item)
     * on which user has clicked to perform that specific action only
     * NOTE: In case of folder we do not have overflow menus
     * as of now and hence, left empty
     *
     * @param position Int
     * @param viewId Int
     */

    override fun onOverflowItemClick(position: Int, viewId: Int) {
        if (!mIsMultiSelectMode) {
            doActionOnOverflowItemClick(position, viewId)
        }
    }

    /**
     * This method is called when application does not
     * have permission to read or write in external sd card
     * It opens up a system activity where user can navigate
     * to external sd card and click on select button on which
     * it will callback to onActivity result with path to external sd card
     */

    override fun showPermissionForSdCard() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            startActivityForResult(intent, Constants.Permission.SD_CARD_WRITE_PERMISSION_REQUEST_CODE)
        } else {
            Toast.makeText(this, R.string.do_operation_again, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * This method gets called when delete operation is successful
     * it gives list of indices of item which are hard deleted and now
     * supposed to be update on UI
     *
     * @param listOfIndexes List<Int>
     */

    override fun onDeleteSuccess(listOfIndexes: List<Int>) {
        hideWaitingSign()
        var k = 0
        for (position in listOfIndexes) {
            if (videoListViewModel.removeElementAt(position - k)) {
               adapter.notifyItemRemoved(position - k)
                k++
            }
        }
        //adapter.submitList(videoListViewModel.filesList.value!!)
        VideoChangeObservable.setChangedOverride()
        Toast.makeText(this, "${listOfIndexes.size} " + getString(R.string.songs_deleted_toast), Toast.LENGTH_SHORT).show()
    }

    /**
     * This method is called when an error occurred while
     * executing the delete operation
     */

    override fun onDeleteError() {
        hideWaitingSign()
        Toast.makeText(this, R.string.error_occurred_while_deleting_videos, Toast.LENGTH_SHORT).show()
    }

    /**
     * This method shows a confirmation dialog for deletion of multiple videos
     * it records two user responses
     * YES -> Execute delete operation
     * NO -> Cancels operation execution
     */

    private fun showMultiDeleteConfirmation(listOfIndices: List<Int>) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete)
            .setMessage("Are you sure you want to delete this ${adapter.getSelectedItemCount()} video(s)?")
            .setPositiveButton(android.R.string.yes) { _, _ ->
                showWaitingSign()
                videoListViewModel.deleteVideo(listOfIndices)
            }
            .setNegativeButton(android.R.string.no, null)
            .setCancelable(false)
            .show()
    }

    /**
     * This method shows a confirmation dialog for deletion of single video
     * it takes the position of video which user has selected
     * Dialog records two user responses
     * YES -> Execute delete operation
     * NO -> Cancel the operation
     *
     * @param position Int
     */

    private fun showDeleteConfirmation(position: Int) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete)
            .setMessage(R.string.delete_confirmation_msg)
            .setPositiveButton(android.R.string.yes) { _, _ ->
                showWaitingSign()
                videoListViewModel.deleteVideo(listOf(position))
            }
            .setNegativeButton(android.R.string.no, null)
            .setCancelable(false)
            .show()
    }

    /**
     * hides toolbar
     */

    private fun hideToolbar() {
        mToolbar.visibility = View.GONE
    }

    /**
     * shows toolbar
     */

    private fun showToolbar() {
        mToolbar.visibility = View.VISIBLE
    }

    /**
     * hides empty list msg
     */

    private fun hideEmptyListMsg() {
        tvEmptyList.visibility = View.GONE
    }

    /**
     * shows empty list msg
     */

    private fun showEmptyListMsg() {
        tvEmptyList.visibility = View.VISIBLE
    }

    /**
     * hides video list
     */

    private fun hideVideoList() {
        rvVideoList.visibility = View.GONE
    }

    /**
     * shows video list
     */

    private fun showVideoList() {
        rvVideoList.visibility = View.VISIBLE
    }

    /**
     * hides waiting sign
     */

    private fun hideWaitingSign() {
        pgWaiting.visibility = View.GONE
    }

    /**
     * shows waiting sign
     */

    private fun showWaitingSign() {
        pgWaiting.visibility = View.VISIBLE
    }

}