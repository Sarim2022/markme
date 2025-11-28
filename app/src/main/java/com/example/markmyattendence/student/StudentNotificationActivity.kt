package com.example.markmyattendence.student

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.markmyattendence.R
import com.example.markmyattendence.databinding.ActivityNotificationBinding
import com.example.markmyattendence.databinding.ActivityStudentNotificationBinding
import com.example.markmyattendence.notificationUI.OtherFragment
import com.example.markmyattendence.notificationUI.RequestFragment
import com.google.android.material.tabs.TabLayout

class StudentNotificationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStudentNotificationBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudentNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupTabs()

        // Load the default fragment (Request) when the activity starts
        replaceFragment(StuRequestFragment())
    }
    private fun setupToolbar() {
        binding.ivBack.setOnClickListener {
            finish()
        }
        // Optional: If you use the standard Toolbar and want to show the back arrow
        // setSupportActionBar(binding.toolbar)
        // supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
    private fun setupTabs() {
        // Clear existing tabs just in case
        binding.tlLayoutNotifi.removeAllTabs()

        // 1. Add Request Tab (Position 0 - Default)
        binding.tlLayoutNotifi.addTab(binding.tlLayoutNotifi.newTab().setText("My Request"))

        // 2. Add All Notifications Tab (Position 1)
        binding.tlLayoutNotifi.addTab(binding.tlLayoutNotifi.newTab().setText("All Notification"))

        // Listen for tab selections
        binding.tlLayoutNotifi.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                // Determine which fragment to load based on the selected tab position
                when (tab.position) {
                    0 -> replaceFragment(StuRequestFragment())
                    1 -> replaceFragment(StuNotificFragment())
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) { /* Not needed for simple tab logic */ }
            override fun onTabReselected(tab: TabLayout.Tab) { /* Not needed for simple tab logic */ }
        })
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainernotifi.id, fragment)
            .commit()
    }

    override fun finish() {
        super.finish()
        // Use your custom transition
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }
}