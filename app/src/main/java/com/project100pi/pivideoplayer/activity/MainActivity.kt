package com.project100pi.pivideoplayer.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.project100pi.pivideoplayer.listeners.OnClickListener
import com.project100pi.pivideoplayer.R
import com.project100pi.pivideoplayer.utils.Constants
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
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
import com.project100pi.pivideoplayer.utils.Constants.PERMISSION_REQUEST_CODE
import com.project100pi.pivideoplayer.factory.MainViewModelFactory
import com.project100pi.pivideoplayer.listeners.ItemDeleteListener
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity(), OnClickListener, ItemDeleteListener {

    @BindView(R.id.rv_video_file_list) lateinit var  recyclerView: RecyclerView
    @BindView(R.id.folder_view_container) lateinit var mFolderViewContainer: View
    @BindView(R.id.folder_up_text) lateinit var folderUpText: TextView
    @BindView(R.id.folder_up_image) lateinit var folderUpIconImage: ImageView
    @BindView(R.id.anim_toolbar) lateinit var mToolbar: Toolbar

    private lateinit var model: MainViewModel
    private var adapter: StorageFileAdapter? = null
    private var actionModeCallback = ActionModeCallback()
    private var actionMode: ActionMode? = null
    private val mContext = this
    companion object {
        var mIsMultiSelectMode: Boolean = false
    }

    private fun init() {
        setSupportActionBar(mToolbar)

        val application = requireNotNull(this).application
        val viewModelFactory = MainViewModelFactory(this , application)
        model = ViewModelProviders.of(this, viewModelFactory).get(MainViewModel::class.java)

        val folderUpIcon = ContextCompat.getDrawable(this, R.drawable.folder_up)
        this.folderUpIconImage.setImageDrawable(folderUpIcon)

        this.mFolderViewContainer.setOnClickListener{
            model.MODE = Constants.FOLDER_VIEW
            setAdapter()
            folderUpText.text = "..."
            model.onBackFolderPressed()
            mFolderViewContainer.visibility = View.GONE
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ButterKnife.bind(this)

        init()

        if (!checkPermission()) {
            requestPermission()
        } else {
            model.loadAllFolderData()
        }

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
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when(requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    model.loadAllFolderData()
                }
            }
        }
    }

    private fun observeForObservers() {
        model.foldersListExposed.observe(this, Observer {
            if (it != null)
                setAdapter()
        })
    }

    private fun setAdapter(){

        if (adapter == null) {

            adapter = StorageFileAdapter(this, R.layout.row_folder_item, this)
            val linearLayout = LinearLayoutManager(this)
            linearLayout.orientation = LinearLayoutManager.VERTICAL
            recyclerView.layoutManager = linearLayout
            recyclerView.adapter = adapter

        }

        if (model.MODE == Constants.FOLDER_VIEW) {
            setFoldersList()
        } else if (model.MODE == Constants.SONG_VIEW) {
            folderUpText.text = model.foldersListExposed.value!![model.CURRENT_SONG_FOLDER_INDEX].path
            mFolderViewContainer.visibility = View.VISIBLE
            setSongsList()
        }
    }

    private fun setFoldersList() {
        adapter?.submitList(model.foldersListExposed.value)
    }

    private fun setSongsList() {
        adapter?.submitList(model.foldersListExposed.value?.get(model.CURRENT_SONG_FOLDER_INDEX)?.songsList)
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
                folderUpText.text = model.foldersListExposed.value!![position].path
                mFolderViewContainer.visibility = View.VISIBLE
                model.onItemClicked(position)
                setAdapter()
            } else {

                //Play the video
                var currentVideo = model.foldersListExposed.value!![model.CURRENT_SONG_FOLDER_INDEX].songsList[position]

                val playerIntent = Intent(this, Player::class.java)
                playerIntent.putExtra(Constants.FILE_PATH, currentVideo.path)
                val pathsList = ArrayList<String?>()
                for ((tempPos, folder) in model.foldersListExposed.value!![model.CURRENT_SONG_FOLDER_INDEX].songsList.withIndex()) {
                    if (tempPos >= position) {
                        pathsList.add(folder.path)
                    }
                }
                playerIntent.putExtra(Constants.QUEUE, pathsList)
                startActivity(playerIntent)
            }
        } else {
            toggleSelection(position)
        }

    }


    private fun doActionOnOverflowItemClick(position: Int, viewId: Int) {
        val data = model.foldersListExposed.value!![model.CURRENT_SONG_FOLDER_INDEX].songsList[position]

        when (viewId) {
            R.id.itemPlay -> {
                val playerIntent = Intent(this, Player::class.java)
                playerIntent.putExtra(Constants.FILE_PATH, data.path)
                val pathsList = ArrayList<String?>()
                for ((tempPos, folder) in model.foldersListExposed.value!![model.CURRENT_SONG_FOLDER_INDEX].songsList.withIndex()) {
                    if (tempPos >= position) {
                        pathsList.add(folder.path)
                    }
                }
                playerIntent.putExtra(Constants.QUEUE, pathsList)
                startActivity(playerIntent)
            }
            R.id.itemShare -> {}
            R.id.itemDelete -> {
                model.delete(listOf(position), false, this)
            }
        }
    }

    private fun checkPermission(): Boolean {
        val result = ContextCompat.checkSelfPermission(applicationContext, WRITE_EXTERNAL_STORAGE)
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(WRITE_EXTERNAL_STORAGE),
            PERMISSION_REQUEST_CODE
        )
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
    }

    override fun onDeleteError() {
        Toast.makeText(mContext, "", Toast.LENGTH_SHORT).show()
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
                R.id.itemSelectAll -> {
                    adapter!!.selectAllItems()
                }
                R.id.itemPlay -> {}
                R.id.itemShare -> {}
                R.id.itemDelete -> {
                    if (model.MODE == Constants.SONG_VIEW)
                        model.delete(adapter!!.getSelectedItems(), false, mContext)
                    else
                        model.delete(adapter!!.getSelectedItems(), true, mContext)
                }
            }
            // We have to end the multi select, if the user clicks on an option other than select all
            if (item.itemId != R.id.itemSelectAll)
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
