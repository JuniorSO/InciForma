package com.example.inciforma

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        findViewById<Button>(R.id.btnMapa).setOnClickListener {
            backInMap()
        }
    }

    override fun onBackPressed() {
        backInMap()
    }

    private fun backInMap() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}