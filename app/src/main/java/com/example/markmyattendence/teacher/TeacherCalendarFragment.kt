package com.example.markmyattendence.teacher

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.CalendarView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.markmyattendence.R
import com.example.markmyattendence.data.ClassItem // You need to create this model class
import com.example.markmyattendence.teacher.Adatper.ClassScheduleAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class TeacherCalendarFragment : Fragment(R.layout.fragment_teacher_calendar) {

    private lateinit var calendarView: CalendarView
    private lateinit var recyclerView: RecyclerView
    private lateinit var textViewDailyScheduleHeader: TextView

    // Firebase instances
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var currentUserUid: String? = null

    // Date formatters
    private val firestoreDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
    private val displayDateFormat = SimpleDateFormat("EEEE, dd MMM", Locale.US)


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Initialize Views
        calendarView = view.findViewById(R.id.calendarViewSchedule)
        recyclerView = view.findViewById(R.id.recyclerViewDailySchedule)
        textViewDailyScheduleHeader = view.findViewById(R.id.textViewDailyScheduleHeader)

        // 2. Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        currentUserUid = auth.currentUser?.uid

        if (currentUserUid == null) {
            Log.e("CalendarFragment", "User not logged in!")
            // You should navigate to login screen or show error
            return
        }

        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = ClassScheduleAdapter(emptyList()) // Initialize with an empty list

        // 3. Setup Calendar Listener
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance()
            calendar.set(year, month, dayOfMonth)

            // Format the date to match your Firestore string format (e.g., "Nov 21, 2025")
            val selectedDateFirestoreFormat = firestoreDateFormat.format(calendar.time)

            // Format the date for the header display (e.g., "Schedule for Friday, 21 Nov")
            val selectedDateDisplayFormat = displayDateFormat.format(calendar.time)
            textViewDailyScheduleHeader.text = "Schedule for $selectedDateDisplayFormat"

            fetchClassesForDate(currentUserUid!!, selectedDateFirestoreFormat)
        }

        // 4. Load today's schedule on startup
        val todayFirestoreFormat = firestoreDateFormat.format(Date())
        val todayDisplayFormat = displayDateFormat.format(Date())
        textViewDailyScheduleHeader.text = "Schedule for $todayDisplayFormat"
        fetchClassesForDate(currentUserUid!!, todayFirestoreFormat)
    }

    /**
     * Fetches classes from Firestore for the selected date and the current user (as the teacher).
     * @param userUid The UID of the current teacher.
     * @param selectedDateString The date in "MMM dd, yyyy" format.
     */
    private fun fetchClassesForDate(userUid: String, selectedDateString: String) {
        // NOTE ON EFFICIENCY: Querying by string date is inefficient.
        // For production, change Firestore 'startDate' to a Timestamp object for proper queries.

        db.collection("classes")
            // Query for classes where the teacher is the current user (based on your document structure)
            .whereEqualTo("teacherUid", userUid)
            // Query for classes that have the matching start date string
            .whereEqualTo("startDate", selectedDateString)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val dailyClasses = querySnapshot.toObjects(ClassItem::class.java)

                // Update the RecyclerView
                (recyclerView.adapter as ClassScheduleAdapter).updateList(dailyClasses)

            }
            .addOnFailureListener { e ->
                Log.e("CalendarFragment", "Error fetching classes: ${e.message}", e)
                (recyclerView.adapter as ClassScheduleAdapter).updateList(emptyList()) // Clear list on error
            }
    }

// ... (The existing code for your TeacherCalendarFragment)

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment (if you needed to pass arguments).
         * Since this fragment needs no arguments, it's a simple factory.
         */
        @JvmStatic // Makes it callable statically from Java code
        fun newInstance() = TeacherCalendarFragment()
    }

}