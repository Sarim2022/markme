package com.example.markmyattendence.ChatUI

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.markmyattendence.R
import com.example.markmyattendence.databinding.ActivityChatUiBinding

class ChatUiActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatUiBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityChatUiBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.ivBack.setOnClickListener {
            finish()
        }
    }
    override fun finish() {
        super.finish()
        // The reverse transition of how you opened it:
        // Current (Notification) Activity exits to the right (slide_out_right)
        // Previous Activity enters from the left (slide_in_left)
        // NOTE: The arguments are the NEW Activity's enter and exit animations.
        // When finishing, the first arg is the previous activity's ENTER animation,
        // and the second arg is the current activity's EXIT animation.
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}