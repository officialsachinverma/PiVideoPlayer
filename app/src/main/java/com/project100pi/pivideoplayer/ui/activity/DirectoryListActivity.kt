package com.project100pi.pivideoplayer.ui.activity

import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
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
import butterknife.BindView
import butterknife.ButterKnife
import com.project100pi.library.misc.Logger
import com.project100pi.pivideoplayer.database.TinyDB
import com.project100pi.pivideoplayer.ui.adapters.FoldersAdapter
import com.project100pi.pivideoplayer.ui.activity.viewmodel.factory.DirectoryListViewModelFactory
import com.project100pi.pivideoplayer.listeners.ItemDeleteListener
import com.project100pi.pivideoplayer.model.FolderInfo
import com.project100pi.pivideoplayer.ui.activity.viewmodel.DirectoryListViewModel
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
    private lateinit var adapter: FoldersAdapter
    private var actionModeCallback = ActionModeCallback()
    private var actionMode: ActionMode? = null
    private var videoListData: ArrayList<FolderInfo> = ArrayList()

    private var doubleBackToExitPressedOnce = false

    companion object {

        var mIsMultiSelectMode: Boolean = false

        // starter pattern is more strict approach to starting an activity.
        // Main purpose is to improve more readability, while at the same time
        // decrease code complexity, maintenance costs, and coupling of your components.

        // Read more: https://blog.mindorks.com/learn-to-write-good-code-in-android-starter-pattern
        // https://www.programming-books.io/essential/android/starter-pattern-d2db17d348ca46ce8979c8af6504f018

        // Using starter pattern to start this activity
        fun start(context: Context) {
            context.startActivity(Intent(context, DirectoryListActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_directory_list)
        ButterKnife.bind(this)
        setSupportActionBar(mToolbar)

        init()
    }

    /**
     * Initialises stuffs
     */

    private fun init() {

        val viewModelFactory = DirectoryListViewModelFactory(this, this)
        directoryListViewModel = ViewModelProviders.of(this, viewModelFactory).get(
            DirectoryListViewModel::class.java)

        initAdapter()
        observeForObservers()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) =
        when (item.itemId) {
            R.id.itemSearch -> {
                startSearchActivity()
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
                    val takeFlags = data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    contentResolver.takePersistableUriPermission(sdCardUri!!, takeFlags)

                    Toast.makeText(this, R.string.do_operation_again, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * This method contains all other methods
     * who are observing to a particular thing
     */
    private fun observeForObservers() {
        observeForFolderList()
    }

    /**
     * This method observes Folder List
     */
    private fun observeForFolderList() {
        directoryListViewModel.foldersList.observe(this, Observer {
            if (it != null) {
                if (it.size > 0) {
                    videoListData = it
                    hideEmptyListMsg()
                    hideWaitingSign()
                    showVideoList()
                    submitDataToAdapter()
                } else {
                    showEmptyListMsg()
                    hideWaitingSign()
                    hideVideoList()
                }
            }
        })
    }

    /**
     * This method submits list to the adapter
     */

    private fun submitDataToAdapter(){
        // running on UI thread because video count can
        // get updated when user is in Video List Activity
        // either when a video gets deleted or when a new video
        // gets added
        runOnUiThread {
            adapter.submitList(videoListData)
        }
    }

    /**
     * this method initialises the adapter
     * WARNING : Adapter is a late init property
     * it has to be initialised before using anywhere
     * kotlin won't even allow you to put null check on it
     */

    private fun initAdapter() {
        adapter = FoldersAdapter(this, R.layout.row_directory_item, this)
        val linearLayout = LinearLayoutManager(this)
        linearLayout.orientation = LinearLayoutManager.VERTICAL
        rvVideoList.layoutManager = linearLayout
        rvVideoList.adapter = adapter
    }

    /**
     * this method handles selection of an item in case of
     * multi selection. It takes the position of item which
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

    }

    /**
     * This method triggers action mode when user long presses
     * on a folder
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
     * folder.
     * If multi select mode is not active then, navigate user to VideoListActivity
     * otherwise toggleSelection
     * it takes the position of video which user has selected
     *
     * @param position Int
     */

    override fun onItemSelected(position: Int) {
        if (!mIsMultiSelectMode) {
            startVideoListActivity(position)
        } else {
            toggleSelection(position)
        }
    }

    /**
     * This method starts VideoListActivity with folder name and its path
     * it takes the position of video which user has selected
     *
     * @param position Int
     */

    private fun startVideoListActivity(position: Int) {
        try {
            VideoListActivity.start(this, videoListData[position].folderName, videoListData[position].folderPath)
        } catch (e: ArrayIndexOutOfBoundsException) {
            e.printStackTrace()
            Logger.e(e.message.toString())
            Toast.makeText(this, R.string.error_occurred_while_showing_videos_list, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * This method calls playMultipleVideos with list of indices of selected folders
     */

    private fun playSelectedVideos() {
        directoryListViewModel.playMultipleVideos(adapter.getSelectedItems())
    }

    /**
     * This method calls shareMultipleVideos with
     * list of indices of selected folders
     */

    private fun shareMultipleVideos() {
        directoryListViewModel.shareMultipleVideos(adapter.getSelectedItems())
    }

    /**
     * This method handles back button operation
     * If user presses back button with in 2 secs then
     * activity finishes
     */

    override fun onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed()
            return
        }

        this.doubleBackToExitPressedOnce = true
        Toast.makeText(this, R.string.click_back_to_exit, Toast.LENGTH_SHORT).show()

        Handler().postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
    }

    /**
     * This method is called when application does not
     * have permission to read or write in external sd card
     * It opens up a system activity where user can navigate
     * to external sd card and click on select button on which
     * it will callback to onActivity result with path to external sd card
     */

    override fun showPermissionForSdCard() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        startActivityForResult(intent, Constants.Permission.SD_CARD_WRITE_PERMISSION_REQUEST_CODE)
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
        for(position in listOfIndexes) {
            directoryListViewModel.removeElementAt(position)
            adapter.notifyItemRemoved(position)
        }
        Toast.makeText(
            this,
            "${listOfIndexes.size} " + getString(R.string.songs_deleted_toast),
            Toast.LENGTH_SHORT
        ).show()
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
     * This method starts search activity
     */

    private fun startSearchActivity() {
        SearchActivity.start(this)
    }

    inner class ActionModeCallback: ActionMode.Callback {

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
                    shareMultipleVideos()
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
     * This method shows a confirmation dialog for deletion
     * it records two user responses
     * YES -> Execute delete operation
     * NO -> Cancels operation execution
     */

    private fun showMultiDeleteConfirmation(listOfIndexes: List<Int>) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete)
            .setMessage("Are you sure you want to delete ${adapter.getSelectedItemCount()} video(s)?")
            .setPositiveButton(android.R.string.yes) { _, _ ->
                showWaitingSign()
                directoryListViewModel.deleteFolderContents(listOfIndexes)
            }
            .setNegativeButton(android.R.string.no, null)
            .setCancelable(false)
            .show()
    }

    /**
     * this method hides toolbar
     */

    private fun hideToolbar() {
        mToolbar.visibility = View.GONE
    }

    /**
     * this method shows toolbar
     */

    private fun showToolbar() {
        mToolbar.visibility = View.VISIBLE
    }

    /**
     * this method hides empty msg
     */

    private fun hideEmptyListMsg() {
        tvEmptyList.visibility = View.GONE
    }

    /**
     * this method shows empty msg
     */

    private fun showEmptyListMsg() {
        tvEmptyList.visibility = View.VISIBLE
    }

    /**
     * this method hides video list
     */

    private fun hideVideoList() {
        rvVideoList.visibility = View.GONE
    }

    /**
     * this method shows video list
     */

    private fun showVideoList() {
        rvVideoList.visibility = View.VISIBLE
    }

    /**
     * this method hides waiting sign
     */

    private fun hideWaitingSign() {
        pgWaiting.visibility = View.GONE
    }

    /**
     * this method shows waiting sign
     */

    private fun showWaitingSign() {
        pgWaiting.visibility = View.VISIBLE
    }
}
