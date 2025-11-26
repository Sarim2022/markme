package com.example.markmyattendence.teacher

import com.example.markmyattendence.R
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.markmyattendence.ChatUI.ChatUiActivity
import com.example.markmyattendence.data.ClassModel
import com.example.markmyattendence.databinding.FragmentTeacherHomeNavBinding
import com.example.markmyattendence.notificationUI.NotificationActivity
import com.example.markmyattendence.teacher.Adatper.ClassAdapter
import com.example.markmyattendence.teacher.Adatper.OnClassItemClickListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class TeacherHomeNavFragment : Fragment(), OnClassItemClickListener {

    private val TAG = "TeacherHomeNavFragment"

    private var _binding: FragmentTeacherHomeNavBinding? = null
    private val binding get() = _binding!!

    private lateinit var firestoreDB: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var classAdapter: ClassAdapter

    // Removed: private val db = FirebaseFirestore.getInstance() // Redundant variable

    companion object {
        fun newInstance() = TeacherHomeNavFragment()
    }

    override fun onResume() {
        super.onResume()
        val activity = activity
        if (activity is TeacherHomeActivity) {
            activity.fetchUserInfo(isInitialLoad = false)
        }
        loadTeacherClasses()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTeacherHomeNavBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firestoreDB = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        classAdapter = ClassAdapter(emptyList(), this)
        binding.rvClasses.layoutManager = LinearLayoutManager(context)
        binding.rvClasses.adapter = classAdapter

        setupListeners()

        // 💡 Load classes immediately after setup
        loadTeacherClasses()
    }

    // Extracted listeners into a separate function for clean code
    private fun setupListeners() {
        val defaultBackground = ContextCompat.getDrawable(requireContext(), R.drawable.shape_default_attendance)
        val pressedBackground = ContextCompat.getDrawable(requireContext(), R.drawable.shape_pressed_attendance)

        binding.llActivateAttendance.setOnClickListener {
            // Apply visual press effect
            it.background = pressedBackground
            it.postDelayed({
                it.background = defaultBackground
            }, 300)
            // TODO: Add actual attendance activation logic here
        }

        binding.ivNotification.setOnClickListener {
            val intent = Intent(requireActivity(), NotificationActivity::class.java)
            startActivity(intent)
            requireActivity().overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        binding.ivChatUi.setOnClickListener {
            val intent = Intent(requireActivity(), ChatUiActivity::class.java)
            startActivity(intent)
            requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }


    /**
     * ❌ REMOVED: The checkTeacherClasses() function is removed.
     * The responsibility for checking if classes exist now belongs solely to loadTeacherClasses().
     */


    fun loadTeacherClasses() {


        val currentTeacherUid = auth.currentUser?.uid

        if (currentTeacherUid == null) {
            updateClassUi(emptyList()) // Treat as empty if no user ID
            return
        }

        // 💡 Use the standard way: Query the 'classes' collection based on teacherUid
        firestoreDB.collection("classes")
            .whereEqualTo("teacherUid", currentTeacherUid)
            .get()
            .addOnSuccessListener { result ->
                if (_binding != null) { // CRASH FIX: Ensure view is still active
                    val classList = result.documents.mapNotNull {
                        // Map Firestore document to ClassModel, and attach the document ID as classId
                        it.toObject(ClassModel::class.java)?.apply { classId = it.id }
                    }

                    updateClassUi(classList)
                }
            }
            .addOnFailureListener { exception ->
                if (_binding != null) {
                    Log.e(TAG, "Error getting documents: ", exception)
                    // Display error message
                    binding.tvNoClasses.text = "Error loading classes. Try again."
                    updateClassUi(emptyList())
                }
            }
        // .addOnCompleteListener { binding.progressBar.visibility = View.GONE } // Hide loading
    }

    /**
     * 💡 New helper function to centralize UI updates for class list visibility.
     * @param classList The list of classes fetched from Firestore.
     */
    private fun updateClassUi(classList: List<ClassModel>) {
        if (classList.isEmpty()) {
            binding.tvNoClasses.text = "No classes created yet. Tap '+' to start one!" // Default message
            binding.tvNoClasses.visibility = View.VISIBLE
            binding.rvClasses.visibility = View.GONE
        } else {
            binding.tvNoClasses.visibility = View.GONE
            binding.rvClasses.visibility = View.VISIBLE
            classAdapter.updateList(classList)
        }
    }


    // --- IMPLEMENT THE INTERFACE METHODS (DELETION & CLICK LOGIC) ---

    override fun onDeleteClicked(classId: String) {
        showDeleteConfirmationDialog(classId)
    }

    private fun showDeleteConfirmationDialog(classId: String) {
        // ... (Dialog logic remains the same) ...
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Class")
            .setMessage("Are you sure you want to delete this class? This action cannot be undone.")
            .setPositiveButton("Delete") { dialog, _ ->
                deleteClassFromFirestore(classId)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onClassClicked(classItem: ClassModel) {
        val intent = Intent(requireActivity(), ClassDetailsActivity::class.java)

        intent.putExtra("CLASS_ID", classItem.classId)
        intent.putExtra("CLASS_NAME", classItem.className)

        intent.putExtra("CLASS_ROOM", classItem.classroom) // Assuming ClassModel has a 'classroom' field
        intent.putExtra("START_TIME", classItem.startTime)
        intent.putExtra("END_TIME", classItem.endTime)
        intent.putExtra("CLASS_CODE", classItem.classCodeUid)
        intent.putExtra("AUTO_APPROVE", classItem.autoApprove) // Boolean
        intent.putExtra("MAX_STUDENTS", classItem.maxStudents)
        intent.putExtra("START_DATE", classItem.startDate)

        intent.putStringArrayListExtra("REPEAT_DAYS", ArrayList(classItem.repeatDays))


        startActivity(intent)
        requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }


    private fun deleteClassFromFirestore(classId: String) {
        // ... (Deletion logic remains the same) ...
        firestoreDB.collection("classes").document(classId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(context, "Class deleted successfully!", Toast.LENGTH_SHORT).show()
                // Refresh the list immediately after successful deletion
                loadTeacherClasses()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error deleting document: $classId", e)
                Toast.makeText(context, "Error deleting class: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }


    fun updateGreeting(
        userName: String?,
        collageName: String?,
        department: String?,
        subjectName: String?,
        subjectCode: String?
    ) {
        if (_binding != null) {
            binding.tvUserGreet.text = "Hello, ${userName ?: "Teacher"} 👋"
            binding.tvUserDepartment.text = department ?: "N/A"
            binding.tvUserSubjectCode.text = subjectCode ?: "N/A"
            binding.tvUserSubjectName.text = subjectName ?: "N/A"
            binding.tvUserCollage.text = collageName ?: "N/A"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}