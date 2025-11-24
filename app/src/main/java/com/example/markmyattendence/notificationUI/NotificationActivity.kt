package com.example.markmyattendence.notificationUI

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.markmyattendence.R
import com.example.markmyattendence.data.Notification
import com.example.markmyattendence.databinding.ActivityNotificationBinding
import com.google.android.material.tabs.TabLayout

class NotificationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNotificationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        replaceFragment(RequestFragment())

        binding.tlLayoutNotifi.addTab(binding.tlLayoutNotifi.newTab().setText("Request"))
        binding.tlLayoutNotifi.addTab(binding.tlLayoutNotifi.newTab().setText("All"))

        binding.tlLayoutNotifi.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                // Determine which fragment to load based on the selected tab position
                when (tab.position) {
                    0 -> replaceFragment(RequestFragment())
                    1 -> replaceFragment(OtherFragment())
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {

            }

            override fun onTabReselected(tab: TabLayout.Tab) {

            }
        })

        binding.ivBack.setOnClickListener {
            finish()
        }
    }
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            // Replace the current fragment with the new one in the container
            .replace(binding.fragmentContainernotifi.id, fragment)
            // Use a commit that allows state loss, often safe for simple replacements
            .commit()
    }
    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

}