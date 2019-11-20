package com.project100pi.pivideoplayer.ui.activity

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.AutoCompleteTextView
import android.widget.TextView
import android.widget.Toast
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
import com.project100pi.pivideoplayer.ui.adapters.VideoFilesAdapter
import com.project100pi.pivideoplayer.factory.SearchViewModelFactory
import com.project100pi.pivideoplayer.listeners.ItemDeleteListener
import com.project100pi.pivideoplayer.listeners.OnClickListener
import com.project100pi.pivideoplayer.model.FileInfo
import com.project100pi.pivideoplayer.ui.activity.viewmodel.SearchViewModel
import com.project100pi.pivideoplayer.utils.Constants
import com.project100pi.pivideoplayer.utils.ContextMenuUtil
import java.io.File

class SearchActivity: AppCompatActivity(), OnClickListener, ItemDeleteListener {

    private val TAG = "SearchActivity"

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

    private var videoSearchResultData: ArrayList<FileInfo> = ArrayList()

    private var isSearchTriggered = false

    private lateinit var searchViewModel: SearchViewModel
    private var adapter: VideoFilesAdapter? = null
    private var mIsMultiSelectMode: Boolean = false
    private var actionModeCallback = ActionModeCallback()
    private var actionMode: ActionMode? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        ButterKnife.bind(this)

        val application = requireNotNull(this).application
        val viewModelFactory = SearchViewModelFactory(this , application)
        searchViewModel = ViewModelProviders.of(this, viewModelFactory).get(SearchViewModel::class.java)

        initializeToolbar()
        initializeAutoCompleteTextView()

        if(autoCompleteTextView.requestFocus())
            showKeyboard()

        sorryMessageTextView.visibility = View.VISIBLE
        searchResultsRecyclerView.visibility = View.GONE

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

    private fun initializeToolbar() {
        setSupportActionBar(mToolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun observeForObservers() {
       observeForSearchResultList()
    }

    private fun observeForSearchResultList() {
        searchViewModel.foldersListExposed.observe(this, Observer {
            if (it != null) {
                videoSearchResultData = it
                if (it.size > 0) {
                    sorryMessageTextView.visibility = View.GONE
                    searchResultsRecyclerView.visibility = View.VISIBLE
                    setAdapter()
                } else {
                    sorryMessageTextView.visibility = View.VISIBLE
                    searchResultsRecyclerView.visibility = View.GONE
                }
            }
        })
    }

    private fun setAdapter(){

        if (adapter == null) {

            adapter = VideoFilesAdapter(this, R.layout.row_video_item, this)
            val linearLayout = LinearLayoutManager(this)
            linearLayout.orientation = LinearLayoutManager.VERTICAL
            searchResultsRecyclerView.layoutManager = linearLayout
            searchResultsRecyclerView.adapter = adapter

        }
        setSearchResult()
    }

    private fun setSearchResult() {
        adapter?.submitList(videoSearchResultData)
        isSearchTriggered = false
    }

    private fun initializeAutoCompleteTextView() {
        autoCompleteTextView.dropDownWidth = resources.displayMetrics.widthPixels
        //Only when 2 or more characters is typed we will trigger the search.
        autoCompleteTextView.threshold = 2

        autoCompleteTextView.setCompoundDrawablesWithIntrinsicBounds(
            resources.getDrawable(R.drawable.ic_search_black_24dp),
            null,
            null,
            null
        )
    }

    private fun setListeners() {
        Logger.i("setListeners() :: setting listeners for auto complete textview and viewpager.")
        setOnEditorActionListener()
        autoCompleteTextView.addTextChangedListener(AutoCompleteTextWatcher())
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

    private fun triggerSearch(searchSource: String) {
        val queryText = autoCompleteTextView.text.toString()
        when (searchSource) {
            Constants.SearchSource.SEARCH_LOCAL -> searchViewModel.performSearch(queryText)
        }
        isSearchTriggered = true
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

    private fun doActionOnOverflowItemClick(position: Int, viewId: Int) {
        //val data = videoListData[searchViewModel.currentSongFolderIndex].songsList[position]

        when (viewId) {
            R.id.itemPlay -> {
                launchPlayerActivity(position)
            }
            R.id.itemShare -> {
                shareVideos(position)
            }
            R.id.itemDelete -> {
                searchViewModel.delete(listOf(position), this)
            }
        }
    }

    private fun shareVideos(position: Int) {
        val currentVideo = videoSearchResultData[position]

        startActivity(Intent.createChooser(Intent().setAction(Intent.ACTION_SEND)
            .setType("video/*")
            .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .putExtra(Intent.EXTRA_STREAM,  ContextMenuUtil.getVideoContentUri(this@SearchActivity, File(currentVideo.filePath))), "Share Video"))
    }

    private fun launchPlayerActivity(position: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.System.canWrite(this)) {
                //Play the video
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
            val currentVideo = videoSearchResultData[position]
            val metadata = VideoMetaData(currentVideo._Id.toInt(), currentVideo.fileName, currentVideo.filePath)
            playerIntent.putExtra(Constants.FILE_PATH, metadata)
            playerIntent.putExtra(Constants.Playback.WINDOW, 0)
        } else {
            val metaDataList = ArrayList<VideoMetaData>()
            for(selectedItemPosition in adapter!!.getSelectedItems()) {
                for(selectedItemPosition in adapter!!.getSelectedItems()) {
//                    metaDataList.add(directoryListViewModel.getVideoMetaData(videoListData[directoryListViewModel.currentSongFolderIndex].songsList[selectedItemPosition].folderId)!!)
                    metaDataList.add(
                        VideoMetaData(videoSearchResultData[selectedItemPosition]._Id.toInt(),
                            videoSearchResultData[selectedItemPosition].fileName,
                            videoSearchResultData[selectedItemPosition].filePath)
                    )
                }
            }
            playerIntent.putExtra(Constants.QUEUE, metaDataList)
        }
        startActivity(playerIntent)
    }

    private fun showBrightnessPermissionDialog(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
            intent.data = Uri.parse("package:" + context.packageName)
            context.startActivity(intent)
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

    override fun onDeleteSuccess(listOfIndexes: List<Int>) {
        for(position in listOfIndexes) {
            searchViewModel.removeElementAt(position)
            adapter!!.notifyItemRemoved(position)
        }
        Toast.makeText(
            this@SearchActivity,
            "${listOfIndexes.size} " + getString(R.string.songs_deleted_toast),
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onDeleteError() {
        Toast.makeText(this@SearchActivity, "Some error occurred while deleting video(s)", Toast.LENGTH_SHORT).show()
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

    private fun shareMultipleVideos() {
        val listOfVideoUris = ArrayList<Uri?>()
        for (position in adapter!!.getSelectedItems()) {
            val currentVideo = videoSearchResultData[position]
            listOfVideoUris.add(ContextMenuUtil.getVideoContentUri(this@SearchActivity, File(currentVideo.filePath)))
        }
        startActivity(Intent.createChooser(Intent().setAction(Intent.ACTION_SEND_MULTIPLE)
            .setType("video/*")
            .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .putExtra(Intent.EXTRA_STREAM,  listOfVideoUris), "Share Video"))
    }

    private fun showKeyboard() {
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        val `in` = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        `in`.showSoftInput(autoCompleteTextView, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard() {
        val `in` = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        `in`.hideSoftInputFromWindow(autoCompleteTextView.windowToken, 0)
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
                    searchViewModel.delete(adapter!!.getSelectedItems(), this@SearchActivity)
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

}