package com.example.markmyattendence.teacher

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.markmyattendence.R
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.datepicker.MaterialDatePicker // Required for date pickers
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TeacherStatusFragment : Fragment(R.layout.fragment_teacher_status) {

    // Lateinit for views we'll interact with in multiple methods
    private lateinit var tabLayoutDateRange: TabLayout
    private lateinit var layoutCustomDateRange: View
    private lateinit var editTextToDate: TextInputEditText
    private lateinit var autoCompleteClassSelector: AutoCompleteTextView

    private val TAG = "TeacherStatusFragment"

    // Firebase instances
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fetchClassesFromFirestore()

        val tabLayoutAttendanceFilter = view.findViewById<TabLayout>(R.id.tabLayoutAttendanceFilter)
        tabLayoutDateRange = view.findViewById(R.id.tabLayoutDateRange)
        layoutCustomDateRange = view.findViewById(R.id.layoutCustomDateRange)

        editTextToDate = view.findViewById(R.id.editTextToDate)
        autoCompleteClassSelector = view.findViewById(R.id.autoCompleteClassSelector)

        // -----------------------
        // Attendance Filter Tabs
        // -----------------------
        tabLayoutAttendanceFilter.apply {
            removeAllTabs()
            // Note: In a real app, 'X', 'Y', 'Z' would be replaced by actual counts from data
            addTab(newTab().setText("All (X)"))
            addTab(newTab().setText("Present (Y)"))
            addTab(newTab().setText("Absent (Z)"))
        }

        // -----------------------
        // Date Range Tabs
        // -----------------------
        tabLayoutDateRange.apply {
            removeAllTabs()
            addTab(newTab().setText("Last 7 Days"))
            addTab(newTab().setText("This Month"))

        }

        // Date Selection Listener
        tabLayoutDateRange.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                // Show/Hide the custom date range layout based on selection
                layoutCustomDateRange.visibility = if (tab.position == 2) View.VISIBLE else View.GONE

                when (tab.position) {
                    0 -> {
                        // Last 7 Days logic
                    }
                    1 -> {
                        // This Month logic
                    }
                    2 -> {
                        // Custom Date Range logic - Date pickers are set up to handle clicks
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun fetchClassesFromFirestore() {
        val teacherUid = auth.currentUser?.uid

        if (teacherUid == null) {

            Toast.makeText(context, "Authentication error. Please log in again.", Toast.LENGTH_LONG).show()
            return
        }

        db.collection("classes")
            .whereEqualTo("teacherUid", teacherUid)
            .get()
            .addOnSuccessListener { result ->
                val classNames = mutableListOf<String>()

                for (document in result) {
                    // Assuming the class name field is called "className"
                    val className = document.getString("className")
                    if (className != null) {
                        classNames.add(className)
                    }
                }

                updateClassSelectorAdapter(classNames)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(context, "Failed to load classes. Please try again.", Toast.LENGTH_SHORT).show()
            }
    }
    private fun updateClassSelectorAdapter(classNames: List<String>) {
        if (context != null) {
            // Use a simple dropdown item layout, e.g., android.R.layout.simple_dropdown_item_1line
            val adapter = ArrayAdapter(
                requireContext(),
                R.layout.dropdown_menu_item,
                classNames
            )
            autoCompleteClassSelector.setAdapter(adapter)


        }
    }



    companion object {
        /**
         * Use this factory method to create a new instance of this fragment.
         */
        @JvmStatic
        fun newInstance() = TeacherStatusFragment()
    }
}