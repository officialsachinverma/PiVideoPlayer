package com.project100pi.pivideoplayer.ui.activity

import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.AutoCompleteTextView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
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
import com.project100pi.pivideoplayer.ui.activity.viewmodel.factory.SearchViewModelFactory
import com.project100pi.pivideoplayer.listeners.ItemDeleteListener
import com.project100pi.pivideoplayer.listeners.OnClickListener
import com.project100pi.pivideoplayer.model.VideoTrackInfo
import com.project100pi.pivideoplayer.ui.activity.viewmodel.SearchViewModel
import com.project100pi.pivideoplayer.ui.adapters.SearchResultAdapter
import com.project100pi.pivideoplayer.utils.Constants
import com.project100pi.pivideoplayer.utils.ContextMenuUtil
import java.io.File

class SearchActivity: AppCompatActivity(), OnClickListener, ItemDeleteListener {

    @BindView(R.id.outer_window)
    lateinit var outerLayout: ConstraintLayout

    @BindView(R.id.toolbar)
    lateinit var mToolbar: Toolbar

    @BindView(R.id.autoCompleteTextView3)
    lateinit var autoCompleteTextView: AutoCompleteTextView

    @BindView(R.id.search_result_recycler_view)
    lateinit var searchResultsRecyclerView: RecyclerView

    @BindView(R.id.sorryMessage)
    lateinit var sorryMessageTextView: TextView

    private var videoSearchResultData: ArrayList<VideoTrackInfo> = ArrayList()

    private var isSearchTriggered = false

    private lateinit var searchViewModel: SearchViewModel
    private lateinit var adapter: SearchResultAdapter
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
        fun start(context: Context) {
            context.startActivity(Intent(context, SearchActivity::class.java))
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        ButterKnife.bind(this)

        init()
    }

    /**
     * Initialises stuffs
     */
    private fun init() {

        val viewModelFactory = SearchViewModelFactory(this, this)
        searchViewModel = ViewModelProviders.of(this, viewModelFactory).get(SearchViewModel::class.java)

        initializeToolbar()
        initializeAutoCompleteTextView()
        initAdapter()

        if(autoCompleteTextView.requestFocus())
            showKeyboard()

        observeForObservers()
    }

    override fun onStart() {
        super.onStart()
        setListeners()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
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
     * Sets toolbar as support action bar
     * and enabled back home button
     */
    private fun initializeToolbar() {
        setSupportActionBar(mToolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }
    }

    /**
     * This method contains all other methods
     * who are observing to a particular thing
     */
    private fun observeForObservers() {
       observeForSearchResultList()
    }

    /**
     * This method observes Search Result List
     * whenever data is available we have to set
     * that data on the adapter
     */
    private fun observeForSearchResultList() {
        searchViewModel.searchResultList.observe(this, Observer {
            if (it != null) {
                videoSearchResultData = it
                if (it.size > 0) {
                    hideNoVideoFoundMsg()
                    showSearchResultList()
                    setSearchResult()
                } else {
                    showNoVideoFoundMsg()
                    hideSearchResultList()
                }
            }
        })
    }

    /**
     * this method initialises the adapter
     * WARNING : Adapter is a late init property
     * it has to be initialised before using anywhere
     * kotlin won't even allow you to put null check on it
     */
    private fun initAdapter() {
        adapter = SearchResultAdapter(
            this,
            R.layout.row_video_item,
            this
        )
        val linearLayout = LinearLayoutManager(this)
        linearLayout.orientation = LinearLayoutManager.VERTICAL
        searchResultsRecyclerView.layoutManager = linearLayout
        searchResultsRecyclerView.adapter = adapter
    }

    /**
     * This method submits list to the adapter
     */
    private fun setSearchResult() {
        adapter.submitList(videoSearchResultData)
        isSearchTriggered = false
    }

    /**
     * settings a threshold value for search field
     * value 2 mean search will happen when user
     * will enter at least 2 chars
     */
    private fun initializeAutoCompleteTextView() {
        //Only when 2 or more characters is typed we will trigger the search.
        autoCompleteTextView.threshold = 2

        autoCompleteTextView.setCompoundDrawablesWithIntrinsicBounds(
            resources.getDrawable(R.drawable.ic_search_black_24dp),
            null,
            null,
            null
        )
    }

    /**
     * sets listeners
     */
    private fun setListeners() {
        Logger.i("setListeners() :: setting listeners for auto complete text view and viewpager.")
        setOnEditorActionListener()
        setTextChangeListener()
        setOnFocusChangeListener()
    }

    /**
     * sets TextChangeListener on search field
     */
    private fun setTextChangeListener() {
        autoCompleteTextView.addTextChangedListener(AutoCompleteTextWatcher())
    }

    /**
     * sets setOnFocusChangeListener on search field
     *
     * It handles that ever autoCompleteTextView losses
     * focus the keyboard should hide and when it gains
     * focus keyboard should come up
     */
    private fun setOnFocusChangeListener() {
        autoCompleteTextView.setOnFocusChangeListener { _: View, hasFocus: Boolean ->
            if (hasFocus) {
                if (!isSearchTriggered) {
                    showKeyboard()
                }
            } else {
                hideKeyboard()
            }
        }
    }

    /**
     * sets setOnEditorActionListener on search field
     *
     * It handles that when ever user will press on search
     * icon on soft keyboard, the search should get triggered
     */
    private fun setOnEditorActionListener() {
        /*
         onEditorActionListener is needed to get the Keyevent when search icon is pressed in the keyboard.
         */
        autoCompleteTextView.setOnEditorActionListener { _: TextView, actionId: Int, _: KeyEvent? ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val currentQuery = autoCompleteTextView.text.toString()
                if (!TextUtils.isEmpty(currentQuery)) {
                    autoCompleteTextView.clearFocus()
                    triggerSearch(Constants.SearchSource.SEARCH_LOCAL)
                }
            }
            false
        }
    }

