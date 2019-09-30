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
import com.project100pi.pivideoplayer.AdapterAndListeners.PlayerAdapter
import com.project100pi.pivideoplayer.Model.Track
import com.project100pi.pivideoplayer.R
import com.project100pi.pivideoplayer.Utils.Constants
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import androidx.core.app.ActivityCompat
import com.project100pi.pivideoplayer.Utils.Constants.PERMISSION_REQUEST_CODE


class MainActivity : AppCompatActivity(), OnTrackSelected {

    private lateinit var recyclerView: RecyclerView
    private lateinit var model: MainViewModel
    private lateinit var progressBar: ProgressBar
    private var adapter: PlayerAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        model = ViewModelProviders.of(this).get(MainViewModel::class.java)

        recyclerView = findViewById(R.id.rv_video_file_list)
        progressBar = findViewById(R.id.pb_video)

        if (!checkPermission()) {
            requestPermission()
        } else {
            refreshList()
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
                    refreshList()
                    observeForObservers()
                }
            }
        }
    }

    private fun observeForObservers() {
        model.videoListExposed.observe(this, Observer {
            setAdapter(it)
        })
    }

    private fun setAdapter(tracks:MutableList<Track>){
        if (adapter == null) {
            adapter = PlayerAdapter(this, R.layout.row_item, this, tracks)
            val linearLayout = LinearLayoutManager(this)
            linearLayout.orientation = LinearLayoutManager.VERTICAL
            recyclerView.layoutManager = linearLayout
            recyclerView.adapter = adapter
        }
        adapter?.submitList(tracks)

        progressBar.visibility = View.GONE
    }

    override fun onClick(track: Track) {
        super.onClick(track)

        val playerIntent = Intent(this, Player::class.java)
        playerIntent.putExtra(Constants.FILE_PATH, track.filePath)
        startActivity(playerIntent)
    }

    private fun refreshList() {
        progressBar.visibility = View.VISIBLE
        model.loadVideoFiles(this)
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
