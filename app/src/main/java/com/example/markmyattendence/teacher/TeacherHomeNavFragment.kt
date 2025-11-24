package com.example.markmyattendence.teacher

import com.example.markmyattendence.R
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
// ... (existing imports)
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.markmyattendence.ChatUI.ChatUiActivity
import com.example.markmyattendence.data.ClassModel
import com.example.markmyattendence.databinding.FragmentTeacherHomeNavBinding
import com.example.markmyattendence.notificationUI.NotificationActivity
import com.example.markmyattendence.teacher.Adatper.ClassAdapter
import com.example.markmyattendence.teacher.Adatper.OnClassItemClickListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// ... (Class definition and existing members) ...
class TeacherHomeNavFragment : Fragment(), OnClassItemClickListener {

    private val TAG = "TeacherHomeNavFragment"

    // **View Binding Setup**
    private var _binding: FragmentTeacherHomeNavBinding? = null
    private val binding get() = _binding!!

    private lateinit var firestoreDB: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var classAdapter: ClassAdapter

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

        val defaultBackground = ContextCompat.getDrawable(requireContext(), R.drawable.shape_default_attendance)
        val pressedBackground = ContextCompat.getDrawable(requireContext(), R.drawable.shape_pressed_attendance)

        val button = binding.llActivateAttendance
        binding.llActivateAttendance.setOnClickListener {
            button.background = pressedBackground
            button.postDelayed({
                button.background = defaultBackground
            }, 300)

        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // **Initialize Firebase services**
        firestoreDB = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Setup RecyclerView and Adapter
        classAdapter = ClassAdapter(emptyList(), this)

        binding.rvClasses.layoutManager = LinearLayoutManager(context)
        binding.rvClasses.adapter = classAdapter

        binding.ivNotification.setOnClickListener {
            // 1. Define the Intent
            val intent = Intent(requireActivity(), NotificationActivity::class.java)

            // 2. Launch the Activity
            startActivity(intent)
            requireActivity().overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
        binding.ivChatUi.setOnClickListener {
            val intent = Intent(requireActivity(), ChatUiActivity::class.java)
            startActivity(intent)
            requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        loadTeacherClasses()


    }


    fun loadTeacherClasses() {
        // ... (existing function content) ...
        val currentTeacherUid = auth.currentUser?.uid

        if (currentTeacherUid == null) {
            if (_binding != null) {
                binding.tvNoClasses.visibility = View.VISIBLE
                binding.rvClasses.visibility = View.GONE
            }
            return
        }

        firestoreDB.collection("classes")
            .whereEqualTo("teacherUid", currentTeacherUid)
            .get()
            .addOnSuccessListener { result ->
                if (_binding != null) { // CRASH FIX: Ensure view is still active
                    val classList = result.documents.mapNotNull {
                        it.toObject(ClassModel::class.java)?.apply { classId = it.id }
                    }

                    if (classList.isEmpty()) {
                        binding.tvNoClasses.visibility = View.VISIBLE
                        binding.rvClasses.visibility = View.GONE
                    } else {
                        binding.tvNoClasses.visibility = View.GONE
                        binding.rvClasses.visibility = View.VISIBLE
                        classAdapter.updateList(classList)
                    }
                }
            }
            .addOnFailureListener { exception ->
                if (_binding != null) { // CRASH FIX: Ensure view is still active
                    Log.e(TAG, "Error getting documents: ", exception)
                    binding.tvNoClasses.text = "Error loading classes. Try again."
                    binding.tvNoClasses.visibility = View.VISIBLE
                    binding.rvClasses.visibility = View.GONE
                }
            }
    }

    // --- 3. IMPLEMENT THE INTERFACE METHOD (DELETION LOGIC) ---

    override fun onDeleteClicked(classId: String) {
        showDeleteConfirmationDialog(classId)
    }

    private fun showDeleteConfirmationDialog(classId: String) {
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

        // 💡 Pass the necessary data to the new activity
        intent.putExtra("CLASS_ID", classItem.classId)
        intent.putExtra("CLASS_NAME", classItem.className)

        // You can pass the whole object if ClassModel is Parcelable or Serializable
        // intent.putExtra("CLASS_MODEL", classItem)

        startActivity(intent)

        // Apply the transition animation (slide in from right)
        requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }


    private fun deleteClassFromFirestore(classId: String) {
        // Show progress indicator if you have one
        // binding.progressBar.visibility = View.VISIBLE

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
            .addOnCompleteListener {
                // Hide progress indicator
                // binding.progressBar.visibility = View.GONE
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
        // IMPORTANT: Set binding to null to avoid memory leaks and NullPointerExceptions
        _binding = null
    }
}