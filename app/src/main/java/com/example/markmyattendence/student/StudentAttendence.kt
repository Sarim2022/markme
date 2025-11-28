package com.example.markmyattendence.student

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import com.example.markmyattendence.R
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.ArrayAdapter

import android.widget.Toast

import com.google.android.material.datepicker.MaterialDatePicker // Required for date pickers

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StudentAttendence : Fragment(R.layout.fragment_student_attendence) {

    private lateinit var tabLayoutDateRange: TabLayout
    private lateinit var layoutCustomDateRange: View
    private lateinit var editTextFromDate: TextInputEditText
    private lateinit var editTextToDate: TextInputEditText
    private lateinit var autoCompleteClassSelector: AutoCompleteTextView

    private val TAG = "StudentAttendenceFragment"

    // Firebase instances
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tabLayoutAttendanceFilter = view.findViewById<TabLayout>(R.id.tabLayoutAttendanceFilter)
        tabLayoutDateRange = view.findViewById(R.id.tabLayoutDateRange)
        layoutCustomDateRange = view.findViewById(R.id.layoutCustomDateRange)
        editTextFromDate = view.findViewById(R.id.editTextFromDate)
        editTextToDate = view.findViewById(R.id.editTextToDate)
        autoCompleteClassSelector = view.findViewById(R.id.autoCompleteClassSelector)
        tabLayoutAttendanceFilter.apply {
            removeAllTabs()
            // Note: In a real app, 'X', 'Y', 'Z' would be replaced by actual counts from data
            addTab(newTab().setText("All (X)"))
            addTab(newTab().setText("Present (Y)"))
            addTab(newTab().setText("Absent (Z)"))
        }
        tabLayoutDateRange.apply {
            removeAllTabs()
            addTab(newTab().setText("Last 7 Days"))
            addTab(newTab().setText("This Month"))
            addTab(newTab().setText("Custom"))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_student_attendence, container, false)
    }


    companion object {
        fun newInstance(): StudentAttendence {
            return StudentAttendence()
        }
    }
}