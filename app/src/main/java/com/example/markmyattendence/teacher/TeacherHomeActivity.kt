package com.example.markmyattendence.teacher

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.example.markmyattendence.R
import android.content.Intent
import android.os.Bundle
import com.google.android.material.chip.ChipGroup
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.switchmaterial.SwitchMaterial
import com.example.markmyattendence.StartUI.LoginActivity
import com.example.markmyattendence.data.ClassModel
import com.example.markmyattendence.databinding.ActivityTeacherHomeBinding
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// Removed getAppId as it's not used.

class TeacherHomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTeacherHomeBinding

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    // Use a lateinit property for teacherUid that is set after Auth check in onCreate/fetchUserInfo
    // For simplicity, we keep it nullable and check it before use.
    private val teacherUid: String? = auth.currentUser?.uid

    private val TAG = "TeacherHomeActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTeacherHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Load the container Fragment immediately
        loadFragment(TeacherHomeNavFragment.newInstance())

        // 2. Initialize Activity-level UI components (center plus icon)
        initUI()

        // 3. Fetch user data for personalized content and role check
        fetchUserInfo()

        // 4. Setup Bottom Navigation
        setupBottomNav()
    }

    // --- SETUP & NAVIGATION ---

    private fun setupBottomNav() {
        binding.navView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    binding.ivCreateClass.visibility = View.VISIBLE
                    loadFragment(TeacherHomeNavFragment.newInstance())
                    true
                }
                R.id.navigation_status -> {
                    binding.ivCreateClass.visibility = View.GONE
                    loadFragment(TeacherStatusFragment.newInstance())
                    true
                }
                R.id.navigation_schedule -> {
                    binding.ivCreateClass.visibility = View.GONE
                    loadFragment(TeacherCalendarFragment.newInstance())
                    true
                }
                R.id.navigation_account -> {
                    binding.ivCreateClass.visibility = View.GONE
                    loadFragment(TeacherProfileFragment.newInstance())
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.homeFragmentNav, fragment)
            .commit()
    }


    private fun initUI(){
        // FIX: This now correctly calls the dialog setup function
        binding.ivCreateClass.setOnClickListener {
            // Check UID before attempting to show the dialog
            if (teacherUid.isNullOrEmpty()) {
                Toast.makeText(this, "Please log in again.", Toast.LENGTH_LONG).show()
                handleLogout("Authentication required.")
                return@setOnClickListener
            }
            showCreateClassDialog()
        }
    }

    // --- DIALOG UI LOGIC ---

    /**
     * Initializes and shows the Create Class dialog, handling all input and validation
     * before calling the Firestore save logic.
     */
    private fun showCreateClassDialog() {
        // We ensure teacherUid is available here, as checked in initUI
        val currentTeacherUid = teacherUid ?: return

        val dialogView = layoutInflater.inflate(R.layout.dialog_create_class_modern, null)

        // 1. Setup the AlertDialog
        val dialog = AlertDialog.Builder(this, R.style.ModernDialog)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // 2. Find Views
        val etClassName = dialogView.findViewById<TextInputEditText>(R.id.etClassName)
        val etClassroom = dialogView.findViewById<TextInputEditText>(R.id.etClassroom)
        val etStartDate = dialogView.findViewById<TextInputEditText>(R.id.etStartDate)
        val etStartTime = dialogView.findViewById<TextInputEditText>(R.id.etStartTime)
        val etEndTime = dialogView.findViewById<TextInputEditText>(R.id.etEndTime)
        val etMaxStudents = dialogView.findViewById<TextInputEditText>(R.id.etMaxStudents)
        val switchAutoApprove = dialogView.findViewById<SwitchMaterial>(R.id.switchAutoApprove)
        val chipGroupDays = dialogView.findViewById<ChipGroup>(R.id.chipGroupDays)
        val btnCreateClass = dialogView.findViewById<TextView>(R.id.btnCreateClass)


        // Close button
        dialogView.findViewById<ImageView>(R.id.tvCross)?.setOnClickListener {
            dialog.dismiss()
        }

        // Set click listeners for Date and Time Pickers
        etStartDate.setOnClickListener { showDatePickerDialog(etStartDate) }
        etStartTime.setOnClickListener { showTimePickerDialog(etStartTime) }
        etEndTime.setOnClickListener { showTimePickerDialog(etEndTime) }

        // Create Class Button Logic
        btnCreateClass.setOnClickListener {
            val uniqueCode = generateClassCode()

            // Get data from inputs
            val className = etClassName.text.toString().trim()
            val classroom = etClassroom.text.toString().trim()
            val startDate = etStartDate.text.toString()
            val startTime = etStartTime.text.toString()
            val endTime = etEndTime.text.toString()
            val maxStudentsText = etMaxStudents.text.toString().trim()
            val autoApprove = switchAutoApprove.isChecked

            val selectedChipIds = chipGroupDays.checkedChipIds
            val repeatDays = selectedChipIds.mapNotNull { chipId ->
                dialogView.findViewById<com.google.android.material.chip.Chip>(chipId)?.text?.toString()
            }

            // --- Validation --- (Your existing validation is fine)
            if (className.isEmpty()) { etClassName.error = "Required"; return@setOnClickListener }
            if (classroom.isEmpty()) { etClassroom.error = "Required"; return@setOnClickListener }
            if (startDate.isEmpty()) { etStartDate.error = "Required"; return@setOnClickListener }
            if (startTime.isEmpty()) { etStartTime.error = "Required"; return@setOnClickListener }
            if (endTime.isEmpty()) { etEndTime.error = "Required"; return@setOnClickListener }

            val maxStudents = maxStudentsText.toIntOrNull()

            // Build the Class Model
            val newClass = ClassModel(
                className = className,
                classroom = classroom,
                startDate = startDate,
                startTime = startTime,
                endTime = endTime,
                maxStudents = maxStudents,
                autoApprove = autoApprove,
                repeatDays = repeatDays,
                classCodeUid = uniqueCode,
                studentJoined = emptyList(),
                requestStudent = emptyList(),

            )

            // FIX: Correctly call the asynchronous save function with all parameters.
            saveNewClassToFirestore(
                newClass = newClass,
                context = this,
                dialog = dialog,
                teacherUid = currentTeacherUid // Pass the confirmed UID
            )
            // Note: The dialog will be dismissed in the addOnSuccessListener of saveNewClassToFirestore
        }

        // Transparent rounded background
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    // --- FIRESTORE SAVE LOGIC (This section is robust and remains) ---
    private fun saveNewClassToFirestore(
        newClass: ClassModel,
        context: Context,
        dialog: AlertDialog,
        teacherUid: String // Must pass the authenticated teacher's UID (Non-nullable here)
    ) {
        val db = FirebaseFirestore.getInstance()

        // 1. Get references and generate the class ID
        val newClassRef = db.collection("classes").document()
        val teacherRef = db.collection("users").document(teacherUid)

        // 2. Finalize ClassModel data with generated ID and UID
        val finalClassData = newClass.copy(
            classId = newClassRef.id,
            teacherUid = teacherUid,
        )

        // 3. Start the Batch Write 🚀 (Ensures atomicity)
        db.runBatch { batch ->
            // Operation 1: Create the Class Document
            batch.set(newClassRef, finalClassData, SetOptions.merge())

            // Operation 2: Update the Teacher's myClasses array
            batch.update(
                teacherRef,
                "myClasses",
                FieldValue.arrayUnion(finalClassData.classId)
            )
        }
            .addOnSuccessListener {
                Toast.makeText(context, "${finalClassData.className} created and linked successfully!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()

                val fragment = supportFragmentManager.findFragmentById(R.id.homeFragmentNav)
                if (fragment is TeacherHomeNavFragment) {
                    fragment.loadTeacherClasses()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error creating class: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }


    // --- UTILITY FUNCTIONS ---
    // (generateClassCode, showDatePickerDialog, showTimePickerDialog, fetchUserInfo, handleUserRole, updateFragmentGreeting, fetchTeacherDashboardData, handleLogout, refreshUserData)
    // (These functions remain as they were, they are correctly defined.)

    private fun  generateClassCode():String{
        val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        val codeLength = 7
        val random = kotlin.random.Random.Default
        return (1..codeLength)
            .map { random.nextInt(charPool.size) }
            .map(charPool::get)
            .joinToString("")
    }

    private fun showDatePickerDialog(dateEditText: TextInputEditText) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(selectedYear, selectedMonth, selectedDay)
                dateEditText.setText(dateFormat.format(selectedDate.time))
            },
            year, month, day
        )
        datePickerDialog.show()
    }
    // ... (showTimePickerDialog remains the same)
    private fun showTimePickerDialog(timeEditText: TextInputEditText) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(
            this,
            { _, selectedHour, selectedMinute ->
                val selectedTime = Calendar.getInstance()
                selectedTime.set(Calendar.HOUR_OF_DAY, selectedHour)
                selectedTime.set(Calendar.MINUTE, selectedMinute)
                timeEditText.setText(timeFormat.format(selectedTime.time))
            },
            hour,
            minute,
            false
        )
        timePickerDialog.show()
    }


    fun fetchUserInfo(isInitialLoad: Boolean = true){
        val currentUser = auth.currentUser

        if (currentUser != null) {
            val uid = currentUser.uid

            db.collection("users").document(uid).get()
                .addOnSuccessListener { documentSnapshot: DocumentSnapshot ->
                    if (documentSnapshot.exists()) {
                        val role = documentSnapshot.getString("role")
                        val name = documentSnapshot.getString("name")
                        val collageName = documentSnapshot.getString("collegeName")
                        val department = documentSnapshot.getString("department")
                        val subjectName = documentSnapshot.getString("subject_Name")
                        val subjectCode  = documentSnapshot.getString("subject_code")
                        if (isInitialLoad) {
                            handleUserRole(role,name,collageName,department,subjectName,subjectCode)
                        } else {
                            updateFragmentGreeting(name,collageName,department,subjectName,subjectCode)
                        }

                    } else {
                        Log.e(TAG, "No user data found for UID: $uid. Logging out.")
                        handleLogout("Error: User data missing.")
                    }
                }
                .addOnFailureListener { e: Exception ->
                    Log.e(TAG, "Error fetching user data", e)
                    handleLogout("Failed to load profile data.")
                }
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun handleUserRole(role: String?, name: String?,collageName:String?,department: String?,subjectName: String?,subjectCode: String?) {
        if ("teacher" == role) {
            updateFragmentGreeting(name,collageName,department,subjectName,subjectCode)
            fetchTeacherDashboardData()
        } else {
            Log.e(TAG, "Security Alert: Role $role attempted to access TeacherHomeActivity.")
            handleLogout("Access Denied: Invalid role.")
        }
    }

    private fun updateFragmentGreeting(name: String?,collageName:String?,department: String?,subjectName: String?,subjectCode: String?) {
        if (name.isNullOrEmpty()) return
        val fragment = supportFragmentManager.findFragmentById(R.id.homeFragmentNav)
        if (fragment is TeacherHomeNavFragment) {
            fragment.updateGreeting(name,collageName,department,subjectName,subjectCode)
        }
    }

    private fun fetchTeacherDashboardData() {
        Log.d(TAG, "Loading teacher dashboard specific content...")
    }

    fun refreshUserData() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.homeFragmentNav)
        if (currentFragment is TeacherHomeNavFragment) {
            fetchUserInfo(isInitialLoad = false)
        }
    }

    private fun handleLogout(message: String) {
        auth.signOut()
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}