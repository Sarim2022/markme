package com.example.markmyattendence.student

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import android.widget.ImageView
import com.google.android.material.textfield.TextInputEditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.markmyattendence.R
import com.example.markmyattendence.data.AppCache
import androidx.core.widget.TextViewCompat
import com.example.markmyattendence.StartUI.SignupActivity
import com.example.markmyattendence.data.ClassModel
import com.example.markmyattendence.data.StudentData
import com.example.markmyattendence.databinding.FragmentHomeNavStudentBinding
import com.example.markmyattendence.student.Adapter.StudentClassAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

class homeFragmentNavStudent : Fragment() {
    private var _binding: FragmentHomeNavStudentBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var adapter: StudentClassAdapter // Added adapter
    private val TAG = "StudentNavFragment"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        // Load student profile data
        if (AppCache.studentProfile != null) {
            loadStudentDataToUI(AppCache.studentProfile!!)
        } else {
            auth.currentUser?.uid?.let {
                retrieveAndStoreStudentData(it)
            }
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
            // TODO: Implement attendance logic here
        }

        binding.tvJoinNut.setOnClickListener {
            showJoinClassDialog()
        }
    }

    // --- RECYCLERVIEW AND DATA FETCHING LOGIC ---

    private fun setupRecyclerView() {
        // Initialize the adapter with an empty list and a click listener
        adapter = StudentClassAdapter(emptyList()) { classModel ->

            val intent = Intent(context,ClassDetailsActivity::class.java)
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