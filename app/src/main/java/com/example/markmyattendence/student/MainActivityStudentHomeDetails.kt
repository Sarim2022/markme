package com.example.markmyattendence.student

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.markmyattendence.R // Ensure your R file has the IDs from activity_student_attendance_detail.xml
import com.example.markmyattendence.databinding.ActivityMainStudentHomeDetailsBinding // Assuming this binding file uses the XML you provided
import com.example.markmyattendence.student.Adapter.AttendanceSessionAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Data model for a single attendance session to display in the RecyclerView
data class AttendanceSession(
    val sessionTime: String,
    val isPresent: Boolean,
    val documentId: String
)

// NOTE: I am renaming the activity below to StudentAttendanceDetailActivity
// to align with the functionality, but you can keep your original name
// (MainActivityStudentHomeDetails) if you prefer. Just ensure your AndroidManifest is correct.

class MainActivityStudentHomeDetails : AppCompatActivity() {

    private lateinit var binding: ActivityMainStudentHomeDetailsBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private val sessionsList = mutableListOf<AttendanceSession>()
    private lateinit var sessionAdapter: AttendanceSessionAdapter

    // Variables to hold class and student info
    private var classId: String = ""
    private var studentUid: String? = null

    // Date formatter for display (e.g., Dec 2, 2025)
    private val displayFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    // Date formatter for Firestore document ID (e.g., 20251202)
    private val firestoreKeyFormat = SimpleDateFormat("yyyyMMdd", Locale.US)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Binding
        binding = ActivityMainStudentHomeDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = Firebase.firestore
        auth = FirebaseAuth.getInstance()

        // Get the current authenticated user's ID
        // IMPORTANT: Ensure the user is authenticated before this activity starts.
        studentUid = auth.currentUser?.uid
        if (studentUid == null) {
            Log.e("AttendanceApp", "Student UID is null. User not signed in.")
            // You might want to redirect to a login screen or show an error
        }

        // 2. Receive the Intent data
        classId = intent.getStringExtra("CLASS_ID") ?: ""
        val className = intent.getStringExtra("CLASS_NAME")
        val classCode = intent.getStringExtra("CLASS_CODE")

        // 3. Setup UI with Class Details
        supportActionBar?.title = className ?: "Attendance History"
        binding.tvDetailClassName.text = className ?: "Class Name N/A"
        binding.tvDetailClassCode.text = "Code: ${classCode ?: "N/A"}"

        // 4. Setup RecyclerView (ID: rv_attendance_list from the XML)
        sessionAdapter = AttendanceSessionAdapter(sessionsList)
        binding.rvAttendanceList.layoutManager = LinearLayoutManager(this)
        binding.rvAttendanceList.adapter = sessionAdapter

        // 5. Setup Date Picker Trigger (ID: etCustomSearch from the XML)
        binding.etCustomSearch.setOnClickListener {
            showDatePicker()
        }

        // Initial load: Check attendance for today
        val today = Date()
        binding.etCustomSearch.setText(displayFormat.format(today))
        if (classId.isNotEmpty() && studentUid != null) {
            checkAttendanceStatus(classId, studentUid!!, today)
        } else {
            sessionsList.add(AttendanceSession("Error: Class or User ID missing.", false, ""))
            sessionAdapter.notifyDataSetChanged()
        }

    }

    /**
     * Shows a DatePickerDialog and triggers the attendance check on selection.
     */
    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val picker = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            calendar.set(selectedYear, selectedMonth, selectedDay)
            val selectedDate = calendar.time

            // 1. Update the EditText text
            binding.etCustomSearch.setText(displayFormat.format(selectedDate))

            // 2. Trigger the Firestore check
            if (classId.isNotEmpty() && studentUid != null) {
                checkAttendanceStatus(classId, studentUid!!, selectedDate)
            }
        }, year, month, day)

        picker.show()
    }

    /**
     * Calculates the Firestore key for the start of the next day (exclusive end of query).
     */
    private fun getNextDayKey(selectedDate: Date): String {
        val calendar = Calendar.getInstance().apply {
            time = selectedDate
            add(Calendar.DAY_OF_YEAR, 1) // Move one day forward
        }
        return firestoreKeyFormat.format(calendar.time)
    }

    /**
     * Queries Firestore using a document ID range to find all attendance sessions for the given date.
     */
    private fun checkAttendanceStatus(classId: String, studentUid: String, date: Date) {

        sessionsList.clear()
        sessionAdapter.notifyDataSetChanged()
        binding.progressBar.visibility = View.VISIBLE

        // 1. Define the range keys for the document ID query
        val dateKey = firestoreKeyFormat.format(date)
        val startKey = "${classId}_${dateKey}"
        val endKeyExclusive = "${classId}_${getNextDayKey(date)}" // Next day's key

        Log.d("AttendanceApp", "Querying range: $startKey to $endKeyExclusive")

        db.collection("AttendanceRecords")
            .orderBy(FieldPath.documentId()) // Mandatory for range queries on the document ID
            .startAt(startKey)
            .endBefore(endKeyExclusive)
            .get()
            .addOnSuccessListener { querySnapshot ->
                binding.progressBar.visibility = View.GONE

                if (querySnapshot.isEmpty) {
                    sessionsList.add(AttendanceSession("No sessions recorded for this date.", false, ""))
                } else {
                    for (document in querySnapshot.documents) {
                        // Document ID example: VwTIlbF40b3aniJdIV0s_20251202_1445
                        val docIdParts = document.id.split("_")
                        // Extract time part (1445)
                        val timePart = if (docIdParts.size > 2) docIdParts[2] else ""

                        // Convert time part (HHmm) to readable format (hh:mm a)
                        val sessionTime = try {
                            SimpleDateFormat("HHmm", Locale.US).parse(timePart)?.let { time ->
                                SimpleDateFormat("hh:mm a", Locale.getDefault()).format(time)
                            } ?: "Time N/A"
                        } catch (e: Exception) {
                            "Time N/A"
                        }

                        val attendedStudents = document.get("attendedStudents") as? Map<String, Any>

                        val isPresent = attendedStudents?.containsKey(studentUid) == true

                        sessionsList.add(AttendanceSession(sessionTime, isPresent, document.id))
                    }
                }

                sessionAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                binding.progressBar.visibility = View.GONE
                sessionsList.clear()
                sessionsList.add(AttendanceSession("Error loading data: Check console for logs.", false, ""))
                sessionAdapter.notifyDataSetChanged()
                Log.e("AttendanceApp", "Error fetching attendance records: ", exception)
            }
    }
}
