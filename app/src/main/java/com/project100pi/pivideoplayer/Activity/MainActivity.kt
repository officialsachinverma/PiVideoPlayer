package com.project100pi.pivideoplayer.Activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.project100pi.pivideoplayer.AdapterAndListeners.Listeners.OnTrackSelected
import com.project100pi.pivideoplayer.Model.Track
import com.project100pi.pivideoplayer.R
import com.project100pi.pivideoplayer.Utils.Constants
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.graphics.PorterDuff
import android.opengl.Visibility
import android.os.Environment
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.graphics.ColorUtils
import butterknife.BindView
import butterknife.ButterKnife
import com.project100pi.pivideoplayer.AdapterAndListeners.StorageFileAdapter
import com.project100pi.pivideoplayer.Model.FolderInfo
import com.project100pi.pivideoplayer.Utils.Constants.PERMISSION_REQUEST_CODE
import com.project100pi.pivideoplayer.factory.MainViewModelFactory
import java.io.File
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity(), OnTrackSelected {

    @BindView(R.id.rv_video_file_list) lateinit var  recyclerView: RecyclerView
    @BindView(R.id.folder_view_container) lateinit var mFolderViewContainer: View
    @BindView(R.id.folder_up_text) lateinit var folderUpText: TextView
    @BindView(R.id.folder_up_image) lateinit var folderUpIconImage: ImageView

    private lateinit var model: MainViewModel
    private var adapter: StorageFileAdapter? = null

    private fun init() {
        val application = requireNotNull(this).application
        val viewModelFactory = MainViewModelFactory(this , application)
        model = ViewModelProviders.of(this, viewModelFactory).get(MainViewModel::class.java)

        val folderUpIcon = ContextCompat.getDrawable(this, R.drawable.folder_up)
        this.folderUpIconImage.setImageDrawable(folderUpIcon)

        this.mFolderViewContainer.setOnClickListener{
            model.MODE = Constants.FOLDER_VIEW
            setAdapter(model.foldersListExposed.value!!)
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
            observeForObservers()
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
                    observeForObservers()
                }
            }
        }
    }

    private fun observeForObservers() {
        model.foldersListExposed.observe(this, Observer {
            setAdapter(it)
        })
    }

    private fun setAdapter(tracks:ArrayList<FolderInfo>){

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
        adapter?.submitList(model.foldersListExposed.value?.get(model.CURRENT_SONG_FOLDER_INDEX)?.songsList as List<FolderInfo>)
    }

    override fun onClick(track: Track) {
        val playerIntent = Intent(this, Player::class.java)
        playerIntent.putExtra(Constants.FILE_PATH, track.filePath)
        startActivity(playerIntent)
    }

    override fun onDirectorySelected(position: Int) {
        if (model.MODE != Constants.SONG_VIEW) {
            model.MODE = Constants.SONG_VIEW
            folderUpText.text = model.foldersListExposed.value!![position].path
            mFolderViewContainer.visibility = View.VISIBLE
            model.onItemClicked(position)
            setAdapter(model.foldersListExposed.value!![position].songsList)
        } else {

            //Play the video
            var currentVideo = model.foldersListExposed.value!![model.CURRENT_SONG_FOLDER_INDEX].songsList[position]

            val playerIntent = Intent(this, Player::class.java)
            playerIntent.putExtra(Constants.FILE_PATH, currentVideo.path)
            startActivity(playerIntent)
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
}
