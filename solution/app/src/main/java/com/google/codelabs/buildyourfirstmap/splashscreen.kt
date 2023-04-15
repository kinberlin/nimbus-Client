package com.google.codelabs.buildyourfirstmap

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler

class splashscreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_splashscreen)
        // Create a Handler and a Runnable to launch the new activity after 5 seconds
        Handler().postDelayed({
            val intent = Intent(this, prelaunch_activity::class.java)
            startActivity(intent)
            finish()
        }, 7000) // 5000 milliseconds = 5 seconds
    }
}