package com.example.markmyattendence.teacher

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.markmyattendence.R
import com.example.markmyattendence.data.FileGenerator
import com.example.markmyattendence.data.StudentAttendance
import com.example.markmyattendence.teacher.Adapter.StudentAttendanceAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class TeacherStatusFragment : Fragment(R.layout.fragment_teacher_status) {

    // Lateinit for views
    private lateinit var tabLayoutDateRange: TabLayout
    private lateinit var tabLayoutAttendanceFilter: TabLayout
    private lateinit var layoutCustomDateRange: View
    private lateinit var editTextFromDate: TextInputEditText
    private lateinit var editTextToDate: TextInputEditText
    private lateinit var autoCompleteClassSelector: AutoCompleteTextView
    private lateinit var buttonDownloadAttendance: TextView
    private lateinit var recyclerViewStudentAttendance: RecyclerView
    private lateinit var tvNodataStudent: TextView

    private val TAG = "TeacherStatusFragment"
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // If permission is granted, proceed to generate the file
                generateAttendancePdfReport()
            } else {
                // Permission denied
                Toast.makeText(requireContext(), "Storage permission is required to save the PDF to Downloads.", Toast.LENGTH_LONG).show()
            }
        }

    // Firebase instances
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Selected values
    private var selectedStartDate: Long? = null
    private var selectedEndDate: Long? = null
    private var selectedClassId: String? = null
    private var selectedDownloadFormat: String = "PDF"
    private var selectedAttendanceFilter: String = "All"

    // Adapter and Data
    private lateinit var attendanceAdapter: StudentAttendanceAdapter // 🛑 CORRECTED TYPE

    // Maps to store student data efficiently
    private var classNameToClassIdMap = mapOf<String, String>()
    private var currentClassStudents = mapOf<String, String>() // Map: UID -> Name of enrolled students
    private var allUsersMap = mutableMapOf<String, String>() // Map: UID -> Name for all students

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Views
        tabLayoutAttendanceFilter = view.findViewById(R.id.tabLayoutAttendanceFilter)
        tabLayoutDateRange = view.findViewById(R.id.tabLayoutDateRange)
        layoutCustomDateRange = view.findViewById(R.id.layoutCustomDateRange)
        editTextFromDate = view.findViewById(R.id.editTextFromDate)
        editTextToDate = view.findViewById(R.id.editTextToDate)
        autoCompleteClassSelector = view.findViewById(R.id.autoCompleteClassSelector)
        buttonDownloadAttendance = view.findViewById(R.id.buttonDownloadAttendance)
        recyclerViewStudentAttendance = view.findViewById(R.id.recyclerViewStudentAttendance)
        tvNodataStudent = view.findViewById(R.id.tvNodataStudent)

        // Setup RecyclerView
        attendanceAdapter = StudentAttendanceAdapter(emptyList()) // 🛑 Initialized
        recyclerViewStudentAttendance.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = attendanceAdapter // Set the adapter
        }

        // Setup Tabs
        setupAttendanceFilterTabs()
        setupDateRangeTabs()

        // Setup Date Pickers and Listeners
        setupDatePickers()

        // Fetch user data needed for mapping
        fetchAllUsers()
        fetchClassesFromFirestore()

        buttonDownloadAttendance.setOnClickListener {
            handleDownloadAttendance()
        }
    }

    private fun setupAttendanceFilterTabs() {
        tabLayoutAttendanceFilter.apply {
            removeAllTabs()
            addTab(newTab().setText("All"))
            addTab(newTab().setText("Present"))
            addTab(newTab().setText("Absent"))
        }

        tabLayoutAttendanceFilter.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                selectedAttendanceFilter = tab?.text.toString()
                Log.d(TAG, "Attendance Filter Selected: $selectedAttendanceFilter")
                fetchAttendanceData() // Trigger data refresh/re-filter
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupDateRangeTabs() {
        tabLayoutDateRange.apply {
            removeAllTabs()
            addTab(newTab().setText("Today's"))
            addTab(newTab().setText("Select date"))
        }

        tabLayoutDateRange.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (tab?.position == 1) { // "Select date" tab
                    layoutCustomDateRange.visibility = View.VISIBLE
                    // selectedStartDate and selectedEndDate remain null until date pickers are used
                } else { // "Today's" tab (position 0)
                    layoutCustomDateRange.visibility = View.GONE
                    // Set current day as start and end date for 'Today's' logic
                    val today = Date().time
                    selectedStartDate = today
                    selectedEndDate = today
                }
                fetchAttendanceData() // Trigger data refresh
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Set 'Today's' as default selection and setup default dates
        tabLayoutDateRange.getTabAt(0)?.select()
        val today = Date().time
        selectedStartDate = today
        selectedEndDate = today
    }



    private fun setupDatePickers() {
        // From Date Picker
        editTextFromDate.setOnClickListener {
            showDatePicker(editTextFromDate) { dateInMillis ->
                selectedStartDate = dateInMillis
                editTextFromDate.setText(formatDate(dateInMillis))
                fetchAttendanceData() // Trigger data refresh
            }
        }

        // To Date Picker
        editTextToDate.setOnClickListener {
            showDatePicker(editTextToDate) { dateInMillis ->
                selectedEndDate = dateInMillis
                editTextToDate.setText(formatDate(dateInMillis))
                fetchAttendanceData() // Trigger data refresh
            }
        }
    }

    private fun showDatePicker(targetEditText: TextInputEditText, onDateSelected: (Long) -> Unit) {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select date")
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val timeZone = TimeZone.getDefault()
            val offset = timeZone.getOffset(selection)
            val localTime = selection + offset
            onDateSelected(localTime)
        }

        datePicker.show(childFragmentManager, "DATE_PICKER")
    }

    private fun formatDate(dateInMillis: Long): String {
        val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return formatter.format(Date(dateInMillis))
    }

    private fun formatDateForQuery(dateInMillis: Long): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(dateInMillis))
    }

    private fun fetchAllUsers() {
        db.collection("users")
            .whereEqualTo("role", "student")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val name = document.getString("name")
                    if (name != null) {
                        allUsersMap[document.id] = name
                    }
                }
            }
            .addOnFailureListener {
                Log.e(TAG, "Failed to fetch all student names.")
            }
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
                val nameToId = mutableMapOf<String, String>()

                for (document in result) {
                    val className = document.getString("className")
                    val classId = document.id
                    if (className != null && classId != null) {
                        classNames.add(className)
                        nameToId[className] = classId
                    }
                }
                classNameToClassIdMap = nameToId
                updateClassSelectorAdapter(classNames)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(context, "Failed to load classes. Please try again.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateClassSelectorAdapter(classNames: List<String>) {
        if (context != null) {
            val adapter = ArrayAdapter(
                requireContext(),
                R.layout.dropdown_menu_item,
                classNames
            )
            autoCompleteClassSelector.setAdapter(adapter)

            autoCompleteClassSelector.setOnItemClickListener { parent, view, position, id ->
                val selectedClassName = parent.getItemAtPosition(position).toString()
                selectedClassId = classNameToClassIdMap[selectedClassName]
                Log.d(TAG, "Selected Class ID: $selectedClassId")

                if (selectedClassId != null) {
                    fetchStudentsForClass(selectedClassId!!)
                }
            }
        }
    }

    private fun fetchStudentsForClass(classId: String) {
        db.collection("classes").document(classId)
            .get()
            .addOnSuccessListener { document ->
                val studentUids = document.get("studentJoined") as? List<String> ?: emptyList()
                val studentsInClass = mutableMapOf<String, String>()

                for (uid in studentUids) {
                    val name = allUsersMap[uid]
                    studentsInClass[uid] = name ?: "Unknown Student ($uid)"
                }
                currentClassStudents = studentsInClass
                fetchAttendanceData()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to load class students.", Toast.LENGTH_SHORT).show()
                attendanceAdapter.updateList(emptyList()) // 🛑 Use adapter method
                updateUIVisibility(true, "Failed to load class members.")
            }
    }

    private fun fetchAttendanceData() {
        if (selectedClassId == null || selectedStartDate == null) {
            attendanceAdapter.updateList(emptyList()) // 🛑 Use adapter method
            updateUIVisibility(true, "Please select a Class and Date.")
            return
        }

        val dateToFetch = formatDateForQuery(selectedStartDate!!)

        db.collection("AttendanceRecords")
            .whereEqualTo("classId", selectedClassId)
            .whereEqualTo("date", dateToFetch)
            .limit(1)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val attendedStudentsMap = if (querySnapshot.isEmpty) {
                    null
                } else {
                    querySnapshot.documents.first().get("attendedStudents") as? Map<String, Any>
                }
                processAttendanceData(attendedStudentsMap)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error fetching attendance: ", exception)
                Toast.makeText(context, "Error loading attendance data.", Toast.LENGTH_SHORT).show()
                processAttendanceData(null)
            }
    }

    private fun processAttendanceData(attendedStudentsMap: Map<String, Any>?) {
        // 🛑 CORRECTED: Use StudentAttendance type
        val finalAttendanceList = mutableListOf<StudentAttendance>()
        val presentUids = attendedStudentsMap?.keys ?: emptySet()

        // 1. Iterate over ALL enrolled students
        for ((uid, name) in currentClassStudents) {
            val isPresent = presentUids.contains(uid)

            // 2. Apply Attendance Filter
            val matchesFilter = when (selectedAttendanceFilter) {
                "All" -> true
                "Present" -> isPresent
                "Absent" -> !isPresent
                else -> true
            }

            if (matchesFilter) {
                finalAttendanceList.add(
                    StudentAttendance(
                        studentUid = uid,
                        studentName = name,

                        isPresent = isPresent
                    )
                )
            }
        }

        // 3. Update the RecyclerView
        attendanceAdapter.updateList(finalAttendanceList)

        // 4. Update visibility
        val noDataText = if (currentClassStudents.isEmpty()) {
            "No students enrolled in this class."
        } else {
            "No attendance recorded for this date or no students match the filter."
        }
        updateUIVisibility(finalAttendanceList.isEmpty(), noDataText)

        Log.d(TAG, "Processed Attendance Data: $finalAttendanceList")
        if (finalAttendanceList.isNotEmpty()) {
            Toast.makeText(context, "Attendance loaded for ${formatDate(selectedStartDate!!)}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUIVisibility(isDataEmpty: Boolean, message: String) {
        if (isDataEmpty) {
            recyclerViewStudentAttendance.visibility = View.GONE
            tvNodataStudent.visibility = View.VISIBLE
            tvNodataStudent.text = message
        } else {
            recyclerViewStudentAttendance.visibility = View.VISIBLE
            tvNodataStudent.visibility = View.GONE
        }
    }

    private fun handleDownloadAttendance() {
        if (selectedClassId == null || selectedStartDate == null) {
            Toast.makeText(context, "Please select a class and date.", Toast.LENGTH_SHORT).show()
            return
        }
        val safeContext = requireContext()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(
                safeContext,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            generateAttendancePdfReport()
        }
    }

    private fun generateAttendancePdfReport() {
        val className = classNameToClassIdMap.entries
            .find { it.value == selectedClassId }?.key ?: "Class_Report"

        val dateString = formatDate(selectedStartDate!!)
        val dataForDownload = attendanceAdapter.getStudentList()

        if (dataForDownload.isEmpty()) {
            Toast.makeText(context, "No data available to download.", Toast.LENGTH_SHORT).show()
            return
        }

        val safeContext = requireContext()

        FileGenerator.generatePdf(
            safeContext,
            className,
            dateString,
            dataForDownload
        )
    }


    companion object {
        @JvmStatic
        fun newInstance() = TeacherStatusFragment()
    }
}