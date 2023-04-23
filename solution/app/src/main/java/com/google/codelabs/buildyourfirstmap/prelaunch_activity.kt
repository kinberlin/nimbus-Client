package com.google.codelabs.buildyourfirstmap

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.MediaController
import android.widget.TextView
import android.widget.VideoView
import kotlinx.android.synthetic.main.activity_prelaunch.*
import kotlinx.coroutines.*
import java.lang.Runnable
import java.text.SimpleDateFormat
import java.util.*

class prelaunch_activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prelaunch)

        var textView = findViewById<TextView>(R.id.txt_hour)

        val handler = Handler()
        val runnable = object : Runnable {
            override fun run() {
                val sdf = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
                textView.text = sdf.format(Date())
                handler.postDelayed(this, 1000) // 1000 milliseconds = 1 second
            }
        }
        handler.postDelayed(runnable, 0)

        //Video Play

        var videoView = findViewById<VideoView>(R.id.videoView)

        // Set MediaController to enable play, pause, seek operations
        var mediaController: MediaController = MediaController(this)
        mediaController.setAnchorView(videoView)
        videoView.setMediaController(mediaController)

        // Point the VideoView to our video source
        val videoFile = "android.resource://" + packageName + "/" + R.raw.compass
        videoView.setVideoURI(Uri.parse(videoFile))

        // Start the video
        videoView.start()

        // Set the Repeat video.
        val job = CoroutineScope(Dispatchers.IO).launch {
            Log.d("TAG", "Launched Coroutine")
            while (isActive) {
                videoView.seekTo(0)
                videoView.start()
                delay(3000)
            }
        }
        button.setOnClickListener(View.OnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        })

    }
}