    /**
     * This method is responsible for executing
     * db queried to get the search result based on
     * user input
     * it takes string (entered by user) which is being searched in db
     *
     * @param searchSource String
     */
    private fun triggerSearch(searchSource: String) {
        val queryText = autoCompleteTextView.text.toString()
        when (searchSource) {
            Constants.SearchSource.SEARCH_LOCAL -> searchViewModel.performSearch(queryText)
        }
        isSearchTriggered = true
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
     * This method shows a confirmation dialog for deletion
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
                searchViewModel.deleteSearchedVideos(listOf(position))
            }
            .setNegativeButton(android.R.string.no, null)
            .setCancelable(false)
            .show()
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
        val currentVideo = videoSearchResultData[position]

        startActivity(Intent.createChooser(Intent().setAction(Intent.ACTION_SEND)
            .setType("video/*")
            .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .putExtra(Intent.EXTRA_STREAM,
                ContextMenuUtil.getVideoContentUri(this, File(currentVideo.videoPath))), resources.getString(R.string.share_video)))
    }

    /**
     * calls playVideo method which will generate data
     * and pass it to player activity
     * it takes position of video which is selected by user
     *
     * @param position Int
     */
    private fun launchPlayerActivity(position: Int) {
        playVideo(position, false)
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
        val metaDataList = ArrayList<VideoMetaData>()
        if (!isMultiple) {
            val currentVideo = videoSearchResultData[position]
            val metadata = VideoMetaData(currentVideo._Id, currentVideo.videoName, currentVideo.videoPath)
            metaDataList.add(metadata)

            PlayerActivity.start(this, metaDataList, 0)
        } else {
            for(selectedItemPosition in adapter.getSelectedItems()) {
//              metaDataList.add(directoryListViewModel
//              .getVideoMetaData(videoListData[directoryListViewModel
//              .currentSongFolderIndex].songsList[selectedItemPosition].folderId)!!)
                metaDataList.add(
                    VideoMetaData(
                        videoSearchResultData[selectedItemPosition]._Id,
                        videoSearchResultData[selectedItemPosition].videoName,
                        videoSearchResultData[selectedItemPosition].videoPath)
                    )
            }
            PlayerActivity.start(this, metaDataList)
        }
    }

    /**
     * this method handles selection of an item in case of
     * multi selection. It takes the position of item which
     * is clicked or selected by user
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
        for(position in listOfIndexes) {
            searchViewModel.removeElementAt(position)
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
        Toast.makeText(this, R.string.error_occurred_while_deleting_videos, Toast.LENGTH_SHORT).show()
    }

    /**
     * This method calls playVideo which will generates
     * data and launches player activity
     */
    private fun playSelectedVideos() {
        playVideo(-1, true)
    }

    /**
     * This method creates an intent chooser to show
     * user through apps he can share the videos
     */
    private fun shareMultipleVideos() {
        val listOfVideoUris = ArrayList<Uri?>()
        for (position in adapter.getSelectedItems()) {
            val currentVideo = videoSearchResultData[position]
            listOfVideoUris.add(ContextMenuUtil.getVideoContentUri(this, File(currentVideo.videoPath)))
        }
        startActivity(Intent.createChooser(Intent().setAction(Intent.ACTION_SEND_MULTIPLE)
            .setType("video/*")
            .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .putExtra(Intent.EXTRA_STREAM,  listOfVideoUris), resources.getString(R.string.share_video)))
    }

    /**
     * shows soft keyboard
     */
    private fun showKeyboard() {
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        val `in` = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        `in`.showSoftInput(autoCompleteTextView, InputMethodManager.SHOW_IMPLICIT)
    }

    /**
     * hides soft keyboard
     */
    private fun hideKeyboard() {
        val `in` = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        `in`.hideSoftInputFromWindow(autoCompleteTextView.windowToken, 0)
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

    inner class AutoCompleteTextWatcher: TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            if (s.toString().trim { it <= ' ' }.length >= 2 && autoCompleteTextView.hasFocus()) {

                triggerSearch(Constants.SearchSource.SEARCH_LOCAL)

            }
        }

        override fun afterTextChanged(s: Editable) {

        }
    }

    /**
     * This method shows a confirmation dialog for deletion
     * it records two user responses
     * YES -> Execute delete operation
     * NO -> Cancels operation execution
     */
    private fun showMultiDeleteConfirmation(listOfIndices: List<Int>) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete)
            .setMessage("Are you sure you want to delete this ${adapter.getSelectedItemCount()} video(s)?")
            .setPositiveButton(android.R.string.yes) { _, _ ->
                searchViewModel.deleteSearchedVideos(listOfIndices)
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
     * hides no videos found msg
     */
    private fun hideNoVideoFoundMsg() {
        sorryMessageTextView.visibility = View.GONE
    }

    /**
     * shows no video found msg
     */
    private fun showNoVideoFoundMsg() {
        sorryMessageTextView.visibility = View.VISIBLE
    }

    /**
     * hides search result list
     */
    private fun hideSearchResultList() {
        searchResultsRecyclerView.visibility = View.GONE
    }

    /**
     * shows search result list
     */
    private fun showSearchResultList() {
        searchResultsRecyclerView.visibility = View.VISIBLE
    }

}