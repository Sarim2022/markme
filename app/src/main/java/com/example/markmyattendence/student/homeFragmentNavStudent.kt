package com.example.markmyattendence.student

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import android.content.Intent
import com.example.markmyattendence.R
import com.example.markmyattendence.data.AppCache
import androidx.core.widget.TextViewCompat
import com.example.markmyattendence.StartUI.SignupActivity
import com.example.markmyattendence.data.ClassModel
import com.example.markmyattendence.data.StudentData
import com.example.markmyattendence.databinding.FragmentHomeNavStudentBinding
import com.example.markmyattendence.notificationUI.NotificationActivity
import com.example.markmyattendence.student.Adapter.StudentClassAdapter
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.journeyapps.barcodescanner.ScanIntentResult // If you are using an older dependency setup

class homeFragmentNavStudent : Fragment() {
    private var _binding: FragmentHomeNavStudentBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var adapter: StudentClassAdapter // Added adapter
    private val TAG = "StudentNavFragment"

    // QR Scanner and Permission Launchers
    private val qrScannerLauncher = registerForActivityResult(ScanContract()) { result: ScanIntentResult ->
        if (result.contents == null) {
            Toast.makeText(requireContext(), "QR Code scan cancelled", Toast.LENGTH_SHORT).show()
        } else {
            val qrCodeToken = result.contents
            // Get the student's current class
            getCurrentClassId { classId ->
                if (classId != null) {
                    markAttendance(qrCodeToken, auth.currentUser?.uid ?: "", classId)
                } else {
                    Toast.makeText(requireContext(), "Please select a class first", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    private lateinit var requestCameraPermissionLauncher: ActivityResultLauncher<String>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Camera Permission Request Launcher
        requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                launchQrScanner()
            } else {
                Toast.makeText(requireContext(), "Camera permission is required to scan QR codes", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeNavStudentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        if (AppCache.studentProfile != null) {
            loadStudentDataToUI(AppCache.studentProfile!!)
        } else {
            auth.currentUser?.uid?.let {
                retrieveAndStoreStudentData(it)
            }
        }

        binding.ivNotification.setOnClickListener {
            val intent = Intent(requireActivity(), StudentNotificationActivity::class.java)
            startActivity(intent)
            requireActivity().overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
        binding.ivChatUi.setOnClickListener {
            val intent = Intent(requireActivity(), StudentChatActivity::class.java)
            startActivity(intent)
            requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
        setupRecyclerView()      // 1. Setup the list view
        fetchJoinedClassUids()   // 2. Start fetching the classes

        // UI Interaction Handlers
        val pressedBackground = ContextCompat.getDrawable(requireContext(), R.drawable.shape_pressed_attendance)
        val defaultBackground = ContextCompat.getDrawable(requireContext(), R.drawable.shape_default_attendance)

        binding.llActivateAttendance.setOnClickListener {
            binding.llActivateAttendance.background = pressedBackground
            binding.llActivateAttendance.postDelayed({
                binding.llActivateAttendance.background = defaultBackground
            }, 300)

            // Check camera permission and launch QR scanner
            checkCameraPermissionAndLaunchScanner()
        }

        binding.tvJoinNut.setOnClickListener {
            showJoinClassDialog()
        }
    }

    // --- RECYCLERVIEW AND DATA FETCHING LOGIC ---

    private fun setupRecyclerView() {
        adapter = StudentClassAdapter(emptyList()) { classModel -> // classModel has the correct data!

            val intent = Intent(context, MainActivityStudentHomeDetails::class.java)

            // --- FIX: Use the classModel object to retrieve the specific data ---

            // This is MANDATORY for the attendance query in the Canvas file
            intent.putExtra("CLASS_ID", classModel.classId) // Assuming your model uses 'classId' as the field name

            // These are used for setting the UI title/details
            intent.putExtra("CLASS_NAME", classModel.className)
            intent.putExtra("CLASS_ROOM", classModel.classroom)
            intent.putExtra("START_TIME", classModel.startTime)
            intent.putExtra("END_TIME", classModel.endTime)
            intent.putExtra("CLASS_CODE", classModel.classCodeUid)

            Toast.makeText(requireContext(), "Opening class: ${classModel.className}", Toast.LENGTH_SHORT).show()
            startActivity(intent)
        }
        binding.recyclerViewNav.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewNav.adapter = adapter
    }

    /**
     * Step 1: Fetches the student's document to get the list of joined class UIDs.
     */
    private fun fetchJoinedClassUids() {
        val studentUid = auth.currentUser?.uid
        if (studentUid.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "User not logged in.", Toast.LENGTH_SHORT).show()
            return
        }


        binding.noClassFound.visibility = View.GONE

        db.collection("users").document(studentUid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    @Suppress("UNCHECKED_CAST")
                    // Fetch the list of UIDs from the 'joinedClasses' array
                    val joinedClassUids = document.get("joinedClasses") as? List<String> ?: emptyList()

                    if (joinedClassUids.isNotEmpty()) {
                        // Step 2: If UIDs exist, fetch the corresponding class details
                        fetchClassDetails(joinedClassUids)
                    } else {

                        binding.noClassFound.visibility = View.GONE
                        adapter.updateList(emptyList())
                    }
                } else {
                    Log.e(TAG, "Student document not found for UID: $studentUid")

                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching student classes UIDs: $e")
                Toast.makeText(requireContext(), "Failed to load class list.", Toast.LENGTH_LONG).show()

            }
    }

    /**
     * Step 2: Queries the 'classes' collection to get full class details using the UIDs.
     */
    private fun fetchClassDetails(classUids: List<String>) {

        // Safety check for Firestore 'whereIn' limit (max 10 items)
        if (classUids.size > 10) {
            Log.w(TAG, "Warning: Too many UIDs for a single Firestore 'whereIn' query. Consider splitting the query.")
            // For now, we will only query the first 10
            // classUids = classUids.take(10)
        }

        db.collection("classes")
            .whereIn("classId", classUids)
            .get()
            .addOnSuccessListener { querySnapshot: QuerySnapshot ->


                // Map the documents to the ClassModel data class
                val classList = querySnapshot.documents.mapNotNull { doc ->
                    doc.toObject(ClassModel::class.java)
                }

                if (classList.isNotEmpty()) {
                    adapter.updateList(classList)
                    _binding?.noClassFound?.visibility = View.GONE
                } else {
                    // This happens if the user document had UIDs, but the class documents were deleted.
                    _binding?.noClassFound?.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching class details: $e")
                Toast.makeText(requireContext(), "Failed to load class details.", Toast.LENGTH_LONG).show()

            }
    }

    // --- DIALOG AND JOIN LOGIC ---

    private fun showJoinClassDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_join_class, null)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // Access views inside dialog
        val editTextClassCode = dialogView.findViewById<TextInputEditText>(R.id.editTextClassCode)
        val buttonJoinClass = dialogView.findViewById<TextView>(R.id.buttonJoinClass)

        // Button click inside dialog
        buttonJoinClass.setOnClickListener {
            val code = editTextClassCode.text.toString().trim()
            val studentUid = auth.currentUser?.uid

            if (code.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter Class Code", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (studentUid.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Authentication Error. Please log in again.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Call the check function
            checkClassInFirebase(code, studentUid)
            dialog.dismiss() // Dismiss dialog immediately after database call starts
        }

        dialogView.findViewById<ImageView>(R.id.cross).setOnClickListener {
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun checkClassInFirebase(classCode: String, studentUid: String) {

        // Ensure we have a valid student UID
        if (studentUid.isEmpty()) {
            Toast.makeText(context, "Authentication error. Student UID missing.", Toast.LENGTH_LONG).show()
            return
        }

        db.collection("classes")
            .whereEqualTo("classCodeUid", classCode)
            .get()
            .addOnSuccessListener { querySnapshot ->

                if (querySnapshot.isEmpty) {
                    Toast.makeText(context, "No class found.", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                // --- Class Found ---
                val classDocument = querySnapshot.documents.first()
                val isAutoApprove = classDocument.getBoolean("autoApprove") ?: false
                val classId = classDocument.id

                val classRef = db.collection("classes").document(classId)
                val studentRef = db.collection("users").document(studentUid)
                // Using safe call and fallback for teacherUid
                val teacherRef = db.collection("users").document(classDocument.getString("teacherUid") ?: "")

                // Start Batch Write to ensure data consistency 🚀
                db.runBatch { batch ->
                    if (isAutoApprove) {
                        // --- SCENARIO 1: AUTO-APPROVE (Joined Class) ---
                        batch.update(studentRef, "joinedClasses", FieldValue.arrayUnion(classId))
                        batch.update(classRef, "studentJoined", FieldValue.arrayUnion(studentUid))

                        // Link student to teacher profile
                        val teacherUid = classDocument.getString("teacherUid")
                        if (!teacherUid.isNullOrEmpty()) {
                            batch.update(teacherRef, "myStudent", FieldValue.arrayUnion(studentUid))
                        }

                    } else {
                        // --- SCENARIO 2: MANUAL APPROVAL (Request Sent) ---
                        batch.update(studentRef, "requestedClasses", FieldValue.arrayUnion(classId))
                        batch.update(classRef, "requestStudent", FieldValue.arrayUnion(studentUid))
                    }
                }
                    .addOnSuccessListener {
                        // Batch succeeded, now show the corresponding toast
                        if (isAutoApprove) {
                            Toast.makeText(context, "Class Joined Successfully!", Toast.LENGTH_SHORT).show()
                            // IMPORTANT: Refresh the joined list after a successful join
                            fetchJoinedClassUids()
                        } else {
                            Toast.makeText(context, "Request sent. Awaiting approval.", Toast.LENGTH_LONG).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Failed to complete join operation. Try again.", Toast.LENGTH_LONG).show()
                    }

            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to connect to server. Try again.", Toast.LENGTH_LONG).show()
            }
    }

    // --- PROFILE DATA LOADING ---

    private fun retrieveAndStoreStudentData(uid: String) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val studentProfileData = StudentData(
                        collegeName = document.getString("collegeName") ?: "",
                        department = document.getString("department") ?: "",
                        email = document.getString("email") ?: "",
                        name = document.getString("name") ?: "",
                        role = document.getString("role") ?: "",
                        studentId = document.getString("studentId") ?: "",
                        uid = document.getString("uid") ?: "",
                        // Ensure arrays are retrieved too if needed later, but not necessary for just display
                    )
                    AppCache.setStudentProfile(studentProfileData)
                    loadStudentDataToUI(studentProfileData)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching student data: ${e.message}")
            }
    }

    private fun loadStudentDataToUI(data: StudentData) {
        binding.tvUserGreet.text = "Hello, ${data.name}"
        binding.tvUserCollage.text = data.collegeName
        binding.tvUserSubjectName.text = data.email
        binding.tvUserDepartment.text = data.department
        binding.tvUserSubjectCode.text = data.studentId

        fetchJoinedClassUids()
    }

    // --- QR SCANNER FUNCTIONS ---

    private fun checkCameraPermissionAndLaunchScanner() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                // Permission already granted, launch scanner
                launchQrScanner()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.CAMERA) -> {
                // Show explanation and request permission
                Toast.makeText(requireContext(), "Camera permission is needed to scan QR codes for attendance", Toast.LENGTH_LONG).show()
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            else -> {
                // Request permission directly
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun launchQrScanner() {
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        options.setPrompt("Scan the attendance QR code")
        options.setCameraId(0)  // Use rear camera
        options.setBeepEnabled(true)
        options.setBarcodeImageEnabled(false)
        options.setOrientationLocked(false)

        qrScannerLauncher.launch(options)
    }

    private fun getCurrentClassId(callback: (String?) -> Unit) {
        val studentUid = auth.currentUser?.uid
        if (studentUid.isNullOrEmpty()) {
            callback(null)
            return
        }

        db.collection("users").document(studentUid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    @Suppress("UNCHECKED_CAST")
                    val joinedClassUids = document.get("joinedClasses") as? List<String> ?: emptyList()
                    if (joinedClassUids.isNotEmpty()) {
                        // Show class selection dialog
                        showClassSelectionDialog(joinedClassUids, callback)
                    } else {
                        Toast.makeText(requireContext(), "You haven't joined any classes yet.", Toast.LENGTH_LONG).show()
                        callback(null)
                    }
                } else {
                    callback(null)
                }
            }
            .addOnFailureListener {
                Log.e(TAG, "Error fetching student classes: $it")
                Toast.makeText(requireContext(), "Error loading classes. Please try again.", Toast.LENGTH_LONG).show()
                callback(null)
            }
    }

    private fun showClassSelectionDialog(classIds: List<String>, callback: (String?) -> Unit) {
        // Get class details for display names
        db.collection("classes")
            .whereIn("classId", classIds.take(10)) // Firestore limit
            .get()
            .addOnSuccessListener { querySnapshot ->
                val classMap = mutableMapOf<String, String>() // classId -> displayName

                // --- SUCCESS LISTENER: CRITICAL CHECK (Already Correctly Implemented) ---
                if (!isAdded) {
                    Log.w(TAG, "Fragment is detached, skipping context-dependent operation.")
                    return@addOnSuccessListener
                }
                val safeContext = requireContext()
                // ---------------------------------------------------------------------

                querySnapshot.documents.forEach { doc ->
                    val classId = doc.id
                    val className = doc.getString("className") ?: "Unknown Class"
                    val classroom = doc.getString("classroom") ?: ""
                    val displayName = if (classroom.isNotEmpty()) "$className ($classroom)" else className
                    classMap[classId] = displayName
                }

                // Create dialog with class options
                val classNames = classIds.map { classMap[it] ?: "Unknown Class ($it)" }.toTypedArray()

                AlertDialog.Builder(safeContext) // Use safeContext (which is requireContext())
                    .setTitle("Select Class for Attendance")
                    .setItems(classNames) { _, which ->
                        val selectedClassId = classIds[which]
                        callback(selectedClassId)
                    }
                    .setNegativeButton("Cancel") { dialog, _ ->
                        dialog.dismiss()
                        callback(null)
                    }
                    .setCancelable(true)
                    .show()
            }
            .addOnFailureListener { exception ->
                // --- FAILURE LISTENER: CRITICAL CHECK (New/Modified) ---
                if (!isAdded) {
                    Log.w(TAG, "Fragment detached during class details fetch failure. Skipping UI.")
                    return@addOnFailureListener
                }
                val safeContext = requireContext()
                // -------------------------------------------------------

                Log.e(TAG, "Error fetching class details: $exception")

                // Show error Toast
                Toast.makeText(safeContext, "Error: ${exception.message}", Toast.LENGTH_LONG).show()

                // Fallback: show class IDs directly
                val classNames = classIds.map { "Class ID: $it" }.toTypedArray()

                // Show fallback dialog
                AlertDialog.Builder(safeContext)
                    .setTitle("Select Class for Attendance (Fallback)")
                    .setItems(classNames) { _, which ->
                        val selectedClassId = classIds[which]
                        callback(selectedClassId)
                    }
                    .setNegativeButton("Cancel") { dialog, _ ->
                        dialog.dismiss()
                        callback(null)
                    }
                    .setCancelable(true)
                    .show()
            }
    }
    // --- ATTENDANCE MARKING LOGIC ---

    private fun markAttendance(qrCodeToken: String, studentUid: String, classId: String) {
        Log.d(TAG, "Marking attendance for student: $studentUid, class: $classId, token: $qrCodeToken")

        // Step 1: Fetch the active attendance session for this class
        db.collection("ActiveAttendanceSessions")
            .document(classId)
            .get()
            .addOnSuccessListener { sessionDoc ->
                if (!sessionDoc.exists()) {
                    Toast.makeText(requireContext(), "No active attendance session found for this class", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                val sessionData = sessionDoc.data
                val storedQrToken = sessionData?.get("qrCodeToken") as? String
                val expiryTime = sessionData?.get("expiryTime") as? com.google.firebase.Timestamp

                // Step 2: Validate the QR token and expiry time
                if (storedQrToken != qrCodeToken) {
                    Toast.makeText(requireContext(), "Invalid QR code. Please scan the correct code.", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                if (expiryTime == null || Timestamp.now().toDate().after(expiryTime.toDate())) {
                    Toast.makeText(requireContext(), "QR code has expired. Please ask teacher to refresh.", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                // Step 3: Find the current attendance record (where sessionEnd is null)
                findCurrentAttendanceRecord(classId) { recordId ->
                    if (recordId == null) {
                        Toast.makeText(requireContext(), "No active attendance session found.", Toast.LENGTH_LONG).show()
                        return@findCurrentAttendanceRecord
                    }

                    // Step 4: Perform transaction to mark attendance
                    performAttendanceTransaction(recordId, studentUid)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching active session: $e")
                Toast.makeText(requireContext(), "Error connecting to server. Please try again.", Toast.LENGTH_LONG).show()
            }
    }

    private fun findCurrentAttendanceRecord(classId: String, callback: (String?) -> Unit) {
        // Query AttendanceRecords where classId matches and sessionEnd is null
        db.collection("AttendanceRecords")
            .whereEqualTo("classId", classId)
            .whereEqualTo("sessionEnd", null)
            .limit(1) // Should only be one active session at a time
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val recordDoc = querySnapshot.documents.first()
                    callback(recordDoc.id)
                } else {
                    callback(null)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error finding current attendance record: $e")
                callback(null)
            }
    }

    private fun performAttendanceTransaction(recordId: String, studentUid: String) {
        val recordRef = db.collection("AttendanceRecords").document(recordId)

        db.runTransaction { transaction ->
            // Get the current document
            val snapshot = transaction.get(recordRef)
            if (!snapshot.exists()) {
                throw Exception("Attendance record not found")
            }

            // Check if student already marked attendance
            val attendedStudents = snapshot.get("attendedStudents") as? Map<String, Timestamp> ?: emptyMap()
            if (attendedStudents.containsKey(studentUid)) {
                throw Exception("Attendance already marked")
            }

            // Add student to attendedStudents map
            transaction.update(recordRef, "attendedStudents.$studentUid", Timestamp.now())
        }
        .addOnSuccessListener {
            Log.d(TAG, "Attendance marked successfully for student: $studentUid")
            Toast.makeText(requireContext(), "Attendance Marked Successfully!", Toast.LENGTH_LONG).show()
        }
        .addOnFailureListener { e ->
            Log.e(TAG, "Error marking attendance: $e")
            when (e.message) {
                "Attendance already marked" -> {
                    Toast.makeText(requireContext(), "You have already marked your attendance for this session.", Toast.LENGTH_LONG).show()
                }
                "Attendance record not found" -> {
                    Toast.makeText(requireContext(), "Attendance session has ended or is no longer available.", Toast.LENGTH_LONG).show()
                }
                else -> {
                    Toast.makeText(requireContext(), "Failed to mark attendance. Please try again.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }

    companion object {
        fun newInstance(): homeFragmentNavStudent {
            return homeFragmentNavStudent()
        }
    }
}